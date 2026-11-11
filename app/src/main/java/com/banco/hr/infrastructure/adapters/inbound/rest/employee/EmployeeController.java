package com.banco.hr.infrastructure.adapters.inbound.rest.employee;

import com.banco.hr.application.ports.inbound.ActivateEmployeeUseCase;
import com.banco.hr.application.ports.inbound.CreateEmployeeCommand;
import com.banco.hr.application.ports.inbound.CreateEmployeeUseCase;
import com.banco.hr.application.ports.inbound.DeactivateEmployeeUseCase;
import com.banco.hr.application.ports.inbound.DeleteEmployeeUseCase;
import com.banco.hr.application.ports.inbound.EmployeePageRequest;
import com.banco.hr.application.ports.inbound.ListEmployeesUseCase;
import com.banco.hr.application.ports.inbound.SearchEmployeesByDepartmentUseCase;
import com.banco.hr.domain.exception.DomainException;
import com.banco.hr.domain.model.employee.Employee;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
@Tag(name = "Colaboradores", description = "Operações de gestão de colaboradores.")
public class EmployeeController {

    private final CreateEmployeeUseCase createEmployeeUseCase;
    private final ListEmployeesUseCase listEmployeesUseCase;
    private final DeleteEmployeeUseCase deleteEmployeeUseCase;
    private final SearchEmployeesByDepartmentUseCase searchEmployeesByDepartmentUseCase;
    private final ActivateEmployeeUseCase activateEmployeeUseCase;
    private final DeactivateEmployeeUseCase deactivateEmployeeUseCase;

    public EmployeeController(
            CreateEmployeeUseCase createEmployeeUseCase,
            ListEmployeesUseCase listEmployeesUseCase,
            DeleteEmployeeUseCase deleteEmployeeUseCase,
            SearchEmployeesByDepartmentUseCase searchEmployeesByDepartmentUseCase,
            ActivateEmployeeUseCase activateEmployeeUseCase,
            DeactivateEmployeeUseCase deactivateEmployeeUseCase) {
        this.createEmployeeUseCase = createEmployeeUseCase;
        this.listEmployeesUseCase = listEmployeesUseCase;
        this.deleteEmployeeUseCase = deleteEmployeeUseCase;
        this.searchEmployeesByDepartmentUseCase = searchEmployeesByDepartmentUseCase;
        this.activateEmployeeUseCase = activateEmployeeUseCase;
        this.deactivateEmployeeUseCase = deactivateEmployeeUseCase;
    }

    @PostMapping
    @Operation(summary = "Cadastrar colaborador",
            description = "Cadastra um colaborador vinculado a um departamento e cargo existentes.")
    @ApiResponse(responseCode = "201", description = "Colaborador cadastrado com sucesso.")
    @ApiResponse(responseCode = "400", description = "Os dados informados são inválidos.")
    @ApiResponse(responseCode = "404", description = "Departamento ou cargo não encontrado.")
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado.")
    public ResponseEntity<EmployeeResponse> create(
            @Valid @RequestBody CreateEmployeeRequest request) {
        Employee employee = createEmployeeUseCase.createEmployee(new CreateEmployeeCommand(
                request.name(),
                request.email(),
                request.departmentId(),
                request.positionId()));
        EmployeeResponse response = EmployeeResponse.fromEmployee(employee);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar colaboradores",
            description = "Lista colaboradores não excluídos, com paginação opcional.")
    @ApiResponse(responseCode = "200", description = "Colaboradores listados com sucesso.")
    @ApiResponse(responseCode = "400", description = "Os parâmetros de paginação são inválidos.")
    public ResponseEntity<?> list(
            @RequestParam(name = "offset", required = false) Long offset,
            @RequestParam(name = "limit", required = false) Integer limit) {
        if (offset == null && limit == null) {
            List<EmployeeResponse> employees = listEmployeesUseCase.listEmployees().stream()
                    .map(EmployeeResponse::fromEmployee)
                    .toList();
            return ResponseEntity.ok(employees);
        }
        if (offset == null || limit == null) {
            throw new DomainException("Offset e limit devem ser informados juntos.");
        }
        return ResponseEntity.ok(EmployeePageResponse.fromPage(
                listEmployeesUseCase.listEmployees(new EmployeePageRequest(offset, limit))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir colaborador",
            description = "Altera o status do colaborador para DELETED.")
    @ApiResponse(responseCode = "204", description = "Colaborador excluído com sucesso.")
    @ApiResponse(responseCode = "404", description = "Colaborador não encontrado.")
    @ApiResponse(responseCode = "400", description = "A exclusão não é válida para o estado atual.")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteEmployeeUseCase.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar colaboradores por departamento",
            description = "Busca colaboradores não excluídos pelo nome do departamento.")
    @ApiResponse(responseCode = "200", description = "Busca concluída com sucesso.")
    @ApiResponse(responseCode = "400", description = "Os parâmetros de paginação são inválidos.")
    @ApiResponse(responseCode = "404", description = "Departamento não encontrado.")
    public ResponseEntity<EmployeePageResponse> searchByDepartment(
            @RequestParam("department") String departmentName,
            @RequestParam(name = "offset") long offset,
            @RequestParam(name = "limit") int limit) {
        EmployeePageRequest pageRequest = new EmployeePageRequest(offset, limit);
        return ResponseEntity.ok(EmployeePageResponse.fromPage(
                searchEmployeesByDepartmentUseCase.searchByDepartment(
                        departmentName, pageRequest)));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Ativar cadastro do colaborador")
    @ApiResponse(responseCode = "200", description = "Colaborador ativado com sucesso.")
    @ApiResponse(responseCode = "400", description = "A ativação não é válida para o estado atual.")
    @ApiResponse(responseCode = "404", description = "Colaborador não encontrado.")
    public ResponseEntity<EmployeeResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(EmployeeResponse.fromEmployee(
                activateEmployeeUseCase.activateEmployee(id)));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Desativar cadastro do colaborador")
    @ApiResponse(responseCode = "200", description = "Colaborador desativado com sucesso.")
    @ApiResponse(responseCode = "400", description = "A desativação não é válida para o estado atual.")
    @ApiResponse(responseCode = "404", description = "Colaborador não encontrado.")
    public ResponseEntity<EmployeeResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(EmployeeResponse.fromEmployee(
                deactivateEmployeeUseCase.deactivateEmployee(id)));
    }
}
