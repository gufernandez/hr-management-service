package com.banco.hr.domain.model.department;

import com.banco.hr.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DepartmentTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-11T12:00:00Z");

    @Test
    void createGeneratesIdAndStoresDepartmentData() {
        Department department = Department.create(
                new DepartmentName("People"),
                new CostCenterCode("CC-10"),
                CREATED_AT);

        assertNotNull(department.id());
        assertNotNull(department.id().value());
        assertEquals(new DepartmentName("People"), department.name());
        assertEquals(new CostCenterCode("CC-10"), department.costCenterCode());
        assertEquals(CREATED_AT, department.createdAt());
    }

    @Test
    void departmentIdRejectsNullValue() {
        assertThrows(NullPointerException.class, () -> new DepartmentId(null));
    }

    @Test
    void departmentNameTrimsSurroundingWhitespace() {
        assertEquals("People", new DepartmentName("  People  ").value());
    }

    @Test
    void departmentNameRejectsNullOrBlankValue() {
        assertThrows(DomainException.class, () -> new DepartmentName(null));
        assertThrows(DomainException.class, () -> new DepartmentName("  "));
    }

    @Test
    void costCenterCodeTrimsSurroundingWhitespace() {
        assertEquals("CC-10", new CostCenterCode("  CC-10  ").value());
    }

    @Test
    void costCenterCodeRejectsNullOrBlankValue() {
        assertThrows(DomainException.class, () -> new CostCenterCode(null));
        assertThrows(DomainException.class, () -> new CostCenterCode("  "));
    }

    @Test
    void generatedDepartmentIdsAreUnique() {
        assertNotNull(DepartmentId.generate().value());
        UUID firstId = DepartmentId.generate().value();
        UUID secondId = DepartmentId.generate().value();

        assertNotNull(firstId);
        assertNotNull(secondId);
        org.junit.jupiter.api.Assertions.assertNotEquals(firstId, secondId);
    }
}
