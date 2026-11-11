package com.banco.hr.infrastructure.adapters.inbound.rest.position;

import com.banco.hr.domain.exception.PositionAlreadyExistsException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(assignableTypes = PositionController.class)
public class PositionExceptionHandler {

    @ExceptionHandler(PositionAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handlePositionAlreadyExists(
            PositionAlreadyExistsException exception,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Cargo já cadastrado");
        problem.setInstance(URI.create(request.getRequestURI()));

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }
}
