package com.eneik.production.audit;

import com.eneik.generated.util.TimeProvider;
import com.eneik.generated.util.IdProvider;
import com.eneik.generated.model.Document;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
@Component
public class UserActionAuditAspect {

    private static final Logger log = LoggerFactory.getLogger(UserActionAuditAspect.class);

    private final UserActionAuditRepository auditRepository;
    private final TimeProvider timeProvider;
    private final IdProvider idProvider;

    public UserActionAuditAspect(UserActionAuditRepository auditRepository, TimeProvider timeProvider, IdProvider idProvider) {
        this.auditRepository = auditRepository;
        this.timeProvider = timeProvider;
        this.idProvider = idProvider;
    }

    @Pointcut("execution(* com.eneik.generated.controller.IntegrationsController.syncEiosRoles(..))")
    public void syncRolesPointcut() {}

    @Pointcut("execution(* com.eneik.generated.controller.FinancialDocumentController.updateDocument(..))")
    public void updateFinancialDocPointcut() {}

    @Pointcut("execution(* com.eneik.generated.controller.DocumentController.uploadDocument(..))")
    public void uploadDocumentPointcut() {}

    @AfterReturning(pointcut = "syncRolesPointcut()")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void auditSyncRoles(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length >= 2 && args[1] instanceof com.eneik.generated.controller.IntegrationsController.EiosRoleSyncRequest) {
            try {
                com.eneik.generated.controller.IntegrationsController.EiosRoleSyncRequest payload =
                    (com.eneik.generated.controller.IntegrationsController.EiosRoleSyncRequest) args[1];

                String userIdentity = "SYSTEM_SYNC";
                HttpServletRequest request = getRequest();
                if (request != null) {
                    userIdentity = "INTERNAL_SERVICE";
                }

                saveAudit(userIdentity, "ACCESS_RIGHTS_MODIFICATION", "Synced roles for user: " + payload.getUserId());
            } catch (Exception e) {
                log.error("Failed to save audit log for syncEiosRoles", e);
                throw new RuntimeException("Failed to save audit log", e);
            }
        }
    }

    @AfterReturning(pointcut = "updateFinancialDocPointcut()")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void auditUpdateFinancialDoc(JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();

            HttpServletRequest request = null;
            String docId = "UNKNOWN";
            Document updatedDoc = null;

            for (Object arg : args) {
                if (arg instanceof HttpServletRequest) {
                    request = (HttpServletRequest) arg;
                } else if (arg instanceof UUID) {
                    docId = String.valueOf(arg);
                } else if (arg instanceof Document) {
                    updatedDoc = (Document) arg;
                }
            }

            if (request != null) {
                String roleName = extractRole(request);
                if (roleName == null) roleName = "UNKNOWN";

                String title = updatedDoc != null ? updatedDoc.getTitle() : "UNKNOWN";
                saveAudit(roleName, "METADATA_MODIFICATION", "Updated financial document metadata for ID: " + docId + " with title: " + title);
            }
        } catch (Exception e) {
            log.error("Failed to save audit log for updateFinancialDoc", e);
            throw new RuntimeException("Failed to save audit log", e);
        }
    }

    @AfterReturning(pointcut = "uploadDocumentPointcut()")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void auditUploadDocument(JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            HttpServletRequest request = null;
            String title = "UNKNOWN";

            if (args.length > 0 && args[0] instanceof HttpServletRequest) {
                request = (HttpServletRequest) args[0];
            }
            if (args.length > 2 && args[2] instanceof String) {
                title = String.valueOf(args[2]);
            }

            if (request != null) {
                String role = extractRole(request);
                if (role == null) role = "UNKNOWN";

                saveAudit(role, "METADATA_MODIFICATION", "Uploaded/Updated document metadata for title: " + title);
            }
        } catch (Exception e) {
            log.error("Failed to save audit log for uploadDocument", e);
            throw new RuntimeException("Failed to save audit log", e);
        }
    }

    private void saveAudit(String userIdentity, String actionType, String details) {
        UserActionAudit audit = new UserActionAudit(
                idProvider.generateUuid(),
                userIdentity,
                actionType,
                details,
                timeProvider.now()
        );
        auditRepository.saveAndFlush(audit);
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String extractRole(HttpServletRequest request) {
        Object validatedRole = request.getAttribute("X-User-Role");
        if (validatedRole != null) {
            return (String) validatedRole;
        }

        if (Boolean.TRUE.equals(request.getAttribute("X-Session-Invalid"))) {
            return null;
        }

        if (Boolean.TRUE.equals(request.getAttribute("X-Allow-Fallback"))) {
            String xUserRole = request.getHeader("X-User-Role");
            if (xUserRole != null && !xUserRole.trim().isEmpty()) {
                return xUserRole.trim();
            }

            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7).trim();
                if (!token.isEmpty()) {
                    return "FALLBACK_TOKEN_USER";
                }
            }
        }

        return "UNKNOWN_USER";
    }
}
