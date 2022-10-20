package com.example.servicemanagement.controller;

import com.example.servicemanagement.entity.Tenant;
import com.example.servicemanagement.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value="/tenant")
public class TenantController {

    @Autowired
    TenantService tenantService;

    @GetMapping("/apartment/{id}")
    public List<Tenant> getAllByApartmentId(
            @PathVariable("id") Integer apartmentId
    ) {
        return this.tenantService.getAllByApartmentId(apartmentId);
    }

    @PostMapping("/")
    public void create(
            @RequestBody Tenant body
    ) {
        this.tenantService.create(body);
    }

    @PutMapping("/{id}")
    public void update(
            @PathVariable("id") Integer id,
            @RequestBody Tenant body
    ) {
        this.tenantService.update(id, body);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable("id") Integer id
    ) {
        this.tenantService.delete(id);
    }
}
