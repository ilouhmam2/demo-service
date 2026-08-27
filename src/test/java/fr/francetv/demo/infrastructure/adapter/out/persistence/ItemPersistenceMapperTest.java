package fr.francetv.demo.infrastructure.adapter.out.persistence;

import fr.francetv.demo.domain.model.Item;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ItemPersistenceMapperTest {

    private final ItemPersistenceMapper mapper = Mappers.getMapper(ItemPersistenceMapper.class);

    // ── toDomain ─────────────────────────────────────────────────────────────

    @Test
    void toDomain_allFields_mappedCorrectly() {
        Instant now = Instant.now();
        ItemEntity entity = entityWith(1L, "Widget", "A widget", now, now);

        Item item = mapper.toDomain(entity);

        assertThat(item.getId()).isEqualTo(1L);
        assertThat(item.getName()).isEqualTo("Widget");
        assertThat(item.getDescription()).isEqualTo("A widget");
        assertThat(item.getCreatedAt()).isEqualTo(now);
        assertThat(item.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void toDomain_nullSource_returnsNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toDomain_nullDescription_mappedAsNull() {
        ItemEntity entity = entityWith(1L, "Widget", null, Instant.now(), Instant.now());

        Item item = mapper.toDomain(entity);

        assertThat(item.getDescription()).isNull();
    }

    // ── toEntity ─────────────────────────────────────────────────────────────

    @Test
    void toEntity_nameAndDescription_mappedCorrectly() {
        Item item = new Item(1L, "Widget", "A widget", Instant.now(), Instant.now());

        ItemEntity entity = mapper.toEntity(item);

        assertThat(entity.getName()).isEqualTo("Widget");
        assertThat(entity.getDescription()).isEqualTo("A widget");
    }

    @Test
    void toEntity_idAndTimestamps_areIgnored() {
        Instant now = Instant.now();
        Item item = new Item(99L, "Widget", "A widget", now, now);

        ItemEntity entity = mapper.toEntity(item);

        assertThat(entity.getId()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
    }

    @Test
    void toEntity_nullSource_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toEntity_nullDescription_mappedAsNull() {
        Item item = new Item(1L, "Widget", null, Instant.now(), Instant.now());

        ItemEntity entity = mapper.toEntity(item);

        assertThat(entity.getName()).isEqualTo("Widget");
        assertThat(entity.getDescription()).isNull();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ItemEntity entityWith(Long id, String name, String description, Instant createdAt, Instant updatedAt) {
        ItemEntity e = new ItemEntity();
        e.setName(name);
        e.setDescription(description);
        try {
            var idField = ItemEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(e, id);
            var createdField = ItemEntity.class.getDeclaredField("createdAt");
            createdField.setAccessible(true);
            createdField.set(e, createdAt);
            var updatedField = ItemEntity.class.getDeclaredField("updatedAt");
            updatedField.setAccessible(true);
            updatedField.set(e, updatedAt);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to set ItemEntity fields via reflection", ex);
        }
        return e;
    }
}
