package com.example.servicemanagement.service;

import com.example.servicemanagement.entity.Tenant;
import com.example.servicemanagement.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TenantService {
    @Autowired
    TenantRepository tenantRepository;

    public Tenant getTenantById(Integer id) {
        return this.tenantRepository.findTenantById(id);
    }
}
