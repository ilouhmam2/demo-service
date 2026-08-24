package fr.francetv.demo.application.usecase;

import fr.francetv.demo.domain.model.Item;
import fr.francetv.demo.domain.port.in.GetItemSoapUseCase;
import fr.francetv.demo.domain.port.out.ItemSoapPort;
import org.springframework.stereotype.Component;

@Component
public class GetItemSoapService implements GetItemSoapUseCase {

    private final ItemSoapPort itemSoapPort;

    public GetItemSoapService(ItemSoapPort itemSoapPort) {
        this.itemSoapPort = itemSoapPort;
    }

    @Override
    public Item getItemViaSoap(long id) {
        return itemSoapPort.getItem(id);
    }
}
