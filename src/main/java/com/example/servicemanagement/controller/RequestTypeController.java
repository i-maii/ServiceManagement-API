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
        return this.requestTypeService.getAll();
    }

    @GetMapping("/{role}")
    public List<RequestType> getRequestTypeByRole(
            @PathVariable("role") String role) {
        return this.requestTypeService.getRequestTypeByRole(role);
    }
}
