
package dk.ek.barbercom.mapper;

import dk.ek.barbercom.dto.UserDTO;
import dk.ek.barbercom.model.User;

public class UserMapper {

    public static UserDTO toDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),

                // Never expose the stored password hash
                null,

                user.getPhoneNumber(),
                user.getAddress(),
                user.getZipCode(),
                user.getRole(),
                user.getBusinessName()
        );
    }
}
