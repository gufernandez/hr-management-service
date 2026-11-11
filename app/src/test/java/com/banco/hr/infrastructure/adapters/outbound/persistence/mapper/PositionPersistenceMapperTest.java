package com.banco.hr.infrastructure.adapters.outbound.persistence.mapper;

import com.banco.hr.domain.model.position.Position;
import com.banco.hr.domain.model.position.PositionId;
import com.banco.hr.domain.model.position.PositionLevel;
import com.banco.hr.domain.model.position.PositionTitle;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.PositionJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PositionPersistenceMapperTest {

    private static final UUID POSITION_ID =
            UUID.fromString("75cbb9f4-96c9-4c58-b849-0b0dfb54be0d");
    private static final Instant CREATED_AT = Instant.parse("2026-11-11T12:00:00Z");

    private final PositionPersistenceMapper mapper = new PositionPersistenceMapper();

    @Test
    @DisplayName("Deve mapear todos os campos do domínio para a entidade JPA")
    void toEntityCopiesAllPositionFields() {
        // Preparação: cria agregado com valores conhecidos para validar o mapeamento campo a campo.
        Position position = Position.reconstitute(
                new PositionId(POSITION_ID),
                new PositionTitle("Software Engineer"),
                new PositionLevel("Senior"),
                CREATED_AT);

        // Execução: converte o agregado para o formato persistido.
        PositionJpaEntity entity = mapper.toEntity(position);

        // Verificação: os valores persistidos devem corresponder aos dados do domínio.
        assertEquals(POSITION_ID, entity.getId());
        assertEquals("Software Engineer", entity.getTitle());
        assertEquals("Senior", entity.getLevel());
        assertEquals(CREATED_AT, entity.getCreatedAt());
    }

    @Test
    @DisplayName("Deve reconstruir o agregado a partir de todos os campos da entidade JPA")
    void toDomainReconstitutesPositionWithAllEntityFields() {
        // Preparação: cria entidade com valores conhecidos como se viessem do banco.
        PositionJpaEntity entity = new PositionJpaEntity(
                POSITION_ID, "Software Engineer", "Senior", CREATED_AT);

        // Execução: reconstrói o modelo de domínio a partir da entidade.
        Position position = mapper.toDomain(entity);

        // Verificação: nenhum campo deve ser perdido na conversão para o domínio.
        assertEquals(new PositionId(POSITION_ID), position.id());
        assertEquals(new PositionTitle("Software Engineer"), position.title());
        assertEquals(new PositionLevel("Senior"), position.level());
        assertEquals(CREATED_AT, position.createdAt());
    }
}
