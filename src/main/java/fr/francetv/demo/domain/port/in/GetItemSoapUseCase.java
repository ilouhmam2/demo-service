package fr.francetv.demo.domain.port.in;

import fr.francetv.demo.domain.model.Item;

public interface GetItemSoapUseCase {

    Item getItemViaSoap(long id);
}
