package com.eneik.generated;

import com.eneik.generated.model.Task;
import com.eneik.generated.repository.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class GitHubWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        taskRepository.deleteAll();
    }

    @Test
    public void testClosedUnmergedPrTransitionsTaskToFailed() throws Exception {
        // Given a task associated with GitHub PR 4001, currently in "in_progress" status
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Task 4001", "in_progress", 4001, "open", false);
        taskRepository.saveAndFlush(task);

        // When a PR webhook payload arrives indicating a 'closed' action with 'merged: false'
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "closed");
        payload.put("number", 4001);

        Map<String, Object> prDetails = new HashMap<>();
        prDetails.put("number", 4001);
        prDetails.put("state", "closed");
        prDetails.put("merged", false);
        payload.put("pull_request", prDetails);

        mockMvc.perform(post("/api/v1/integrations/github/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.updatedTasksCount", is(1)));

        // Then the corresponding task status is updated to "failed"
        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("failed", reloaded.getStatus());
        assertEquals("closed", reloaded.getGithubPrState());
        assertEquals(false, reloaded.getGithubPrMerged());
    }

    @Test
    public void testClosedUnmergedPrForDoneTaskTransitionsToFailed() throws Exception {
        // Given an existing task status that is already set to done
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Task 4002", "done", 4002, "open", false);
        taskRepository.saveAndFlush(task);

        // When a late PR closed event arrives with 'merged: false'
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "closed");
        payload.put("number", 4002);

        Map<String, Object> prDetails = new HashMap<>();
        prDetails.put("number", 4002);
        prDetails.put("state", "closed");
        prDetails.put("merged", false);
        payload.put("pull_request", prDetails);

        mockMvc.perform(post("/api/v1/integrations/github/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.updatedTasksCount", is(1)));

        // Then the system must safely transition it to failed
        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("failed", reloaded.getStatus());
    }

    @Test
    public void testLatePrClosedEventForAlreadyFailedTaskIsSafelyIgnored() throws Exception {
        // Given an existing task status that is already set to failed
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Task 4003", "failed", 4003, "closed", false);
        taskRepository.saveAndFlush(task);

        // When a late PR closed event arrives with 'merged: false'
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "closed");
        payload.put("number", 4003);

        Map<String, Object> prDetails = new HashMap<>();
        prDetails.put("number", 4003);
        prDetails.put("state", "closed");
        prDetails.put("merged", false);
        payload.put("pull_request", prDetails);

        // The endpoint should process it successfully without doing redundant changes
        mockMvc.perform(post("/api/v1/integrations/github/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.updatedTasksCount", is(0)));

        // The status remains failed
        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("failed", reloaded.getStatus());
    }

    @Test
    public void testClosedMergedPrTransitionsTaskToDone() throws Exception {
        // Given a task associated with GitHub PR 4004, currently in "open" status
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Task 4004", "open", 4004, "open", false);
        taskRepository.saveAndFlush(task);

        // When a PR webhook payload arrives indicating a 'closed' action with 'merged: true'
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "closed");
        payload.put("number", 4004);

        Map<String, Object> prDetails = new HashMap<>();
        prDetails.put("number", 4004);
        prDetails.put("state", "closed");
        prDetails.put("merged", true);
        payload.put("pull_request", prDetails);

        mockMvc.perform(post("/api/v1/integrations/github/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.updatedTasksCount", is(1)));

        // Then the corresponding task status is updated to "done"
        Task reloaded = taskRepository.findById(taskId).orElseThrow();
        assertEquals("done", reloaded.getStatus());
        assertEquals("closed", reloaded.getGithubPrState());
        assertEquals(true, reloaded.getGithubPrMerged());
    }
}
