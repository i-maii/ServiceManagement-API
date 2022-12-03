package com.example.servicemanagement.service;

import com.example.servicemanagement.entity.ApartmentDistance;
import com.example.servicemanagement.repository.ApartmentDistanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ApartmentDistanceService {

    @Autowired
    ApartmentDistanceRepository apartmentDistanceRepository;

    public Integer getNearest(Integer start, List<Integer> destination) {
        return this.apartmentDistanceRepository.findNearest(start, destination);
    }

    public boolean checkCanWalk(Integer start, Integer destination) {
        return this.apartmentDistanceRepository.checkCanWalk(start, destination);
    }

    public ApartmentDistance getApartmentDistanceByStartAndDestination(Integer start, Integer destination) {
        return this.apartmentDistanceRepository.findApartmentDistanceByStartAndDestination(start, destination);
    }

    public void updateDistance(float distance, Integer start, Integer destination) {
        ApartmentDistance direct = this.apartmentDistanceRepository.findApartmentDistanceByStartAndDestination(start, destination);
        direct.setDistance(distance);
        this.apartmentDistanceRepository.saveAndFlush(direct);

        ApartmentDistance revert = this.apartmentDistanceRepository.findApartmentDistanceByStartAndDestination(destination, start);
        revert.setDistance(distance);
        this.apartmentDistanceRepository.saveAndFlush(revert);
    }

    public void createDistance(float distance, Integer newApartmentId, Integer id) {
        ApartmentDistance direct = new ApartmentDistance();
        direct.setStart(newApartmentId);
        direct.setDestination(id);
        direct.setDistance(distance);
        this.apartmentDistanceRepository.saveAndFlush(direct);

        ApartmentDistance revert = new ApartmentDistance();
        revert.setStart(id);
        revert.setDestination(newApartmentId);
        revert.setDistance(distance);
        this.apartmentDistanceRepository.saveAndFlush(revert);
    }

    public void createOwnDistance(Integer id) {
        ApartmentDistance apartmentDistance = new ApartmentDistance();
        apartmentDistance.setStart(id);
        apartmentDistance.setDestination(id);
        apartmentDistance.setDistance(0);
        this.apartmentDistanceRepository.saveAndFlush(apartmentDistance);
    }

    public void updateDistanceStore(float distanceStore, Integer id) {
        ApartmentDistance apartmentDistance = this.apartmentDistanceRepository.findApartmentDistanceByStartAndDestination(0, id);
        apartmentDistance.setDistance(distanceStore);
        this.apartmentDistanceRepository.saveAndFlush(apartmentDistance);
    }

    public void createDistanceStore(float distanceStore, Integer id) {
        ApartmentDistance apartmentDistance = new ApartmentDistance();
        apartmentDistance.setStart(0);
        apartmentDistance.setDestination(id);
        apartmentDistance.setDistance(distanceStore);
        this.apartmentDistanceRepository.saveAndFlush(apartmentDistance);
    }

    public void deleteDistance(Integer id) {
        this.apartmentDistanceRepository.deleteDistance(id);
    }
}
