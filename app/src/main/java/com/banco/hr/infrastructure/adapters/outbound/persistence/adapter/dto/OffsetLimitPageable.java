package com.banco.hr.infrastructure.adapters.outbound.persistence.adapter.dto;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class OffsetLimitPageable implements Pageable {

    private final long offset;
    private final int limit;

    public OffsetLimitPageable(long offset, int limit) {
        if (offset < 0 || limit < 1) {
            throw new IllegalArgumentException("Os parâmetros de paginação são inválidos.");
        }
        this.offset = offset;
        this.limit = limit;
    }

    @Override
    public int getPageNumber() {
        return Math.toIntExact(offset / limit);
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return Sort.unsorted();
    }

    @Override
    public Pageable next() {
        return new OffsetLimitPageable(Math.addExact(offset, limit), limit);
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious()
                ? new OffsetLimitPageable(Math.max(0, offset - limit), limit)
                : first();
    }

    @Override
    public Pageable first() {
        return new OffsetLimitPageable(0, limit);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("O número da página não pode ser negativo.");
        }
        return new OffsetLimitPageable(Math.multiplyExact((long) pageNumber, limit), limit);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }
}
