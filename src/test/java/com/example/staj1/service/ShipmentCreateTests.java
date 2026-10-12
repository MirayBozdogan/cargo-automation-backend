package com.example.staj1.service;

import com.example.staj1.Dto.ShipmentRequest;
import com.example.staj1.model.User;
import com.example.staj1.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShipmentCreateTests {
    ShipmentRepository shipments;
    ShipmentService service;
    User user;
    @BeforeEach void setup() {
        shipments = mock(ShipmentRepository.class);
        UserRepository users = mock(UserRepository.class);
        user = new User(); user.setId(7); user.setName("Deneme"); user.setSurname("Musteri"); user.setRole("USER");
        when(users.findByEmail("test@example.test")).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("test@example.test", null));
        service = spy(new ShipmentService(shipments, mock(PriceRepository.class), users, mock(AddressRepository.class)));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    ShipmentRequest request() {
        ShipmentRequest r = new ShipmentRequest();
        r.setWidth(BigDecimal.TEN); r.setLength(BigDecimal.TEN); r.setHeight(BigDecimal.TEN); r.setWeight(BigDecimal.ONE);
        r.setSenderCity("Ankara"); r.setSenderDistrict("Cankaya"); r.setSenderAddressText("Deneme adres");
        r.setReceiverName("Alici"); r.setReceiverSurname("Deneme"); r.setReceiverCity("Istanbul"); r.setReceiverDistrict("Kadikoy"); r.setReceiverAddressText("Alici adres");
        return r;
    }
    @Test void senderComesFromSessionWithoutClientId() {
        ShipmentRequest r = request();
        doReturn(BigDecimal.TEN).when(service).calculatePrice(any(), any(), any(), any());
        when(shipments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var result = service.create(r);
        assertEquals(user.getId(), result.getSender().getId());
        assertEquals(user.getName(), result.getSenderName());
    }
    @Test void cannotImpersonateAnotherSender() {
        ShipmentRequest r = request(); r.setSenderId(8);
        assertThrows(AccessDeniedException.class, () -> service.create(r));
        verifyNoInteractions(shipments);
    }
    @Test void customerCannotSelectRegisteredRecipient() {
        ShipmentRequest r = request(); r.setReceiverId(8);
        assertThrows(AccessDeniedException.class, () -> service.create(r));
        verifyNoInteractions(shipments);
    }
    @Test void manualRecipientFieldsAreRequired() {
        for (int i = 0; i < 5; i++) {
            ShipmentRequest r = request();
            switch (i) {
                case 0 -> r.setReceiverName("  "); case 1 -> r.setReceiverSurname(null);
                case 2 -> r.setReceiverCity(""); case 3 -> r.setReceiverDistrict(null);
                case 4 -> r.setReceiverAddressText("  ");
            }
            assertThrows(IllegalArgumentException.class, () -> service.create(r));
        }
        verifyNoInteractions(shipments);
    }
    @Test void manualSenderAddressIsRequired() {
        ShipmentRequest r = request(); r.setSenderAddressText(null);
        assertThrows(IllegalArgumentException.class, () -> service.create(r));
        verifyNoInteractions(shipments);
    }
}
