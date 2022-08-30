package com.example.servicemanagement.repository;

import com.example.servicemanagement.entity.Technician;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TechnicianRepository extends JpaRepository<Technician, Integer> {
    int countTechnicianByAvailable(boolean isAvailable);
    List<Technician> findTechniciansByAvailable(boolean isAvailable);

    @Query(nativeQuery = true, value = "SELECT t.is_available FROM TECHNICIAN t WHERE t.id = (SELECT ta.technician_id FROM TECHNICIAN_ABILITIES ta GROUP BY ta.technician_id ORDER BY COUNT(*) ASC LIMIT 1)")
    boolean isLowestAbilitiesTechnicianAvailable();

    @Query(nativeQuery = true, value = "SELECT t.request_type_id FROM TECHNICIAN_ABILITIES t WHERE t.technician_id = (SELECT ta.technician_id FROM TECHNICIAN_ABILITIES ta GROUP BY ta.technician_id ORDER BY COUNT(*) ASC LIMIT 1)")
    List<Integer> findRequestTypeOfLowestTechnician();
}
