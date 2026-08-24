package fr.francetv.demo.infrastructure.adapter.in.web;

import fr.francetv.demo.domain.model.Item;
import fr.francetv.demo.infrastructure.adapter.in.web.dto.CreateItemRequest;
import fr.francetv.demo.infrastructure.adapter.in.web.dto.ItemResponse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ItemMapperTest {

    private final ItemMapper mapper = Mappers.getMapper(ItemMapper.class);

    // ── toResponse ────────────────────────────────────────────────────────────

    @Test
    void toResponse_allFields_mappedCorrectly() {
        Instant now = Instant.now();
        Item item = new Item(1L, "Widget", "A widget", now, now);

        ItemResponse response = mapper.toResponse(item);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Widget");
        assertThat(response.description()).isEqualTo("A widget");
        assertThat(response.createdAt()).isEqualTo(now);
        assertThat(response.updatedAt()).isEqualTo(now);
    }

    @Test
    void toResponse_nullSource_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    void toResponse_nullDescription_mappedAsNull() {
        Item item = new Item(2L, "Widget", null, Instant.now(), Instant.now());

        ItemResponse response = mapper.toResponse(item);

        assertThat(response.description()).isNull();
    }

    // ── toItem ────────────────────────────────────────────────────────────────

    @Test
    void toItem_nameAndDescription_mappedCorrectly() {
        CreateItemRequest request = new CreateItemRequest("Widget", "A widget");

        Item item = mapper.toItem(request);

        assertThat(item.getName()).isEqualTo("Widget");
        assertThat(item.getDescription()).isEqualTo("A widget");
    }

    @Test
    void toItem_idAndTimestamps_areNull() {
        CreateItemRequest request = new CreateItemRequest("Widget", "A widget");

        Item item = mapper.toItem(request);

        assertThat(item.getId()).isNull();
        assertThat(item.getCreatedAt()).isNull();
        assertThat(item.getUpdatedAt()).isNull();
    }

    @Test
    void toItem_nullSource_returnsNull() {
        assertThat(mapper.toItem(null)).isNull();
    }

    @Test
    void toItem_nullDescription_mappedAsNull() {
        CreateItemRequest request = new CreateItemRequest("Widget", null);

        Item item = mapper.toItem(request);

        assertThat(item.getName()).isEqualTo("Widget");
        assertThat(item.getDescription()).isNull();
    }
}
