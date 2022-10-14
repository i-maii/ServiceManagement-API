package com.example.servicemanagement.controller;

import com.example.servicemanagement.dto.ScheduleDto;
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
    public void getPossibleServiceRequest() throws ParseException {
        this.scheduleService.findRequestWithSpecificHour();
    }

//    @GetMapping("/technician/{userId}")
//    public List<RequestListDto> getSchedule(
//            @PathVariable("userId") Integer userId
//    ) {
//        return this.scheduleService.getScheduleByUserId(userId);
//    }

    @GetMapping("/technician/{userId}")
    public List<ScheduleDto> getSchedule(
            @PathVariable("userId") Integer userId
    ) {
        return this.scheduleService.getScheduleByUserId(userId);
    }

    @PostMapping("/{id}/request/{requestId}")
    public void closeTaskSchedule(
            @PathVariable("id") Integer scheduleId,
            @PathVariable("requestId") Integer requestId
    ) {
        this.scheduleService.closeTask(scheduleId, requestId);
    }
}
