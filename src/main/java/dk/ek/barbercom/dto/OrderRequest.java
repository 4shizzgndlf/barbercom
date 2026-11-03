package dk.ek.barbercom.dto;

import java.util.List;

public record OrderRequest(List<OrderItemRequest> items) { }
