package dk.ek.barbercom.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BusinessSaleDTO(
        Long orderId,
        LocalDateTime purchasedAt,
        String customerName,
        String customerEmail,
        Integer customerPhoneNumber,
        String customerAddress,
        Integer customerZipCode,
        String productName,
        Integer quantity,
        BigDecimal unitPrice
) { }
