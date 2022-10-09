package com.example.servicemanagement.service;

import com.example.servicemanagement.entity.Apartment;
import com.example.servicemanagement.repository.ApartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApartmentService {
    @Autowired
    ApartmentRepository apartmentRepository;

    public Apartment getApartmentById(Integer id) {
        return this.apartmentRepository.findApartmentById(id);
    }

    public List<Apartment> getAll() {
        return this.apartmentRepository.findAll();
    }

    public Apartment getPreviousApartmentByTechnicianId(Integer technicianId) {
        return this.apartmentRepository.findPreviousApartmentByTechnicianId(technicianId);
    }
}
