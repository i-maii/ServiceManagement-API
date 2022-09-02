package com.example.servicemanagement.controller;

import com.example.servicemanagement.dto.TechnicianPlanDto;
import com.example.servicemanagement.entity.Request;
import com.example.servicemanagement.repository.RequestRepository;
import com.example.servicemanagement.service.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;

@RestController
@RequestMapping(value="/request")
public class RequestController {

    @Autowired
    RequestService requestService;

    @Autowired
    RequestRepository requestRepository;

    @GetMapping("/")
    public List<TechnicianPlanDto> getPossibleServiceRequest() throws ParseException {
        return this.requestService.reorderPriority(this.requestRepository.findAll());
    }
}
