package fr.francetv.demo.infrastructure.adapter.out.soap;

import fr.francetv.demo.domain.exception.ExternalServiceException;
import fr.francetv.demo.domain.model.Item;
import fr.francetv.demo.soap.client.GetItemRequest;
import fr.francetv.demo.soap.client.GetItemResponse;
import fr.francetv.demo.soap.client.ItemServicePortType;
import jakarta.xml.soap.SOAPFault;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ItemSoapAdapterTest {

    @Mock
    ItemServicePortType itemService;

    ItemSoapAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ItemSoapAdapter(itemService);
    }

    @Test
    void getItem_returnsItem_fromSoapResponse() {
        GetItemResponse response = new GetItemResponse();
        response.setId(42L);
        response.setName("Test Widget");
        given(itemService.getItem(any())).willReturn(response);

        Item item = adapter.getItem(42L);

        ArgumentCaptor<GetItemRequest> captor = ArgumentCaptor.forClass(GetItemRequest.class);
        verify(itemService).getItem(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(42L);
        assertThat(item.getId()).isEqualTo(42L);
        assertThat(item.getName()).isEqualTo("Test Widget");
    }

    @Test
    void getItem_soapFault_mapsToBadGateway() {
        SOAPFault fault = mock(SOAPFault.class);
        given(fault.getFaultString()).willReturn("Item not found");
        // Pre-construct to avoid mock call inside willThrow() argument evaluation
        SOAPFaultException soapException = new SOAPFaultException(fault);

        given(itemService.getItem(any())).willThrow(soapException);

        assertThatThrownBy(() -> adapter.getItem(99L))
            .isInstanceOf(ExternalServiceException.class)
            .satisfies(e -> assertThat(((ExternalServiceException) e).getKind())
                .isEqualTo(ExternalServiceException.Kind.BAD_GATEWAY));
    }
}

