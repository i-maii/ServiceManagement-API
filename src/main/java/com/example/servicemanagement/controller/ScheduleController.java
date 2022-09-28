package com.example.servicemanagement.controller;

import com.example.servicemanagement.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.NoSuchElementException;

@RestController
@RequestMapping(value="/schedule")
public class ScheduleController {

    @Autowired
    ScheduleService scheduleService;

    @GetMapping("/")
    public void getPossibleServiceRequest() throws ParseException {
        this.scheduleService.findRequestWithSpecificHour();
    }

    @GetMapping("/test")
    public void test() throws NoSuchElementException {
        this.scheduleService.findRoute();
    }
}
