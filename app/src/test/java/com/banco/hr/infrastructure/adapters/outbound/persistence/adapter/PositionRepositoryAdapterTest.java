package com.banco.hr.infrastructure.adapters.outbound.persistence.adapter;

import com.banco.hr.domain.exception.PositionAlreadyExistsException;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    @DisplayName("Deve consultar a existência pelo título e nível")
    void existsByTitleAndLevelDelegatesBothValuesToJpaRepository() {
        // Preparação: simula a existência da combinação informada na persistência.
        when(positionJpaRepository.existsByTitleAndLevel("Software Engineer", "Senior"))
                .thenReturn(true);

        // Execução: consulta a combinação por meio da porta de repositório.
        boolean exists = positionRepositoryAdapter.existsByTitleAndLevel(
                new PositionTitle("Software Engineer"), new PositionLevel("Senior"));

        // Verificação: o resultado e ambos os critérios devem ser preservados.
        assertTrue(exists);
        verify(positionJpaRepository).existsByTitleAndLevel("Software Engineer", "Senior");
    }

    @Test
    @DisplayName("Deve verificar existência do cargo pelo UUID")
    void existsByIdDelegatesIdToJpaRepository() {
        // Preparação: simula cargo existente para validar o UUID usado no cadastro.
        when(positionJpaRepository.existsById(POSITION_ID)).thenReturn(true);

        // Execução: consulta a referência pela porta de lookup.
        boolean exists = positionRepositoryAdapter.existsById(new PositionId(POSITION_ID));

        // Verificação: o identificador é encaminhado ao repositório JPA.
        assertTrue(exists);
        verify(positionJpaRepository).existsById(POSITION_ID);
    }

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

    @Test
    @DisplayName("Deve traduzir violação da combinação única para exceção de cargo existente")
    void saveTranslatesDuplicateTitleAndLevelConstraintViolation() {
        // Preparação: simula violação da constraint por outra gravação concorrente.
        Position position = Position.reconstitute(
                new PositionId(POSITION_ID),
                new PositionTitle("Software Engineer"),
                new PositionLevel("Senior"),
                CREATED_AT);
        PositionJpaEntity entity =
                new PositionJpaEntity(POSITION_ID, "Software Engineer", "Senior", CREATED_AT);
        when(positionPersistenceMapper.toEntity(position)).thenReturn(entity);
        when(positionJpaRepository.save(entity))
                .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));
        when(positionJpaRepository.existsByTitleAndLevel("Software Engineer", "Senior"))
                .thenReturn(true);

        // Verificação: a duplicidade deve usar a mesma exceção de domínio do caso usual.
        assertThrows(PositionAlreadyExistsException.class,
                () -> positionRepositoryAdapter.save(position));
        verify(positionJpaRepository).existsByTitleAndLevel("Software Engineer", "Senior");
    }
}
