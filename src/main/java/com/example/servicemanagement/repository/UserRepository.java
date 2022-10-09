package com.example.servicemanagement.repository;

import com.example.servicemanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
    User findUserByUsernameAndPassword(String username, String password);

    User findUserById(Integer userId);
}
