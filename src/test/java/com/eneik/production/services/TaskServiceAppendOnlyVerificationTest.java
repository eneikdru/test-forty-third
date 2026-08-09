package com.eneik.production.services;

import com.eneik.generated.Application;
import com.eneik.generated.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class, properties = {"spring.main.allow-bean-definition-overriding=true"})
@ActiveProfiles("test")
public class TaskServiceAppendOnlyVerificationTest {

    @Autowired
    private TaskService taskService;

    @Test
    public void testTaskServicePatchIsUsedAsPrimaryBean() {
        // Verify that the active bean is the overriding subclass, proving append-only extension
        assertNotNull(taskService, "TaskService bean must be injected");
        assertTrue(taskService instanceof TaskServicePatch, "The active TaskService must be TaskServicePatch (append-only overriding component)");
    }

    @Test
    public void testGeneratedTaskServiceFileIsUntouched() throws IOException, InterruptedException {
        // Path to the generated file
        File generatedFile = new File("src/main/java/com/eneik/generated/service/TaskService.java");
        assertTrue(generatedFile.exists(), "Generated TaskService.java must exist");

        // 1. Content Analysis: Ensure patch methods do not reside in the generated TaskService class
        String generatedContent = Files.readString(generatedFile.toPath());
        assertFalse(generatedContent.contains("revertToUnmergedPrState"),
                "The generated file must not contain the patch method 'revertToUnmergedPrState'");
        assertFalse(generatedContent.contains("determineTargetStatus"),
                "The generated file must not contain the patch method 'determineTargetStatus'");

        // Path to the patch file
        File patchFile = new File("src/main/java/com/eneik/production/services/TaskServicePatch.java");
        assertTrue(patchFile.exists(), "Patch TaskServicePatch.java must exist");

        // Content Analysis: Ensure patch methods reside in the overriding TaskServicePatch class instead
        String patchContent = Files.readString(patchFile.toPath());
        assertTrue(patchContent.contains("revertToUnmergedPrState"),
                "The patch file must contain the overriding logic 'revertToUnmergedPrState'");
        assertTrue(patchContent.contains("determineTargetStatus"),
                "The patch file must contain the overriding logic 'determineTargetStatus'");

        // 2. Git Integrity Analysis: Verify using Git that the generated file has no uncommitted changes
        ProcessBuilder processBuilder = new ProcessBuilder(
                "git", "diff", "--exit-code", "src/main/java/com/eneik/generated/service/TaskService.java"
        );
        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        assertEquals(0, exitCode, "Generated TaskService.java has uncommitted git modifications! It must remain untouched.");
    }
}
