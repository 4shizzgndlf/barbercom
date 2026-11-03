
package dk.ek.barbercom.config;

import dk.ek.barbercom.model.Role;
import dk.ek.barbercom.model.User;
import dk.ek.barbercom.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class InitData implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public InitData(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        // Browsers reject Danish characters in the local part of an e-mail
        // address. Move the existing account rather than recreating it, so its
        // password, role and any existing bookings are preserved.
        migrateEmailIfNeeded("frisør@salon.dk", "frisor@salon.dk");

        createUserIfMissing(
                "Lucas",
                "Hoffmann Christensen",
                "loegcas@gmail.com",
                "Hda34ewy",
                61128112,
                "Vesterbrogade 181, 1th",
                1800,
                Role.ADMIN,
                null
        );

        createUserIfMissing(
                "John",
                "Doe",
                "john@example.dk",
                "examplepass",
                12345678,
                "Danmarksgade 1",
                1234,
                Role.USER,
                null
        );

        createUserIfMissing(
                "Mohammed",
                "Salvari",
                "frisor@salon.dk",
                "barberpass",
                87654321,
                "Københavnsvej 2",
                4321,
                Role.BUSINESS,
                "Salvari Barbershop"
        );

        createUserIfMissing(
                "Emma",
                "Nielsen",
                "emma@northsidebarber.dk",
                "barberpass",
                22334455,
                "Nørrebrogade 83, 2200 København N",
                2200,
                Role.BUSINESS,
                "Northside Barber"
        );

        createUserIfMissing(
                "Noah",
                "Larsen",
                "noah@cphgrooming.dk",
                "barberpass",
                33445566,
                "Istedgade 34, 1650 København V",
                1650,
                Role.BUSINESS,
                "CPH Grooming"
        );

        createUserIfMissing(
                "Freja",
                "Holm",
                "freja@frejaholmstudio.dk",
                "barberpass",
                44556677,
                "Østerbrogade 101, 2100 København Ø",
                2100,
                Role.BUSINESS,
                "Freja Holm Studio"
        );
        checkAdminPassword();
    }

    private void migrateEmailIfNeeded(String oldEmail, String newEmail) {
        if (userRepository.existsByEmail(newEmail)) {
            return;
        }

        userRepository.findByEmail(oldEmail).ifPresent(user -> {
            user.setEmail(newEmail);
            userRepository.save(user);
        });
    }

    private void createUserIfMissing(
            String firstName,
            String lastName,
            String email,
            String rawPassword,
            Integer phoneNumber,
            String address,
            Integer zipCode,
            Role role,
            String businessName) {

        String normalizedEmail = email.trim().toLowerCase();

        if (!userRepository.existsByEmail(normalizedEmail)) {
            User user = new User(
                    null,
                    firstName,
                    lastName,
                    normalizedEmail,
                    passwordEncoder.encode(rawPassword),
                    phoneNumber,
                    address,
                    zipCode,
                    role
            );
            user.setBusinessName(businessName);

            userRepository.save(user);
        } else if (role == Role.BUSINESS) {
            userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
                if (user.getBusinessName() == null || user.getBusinessName().isBlank()) {
                    user.setBusinessName(businessName);
                    userRepository.save(user);
                }
            });
        }
    }

    private void checkAdminPassword() {
        userRepository.findByEmail("loegcas@gmail.com")
                .ifPresent(user -> {
                    System.out.println(
                            "Admin user exists: " + user.getEmail()
                    );

                    System.out.println(
                            "Password matches: " +
                                    passwordEncoder.matches(
                                            "Hda34ewy",
                                            user.getPassword()
                                    )
                    );

                    System.out.println(
                            "Role: " + user.getRole()
                    );
                });
    }
}
