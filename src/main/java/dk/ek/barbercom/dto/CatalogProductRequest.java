package dk.ek.barbercom.dto;
import java.math.BigDecimal;
public record CatalogProductRequest(String name, String description, BigDecimal price, String category, String type, Long sellerId) { }
