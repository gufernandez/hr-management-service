package com.banco.hr.application.ports.inbound;

import com.banco.hr.application.ports.inbound.dto.PageQuery;
import com.banco.hr.domain.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmployeePageRequestTest {

    @Test
    @DisplayName("Deve aceitar offset zero e limite positivo")
    void acceptsValidOffsetAndLimit() {
        // Verificação: a paginação válida preserva os valores solicitados.
        PageQuery request = new PageQuery(0, 25);
        assertEquals(0, request.offset());
        assertEquals(25, request.limit());
    }

    @Test
    @DisplayName("Deve rejeitar offset negativo ou limite não positivo")
    void rejectsInvalidOffsetAndLimit() {
        // Verificação: parâmetros inválidos não podem produzir paginação ambígua.
        assertThrows(DomainException.class, () -> new PageQuery(-1, 25));
        assertThrows(DomainException.class, () -> new PageQuery(0, 0));
        assertThrows(DomainException.class, () -> new PageQuery(0, -1));
    }
}
