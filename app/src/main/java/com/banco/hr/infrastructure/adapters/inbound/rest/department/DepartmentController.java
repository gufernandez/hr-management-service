package com.banco.hr.infrastructure.adapters.inbound.rest.department;

import com.banco.hr.application.ports.inbound.CreateDepartmentUseCase;
import com.banco.hr.application.ports.inbound.dto.CreateDepartmentCommand;
import com.banco.hr.domain.model.department.Department;
import com.banco.hr.infrastructure.adapters.inbound.rest.department.dto.CreateDepartmentRequest;
import com.banco.hr.infrastructure.adapters.inbound.rest.department.dto.DepartmentResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/departments")
@Tag(name = "Departamentos", description = "Operações de gestão de departamentos.")
public class DepartmentController {

    private final CreateDepartmentUseCase createDepartmentUseCase;

    public DepartmentController(CreateDepartmentUseCase createDepartmentUseCase) {
        this.createDepartmentUseCase = createDepartmentUseCase;
    }

    @PostMapping
    @Operation(
            summary = "Criar departamento",
            description = "Cadastra um departamento com nome e código do centro de custo.")
    @ApiResponse(responseCode = "201", description = "Departamento criado com sucesso.")
    @ApiResponse(responseCode = "400", description = "Os dados informados são inválidos.")
    @ApiResponse(responseCode = "409", description = "Já existe um departamento com esse nome.")
    public ResponseEntity<DepartmentResponse> create(
            @Valid @RequestBody CreateDepartmentRequest request) {
        Department department = createDepartmentUseCase.createDepartment(
                new CreateDepartmentCommand(request.name(), request.costCenterCode()));
        DepartmentResponse response = DepartmentResponse.fromDepartment(department);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }
}
