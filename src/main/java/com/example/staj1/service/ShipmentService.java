package com.example.staj1.service;

import com.example.staj1.Dto.CalculatePriceResponse;
import com.example.staj1.Dto.ShipmentRequest;
import com.example.staj1.Dto.ShipmentResponse;
import com.example.staj1.model.*;
import com.example.staj1.repository.AddressRepository;
import com.example.staj1.repository.UserRepository;
import com.example.staj1.repository.PriceRepository;
import com.example.staj1.repository.ShipmentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final PriceRepository priceRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    public ShipmentService(
            ShipmentRepository shipmentRepository,
            PriceRepository priceRepository,
            UserRepository userRepository,
            AddressRepository addressRepository) {

        this.shipmentRepository = shipmentRepository;
        this.priceRepository = priceRepository;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
    }

    public Page<Shipment> getAll(Pageable pageable) {

        User user = getCurrentUser();

        return shipmentRepository
                .findBySender_IdAndDeletedFalseOrReceiver_IdAndDeletedFalse(
                        user.getId(),
                        user.getId(),
                        pageable);
    }

    public ShipmentResponse getById(Integer id) {

        User user = getCurrentUser();

        Shipment shipment = shipmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Gönderi bulunamadı."));

        checkAccess(shipment, user);

        return toResponse(shipment);
    }

    public Shipment getByBarcode(String barcode) {

        User user = getCurrentUser();

        Shipment shipment = shipmentRepository
                .findByBarcodeAndDeletedFalse(barcode)
                .orElseThrow(() ->
                        new EntityNotFoundException("Barkod bulunamadı."));

        checkAccess(shipment, user);

        return shipment;
    }

    public ShipmentResponse create(ShipmentRequest shipmentRequest) {

        User currentUser = getCurrentUser();

        // Sadece giriş yapan kullanıcı kendi adına gönderi oluşturabilir.
        if (shipmentRequest.getSenderId() == null ||
                !currentUser.getId().equals(shipmentRequest.getSenderId())) {

            throw new AccessDeniedException(
                    "Bu kullanıcı adına gönderi oluşturamazsınız.");
        }

        User sender = null;
        User receiver = null;

        // Gönderici kullanıcı varsa bul
        if (shipmentRequest.getSenderId() != null) {

            sender = userRepository.findById(
                    shipmentRequest.getSenderId()
            ).orElseThrow(() ->
                    new EntityNotFoundException("Gönderici bulunamadı."));
        }

        // Alıcı kullanıcı varsa bul
        if (shipmentRequest.getReceiverId() != null) {

            receiver = userRepository.findById(
                    shipmentRequest.getReceiverId()
            ).orElseThrow(() ->
                    new EntityNotFoundException("Alıcı bulunamadı."));
        }

        Address senderAddress = null;
        Address receiverAddress = null;

        // Gönderici kayıtlı adresi kullanıyorsa
        if (shipmentRequest.getSenderAddressId() != null) {

            if (sender == null) {
                throw new IllegalArgumentException(
                        "Gönderici adresi kullanmak için gönderici kullanıcı seçilmelidir.");
            }

            senderAddress = addressRepository.findById(
                    shipmentRequest.getSenderAddressId()
            ).orElseThrow(() ->
                    new EntityNotFoundException("Gönderici adresi bulunamadı."));

            if (!senderAddress.getUser().getId().equals(sender.getId())) {
                throw new IllegalArgumentException(
                        "Gönderici adresi bu kullanıcıya ait değil.");
            }
        }

        // Alıcı kayıtlı adresi kullanıyorsa
        if (shipmentRequest.getReceiverAddressId() != null) {

            if (receiver == null) {
                throw new IllegalArgumentException(
                        "Alıcı adresi kullanmak için alıcı kullanıcı seçilmelidir.");
            }

            receiverAddress = addressRepository.findById(
                    shipmentRequest.getReceiverAddressId()
            ).orElseThrow(() ->
                    new EntityNotFoundException("Alıcı adresi bulunamadı."));

            if (!receiverAddress.getUser().getId().equals(receiver.getId())) {
                throw new IllegalArgumentException(
                        "Alıcı adresi bu kullanıcıya ait değil.");
            }
        }

        Shipment shipment = new Shipment();

        shipment.setWidth(shipmentRequest.getWidth());
        shipment.setLength(shipmentRequest.getLength());
        shipment.setHeight(shipmentRequest.getHeight());
        shipment.setWeight(shipmentRequest.getWeight());

        shipment.setSender(sender);
        shipment.setReceiver(receiver);

        if (sender != null) {
            shipment.setSenderName(sender.getName());
            shipment.setSenderSurname(sender.getSurname());
        }

        if (receiver != null) {
            shipment.setReceiverName(receiver.getName());
            shipment.setReceiverSurname(receiver.getSurname());
        }

        // Gönderici adresi
        if (senderAddress != null) {

            if (shipmentRequest.getSenderName() != null ||
                    shipmentRequest.getSenderSurname() != null ||
                    shipmentRequest.getSenderCity() != null ||
                    shipmentRequest.getSenderDistrict() != null ||
                    shipmentRequest.getSenderAddressText() != null) {

                throw new IllegalArgumentException(
                        "Kayıtlı gönderici adresi seçildiğinde manuel gönderici bilgileri gönderilemez.");
            }

            shipment.setSenderAddressId(senderAddress.getId());
            shipment.setSenderCity(senderAddress.getCity().getName());
            shipment.setSenderDistrict(senderAddress.getDistrict().getName());

            String senderAddressText =
                    senderAddress.getNeighborhood()
                            + ", Bina No: " + senderAddress.getBuildingNo()
                            + ", Daire No: " + senderAddress.getApartmentNo();

            shipment.setSenderAddressText(senderAddressText);

        } else if (sender != null) {

            shipment.setSenderCity(shipmentRequest.getSenderCity());
            shipment.setSenderDistrict(shipmentRequest.getSenderDistrict());
            shipment.setSenderAddressText(shipmentRequest.getSenderAddressText());

        } else {

            shipment.setSenderName(shipmentRequest.getSenderName());
            shipment.setSenderSurname(shipmentRequest.getSenderSurname());
            shipment.setSenderCity(shipmentRequest.getSenderCity());
            shipment.setSenderDistrict(shipmentRequest.getSenderDistrict());
            shipment.setSenderAddressText(shipmentRequest.getSenderAddressText());
        }

        // Alıcı adresi
        if (receiverAddress != null) {

            if (shipmentRequest.getReceiverName() != null ||
                    shipmentRequest.getReceiverSurname() != null ||
                    shipmentRequest.getReceiverCity() != null ||
                    shipmentRequest.getReceiverDistrict() != null ||
                    shipmentRequest.getReceiverAddressText() != null) {

                throw new IllegalArgumentException(
                        "Kayıtlı alıcı adresi seçildiğinde manuel alıcı bilgileri gönderilemez.");
            }

            shipment.setReceiverAddressId(receiverAddress.getId());
            shipment.setReceiverCity(receiverAddress.getCity().getName());
            shipment.setReceiverDistrict(receiverAddress.getDistrict().getName());

            String receiverAddressText =
                    receiverAddress.getNeighborhood()
                            + ", Bina No: " + receiverAddress.getBuildingNo()
                            + ", Daire No: " + receiverAddress.getApartmentNo();

            shipment.setReceiverAddressText(receiverAddressText);

        } else if (receiver != null) {

            shipment.setReceiverCity(shipmentRequest.getReceiverCity());
            shipment.setReceiverDistrict(shipmentRequest.getReceiverDistrict());
            shipment.setReceiverAddressText(shipmentRequest.getReceiverAddressText());

        } else {

            shipment.setReceiverName(shipmentRequest.getReceiverName());
            shipment.setReceiverSurname(shipmentRequest.getReceiverSurname());
            shipment.setReceiverCity(shipmentRequest.getReceiverCity());
            shipment.setReceiverDistrict(shipmentRequest.getReceiverDistrict());
            shipment.setReceiverAddressText(shipmentRequest.getReceiverAddressText());
        }

        BigDecimal price = calculatePrice(
                shipmentRequest.getWidth(),
                shipmentRequest.getLength(),
                shipmentRequest.getHeight(),
                shipmentRequest.getWeight()
        );

        shipment.setPrice(price);

        shipment.setBarcode(generateBarcode());

        Shipment savedShipment = shipmentRepository.save(shipment);

        return toResponse(savedShipment);
    }

    public ShipmentResponse update(
            Integer id,
            ShipmentRequest request) {

        User currentUser = getCurrentUser();

        // Önce gönderiyi bul
        Shipment shipment = shipmentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Gönderi bulunamadı."));

        // Sadece gönderici güncelleyebilir
        if (shipment.getSender() == null ||
                !shipment.getSender().getId().equals(currentUser.getId())) {

            throw new AccessDeniedException(
                    "Bu gönderiyi güncelleme yetkiniz yok.");
        }

        User sender = null;
        User receiver = null;

        if (request.getSenderId() != null) {

            sender = userRepository.findById(request.getSenderId())
                    .orElseThrow(() ->
                            new EntityNotFoundException("Gönderici bulunamadı."));

            // Güncellemede de gönderici giriş yapan kullanıcı olmalı
            if (!sender.getId().equals(currentUser.getId())) {
                throw new AccessDeniedException(
                        "Bu kullanıcı adına gönderi güncelleyemezsiniz.");
            }
        }

        if (request.getReceiverId() != null) {

            receiver = userRepository.findById(request.getReceiverId())
                    .orElseThrow(() ->
                            new EntityNotFoundException("Alıcı bulunamadı."));
        }

        Address senderAddress = null;
        Address receiverAddress = null;

        if (request.getSenderAddressId() != null) {

            if (sender == null) {
                throw new IllegalArgumentException(
                        "Gönderici adresi kullanmak için gönderici kullanıcı seçilmelidir.");
            }

            senderAddress = addressRepository.findById(
                    request.getSenderAddressId()
            ).orElseThrow(() ->
                    new EntityNotFoundException("Gönderici adresi bulunamadı."));

            if (!senderAddress.getUser().getId().equals(sender.getId())) {
                throw new IllegalArgumentException(
                        "Gönderici adresi bu kullanıcıya ait değil.");
            }
        }

        if (request.getReceiverAddressId() != null) {

            if (receiver == null) {
                throw new IllegalArgumentException(
                        "Alıcı adresi kullanmak için alıcı kullanıcı seçilmelidir.");
            }

            receiverAddress = addressRepository.findById(
                    request.getReceiverAddressId()
            ).orElseThrow(() ->
                    new EntityNotFoundException("Alıcı adresi bulunamadı."));

            if (!receiverAddress.getUser().getId().equals(receiver.getId())) {
                throw new IllegalArgumentException(
                        "Alıcı adresi bu kullanıcıya ait değil.");
            }
        }

        shipment.setWidth(request.getWidth());
        shipment.setLength(request.getLength());
        shipment.setHeight(request.getHeight());
        shipment.setWeight(request.getWeight());

        shipment.setSender(sender);
        shipment.setReceiver(receiver);

        // Gönderici adresi
        if (senderAddress != null) {

            shipment.setSenderAddressId(senderAddress.getId());
            shipment.setSenderName(sender.getName());
            shipment.setSenderSurname(sender.getSurname());
            shipment.setSenderCity(senderAddress.getCity().getName());
            shipment.setSenderDistrict(senderAddress.getDistrict().getName());

            String senderAddressText =
                    senderAddress.getNeighborhood()
                            + ", Bina No: " + senderAddress.getBuildingNo()
                            + ", Daire No: " + senderAddress.getApartmentNo();

            shipment.setSenderAddressText(senderAddressText);

        } else if (sender != null) {

            shipment.setSenderAddressId(null);
            shipment.setSenderName(sender.getName());
            shipment.setSenderSurname(sender.getSurname());
            shipment.setSenderCity(request.getSenderCity());
            shipment.setSenderDistrict(request.getSenderDistrict());
            shipment.setSenderAddressText(request.getSenderAddressText());

        } else {

            shipment.setSenderAddressId(null);
            shipment.setSenderName(request.getSenderName());
            shipment.setSenderSurname(request.getSenderSurname());
            shipment.setSenderCity(request.getSenderCity());
            shipment.setSenderDistrict(request.getSenderDistrict());
            shipment.setSenderAddressText(request.getSenderAddressText());
        }

        // Alıcı adresi
        if (receiverAddress != null) {

            shipment.setReceiverAddressId(receiverAddress.getId());
            shipment.setReceiverName(receiver.getName());
            shipment.setReceiverSurname(receiver.getSurname());
            shipment.setReceiverCity(receiverAddress.getCity().getName());
            shipment.setReceiverDistrict(receiverAddress.getDistrict().getName());

            String receiverAddressText =
                    receiverAddress.getNeighborhood()
                            + ", Bina No: " + receiverAddress.getBuildingNo()
                            + ", Daire No: " + receiverAddress.getApartmentNo();

            shipment.setReceiverAddressText(receiverAddressText);

        } else if (receiver != null) {

            shipment.setReceiverAddressId(null);
            shipment.setReceiverName(receiver.getName());
            shipment.setReceiverSurname(receiver.getSurname());
            shipment.setReceiverCity(request.getReceiverCity());
            shipment.setReceiverDistrict(request.getReceiverDistrict());
            shipment.setReceiverAddressText(request.getReceiverAddressText());

        } else {

            shipment.setReceiverAddressId(null);
            shipment.setReceiverName(request.getReceiverName());
            shipment.setReceiverSurname(request.getReceiverSurname());
            shipment.setReceiverCity(request.getReceiverCity());
            shipment.setReceiverDistrict(request.getReceiverDistrict());
            shipment.setReceiverAddressText(request.getReceiverAddressText());
        }

        BigDecimal price = calculatePrice(
                request.getWidth(),
                request.getLength(),
                request.getHeight(),
                request.getWeight()
        );

        shipment.setPrice(price);

        return toResponse(shipmentRepository.save(shipment));
    }

    public void delete(Integer id) {

        User currentUser = getCurrentUser();

        // Önce gönderiyi bul
        Shipment shipment = shipmentRepository
                .findByIdAndDeletedFalse(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Gönderi bulunamadı."));

        // Sadece gönderici silebilir
        if (shipment.getSender() == null ||
                !shipment.getSender().getId().equals(currentUser.getId())) {

            throw new AccessDeniedException(
                    "Bu gönderiyi silme yetkiniz yok.");
        }

        // Soft delete
        shipment.setDeleted(true);
        shipmentRepository.save(shipment);
    }

    public BigDecimal calculatePrice(
            BigDecimal width,
            BigDecimal length,
            BigDecimal height,
            BigDecimal weight) {

        BigDecimal desi = width
                .multiply(length)
                .multiply(height)
                .divide(
                        BigDecimal.valueOf(3000),
                        2,
                        RoundingMode.HALF_UP
                );

        BigDecimal pricingValue;

        if (desi.compareTo(weight) > 0) {
            pricingValue = desi;
        } else {
            pricingValue = weight;
        }

        Price price = priceRepository
                .findByMinDesiLessThanEqualAndMaxDesiGreaterThan(
                        pricingValue,
                        pricingValue
                );

        if (price == null) {
            throw new EntityNotFoundException(
                    "Bu desi/ağırlık değeri için fiyat bulunamadı.");
        }

        return price.getPrice();
    }

    public CalculatePriceResponse calculatePriceDetail(
            BigDecimal width,
            BigDecimal length,
            BigDecimal height,
            BigDecimal weight) {

        BigDecimal desi = width
                .multiply(length)
                .multiply(height)
                .divide(
                        BigDecimal.valueOf(3000),
                        2,
                        RoundingMode.HALF_UP
                );

        BigDecimal calculatedValue;
        String calculatedBy;

        if (desi.compareTo(weight) > 0) {
            calculatedValue = desi;
            calculatedBy = "DESI";
        } else {
            calculatedValue = weight;
            calculatedBy = "WEIGHT";
        }

        Price price = priceRepository
                .findByMinDesiLessThanEqualAndMaxDesiGreaterThan(
                        calculatedValue,
                        calculatedValue
                );

        if (price == null) {
            throw new EntityNotFoundException(
                    "Bu değer için fiyat bulunamadı.");
        }

        CalculatePriceResponse response = new CalculatePriceResponse();

        response.setWidth(width);
        response.setLength(length);
        response.setHeight(height);
        response.setWeight(weight);
        response.setDesi(desi);
        response.setCalculatedValue(calculatedValue);
        response.setCalculatedBy(calculatedBy);
        response.setPrice(price.getPrice());

        return response;
    }

    private String generateBarcode() {

        String barcode;

        do {
            barcode = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 12)
                    .toUpperCase();

        } while (shipmentRepository.existsByBarcode(barcode));

        return barcode;
    }

    private ShipmentResponse toResponse(Shipment shipment) {

        ShipmentResponse response = new ShipmentResponse();

        response.setId(shipment.getId());
        response.setBarcode(shipment.getBarcode());
        response.setWidth(shipment.getWidth());
        response.setLength(shipment.getLength());
        response.setHeight(shipment.getHeight());
        response.setWeight(shipment.getWeight());
        response.setPrice(shipment.getPrice());

        response.setSender(shipment.getSender());
        response.setReceiver(shipment.getReceiver());

        response.setSenderAddressId(shipment.getSenderAddressId());
        response.setReceiverAddressId(shipment.getReceiverAddressId());

        response.setSenderName(shipment.getSenderName());
        response.setSenderSurname(shipment.getSenderSurname());
        response.setSenderCity(shipment.getSenderCity());
        response.setSenderDistrict(shipment.getSenderDistrict());
        response.setSenderAddressText(shipment.getSenderAddressText());

        response.setReceiverName(shipment.getReceiverName());
        response.setReceiverSurname(shipment.getReceiverSurname());
        response.setReceiverCity(shipment.getReceiverCity());
        response.setReceiverDistrict(shipment.getReceiverDistrict());
        response.setReceiverAddressText(shipment.getReceiverAddressText());

        return response;
    }

    private User getCurrentUser() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Kullanıcı bulunamadı."));
    }

    private void checkAccess(
            Shipment shipment,
            User user) {

        boolean allowed =
                (shipment.getSender() != null &&
                        shipment.getSender().getId().equals(user.getId()))
                        ||
                        (shipment.getReceiver() != null &&
                                shipment.getReceiver().getId().equals(user.getId()));

        if (!allowed) {
            throw new AccessDeniedException(
                    "Bu gönderiye erişim yetkiniz yok.");
        }
    }
}