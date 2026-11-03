
package dk.ek.barbercom.dto;

import dk.ek.barbercom.model.Role;

public record UserDTO(
        Long id,
        String firstName,
        String lastName,
        String email,
        String password,
        Integer phoneNumber,
        String address,
        Integer zipCode,
        Role role,
        String businessName
) {
}
