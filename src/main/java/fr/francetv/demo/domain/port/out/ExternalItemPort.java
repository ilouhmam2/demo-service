package fr.francetv.demo.domain.port.out;

import fr.francetv.demo.domain.model.Item;

import java.util.List;

public interface ExternalItemPort {

    List<Item> fetchAll();
}
