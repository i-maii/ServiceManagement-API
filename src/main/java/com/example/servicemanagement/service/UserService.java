package com.example.servicemanagement.service;

import com.example.servicemanagement.entity.User;
import com.example.servicemanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static com.example.servicemanagement.constant.Constant.ERR_WRONG_USERNAME_PASSWORD;

@Service
public class UserService {

    @Autowired
    UserRepository userRepository;

    public User login(String username, String password) {
        User user = this.userRepository.findUserByUsernameAndPassword(username, password);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ERR_WRONG_USERNAME_PASSWORD);
        }

        return user;
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
        user.setUsername(body.getUsername());
        user.setPhoneNo(body.getPhoneNo());
        user.setPassword(body.getPassword());

        this.userRepository.saveAndFlush(user);
    }

    public void delete(Integer id) {
        this.userRepository.deleteById(id);
    }

    public void saveNotificationToken(Integer userId, String token) {
        User user = this.userRepository.findUserById(userId);
        user.setNotificationToken(token);

        this.userRepository.saveAndFlush(user);
    }

    public List<User> getUserByRoleId(Integer roleId) {
        return this.userRepository.findUsersByRoleId(roleId);
    }

    public List<User> getUserByRoleNames(List<String> roleName) {
        return this.userRepository.findUsersByRoleNameIn(roleName);
    }
}
