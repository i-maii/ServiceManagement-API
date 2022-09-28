package com.example.servicemanagement.service;

import com.example.servicemanagement.entity.Apartment;
import com.example.servicemanagement.repository.ApartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApartmentService {
    @Autowired
    ApartmentRepository apartmentRepository;

    public Apartment getApartmentById(Integer id) {
        return this.apartmentRepository.findApartmentById(id);
    }

    public Apartment getPreviousApartmentByTechnicianId(Integer technicianId) {
        return this.apartmentRepository.findPreviousApartmentByTechnicianId(technicianId);
    }
}
