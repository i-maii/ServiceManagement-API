package com.example.servicemanagement.service;

import com.example.servicemanagement.entity.RequestType;
import com.example.servicemanagement.repository.RequestTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RequestTypeService {
    @Autowired
    RequestTypeRepository requestTypeRepository;

    public RequestType getRequestTypeById(Integer id) {
        return this.requestTypeRepository.findRequestTypeById(id);
    }

    public List<RequestType> getRequestTypeForTechnician() {
        return this.requestTypeRepository.findRequestTypeByRoleName("technician");
    }
}
