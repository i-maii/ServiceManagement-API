package com.example.servicemanagement.repository;

import com.example.servicemanagement.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Integer> {
    List<Request> findRequestsByStatus(String status);
}
