package com.example.servicemanagement.controller;

import com.example.servicemanagement.service.PushNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value="/push-notification")
public class PushNotificationController {

    @Autowired
    PushNotificationService pushNotificationService;

    @GetMapping("/service/{id}")
    public void sendServiceNotification(
            @PathVariable("id") Integer id
    ) {
        this.pushNotificationService.sendServiceNotificationByScheduleId(id);
    }

    @GetMapping("/schedule/{id}")
    public void sendScheduleNotification(
            @PathVariable("id") Integer id
    ) {
        this.pushNotificationService.sendScheduleNotificationByTechnicianId(id);
    }

    @GetMapping("/estimation/{id}/user/{userId}")
    public void sendEstimationNotification(
            @PathVariable("id") Integer id,
            @PathVariable("userId") Integer userId
    ) {
        this.pushNotificationService.sendEstimationNotificationByRequestId(id, userId);
    }

    @GetMapping("/service/other/{id}/user/{userId}")
    public void sendServiceOtherNotification(
            @PathVariable("id") Integer id,
            @PathVariable("userId") Integer userId
    ) {
        this.pushNotificationService.sendServiceOtherNotificationByRequestId(id, userId);
    }
}
