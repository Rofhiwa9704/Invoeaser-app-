package co.za.kingstechco.kingstechco.invoeaserapp.util;

import co.za.kingstechco.kingstechco.invoeaserapp.dto.LoginRequestDto;
import co.za.kingstechco.kingstechco.invoeaserapp.dto.UserRegistrationDto;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.Role;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.User;
import co.za.kingstechco.kingstechco.invoeaserapp.repository.RoleRepository;
import co.za.kingstechco.kingstechco.invoeaserapp.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for test authentication operations.
 */
public class TestAuthUtils {

    /**
     * Registers a user with ADMIN role and returns JWT token.
     */
    public static String registerAndAuthenticateAdminUser(String username, String email, String password,
                                                         TestRestTemplate restTemplate,
                                                         UserRepository userRepository,
                                                         RoleRepository roleRepository,
                                                         PasswordEncoder passwordEncoder) throws Exception {
        
        // Create admin user directly in database
        User adminUser = new User();
        adminUser.setUsername(username);
        adminUser.setEmail(email);
        adminUser.setFirstName("Test");
        adminUser.setLastName("Admin");
        adminUser.setPasswordHash(passwordEncoder.encode(password));

        // Find or create ADMIN role
        Role adminRole = roleRepository.findByRoleName("ADMIN")
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setRoleName("ADMIN");
                    return roleRepository.save(newRole);
                });

        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        adminUser.setRoles(roles);

        userRepository.save(adminUser);

        // Authenticate and get token
        LoginRequestDto login = new LoginRequestDto();
        login.setUsername(username);
        login.setPassword(password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login", 
                new HttpEntity<>(login, headers), 
                String.class
        );

        if (loginResponse.getStatusCode().is2xxSuccessful()) {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(loginResponse.getBody()).get("access_token").asText();
        } else {
            throw new RuntimeException("Failed to authenticate admin user: " + loginResponse.getBody());
        }
    }

    /**
     * Creates an admin user registration DTO.
     */
    public static UserRegistrationDto createAdminRegistrationDto(String username, String email, String password) {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername(username);
        dto.setEmail(email);
        dto.setPassword(password);
        dto.setFirstName("Test");
        dto.setLastName("Admin");
        return dto;
    }
}