package com.banco.hr.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.banco.hr.application.ports.inbound.dto.CreatePositionCommand;
import com.banco.hr.application.ports.outbound.PositionRepositoryPort;
import com.banco.hr.domain.exception.DomainException;
import com.banco.hr.domain.exception.PositionAlreadyExistsException;
import com.banco.hr.domain.model.position.Position;
import com.banco.hr.domain.model.position.PositionLevel;
import com.banco.hr.domain.model.position.PositionTitle;

@ExtendWith(MockitoExtension.class)
class CreatePositionUseCaseImplTest {

    private static final Instant NOW = Instant.parse("2026-11-11T12:00:00Z");

    @Mock
    private PositionRepositoryPort positionRepository;

    private CreatePositionUseCaseImpl useCase;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        // Preparação: relógio fixo torna determinística a data criada pelo caso de uso.
        useCase = new CreatePositionUseCaseImpl(
                positionRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Deve salvar e retornar cargo com ID gerado e data atual")
    void createPositionSavesAndReturnsPositionWithGeneratedIdAndTimestamp() {
        // Preparação: simula uma persistência bem-sucedida.
        CreatePositionCommand command = new CreatePositionCommand("Software Engineer", "Senior");
        when(positionRepository.existsByTitleAndLevel(
                new PositionTitle("Software Engineer"), new PositionLevel("Senior")))
                .thenReturn(false);
        when(positionRepository.save(any(Position.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Execução: cria cargo por meio do caso de uso.
        Position result = useCase.createPosition(command);

        // Verificação: confere os dados gerados e que a gravação foi realizada.
        assertNotNull(result.id().value());
        assertEquals("Software Engineer", result.title().value());
        assertEquals("Senior", result.level().value());
        assertEquals(NOW, result.createdAt());
        verify(positionRepository).existsByTitleAndLevel(
                new PositionTitle("Software Engineer"), new PositionLevel("Senior"));
        verify(positionRepository).save(result);
    }

    @Test
    @DisplayName("Deve normalizar título e nível antes de consultar e salvar")
    void createPositionTrimsValuesBeforeSaving() {
        // Preparação: envia caixa divergente e espaços repetidos para validar a normalização.
        CreatePositionCommand command =
                new CreatePositionCommand(" engenheiro  de software ", " jUNIOR ");
        when(positionRepository.existsByTitleAndLevel(
                new PositionTitle("Engenheiro de Software"), new PositionLevel("Junior")))
                .thenReturn(false);
        when(positionRepository.save(any(Position.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Execução: processa os valores informados no comando.
        Position result = useCase.createPosition(command);

        // Verificação: a gravação recebe valores normalizados.
        assertEquals("Engenheiro de Software", result.title().value());
        assertEquals("Junior", result.level().value());
        verify(positionRepository).existsByTitleAndLevel(
                new PositionTitle("Engenheiro de Software"), new PositionLevel("Junior"));
        verify(positionRepository).save(result);
    }

    @Test
    @DisplayName("Deve rejeitar cargo com mesmo título e nível sem tentar salvar")
    void createPositionRejectsDuplicateTitleAndLevelWithoutSaving() {
        // Preparação: informa que a combinação de título e nível já existe.
        CreatePositionCommand command =
                new CreatePositionCommand("Software Engineer", "Senior");
        when(positionRepository.existsByTitleAndLevel(
                new PositionTitle("Software Engineer"), new PositionLevel("Senior")))
                .thenReturn(true);

        // Verificação: a duplicidade deve gerar exceção de domínio e impedir a gravação.
        assertThrows(PositionAlreadyExistsException.class,
                () -> useCase.createPosition(command));

        verify(positionRepository).existsByTitleAndLevel(
                new PositionTitle("Software Engineer"), new PositionLevel("Senior"));
        verify(positionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar título inválido antes de acessar o repositório")
    void createPositionRejectsInvalidTitleBeforeAccessingRepository() {
        // Preparação: título em branco viola a invariável do value object PositionTitle.
        CreatePositionCommand command = new CreatePositionCommand("  ", "Senior");

        // Verificação: a regra deve falhar antes de qualquer gravação.
        DomainException exception = assertThrows(
                DomainException.class, () -> useCase.createPosition(command));
        assertNotNull(exception);

        verify(positionRepository, never()).existsByTitleAndLevel(any(), any());
        verify(positionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar nível inválido antes de acessar o repositório")
    void createPositionRejectsInvalidLevelBeforeAccessingRepository() {
        // Preparação: nível em branco viola a invariável do value object PositionLevel.
        CreatePositionCommand command = new CreatePositionCommand("Developer", "  ");

        // Verificação: a regra deve falhar antes de qualquer gravação.
        DomainException exception = assertThrows(
                DomainException.class, () -> useCase.createPosition(command));
        assertNotNull(exception);

        verify(positionRepository, never()).existsByTitleAndLevel(any(), any());
        verify(positionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar comando nulo sem acessar o repositório")
    void createPositionRejectsNullCommand() {
        // Verificação: comando ausente não deve iniciar operações de persistência.
        NullPointerException exception = assertThrows(
                NullPointerException.class, () -> useCase.createPosition(null));
        assertNotNull(exception);

        verify(positionRepository, never()).existsByTitleAndLevel(any(), any());
        verify(positionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve propagar falha ocorrida durante a persistência")
    void createPositionPropagatesRepositoryFailure() {
        // Preparação: repositório falha ao salvar o cargo.
        CreatePositionCommand command = new CreatePositionCommand("Developer", "Junior");
        IllegalStateException failure = new IllegalStateException("Repository failure");
        when(positionRepository.existsByTitleAndLevel(
                new PositionTitle("Developer"), new PositionLevel("Junior")))
                .thenReturn(false);
        when(positionRepository.save(any(Position.class))).thenThrow(failure);

        // Verificação: falhas de persistência não devem ser ocultadas pelo caso de uso.
        IllegalStateException result = assertThrows(
                IllegalStateException.class, () -> useCase.createPosition(command));

        assertEquals(failure, result);
    }
}
