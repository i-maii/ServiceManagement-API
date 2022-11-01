package com.example.servicemanagement.service;

import com.example.servicemanagement.dto.PushNotificationDto;
import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FCMService {
    private Logger logger = LoggerFactory.getLogger(FCMService.class);

    public void sendMessageToToken(PushNotificationDto request) {
        Message message = getPreconfiguredMessageToToken(request);
        send(message);
    }

    private Message getPreconfiguredMessageToToken(PushNotificationDto request) {
        return getPreconfiguredMessageBuilder(request).setToken(request.getToken())
                .build();
    }

    private void send(Message message) {
        FirebaseMessaging.getInstance().sendAsync(message);
    }

    private Message.Builder getPreconfiguredMessageBuilder(PushNotificationDto request) {
        return Message.builder().setNotification(Notification.builder().setTitle(request.getTitle()).setBody(request.getMessage()).build());
    }
}
