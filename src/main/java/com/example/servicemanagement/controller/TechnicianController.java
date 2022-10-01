package com.example.servicemanagement.controller;

import com.example.servicemanagement.entity.Technician;
import com.example.servicemanagement.service.TechnicianService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value="/technician")
public class TechnicianController {

    @Autowired
    TechnicianService technicianService;

    @GetMapping("/")
    public List<Technician> getAllTechnician() {
        return this.technicianService.getAllTechnician();
    }
}
