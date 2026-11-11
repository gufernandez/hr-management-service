package com.banco.hr.domain.model.department;

import com.banco.hr.domain.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DepartmentTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-11T12:00:00Z");

    @Test
    @DisplayName("Deve criar departamento com identificador e dados informados")
    void createGeneratesIdAndStoresDepartmentData() {
        // Preparação: informa os valores que devem compor o novo departamento.
        Department department = Department.create(
                new DepartmentName("People"),
                new CostCenterCode("CC-10"),
                CREATED_AT);

        // Verificação: o agregado deve gerar um ID e preservar os dados recebidos.
        assertNotNull(department.id());
        assertNotNull(department.id().value());
        assertEquals(new DepartmentName("People"), department.name());
        assertEquals(new CostCenterCode("CC-10"), department.costCenterCode());
        assertEquals(CREATED_AT, department.createdAt());
    }

    @Test
    @DisplayName("Deve rejeitar identificador de departamento nulo")
    void departmentIdRejectsNullValue() {
        // Verificação: um ID sem UUID é inválido e deve ser rejeitado pelo value object.
        assertNotNull(assertThrows(NullPointerException.class, () -> new DepartmentId(null)));
    }

    @Test
    @DisplayName("Deve remover espaços no início e no fim do nome")
    void departmentNameTrimsSurroundingWhitespace() {
        // Verificação: nomes normalizados permitem comparações consistentes no domínio.
        assertEquals("People", new DepartmentName("  People  ").value());
    }

    @Test
    @DisplayName("Deve rejeitar nome nulo ou composto apenas por espaços")
    void departmentNameRejectsNullOrBlankValue() {
        // Verificação: nome é obrigatório e não pode ficar vazio após a normalização.
        assertThrows(DomainException.class, () -> new DepartmentName(null));
        assertThrows(DomainException.class, () -> new DepartmentName("  "));
    }

    @Test
    @DisplayName("Deve remover espaços no início e no fim do código do centro de custo")
    void costCenterCodeTrimsSurroundingWhitespace() {
        // Verificação: o código deve ser armazenado sem espaços excedentes.
        assertEquals("CC-10", new CostCenterCode("  CC-10  ").value());
    }

    @Test
    @DisplayName("Deve rejeitar código de centro de custo nulo ou composto apenas por espaços")
    void costCenterCodeRejectsNullOrBlankValue() {
        // Verificação: o código do centro de custo é obrigatório e deve conter algum valor.
        assertThrows(DomainException.class, () -> new CostCenterCode(null));
        assertThrows(DomainException.class, () -> new CostCenterCode("  "));
    }

    @Test
    @DisplayName("Deve gerar identificadores distintos para departamentos")
    void generatedDepartmentIdsAreUnique() {
        // Preparação: gera dois identificadores independentes para comparar.
        assertNotNull(DepartmentId.generate().value());
        UUID firstId = DepartmentId.generate().value();
        UUID secondId = DepartmentId.generate().value();

        // Verificação: ambos devem existir e não podem identificar o mesmo departamento.
        assertNotNull(firstId);
        assertNotNull(secondId);
        org.junit.jupiter.api.Assertions.assertNotEquals(firstId, secondId);
    }
}
