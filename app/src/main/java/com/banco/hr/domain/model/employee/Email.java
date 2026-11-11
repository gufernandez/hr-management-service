package com.banco.hr.domain.model.employee;

import com.banco.hr.domain.exception.DomainException;

import java.util.Locale;
import java.util.regex.Pattern;

public record Email(String value) {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public Email {
        if (value == null || value.isBlank()) {
            throw new DomainException("O e-mail do colaborador é obrigatório.");
        }

        value = value.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new DomainException("O e-mail do colaborador é inválido.");
        }
    }
}
