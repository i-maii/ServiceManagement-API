package com.example.servicemanagement.controller;

import com.example.servicemanagement.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value="/config")
public class ConfigController {

    @Autowired
    ConfigService configService;

    @GetMapping("/update")
    public void updateConfig() {
        this.configService.findConfiguration();
    }
}
