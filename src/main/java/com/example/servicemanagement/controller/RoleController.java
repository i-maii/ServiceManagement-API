package com.example.servicemanagement.controller;

import com.example.servicemanagement.entity.Role;
import com.example.servicemanagement.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value="/role")
public class RoleController {

    @Autowired
    RoleService roleService;

    @GetMapping("/")
    public List<Role> getAll() {
        return this.roleService.getAll();
    }
}
