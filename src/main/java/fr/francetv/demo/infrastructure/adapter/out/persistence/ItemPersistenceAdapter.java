package fr.francetv.demo.infrastructure.adapter.out.persistence;

import fr.francetv.demo.domain.model.Item;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ItemPersistenceAdapter implements fr.francetv.demo.domain.port.out.ItemRepository {

    private final ItemRepository jpaRepository;
    private final ItemPersistenceMapper persistenceMapper;

    public ItemPersistenceAdapter(ItemRepository jpaRepository, ItemPersistenceMapper persistenceMapper) {
        this.jpaRepository = jpaRepository;
        this.persistenceMapper = persistenceMapper;
    }

    @Override
    public Item save(Item item) {
        ItemEntity entity = persistenceMapper.toEntity(item);
        return persistenceMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Item> findById(Long id) {
        return jpaRepository.findById(id).map(persistenceMapper::toDomain);
    }

    @Override
    public List<Item> findAll() {
        return jpaRepository.findAll().stream()
                .map(persistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
