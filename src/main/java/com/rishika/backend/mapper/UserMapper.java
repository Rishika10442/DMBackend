package com.rishika.backend.mapper;

import com.rishika.backend.dto.UserRequest;
import org.springframework.stereotype.Service;
import com.rishika.backend.entity.User;
@Service
public class UserMapper {
    public User toEntity(UserRequest request, String encryptedPassword) {
        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPassword(request.password());
        user.setEncryptedpassword(encryptedPassword);
         user.setUserInfo(request.userInfo());

        return user;
    }
}
