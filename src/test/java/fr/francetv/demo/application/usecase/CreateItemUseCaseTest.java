package fr.francetv.demo.application.usecase;

import fr.francetv.demo.domain.exception.ItemNotFoundException;
import fr.francetv.demo.domain.model.Item;
import fr.francetv.demo.domain.port.out.ItemEventPublisher;
import fr.francetv.demo.domain.port.out.ItemRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateItemUseCaseTest {

    @Mock
    ItemRepository itemRepository;

    @Mock
    ItemEventPublisher itemEventPublisher;

    CreateItemUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateItemUseCase(itemRepository, itemEventPublisher, new SimpleMeterRegistry());
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_savesItemViaRepository() {
        Item saved = new Item(1L, "Widget", "A widget", Instant.now(), Instant.now());
        given(itemRepository.save(any())).willReturn(saved);

        Item result = useCase.create("Widget", "A widget");

        verify(itemRepository).save(any(Item.class));
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Widget");
    }

    @Test
    void create_publishesItemCreatedEvent() {
        Item saved = new Item(1L, "Widget", "A widget", Instant.now(), Instant.now());
        given(itemRepository.save(any())).willReturn(saved);

        useCase.create("Widget", "A widget");

        verify(itemEventPublisher).publishItemCreated(saved);
    }

    @Test
    void create_nullDescription_isAllowed() {
        Item saved = new Item(2L, "Widget", null, Instant.now(), Instant.now());
        given(itemRepository.save(any())).willReturn(saved);

        Item result = useCase.create("Widget", null);

        assertThat(result.getDescription()).isNull();
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_existingId_returnsItem() {
        Item item = new Item(1L, "Widget", "A widget", Instant.now(), Instant.now());
        given(itemRepository.findById(1L)).willReturn(Optional.of(item));

        Item result = useCase.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Widget");
    }

    @Test
    void findById_nonExistingId_throws404() {
        given(itemRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.findById(999L))
            .isInstanceOf(ItemNotFoundException.class)
            .hasMessageContaining("Item not found: 999");
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void findAll_delegatesToRepository() {
        Item item1 = new Item(1L, "Widget A", null, Instant.now(), Instant.now());
        Item item2 = new Item(2L, "Widget B", null, Instant.now(), Instant.now());
        given(itemRepository.findAll()).willReturn(List.of(item1, item2));

        List<Item> result = useCase.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Item::getName)
                .containsExactly("Widget A", "Widget B");
    }

    @Test
    void findAll_emptyRepository_returnsEmptyList() {
        given(itemRepository.findAll()).willReturn(List.of());

        assertThat(useCase.findAll()).isEmpty();
    }
}
