package com.example.servicemanagement.service;

import com.example.servicemanagement.dto.PushNotificationDto;
import com.example.servicemanagement.entity.*;
import com.example.servicemanagement.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


@Service
public class PushNotificationService {
    private Logger logger = LoggerFactory.getLogger(PushNotificationService.class);
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    @Autowired
    FCMService fcmService;

    @Autowired
    ScheduleRepository scheduleRepository;

    @Autowired
    TechnicianRepository technicianRepository;

    @Autowired
    RequestRepository requestRepository;

    @Autowired
    TenantRepository tenantRepository;

    @Autowired
    UserRepository userRepository;

    public void sendServicePushNotification(String notificationToken, Time startTime, Time endTime, String requestType) {
        PushNotificationDto request = new PushNotificationDto();
        request.setMessage("วันที่ " + dateFormat.format(Calendar.getInstance().getTime()) + " เวลา " + timeFormat.format(new Date(startTime.getTime())) + "-" + timeFormat.format(new Date(endTime.getTime())) + "น. เข้าซ่อม" + requestType);
        request.setTitle("แจ้งเตือนเข้าซ่อม");
        request.setToken(notificationToken);
        try {
            fcmService.sendMessageToToken(request);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public void sendSchedulePushNotification(String notificationToken) {
        PushNotificationDto request = new PushNotificationDto();
        request.setMessage("จัดแผนงานซ่อม วันที่ " + dateFormat.format(Calendar.getInstance().getTime()) + " เรียบร้อยแล้ว");
        request.setTitle("แจ้งเตือนแผนงานซ่อม");
        request.setToken(notificationToken);
        try {
            fcmService.sendMessageToToken(request);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public void sendEstimationPushNotification(String notificationToken, String apartmentName, String roomNo, String requestType) {
        PushNotificationDto request = new PushNotificationDto();
        request.setMessage("ประเมินงานซ่อม " + requestType + " หอ" + apartmentName + " ห้อง" + roomNo);
        request.setTitle("แจ้งเตือนประเมินรายการซ่อม");
        request.setToken(notificationToken);
        try {
            fcmService.sendMessageToToken(request);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public void sendServiceOtherPushNotification(String notificationToken, String apartmentName, String roomNo, String requestType) {
        PushNotificationDto request = new PushNotificationDto();
        request.setMessage("หอ" + apartmentName + " ห้อง" + roomNo + " " + requestType);
        request.setTitle("แจ้งรายการซ่อม");
        request.setToken(notificationToken);
        try {
            fcmService.sendMessageToToken(request);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public void sendServiceNotificationByScheduleId(Integer id) {
        Schedule schedule = this.scheduleRepository.findScheduleById(id);

        sendServicePushNotification(schedule.getRequest().getUser().getNotificationToken(), schedule.getServiceStartTime(), schedule.getServiceEndTime(), schedule.getRequest().getRequestType().getName());
    }

    public void sendScheduleNotificationByTechnicianId(Integer id) {
        Technician technician = this.technicianRepository.findTechnicianById(id);

        sendSchedulePushNotification(technician.getUser().getNotificationToken());
    }

    public void sendEstimationNotificationByRequestId(Integer id, Integer userId) {
        Request request = this.requestRepository.findRequestById(id);
        Tenant tenant = this.tenantRepository.findTenantByUserId(request.getUser().getId());
        User user = this.userRepository.findUserById(userId);

        sendEstimationPushNotification(user.getNotificationToken(), request.getApartment().getName(), tenant.getRoomNo(), request.getRequestType().getName());
    }

    public void sendServiceOtherNotificationByRequestId(Integer id, Integer userId) {
        Request request = this.requestRepository.findRequestById(id);
        Tenant tenant = this.tenantRepository.findTenantByUserId(request.getUser().getId());
        User user = this.userRepository.findUserById(userId);

        sendServiceOtherPushNotification(user.getNotificationToken(), request.getApartment().getName(), tenant.getRoomNo(), request.getRequestType().getName());
    }
}
