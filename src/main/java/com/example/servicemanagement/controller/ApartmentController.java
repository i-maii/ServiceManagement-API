package com.example.servicemanagement.controller;

import com.example.servicemanagement.dto.DistanceDto;
import com.example.servicemanagement.entity.Apartment;
import com.example.servicemanagement.service.ApartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/")
    public void create(
            @RequestBody Apartment body
    ) {
        this.apartmentService.create(body);
    }

    @PutMapping("/{id}")
    public void update(
            @PathVariable("id") Integer id,
            @RequestBody Apartment body
    ) {
        this.apartmentService.update(id, body);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable("id") Integer id
    ) {
        this.apartmentService.delete(id);
    }

    @GetMapping("/test")
    public List<Apartment> test() {
        return this.apartmentService.test();
    }
}
