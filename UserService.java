package co.za.kingstechco.kingstechco.invoeaserapp.service;

import co.za.kingstechco.kingstechco.invoeaserapp.entity.User;
import co.za.kingstechco.kingstechco.invoeaserapp.dto.UserRegistrationDto;

public interface UserService {
    User registerUser(UserRegistrationDto userRegistrationDto);
    User registerAdminUser(UserRegistrationDto userRegistrationDto);
    User getUserByUsername(String username);
}
