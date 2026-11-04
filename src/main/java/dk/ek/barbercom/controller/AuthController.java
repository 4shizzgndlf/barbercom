
package dk.ek.barbercom.controller;

import dk.ek.barbercom.dto.UserDTO;
import dk.ek.barbercom.model.Role;
import dk.ek.barbercom.service.UserService;
import dk.ek.barbercom.service.BusinessSubscriptionService;
import dk.ek.barbercom.service.RegistrationNotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Controller
public class AuthController {

    private final UserService userService;
    private final BusinessSubscriptionService businessSubscriptions;
    private final RegistrationNotificationService registrationNotifications;

    public AuthController(UserService userService, BusinessSubscriptionService businessSubscriptions,
            RegistrationNotificationService registrationNotifications) {
        this.userService = userService;
        this.businessSubscriptions = businessSubscriptions;
        this.registrationNotifications = registrationNotifications;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/api/auth/status")
    public Map<String, Boolean> loginStatus(Authentication authentication) {
        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        return Map.of("authenticated", authenticated);
    }

    /**
     * Registration endpoint used by the static registration page.
     * The service hashes the submitted password before persisting the user.
     */
    @PostMapping("/api/auth/register")
    public ResponseEntity<?> registerApiUser(@RequestBody UserDTO userDTO) {
        try {
            if (userDTO.role() == Role.BUSINESS) {
                return ResponseEntity.status(HttpStatus.CREATED).body(businessSubscriptions.startRegistration(userDTO));
            }
            UserDTO createdUser = userService.createUser(userDTO);
            registrationNotifications.sendPrivateAccountConfirmation(createdUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(new ErrorResponse(e.getReason()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new ErrorResponse("Kunne ikke starte Stripe-betalingen. Prøv igen."));
        }
    }

    @PostMapping("/register")
    public String registerUser(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) Integer phoneNumber,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) Integer zipCode,
            @RequestParam(defaultValue = "USER") Role role,
            @RequestParam(required = false) String businessName,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            UserDTO userDTO = new UserDTO(
                    null,
                    firstName,
                    lastName,
                    email,
                    password,
                    phoneNumber,
                    address,
                    zipCode,
                    role,
                    businessName
            );

            userService.createUser(userDTO);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Account created successfully. You can now log in."
            );

            return "redirect:/login?registered";

        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        }
    }

    private record ErrorResponse(String message) {
    }
}
