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

    public User create(User user) {
        return this.userRepository.saveAndFlush(user);
    }

    public void update(Integer id, User body) {
        User user = this.userRepository.findUserById(id);
        user.setName(body.getName());
        user.setPhoneNo(body.getPhoneNo());
        user.setPassword(body.getPassword());

        this.userRepository.saveAndFlush(user);
    }

    public void delete(Integer id) {
        this.userRepository.deleteById(id);
    }
}
