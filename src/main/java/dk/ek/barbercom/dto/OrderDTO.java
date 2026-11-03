package dk.ek.barbercom.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDTO(Long id, LocalDateTime purchasedAt, BigDecimal totalAmount, List<OrderItemDTO> items) { }
