package fr.francetv.demo.infrastructure.adapter.in.web.dto;

import java.time.Instant;

public record ItemResponse(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {}
