package com.example.servicemanagement.controller;

import com.example.servicemanagement.entity.Apartment;
import com.example.servicemanagement.service.ApartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value="/apartment")
public class ApartmentController {

    @Autowired
    ApartmentService apartmentService;

    @GetMapping("/")
    public List<Apartment> getAll() {
        return this.apartmentService.getAll();
    }
}
