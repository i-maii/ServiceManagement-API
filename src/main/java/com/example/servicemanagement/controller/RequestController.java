package com.example.servicemanagement.controller;

import com.example.servicemanagement.dto.*;
import com.example.servicemanagement.entity.Request;
import com.example.servicemanagement.repository.RequestRepository;
import com.example.servicemanagement.service.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.servicemanagement.constant.Constant.STATUS_READY_FOR_PLAN;

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
    public List<EstimateDto> getAllEstimationRequest(
    ) {
        return this.requestService.getAllEstimateRequest();
    }

    @GetMapping("/estimate-value")
    public EstimateValueDto getEstimateValue(
    ) {
        return this.requestService.getEstimateValue();
    }

    @PostMapping("/{id}/estimate-value")
    public void updateEstimateValue(
            @PathVariable("id") Integer requestId,
            @RequestBody UpdateEstimateValueDto body
    ) {
        this.requestService.updateEstimate(requestId, body);
    }

    @GetMapping("/admin")
    public List<RequestListDto> getAdminRequestList(
    ) {
        return this.requestService.getAdminRequestList();
    }

    @PostMapping("/{id}/close")
    public void closeTask(
            @PathVariable("id") Integer requestId
    ) {
        this.requestService.closeTask(requestId);
    }
}
