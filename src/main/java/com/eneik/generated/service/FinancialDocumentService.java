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

    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String message) {
            super(message);
        }
    }
}
