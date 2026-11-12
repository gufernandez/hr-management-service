package com.banco.hr.application.ports.inbound.dto;

import com.banco.hr.domain.exception.DomainException;

public record PageQuery(long offset, int limit) {

    public PageQuery {
        if (offset < 0) {
            throw new DomainException("O offset da paginação não pode ser negativo.");
        }
        if (limit < 1) {
            throw new DomainException("O limite da paginação deve ser maior que zero.");
        }
    }
}
