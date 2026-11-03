package dk.ek.barbercom.dto;

import java.math.BigDecimal;

public record OrderItemDTO(String name, Integer quantity, BigDecimal price, String sellerName) { }
