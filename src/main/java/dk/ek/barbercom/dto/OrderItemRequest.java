package dk.ek.barbercom.dto;

import java.math.BigDecimal;

public record OrderItemRequest(String name, Integer quantity, BigDecimal price, Long sellerId) { }
