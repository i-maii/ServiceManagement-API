package com.example.servicemanagement.controller;

import com.example.servicemanagement.dto.TechnicianPlanDto;
import com.example.servicemanagement.entity.Request;
import com.example.servicemanagement.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;

@RestController
@RequestMapping(value="/schedule")
public class ScheduleController {

    @Autowired
    ScheduleService scheduleService;

    @GetMapping("/")
    public List<List<TechnicianPlanDto>> getPossibleServiceRequest() throws ParseException {
        return this.scheduleService.findRequestWithSpecificHour(8);
    }
}
