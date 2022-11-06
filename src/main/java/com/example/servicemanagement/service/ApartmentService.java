package com.example.servicemanagement.service;

import com.example.servicemanagement.dto.DistanceDto;
import com.example.servicemanagement.entity.Apartment;
import com.example.servicemanagement.repository.ApartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static com.example.servicemanagement.constant.Constant.*;

@Service
public class ApartmentService {
    @Autowired
    ApartmentRepository apartmentRepository;

    @Autowired
    ApartmentDistanceService apartmentDistanceService;

    @Value("${googleapi.key}")
    private String googleApiKey;

    @Value("${store.latitude}")
    private float latitude;

    @Value("${store.longitude}")
    private float longitude;

    public Apartment getApartmentById(Integer id) {
        return this.apartmentRepository.findApartmentById(id);
    }

    public List<Apartment> getAll() {
        return this.apartmentRepository.findAll();
    }

    public Apartment getPreviousApartmentByTechnicianId(Integer technicianId) {
        return this.apartmentRepository.findPreviousApartmentByTechnicianId(technicianId);
    }

    public void create(Apartment body) {
        boolean isDup = this.apartmentRepository.checkCreateDuplicate(body.getName(), body.getLatitude(), body.getLongitude());

        if (isDup) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ERR_INSERT_DUPLICATE_APARTMENT);
        }

        List<Apartment> apartments = this.apartmentRepository.findAll();
        Apartment newApartment = this.apartmentRepository.saveAndFlush(body);

        float distanceStore = getDistance(newApartment.getLatitude(), newApartment.getLongitude(), latitude, longitude);
        this.apartmentDistanceService.createDistanceStore(distanceStore, newApartment.getId());

        for (Apartment apartment: apartments) {
            float distance = getDistance(newApartment.getLatitude(), newApartment.getLongitude(), apartment.getLatitude(), apartment.getLongitude());
            this.apartmentDistanceService.createDistance(distance, newApartment.getId(), apartment.getId());
        }
    }

    public void update(Integer id, Apartment body) {
        boolean isDup = this.apartmentRepository.checkUpdateDuplicate(id, body.getName(), body.getLatitude(), body.getLongitude());

        if (isDup) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ERR_UPDATE_INVALID_APARTMENT);
        }

        Apartment apartment = this.apartmentRepository.findApartmentById(id);
        apartment.setName(body.getName());
        apartment.setAddress(body.getAddress());
        apartment.setLatitude(body.getLatitude());
        apartment.setLongitude(body.getLongitude());

        this.apartmentRepository.saveAndFlush(apartment);

        float distanceStore = getDistance(apartment.getLatitude(), apartment.getLongitude(), latitude, longitude);
        this.apartmentDistanceService.updateDistanceStore(distanceStore, apartment.getId());

        List<Apartment> apartments = this.apartmentRepository.findApartmentsByIdIsNot(id);
        for (Apartment a: apartments) {
            float distance = getDistance(apartment.getLatitude(), apartment.getLongitude(), a.getLatitude(), a.getLongitude());
            this.apartmentDistanceService.updateDistance(distance, a.getId(), apartment.getId());
        }
    }

    public void delete(Integer id) {
        boolean canDelete = this.apartmentRepository.checkCanDelete(id);

        if (!canDelete) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ERR_DELETE_INVALID_APARTMENT);
        }

        this.apartmentRepository.deleteById(id);
        this.apartmentDistanceService.deleteDistance(id);
    }

    public List<Apartment> test() {
        return this.apartmentRepository.findApartmentsByIdIsNot(1);
    }

    private float getDistance(double originLat, double originLng, double destinationLat, double destinationLng) {
        String origin = originLat + "," + originLng;
        String destination = destinationLat + "," + destinationLng;
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://maps.googleapis.com/maps/api/distancematrix/json?";
        ResponseEntity<DistanceDto> response = restTemplate.getForEntity(url + "destinations=" + destination + "&origins=" + origin + "&key=" + googleApiKey, DistanceDto.class);

        if (response.getBody() != null) {
            Integer distance = response.getBody().getRows().get(0).getElements().get(0).getDistance().getValue();
            return Math.round(distance / 1000.0);
        } else {
            return 0;
        }
    }
}
