package com.example.servicemanagement.service;

import com.example.servicemanagement.repository.ApartmentDistanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApartmentDistanceService {

    @Autowired
    ApartmentDistanceRepository apartmentDistanceRepository;

    public Integer getNearest(Integer start, List<Integer> destination) {
        return this.apartmentDistanceRepository.findNearest(start, destination);
    }

    public boolean checkCanWalk(Integer start, Integer destination) {
        return this.apartmentDistanceRepository.checkCanWalk(start, destination);
    }
}
