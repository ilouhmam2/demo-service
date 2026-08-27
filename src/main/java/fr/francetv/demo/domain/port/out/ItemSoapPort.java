package fr.francetv.demo.domain.port.out;

import fr.francetv.demo.domain.model.Item;

public interface ItemSoapPort {

    Item getItem(long id);
}
