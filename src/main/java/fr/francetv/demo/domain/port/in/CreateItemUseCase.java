package fr.francetv.demo.domain.port.in;

import fr.francetv.demo.domain.model.Item;

public interface CreateItemUseCase {

    Item create(String name, String description);
}
