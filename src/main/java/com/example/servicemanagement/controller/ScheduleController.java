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
    public void getRequestPlan() throws ParseException {
        this.scheduleService.findRequestPlan();
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

    @PostMapping("/{id}/{action}/request/{requestId}")
    public void closeTaskSchedule(
            @PathVariable("id") Integer scheduleId,
            @PathVariable("action") String action,
            @PathVariable(value = "requestId", required = false) Integer requestId
    ) {
        this.scheduleService.closeTask(scheduleId, requestId, action);
    }

    @PostMapping("/{id}/close")
    public void closeTaskSchedule(
            @PathVariable("id") Integer scheduleId
    ) {
        this.scheduleService.closeTask(scheduleId, null, "close");
    }

    @GetMapping("/driver/{userId}")
    public boolean checkDriver(
            @PathVariable("userId") Integer userId
    ) {
        return this.scheduleService.checkDriver(userId);
    }
}
