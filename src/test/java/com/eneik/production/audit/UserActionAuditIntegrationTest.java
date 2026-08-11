package com.eneik.production.audit;

import com.eneik.generated.Application;
import com.eneik.generated.controller.IntegrationsController;
import com.eneik.generated.controller.FinancialDocumentController;
import com.eneik.generated.controller.DocumentController;
import com.eneik.generated.model.Document;
import com.eneik.generated.util.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class)
@Transactional
public class UserActionAuditIntegrationTest {

    @Autowired
    private IntegrationsController integrationsController;

    @Autowired
    private FinancialDocumentController financialDocumentController;

    @Autowired
    private DocumentController documentController;

    @Autowired
    private UserActionAuditRepository auditRepository;

    @Autowired
    private TimeProvider timeProvider;

    @BeforeEach
    public void setup() {
        auditRepository.deleteAll();
    }

    @Test
    public void testAuditSyncRoles() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer INTERNAL_SERVICE_KEY");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        IntegrationsController.EiosRoleSyncRequest syncReq = new IntegrationsController.EiosRoleSyncRequest();
        UUID userId = UUID.randomUUID();
        syncReq.setUserId(userId);
        syncReq.setRoles(List.of("Student"));

        ResponseEntity<?> response = integrationsController.syncEiosRoles(request, syncReq);
        assertEquals(200, response.getStatusCodeValue());

        List<UserActionAudit> audits = auditRepository.findAll();
        assertFalse(audits.isEmpty());
        UserActionAudit audit = audits.get(0);
        assertEquals("INTERNAL_SERVICE", audit.getUserIdentity());
        assertEquals("ACCESS_RIGHTS_MODIFICATION", audit.getActionType());
        assertTrue(audit.getDetails().contains(userId.toString()));
        assertNotNull(audit.getCreatedAt());
    }

    @Test
    public void testAuditUploadDocument() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("X-User-Role", "Administrator"); // Set role directly
        request.setParameter("title", "New Document Title");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy content".getBytes());

        ResponseEntity<?> response = documentController.uploadDocument(
                request, file, "New Document Title", "Desc", "Project", "infinite", "both", "other", "doc-123", null
        );
        assertEquals(201, response.getStatusCodeValue());

        List<UserActionAudit> audits = auditRepository.findAll();
        assertFalse(audits.isEmpty(), "Audit log for upload document should be created");
        UserActionAudit audit = audits.get(0);
        assertEquals("Administrator", audit.getUserIdentity());
        assertEquals("METADATA_MODIFICATION", audit.getActionType());
        assertTrue(audit.getDetails().contains("New Document Title"));
    }
}
