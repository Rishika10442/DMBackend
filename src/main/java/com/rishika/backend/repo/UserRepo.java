package com.rishika.backend.repo;

import com.rishika.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
public interface UserRepo extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

}