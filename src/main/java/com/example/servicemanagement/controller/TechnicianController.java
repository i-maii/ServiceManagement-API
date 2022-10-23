package com.example.servicemanagement.controller;

import com.example.servicemanagement.entity.Technician;
import com.example.servicemanagement.service.TechnicianService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/")
    public void create(
            @RequestBody Technician body
    ) {
        this.technicianService.create(body);
    }

    @PutMapping("/{id}")
    public void update(
            @PathVariable("id") Integer id,
            @RequestBody Technician body
    ) {
        this.technicianService.update(id, body);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable("id") Integer id
    ) {
        this.technicianService.delete(id);
    }
}
