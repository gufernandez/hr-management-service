package com.banco.hr.domain.exception;

public class InvalidEmployeeEventException extends RuntimeException {

    public InvalidEmployeeEventException(String message) {
        super(message);
    }

    public InvalidEmployeeEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
