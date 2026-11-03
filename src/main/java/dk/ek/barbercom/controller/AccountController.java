package dk.ek.barbercom.controller;

import dk.ek.barbercom.dto.UserDTO;
import dk.ek.barbercom.mapper.UserMapper;
import dk.ek.barbercom.model.User;
import dk.ek.barbercom.repository.UserRepository;
import dk.ek.barbercom.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    private final UserRepository userRepository;
    private final UserService userService;

    public AccountController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<UserDTO> currentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .map(UserMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<UserDTO> updateCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UserDTO userDTO) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        return userService.updateUser(user.getId(), userDTO)
                .map(updated -> {
                    UserDetails updatedPrincipal = org.springframework.security.core.userdetails.User
                            .withUsername(updated.email())
                            .password(userDetails.getPassword())
                            .authorities(userDetails.getAuthorities())
                            .build();
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                    updatedPrincipal, null,
                                    updatedPrincipal.getAuthorities()));
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
