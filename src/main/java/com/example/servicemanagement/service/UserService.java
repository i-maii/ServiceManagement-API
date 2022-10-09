package com.example.servicemanagement.service;

import com.example.servicemanagement.entity.User;
import com.example.servicemanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    UserRepository userRepository;

    public User login(String username, String password) {
        return this.userRepository.findUserByUsernameAndPassword(username, password);
    }

    public User getById(Integer userId) {
        return this.userRepository.findUserById(userId);
    }
}
