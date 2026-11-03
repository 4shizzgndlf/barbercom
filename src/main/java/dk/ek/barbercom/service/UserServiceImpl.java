
package dk.ek.barbercom.service;

import dk.ek.barbercom.dto.UserDTO;
import dk.ek.barbercom.mapper.UserMapper;
import dk.ek.barbercom.model.Role;
import dk.ek.barbercom.model.User;
import dk.ek.barbercom.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserMapper::toDTO)
                .toList();
    }

    @Override
    public Optional<UserDTO> getUserById(Long id) {
        return userRepository.findById(id)
                .map(UserMapper::toDTO);
    }

    @Override
    public UserDTO createUser(UserDTO userDTO) {
        if (userDTO.email() == null
                || userDTO.email().isBlank()) {
            throw new IllegalArgumentException("E-mail er påkrævet");
        }

        if (userDTO.password() == null
                || userDTO.password().length() < 8) {
            throw new IllegalArgumentException(
                    "Adgangskoden skal indeholde mindst 8 tegn");
        }

        String email = userDTO.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Der findes allerede en konto med denne e-mail");
        }

        User user = new User();
        user.setFirstName(userDTO.firstName());
        user.setLastName(userDTO.lastName());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(userDTO.password()));
        user.setPhoneNumber(userDTO.phoneNumber());
        user.setAddress(userDTO.address());
        user.setZipCode(userDTO.zipCode());

        // Registration may only create a private customer or a business barber.
        // ADMIN is deliberately never selectable from the public form.
        boolean isBusiness = userDTO.role() == Role.BUSINESS;
        if (isBusiness && (userDTO.businessName() == null || userDTO.businessName().isBlank())) {
            throw new IllegalArgumentException("Virksomhedsnavn er påkrævet for en business-konto");
        }
        user.setRole(isBusiness ? Role.BUSINESS : Role.USER);
        user.setBusinessName(isBusiness ? userDTO.businessName().trim() : null);

        return UserMapper.toDTO(userRepository.save(user));
    }

    @Override
    public Optional<UserDTO> updateUser(Long id, UserDTO userDTO) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setFirstName(userDTO.firstName());
                    user.setLastName(userDTO.lastName());

                    if (userDTO.email() != null
                            && !userDTO.email().isBlank()) {

                        String email = userDTO.email()
                                .trim().toLowerCase();

                        if (!email.equalsIgnoreCase(user.getEmail())
                                && userRepository.existsByEmail(email)) {
                            throw new IllegalArgumentException(
                                    "Der findes allerede en konto med denne e-mail");
                        }

                        user.setEmail(email);
                    }

                    user.setPhoneNumber(userDTO.phoneNumber());
                    user.setAddress(userDTO.address());
                    user.setZipCode(userDTO.zipCode());

                    if (user.getRole() == Role.BUSINESS
                            && userDTO.businessName() != null
                            && !userDTO.businessName().isBlank()) {
                        user.setBusinessName(userDTO.businessName().trim());
                    }

                    if (userDTO.password() != null
                            && !userDTO.password().isBlank()) {

                        if (userDTO.password().length() < 8) {
                            throw new IllegalArgumentException(
                                    "Adgangskoden skal indeholde mindst 8 tegn");
                        }

                        user.setPassword(
                                passwordEncoder.encode(userDTO.password()));
                    }

                    User savedUser = userRepository.saveAndFlush(user);

                    System.out.println("USER SAVED SUCCESSFULLY");
                    System.out.println("ID: " + savedUser.getId());
                    System.out.println("Email: " + savedUser.getEmail());

                    return UserMapper.toDTO(savedUser);
                });
    }

    @Override
    public boolean deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            return false;
        }

        userRepository.deleteById(id);
        return true;
    }
}
