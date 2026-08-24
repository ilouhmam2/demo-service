package fr.francetv.demo.application.usecase;

import fr.francetv.demo.domain.model.Item;
import fr.francetv.demo.domain.port.in.SyncExternalItemsUseCase;
import fr.francetv.demo.domain.port.out.ExternalItemPort;
import fr.francetv.demo.domain.port.out.ItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SyncExternalItemsService implements SyncExternalItemsUseCase {

    private static final Logger log = LoggerFactory.getLogger(SyncExternalItemsService.class);

    private final ExternalItemPort externalItemPort;
    private final ItemRepository itemRepository;

    public SyncExternalItemsService(ExternalItemPort externalItemPort, ItemRepository itemRepository) {
        this.externalItemPort = externalItemPort;
        this.itemRepository = itemRepository;
    }

    @Override
    public int sync() {
        List<Item> items = externalItemPort.fetchAll();
        items.forEach(item -> {
            Item saved = itemRepository.save(item);
            log.info("external.item.synced id={} name={}", saved.getId(), saved.getName());
        });
        return items.size();
    }
}
