package com.example.servicemanagement.service;

import com.example.servicemanagement.entity.ApartmentDistance;
import com.example.servicemanagement.repository.ApartmentDistanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApartmentDistanceService {

    @Autowired
    ApartmentDistanceRepository apartmentDistanceRepository;

    public List<ApartmentDistance> getDistanceByDestination(List<Integer> destination) {
        return this.apartmentDistanceRepository.findDistanceByDestination(destination);
    }

    public List<ApartmentDistance> getDistanceByStartAndDestinations(Integer start, List<Integer> destination) {
        return this.apartmentDistanceRepository.findByStartAndDestinationIn(start, destination);
    }

    public Integer getNearestApartmentByStartAndNoOfTechnician(Integer start, Integer noOfTechnician) {
        return this.apartmentDistanceRepository.findNearestSameApartment(start, noOfTechnician);
    }

    public Integer getNearest(Integer start, List<Integer> destination) {
        return this.apartmentDistanceRepository.findNearest(start, destination);
    }
}
