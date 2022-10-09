package com.example.servicemanagement.controller;

import com.example.servicemanagement.dto.RequestDto;
import com.example.servicemanagement.entity.Request;
import com.example.servicemanagement.repository.RequestRepository;
import com.example.servicemanagement.service.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.servicemanagement.constant.Constant.*;

@RestController
@RequestMapping(value="/request")
public class RequestController {

    @Autowired
    RequestService requestService;

    @Autowired
    RequestRepository requestRepository;

    @PostMapping("/")
    public void createRequest(
            @RequestBody RequestDto body
    ) {
        this.requestService.createRequest(body);
    }

    @PutMapping("/{id}")
    public void updateRequest(
            @PathVariable("id") Integer id,
            @RequestBody Request body
    ) {
        this.requestService.updateRequest(id, body);
    }

    @GetMapping("/{userId}")
    public List<Request> getAllRequestByUserId(
            @PathVariable("userId") Integer userId
    ) {
        return this.requestService.getRequestListByUserId(userId);
    }

    @GetMapping("/status/ready-for-estimation")
    public List<Request> getAllReadyForEstimationRequest(
    ) {
        return this.requestService.getRequestByStatus(STATUS_READY_FOR_ESTIMATION);
    }

    @GetMapping("/test")
    public List<Request> getAll(
    ) {
        return this.requestService.getAllRequestForPlanning();
    }
}
