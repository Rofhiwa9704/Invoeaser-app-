package co.za.kingstechco.kingstechco.invoeaserapp.service.impl;

import co.za.kingstechco.kingstechco.invoeaserapp.entity.Role;
import co.za.kingstechco.kingstechco.invoeaserapp.entity.User;
import co.za.kingstechco.kingstechco.invoeaserapp.dto.UserRegistrationDto;
import co.za.kingstechco.kingstechco.invoeaserapp.repository.RoleRepository;
import co.za.kingstechco.kingstechco.invoeaserapp.repository.UserRepository;
import co.za.kingstechco.kingstechco.invoeaserapp.service.UserService;
import co.za.kingstechco.kingstechco.invoeaserapp.exception.ResourceNotFoundException;
import co.za.kingstechco.kingstechco.invoeaserapp.util.RoleConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User registerUser(UserRegistrationDto userRegistrationDto) {
        User user = new User();
        user.setUsername(userRegistrationDto.getUsername());
        user.setEmail(userRegistrationDto.getEmail());
        user.setFirstName(userRegistrationDto.getFirstName());
        user.setLastName(userRegistrationDto.getLastName());
        user.setPasswordHash(passwordEncoder.encode(userRegistrationDto.getPassword()));

        return userRepository.save(user);
    }

    @Override
    public User registerAdminUser(UserRegistrationDto userRegistrationDto) {
        User user = new User();
        user.setUsername(userRegistrationDto.getUsername());
        user.setEmail(userRegistrationDto.getEmail());
        user.setFirstName(userRegistrationDto.getFirstName());
        user.setLastName(userRegistrationDto.getLastName());
        user.setPasswordHash(passwordEncoder.encode(userRegistrationDto.getPassword()));

        // Assign ADMIN role
        Role adminRole = roleRepository.findByRoleName("ADMIN")
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setRoleName("ADMIN");
                    return roleRepository.save(newRole);
                });

        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        user.setRoles(roles);

        return userRepository.save(user);
    }

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }
}

