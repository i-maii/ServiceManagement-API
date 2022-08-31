package com.example.servicemanagement.service;

import com.example.servicemanagement.entity.Technician;
import com.example.servicemanagement.repository.TechnicianRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TechnicianService {

    @Autowired
    TechnicianRepository technicianRepository;

    public int getNumberOfAvailableTechnician() {
        return this.technicianRepository.countTechnicianByAvailable(true);
    }

    public List<Technician> getAvailableTechnician() {
        return this.technicianRepository.findTechniciansByAvailable(true);
    }

    public List<Technician> getAllTechnician() {
        return this.technicianRepository.findAll();
    }

    public Technician getTechnicianById(Integer id) {
        return this.technicianRepository.findTechnicianById(id);
    }

    public Technician getLowestTechnician() {
        return this.technicianRepository.findLowestTechnician();
    }

    public boolean checkLowestAbilitiesTechnicianAvailable() {
        return this.technicianRepository.isLowestAbilitiesTechnicianAvailable();
    }

    public List<Integer> getAllRequestTypeOfLowestTechnician() {
        return this.technicianRepository.findRequestTypeOfLowestTechnician();
    }

    public boolean test() {
        return this.technicianRepository.isLowestAbilitiesTechnicianAvailable();
    }
}
