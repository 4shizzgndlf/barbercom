package dk.ek.barbercom.dto;
public record BusinessRequest(String firstName, String lastName, String businessName, String email, String password, Integer phoneNumber, String address, Integer zipCode) { }
