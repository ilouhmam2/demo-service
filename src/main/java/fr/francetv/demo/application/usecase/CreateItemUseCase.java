package fr.francetv.demo.application.usecase;

import fr.francetv.demo.domain.exception.ItemNotFoundException;
import fr.francetv.demo.domain.model.Item;
import fr.francetv.demo.domain.port.in.FindItemUseCase;
import fr.francetv.demo.domain.port.out.ItemEventPublisher;
import fr.francetv.demo.domain.port.out.ItemRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CreateItemUseCase
        implements fr.francetv.demo.domain.port.in.CreateItemUseCase, FindItemUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateItemUseCase.class);

    private final ItemRepository itemRepository;
    private final ItemEventPublisher itemEventPublisher;
    private final Counter itemsCreatedCounter;

    public CreateItemUseCase(ItemRepository itemRepository,
                             ItemEventPublisher itemEventPublisher,
                             MeterRegistry meterRegistry) {
        this.itemRepository = itemRepository;
        this.itemEventPublisher = itemEventPublisher;
        this.itemsCreatedCounter = Counter.builder("items.created")
                .description("Number of items created")
                .register(meterRegistry);
    }

    @Override
    public Item create(String name, String description) {
        Item item = new Item(null, name, description, null, null);
        Item saved = itemRepository.save(item);
        itemsCreatedCounter.increment();
        log.info("item.created id={} name={}", saved.getId(), saved.getName());
        itemEventPublisher.publishItemCreated(saved);
        return saved;
    }

    @Override
    public Item findById(Long id) {
        return itemRepository.findById(id)
            .orElseThrow(() -> new ItemNotFoundException(id));
    }

    @Override
    public List<Item> findAll() {
        return itemRepository.findAll();
    }
}
