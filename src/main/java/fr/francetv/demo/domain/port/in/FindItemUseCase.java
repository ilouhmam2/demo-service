package fr.francetv.demo.domain.port.in;

import fr.francetv.demo.domain.model.Item;

import java.util.List;

public interface FindItemUseCase {

    Item findById(Long id);

    List<Item> findAll();
}
