package com.banco.hr.infrastructure.adapters.inbound.rest.position;

import com.banco.hr.application.ports.inbound.CreatePositionCommand;
import com.banco.hr.application.ports.inbound.CreatePositionUseCase;
import com.banco.hr.domain.model.position.Position;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/positions")
@Tag(name = "Cargos", description = "Operações de gestão de cargos.")
public class PositionController {

    private final CreatePositionUseCase createPositionUseCase;

    public PositionController(CreatePositionUseCase createPositionUseCase) {
        this.createPositionUseCase = createPositionUseCase;
    }

    @PostMapping
    @Operation(
            summary = "Criar cargo",
            description = "Cadastra um cargo com título e nível.")
    @ApiResponse(responseCode = "201", description = "Cargo criado com sucesso.")
    @ApiResponse(responseCode = "400", description = "Os dados informados são inválidos.")
    public ResponseEntity<PositionResponse> create(
            @Valid @RequestBody CreatePositionRequest request) {
        Position position = createPositionUseCase.createPosition(
                new CreatePositionCommand(request.title(), request.level()));
        PositionResponse response = PositionResponse.fromPosition(position);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }
}
