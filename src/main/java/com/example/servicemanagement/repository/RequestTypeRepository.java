package com.example.servicemanagement.repository;

import com.example.servicemanagement.entity.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestTypeRepository extends JpaRepository<RequestType, Integer> {
    RequestType findRequestTypeById(Integer id);

    List<RequestType> findRequestTypeByRoleName(String roleName);
}
