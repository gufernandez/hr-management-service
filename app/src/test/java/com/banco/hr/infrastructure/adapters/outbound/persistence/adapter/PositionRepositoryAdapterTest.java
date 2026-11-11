package com.banco.hr.infrastructure.adapters.outbound.persistence.adapter;

import com.banco.hr.domain.model.position.Position;
import com.banco.hr.domain.model.position.PositionId;
import com.banco.hr.domain.model.position.PositionLevel;
import com.banco.hr.domain.model.position.PositionTitle;
import com.banco.hr.infrastructure.adapters.outbound.persistence.entity.PositionJpaEntity;
import com.banco.hr.infrastructure.adapters.outbound.persistence.mapper.PositionPersistenceMapper;
import com.banco.hr.infrastructure.adapters.outbound.persistence.repository.PositionJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PositionRepositoryAdapterTest {

    private static final UUID POSITION_ID =
            UUID.fromString("75cbb9f4-96c9-4c58-b849-0b0dfb54be0d");
    private static final Instant CREATED_AT = Instant.parse("2026-11-11T12:00:00Z");

    @Mock
    private PositionJpaRepository positionJpaRepository;

    @Mock
    private PositionPersistenceMapper positionPersistenceMapper;

    @InjectMocks
    private PositionRepositoryAdapter positionRepositoryAdapter;

    @Test
    @DisplayName("Deve mapear, persistir e devolver o cargo salvo")
    void saveMapsAndPersistsEntityThenReturnsMappedPosition() {
        // Preparação: configura o fluxo esperado entre domínio, mapper e repositório JPA.
        Position position = Position.reconstitute(
                new PositionId(POSITION_ID),
                new PositionTitle("Software Engineer"),
                new PositionLevel("Senior"),
                CREATED_AT);
        PositionJpaEntity entity =
                new PositionJpaEntity(POSITION_ID, "Software Engineer", "Senior", CREATED_AT);
        PositionJpaEntity savedEntity =
                new PositionJpaEntity(POSITION_ID, "Software Engineer", "Senior", CREATED_AT);
        Position savedPosition = Position.reconstitute(
                new PositionId(POSITION_ID),
                new PositionTitle("Software Engineer"),
                new PositionLevel("Senior"),
                CREATED_AT);
        when(positionPersistenceMapper.toEntity(position)).thenReturn(entity);
        when(positionJpaRepository.save(entity)).thenReturn(savedEntity);
        when(positionPersistenceMapper.toDomain(savedEntity)).thenReturn(savedPosition);

        // Execução: salva o agregado pela porta de saída implementada pelo adaptador.
        Position result = positionRepositoryAdapter.save(position);

        // Verificação: o adaptador retorna o domínio reconstruído e executa as conversões.
        assertSame(savedPosition, result);
        verify(positionPersistenceMapper).toEntity(position);
        verify(positionJpaRepository).save(entity);
        verify(positionPersistenceMapper).toDomain(savedEntity);
    }

    @Test
    @DisplayName("Deve propagar falha do repositório JPA ao salvar cargo")
    void savePropagatesJpaRepositoryFailure() {
        // Preparação: configura a entidade mapeada e uma falha de persistência conhecida.
        Position position = Position.reconstitute(
                new PositionId(POSITION_ID),
                new PositionTitle("Software Engineer"),
                new PositionLevel("Senior"),
                CREATED_AT);
        PositionJpaEntity entity =
                new PositionJpaEntity(POSITION_ID, "Software Engineer", "Senior", CREATED_AT);
        IllegalStateException failure = new IllegalStateException("Database failure");
        when(positionPersistenceMapper.toEntity(position)).thenReturn(entity);
        when(positionJpaRepository.save(entity)).thenThrow(failure);

        // Verificação: a falha deve ser propagada, sem retorno de sucesso aparente.
        IllegalStateException result = assertThrows(
                IllegalStateException.class, () -> positionRepositoryAdapter.save(position));

        assertSame(failure, result);
    }
}
