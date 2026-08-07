package com.eneik.generated.service;

import com.eneik.generated.model.Document;
import com.eneik.generated.model.Role;
import com.eneik.generated.model.SchemaTag;
import com.eneik.generated.repository.DocumentRepository;
import com.eneik.generated.repository.RoleRepository;
import com.eneik.generated.repository.SchemaTagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class FinancialDocumentService {

    private final RoleRepository roleRepository;
    private final SchemaTagRepository schemaTagRepository;
    private final DocumentRepository documentRepository;

    public FinancialDocumentService(RoleRepository roleRepository,
                                    SchemaTagRepository schemaTagRepository,
                                    DocumentRepository documentRepository) {
        this.roleRepository = roleRepository;
        this.schemaTagRepository = schemaTagRepository;
        this.documentRepository = documentRepository;
    }

    @Transactional(readOnly = true)
    public List<Document> getDocumentsForRoleAndTag(String roleName, String tagName) {
        // Find role
        Optional<Role> roleOpt = roleRepository.findByName(roleName);
        if (roleOpt.isEmpty()) {
            throw new SecurityException("Role not found: " + roleName);
        }
        Role role = roleOpt.get();

        // Check if role has access to requested tag
        boolean hasAccess = role.getSchemaTags().stream()
                .anyMatch(tag -> tag.getName().equalsIgnoreCase(tagName));

        if (!hasAccess) {
            throw new AccessDeniedException("User role '" + roleName + "' does not have access to '" + tagName + "' documents.");
        }

        // Find the tag to filter documents
        Optional<SchemaTag> tagOpt = schemaTagRepository.findByName(tagName);
        if (tagOpt.isEmpty()) {
            return Collections.emptyList();
        }

        return documentRepository.findBySchemaTagsIn(List.of(tagOpt.get().getId()));
    }

    @Transactional
    public Document createDocument(String roleName, Document doc) {
        verifyWriteAccess(roleName);
        if (doc.getId() == null) {
            doc.setId(UUID.randomUUID());
        }
        return documentRepository.save(doc);
    }

    @Transactional
    public Document updateDocument(String roleName, UUID id, Document updatedDoc) {
        verifyWriteAccess(roleName);
        Document existing = documentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Document not found with ID: " + id));

        existing.setTitle(updatedDoc.getTitle());
        existing.setDescription(updatedDoc.getDescription());
        existing.setDocumentType(updatedDoc.getDocumentType());
        existing.setAcademicYear(updatedDoc.getAcademicYear());
        existing.setStatus(updatedDoc.getStatus());
        existing.setProgram(updatedDoc.getProgram());
        existing.setProcess(updatedDoc.getProcess());
        existing.setApprovalDate(updatedDoc.getApprovalDate());
        existing.setDocumentNumber(updatedDoc.getDocumentNumber());
        existing.setResponsibleName(updatedDoc.getResponsibleName());
        existing.setResponsibleTitle(updatedDoc.getResponsibleTitle());
        existing.setResponsibleUnit(updatedDoc.getResponsibleUnit());
        existing.setUpdatedAt(java.time.LocalDateTime.now());

        return documentRepository.save(existing);
    }

    @Transactional
    public void deleteDocument(String roleName, UUID id) {
        verifyWriteAccess(roleName);
        if (!documentRepository.existsById(id)) {
            throw new NoSuchElementException("Document not found with ID: " + id);
        }
        documentRepository.deleteById(id);
    }

    private void verifyWriteAccess(String roleName) {
        if (!"Economist".equalsIgnoreCase(roleName) && !"HR".equalsIgnoreCase(roleName)) {
            throw new AccessDeniedException("User role '" + roleName + "' does not have permission to modify financial/HR documents.");
        }
    }

    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String message) {
            super(message);
        }
    }
}
