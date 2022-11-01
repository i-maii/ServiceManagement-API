package com.example.servicemanagement.repository;

import com.example.servicemanagement.entity.Technician;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TechnicianRepository extends JpaRepository<Technician, Integer> {
    Technician findTechnicianById(Integer id);
    List<Technician> findTechnicianByIdIn(List<Integer> id);

    @Query(nativeQuery = true, value = "SELECT t.* FROM TECHNICIAN t WHERE t.id = (SELECT ta.technician_id FROM TECHNICIAN_ABILITIES ta GROUP BY ta.technician_id ORDER BY COUNT(*) ASC LIMIT 1)")
    Technician findLowestTechnician();

    @Query(nativeQuery = true, value = "SELECT t.request_type_id FROM TECHNICIAN_ABILITIES t WHERE t.technician_id = (SELECT ta.technician_id FROM TECHNICIAN_ABILITIES ta GROUP BY ta.technician_id ORDER BY COUNT(*) ASC LIMIT 1)")
    List<Integer> findRequestTypeOfLowestTechnician();

    Technician findTechnicianByUserId(Integer userId);

    @Query(nativeQuery = true, value = "SELECT CASE WHEN COUNT(*) > 0 THEN 'true' ELSE 'false' END " +
            "FROM TECHNICIAN t " +
            "INNER JOIN `USER` u ON t.user_id = u.id " +
            "WHERE t.id != :technician_id " +
            "AND username = :username")
    boolean checkUpdateDuplicate(@Param("technician_id") Integer technicianId, @Param("username") String username);

    @Query(nativeQuery = true, value = "SELECT CASE WHEN COUNT(*) > 0 THEN 'true' ELSE 'false' END " +
            "FROM TECHNICIAN t " +
            "INNER JOIN `USER` u ON t.user_id = u.id " +
            "WHERE username = :username")
    boolean checkCreateDuplicate(@Param("username") String username);

    @Query(nativeQuery = true, value = "SELECT CASE WHEN COUNT(*) > 0 THEN 'false' ELSE 'true' END " +
            "FROM SCHEDULE " +
            "WHERE technician_id = :technician_id")
    boolean checkCanDelete(@Param("technician_id") Integer technicianId);
}
