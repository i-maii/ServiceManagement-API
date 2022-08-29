package com.example.servicemanagement.controller;

import com.example.servicemanagement.dto.LoginDto;
import com.example.servicemanagement.entity.User;
import com.example.servicemanagement.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value="/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    UserService userService;

    @PostMapping("/login")
    public User login(
            @RequestBody LoginDto loginDto) {
        return this.userService.login(loginDto.getUsername(), loginDto.getPassword());
    }
}
