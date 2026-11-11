package com.banco.hr.infrastructure.adapters.inbound.rest.employee;

import com.banco.hr.domain.exception.DepartmentNameNotFoundException;
import com.banco.hr.domain.exception.DepartmentNotFoundException;
import com.banco.hr.domain.exception.DomainException;
import com.banco.hr.domain.exception.EmployeeAlreadyExistsException;
import com.banco.hr.domain.exception.EmployeeNotFoundException;
import com.banco.hr.domain.exception.PositionNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(assignableTypes = EmployeeController.class)
public class EmployeeExceptionHandler {

    @ExceptionHandler({
            EmployeeNotFoundException.class,
            DepartmentNotFoundException.class,
            PositionNotFoundException.class,
            DepartmentNameNotFoundException.class
    })
    public ResponseEntity<ProblemDetail> handleNotFound(
            DomainException exception,
            HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Recurso não encontrado", exception, request);
    }

    @ExceptionHandler(EmployeeAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleEmployeeAlreadyExists(
            EmployeeAlreadyExistsException exception,
            HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Colaborador já cadastrado", exception, request);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomainException(
            DomainException exception,
            HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Solicitação inválida", exception, request);
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String title,
            DomainException exception,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(status).body(problem);
    }
}
