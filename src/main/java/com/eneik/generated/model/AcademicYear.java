package com.eneik.generated.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class AcademicYear {

    @Column(name = "academic_year", nullable = false)
    private String value;

    public AcademicYear() {
        this.value = "infinite";
    }

    public AcademicYear(String value) {
        if (value == null || value.trim().isEmpty()) {
            this.value = "infinite";
        } else {
            this.value = value.trim();
        }
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AcademicYear that = (AcademicYear) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
