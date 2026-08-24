package fr.francetv.demo.infrastructure.adapter.in.web;

import fr.francetv.demo.domain.exception.ExternalServiceException;
import fr.francetv.demo.domain.exception.ItemNotFoundException;
import fr.francetv.demo.domain.model.Item;
import fr.francetv.demo.domain.port.in.CreateItemUseCase;
import fr.francetv.demo.domain.port.in.FindItemUseCase;
import fr.francetv.demo.domain.port.in.GetItemSoapUseCase;
import fr.francetv.demo.domain.port.in.SyncExternalItemsUseCase;
import fr.francetv.demo.infrastructure.adapter.in.web.dto.CreateItemRequest;
import fr.francetv.demo.infrastructure.adapter.in.web.dto.ItemResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final CreateItemUseCase createItemUseCase;
    private final FindItemUseCase findItemUseCase;
    private final SyncExternalItemsUseCase syncExternalItemsUseCase;
    private final GetItemSoapUseCase getItemSoapUseCase;
    private final ItemMapper itemMapper;

    public ItemController(CreateItemUseCase createItemUseCase, FindItemUseCase findItemUseCase,
                          SyncExternalItemsUseCase syncExternalItemsUseCase,
                          GetItemSoapUseCase getItemSoapUseCase, ItemMapper itemMapper) {
        this.createItemUseCase = createItemUseCase;
        this.findItemUseCase = findItemUseCase;
        this.syncExternalItemsUseCase = syncExternalItemsUseCase;
        this.getItemSoapUseCase = getItemSoapUseCase;
        this.itemMapper = itemMapper;
    }

    @PostMapping
    public ResponseEntity<ItemResponse> create(
            @Valid @RequestBody CreateItemRequest request,
            UriComponentsBuilder uriBuilder) {
        Item item = createItemUseCase.create(request.name(), request.description());
        URI location = uriBuilder.path("/api/items/{id}").buildAndExpand(item.getId()).toUri();
        return ResponseEntity.created(location).body(itemMapper.toResponse(item));
    }

    @GetMapping("/{id}")
    public ItemResponse findById(@PathVariable Long id) {
        try {
            Item item = findItemUseCase.findById(id);
            return itemMapper.toResponse(item);
        } catch (ItemNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    @GetMapping
    public List<ItemResponse> findAll() {
        return findItemUseCase.findAll().stream()
                .map(itemMapper::toResponse)
                .toList();
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Integer>> sync() {
        try {
            int count = syncExternalItemsUseCase.sync();
            return ResponseEntity.ok(Map.of("synced", count));
        } catch (ExternalServiceException ex) {
            throw mapExternalServiceException(ex);
        }
    }

    @GetMapping("/{id}/soap-check")
    public ItemResponse soapCheck(@PathVariable Long id) {
        try {
            Item item = getItemSoapUseCase.getItemViaSoap(id);
            return itemMapper.toResponse(item);
        } catch (ExternalServiceException ex) {
            throw mapExternalServiceException(ex);
        }
    }

    private ResponseStatusException mapExternalServiceException(ExternalServiceException ex) {
        HttpStatus status = switch (ex.getKind()) {
            case BAD_GATEWAY -> HttpStatus.BAD_GATEWAY;
            case SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        return new ResponseStatusException(status, ex.getMessage(), ex);
    }
}
