package dk.ek.barbercom.dto;
import java.math.BigDecimal;
import java.util.List;
public record AdminDashboardDTO(long userCount, long businessCount, long orderCount, long bookingCount, long cancellationCount, BigDecimal totalRevenue, List<UserDTO> users, List<UserDTO> businesses, List<CatalogProductDTO> products) { }
