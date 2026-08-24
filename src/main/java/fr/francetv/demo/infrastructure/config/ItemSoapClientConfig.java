package fr.francetv.demo.infrastructure.config;

import fr.francetv.demo.soap.client.ItemServicePortType;
import fr.francetv.foundation.soapclient.factory.SoapClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ItemSoapClientConfig {

    @Bean
    public ItemServicePortType itemServicePortType(
            SoapClientFactory soapClientFactory,
            @Value("${demo.soap.item-service-address:http://localhost:8089/soap/ItemService}") String address) {
        return soapClientFactory.create(ItemServicePortType.class, address);
    }
}
