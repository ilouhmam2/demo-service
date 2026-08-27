package fr.francetv.demo.infrastructure.adapter.out.soap;

import fr.francetv.demo.domain.exception.ExternalServiceException;
import fr.francetv.demo.domain.model.Item;
import fr.francetv.demo.domain.port.out.ItemSoapPort;
import fr.francetv.demo.soap.client.GetItemRequest;
import fr.francetv.demo.soap.client.GetItemResponse;
import fr.francetv.demo.soap.client.ItemServicePortType;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ItemSoapAdapter implements ItemSoapPort {

    private static final Logger log = LoggerFactory.getLogger(ItemSoapAdapter.class);

    private final ItemServicePortType itemService;

    public ItemSoapAdapter(ItemServicePortType itemService) {
        this.itemService = itemService;
    }

    @Override
    public Item getItem(long id) {
        try {
            GetItemRequest request = new GetItemRequest();
            request.setId(id);
            GetItemResponse response = itemService.getItem(request);
            return new Item(response.getId(), response.getName(), null, null, null);
        } catch (SOAPFaultException e) {
            String faultMessage = e.getFault() != null ? e.getFault().getFaultString() : "SOAP error";
            log.error("soap.client.fault id={} fault={}", id, faultMessage);
            throw new ExternalServiceException(
                    ExternalServiceException.Kind.BAD_GATEWAY,
                    "SOAP service error: " + faultMessage,
                    e);
        }
    }
}
