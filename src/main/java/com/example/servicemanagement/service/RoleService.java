package com.example.servicemanagement.service;

import com.example.servicemanagement.entity.Role;
import com.example.servicemanagement.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleService {

    @Autowired
    RoleRepository roleRepository;

    public List<Role> getAll() {
        return this.roleRepository.findAll();
    }
}
