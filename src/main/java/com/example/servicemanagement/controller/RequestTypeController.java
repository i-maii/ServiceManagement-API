package com.example.servicemanagement.controller;

import com.example.servicemanagement.entity.RequestType;
import com.example.servicemanagement.service.RequestTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value="/request-type")
public class RequestTypeController {

    @Autowired
    RequestTypeService requestTypeService;

    @GetMapping("/")
    public List<RequestType> getAll() {
        return this.requestTypeService.getAllOrderByCommonArea();
    }

    @GetMapping("/priority")
    public List<Integer> getAllPriority() {
        return this.requestTypeService.getAllPriority();
    }

    @GetMapping("/{role}")
    public List<RequestType> getRequestTypeByRole(
            @PathVariable("role") String role) {
        return this.requestTypeService.getRequestTypeByRole(role);
    }

    @GetMapping("/common-area")
    public List<RequestType> getRequestTypeCommonArea() {
        return this.requestTypeService.getRequestTypeCommonArea();
    }

    @PostMapping("/")
    public void create(
            @RequestBody RequestType body
    ) {
        this.requestTypeService.create(body);
    }

    @PutMapping("/{id}")
    public void create(
            @PathVariable("id") Integer id,
            @RequestBody RequestType body
    ) {
        this.requestTypeService.update(id, body);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable("id") Integer id
    ) {
        this.requestTypeService.delete(id);
    }
}
