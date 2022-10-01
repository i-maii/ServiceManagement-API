package com.example.servicemanagement.repository;

import com.example.servicemanagement.entity.Technician;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TechnicianRepository extends JpaRepository<Technician, Integer> {
    int countTechnicianByAvailable(boolean isAvailable);
    List<Technician> findTechniciansByAvailable(boolean isAvailable);
    Technician findTechnicianById(Integer id);
    List<Technician> findTechnicianByIdIn(List<Integer> id);

    @Query(nativeQuery = true, value = "SELECT t.* FROM TECHNICIAN t WHERE t.id = (SELECT ta.technician_id FROM TECHNICIAN_ABILITIES ta GROUP BY ta.technician_id ORDER BY COUNT(*) ASC LIMIT 1)")
    Technician findLowestTechnician();

    @Query(nativeQuery = true, value = "SELECT t.is_available FROM TECHNICIAN t WHERE t.id = (SELECT ta.technician_id FROM TECHNICIAN_ABILITIES ta GROUP BY ta.technician_id ORDER BY COUNT(*) ASC LIMIT 1)")
    boolean isLowestAbilitiesTechnicianAvailable();

    @Query(nativeQuery = true, value = "SELECT t.request_type_id FROM TECHNICIAN_ABILITIES t WHERE t.technician_id = (SELECT ta.technician_id FROM TECHNICIAN_ABILITIES ta GROUP BY ta.technician_id ORDER BY COUNT(*) ASC LIMIT 1)")
    List<Integer> findRequestTypeOfLowestTechnician();

    @Query(nativeQuery = true, value = "SELECT t.request_type_id FROM TECHNICIAN_ABILITIES t\n" +
            "INNER JOIN REQUEST_TYPE rt ON t.request_type_id = rt.id\n" +
            "WHERE t.technician_id = (\n" +
            "SELECT ta.technician_id AS ID \n" +
            "FROM TECHNICIAN_ABILITIES ta \n" +
            "GROUP BY ta.technician_id \n" +
            "ORDER BY COUNT(*) ASC\n" +
            "LIMIT 1 )\n" +
            "AND rt.priority IN (1, 2)")
    List<Integer> findPriorityRequestTypeOfLowestTechnician();

    @Query(nativeQuery = true, value = "SELECT * FROM TECHNICIAN t WHERE t.id IN (SELECT DISTINCT s.technician_id FROM SCHEDULE s)")
    List<Technician> findTechnicianSchedule();
}
