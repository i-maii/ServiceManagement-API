package com.example.servicemanagement.repository;

import com.example.servicemanagement.entity.Apartment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApartmentRepository extends JpaRepository<Apartment, Integer> {
}
