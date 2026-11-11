package com.banco.hr.domain.model.position;

import com.banco.hr.domain.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PositionTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-11T12:00:00Z");

    @Test
    @DisplayName("Deve criar cargo com identificador e dados informados")
    void createGeneratesIdAndStoresPositionData() {
        // Preparação: informa os valores que devem compor o novo cargo.
        Position position = Position.create(
                new PositionTitle("Software Engineer"),
                new PositionLevel("Senior"),
                CREATED_AT);

        // Verificação: o agregado gera um ID e preserva os dados recebidos.
        assertNotNull(position.id());
        assertNotNull(position.id().value());
        assertEquals(new PositionTitle("Software Engineer"), position.title());
        assertEquals(new PositionLevel("Senior"), position.level());
        assertEquals(CREATED_AT, position.createdAt());
    }

    @Test
    @DisplayName("Deve rejeitar identificador de cargo nulo")
    void positionIdRejectsNullValue() {
        // Verificação: um ID sem UUID é inválido e deve ser rejeitado pelo value object.
        assertNotNull(assertThrows(NullPointerException.class, () -> new PositionId(null)));
    }

    @Test
    @DisplayName("Deve normalizar o título para title case e compactar espaços")
    void positionTitleTrimsSurroundingWhitespace() {
        // Verificação: caixa e espaços devem ser normalizados conforme o formato persistido.
        assertEquals(
                "Engenheiro de Software",
                new PositionTitle("  engenheiro  de software  ").value());
    }

    @Test
    @DisplayName("Deve rejeitar título nulo ou composto apenas por espaços")
    void positionTitleRejectsNullOrBlankValue() {
        // Verificação: o título é obrigatório e deve conter algum valor.
        assertThrows(DomainException.class, () -> new PositionTitle(null));
        assertThrows(DomainException.class, () -> new PositionTitle("  "));
    }

    @Test
    @DisplayName("Deve normalizar o nível para title case e compactar espaços")
    void positionLevelTrimsSurroundingWhitespace() {
        // Verificação: caixa e espaços devem ser normalizados no nível.
        assertEquals("Junior", new PositionLevel("  jUNIOR  ").value());
    }

    @Test
    @DisplayName("Deve rejeitar nível nulo ou composto apenas por espaços")
    void positionLevelRejectsNullOrBlankValue() {
        // Verificação: o nível é obrigatório e deve conter algum valor.
        assertThrows(DomainException.class, () -> new PositionLevel(null));
        assertThrows(DomainException.class, () -> new PositionLevel("  "));
    }

    @Test
    @DisplayName("Deve gerar identificadores distintos para cargos")
    void generatedPositionIdsAreUnique() {
        // Preparação: gera dois identificadores independentes para comparar.
        UUID firstId = PositionId.generate().value();
        UUID secondId = PositionId.generate().value();

        // Verificação: ambos identificam cargos diferentes.
        assertNotNull(firstId);
        assertNotNull(secondId);
        org.junit.jupiter.api.Assertions.assertNotEquals(firstId, secondId);
    }
}
