package com.eneik.generated;

import com.eneik.generated.model.AcademicYear;
import com.eneik.generated.model.Document;
import com.eneik.generated.model.DocumentStatus;
import com.eneik.generated.model.DocumentType;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentStrictTypeTest {

    @Test
    public void testFieldsAreStrictlyTyped() throws NoSuchFieldException {
        // Retrieve fields from Document class
        Field docTypeField = Document.class.getDeclaredField("documentType");
        Field statusField = Document.class.getDeclaredField("status");
        Field academicYearField = Document.class.getDeclaredField("academicYear");

        // Verify precise class types
        assertEquals(DocumentType.class, docTypeField.getType(), "field 'documentType' must be strictly typed as DocumentType enum");
        assertEquals(DocumentStatus.class, statusField.getType(), "field 'status' must be strictly typed as DocumentStatus enum");
        assertEquals(AcademicYear.class, academicYearField.getType(), "field 'academicYear' must be strictly typed as AcademicYear value object");

        // Verify proper JPA annotations
        assertTrue(docTypeField.isAnnotationPresent(Enumerated.class), "field 'documentType' must be annotated with @Enumerated");
        assertEquals(EnumType.STRING, docTypeField.getAnnotation(Enumerated.class).value(), "@Enumerated must have value = EnumType.STRING for documentType");

        assertTrue(statusField.isAnnotationPresent(Enumerated.class), "field 'status' must be annotated with @Enumerated");
        assertEquals(EnumType.STRING, statusField.getAnnotation(Enumerated.class).value(), "@Enumerated must have value = EnumType.STRING for status");

        assertTrue(academicYearField.isAnnotationPresent(Embedded.class), "field 'academicYear' must be annotated with @Embedded");
    }

    @Test
    public void testDocumentTypeConversionAndDefaults() {
        Document doc = new Document();

        // Test default
        assertEquals("Other", doc.getDocumentType(), "Default documentType should be 'Other'");

        // Test set correct
        doc.setDocumentType("Position");
        assertEquals("Position", doc.getDocumentType());

        // Test case insensitivity fallback
        doc.setDocumentType("procedure");
        assertEquals("Procedure", doc.getDocumentType());

        // Test invalid value falls back to default
        doc.setDocumentType("InvalidType");
        assertEquals("Other", doc.getDocumentType());

        // Test null falls back to default
        doc.setDocumentType(null);
        assertEquals("Other", doc.getDocumentType());
    }

    @Test
    public void testDocumentStatusConversionAndDefaults() {
        Document doc = new Document();

        // Test default
        assertEquals("PROJECT", doc.getStatus(), "Default status should be 'PROJECT'");

        // Test set correct
        doc.setStatus("ACTIVE");
        assertEquals("ACTIVE", doc.getStatus());

        // Test case insensitivity fallback
        doc.setStatus("archived");
        assertEquals("ARCHIVED", doc.getStatus());

        // Test invalid value falls back to default
        doc.setStatus("DRAFT");
        assertEquals("PROJECT", doc.getStatus());

        // Test null falls back to default
        doc.setStatus(null);
        assertEquals("PROJECT", doc.getStatus());
    }

    @Test
    public void testAcademicYearValidation() {
        AcademicYear ay1 = new AcademicYear("2026–2027");
        assertEquals("2026–2027", ay1.getValue());

        AcademicYear ayNull = new AcademicYear(null);
        assertEquals("infinite", ayNull.getValue());

        AcademicYear ayEmpty = new AcademicYear("   ");
        assertEquals("infinite", ayEmpty.getValue());
    }
}
