package fr.francetv.demo.domain.model;

import java.time.Instant;

public class Item {

    private Long id;
    private String name;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;

    public Item(Long id, String name, String description, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
