package com.example.servicemanagement.repository;

import com.example.servicemanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Integer> {
    User findUserByUsernameAndPassword(String username, String password);

    User findUserById(Integer userId);

    List<User> findUsersByRoleId(Integer roleId);

    List<User> findUsersByRoleNameIn(List<String> roleName);
}
