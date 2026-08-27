package fr.francetv.demo.domain.port.out;

import fr.francetv.demo.domain.model.Item;

import java.util.List;
import java.util.Optional;

public interface ItemRepository {

    Item save(Item item);

    Optional<Item> findById(Long id);

    List<Item> findAll();

    void deleteById(Long id);
}
