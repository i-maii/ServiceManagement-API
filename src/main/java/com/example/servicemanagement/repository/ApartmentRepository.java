package com.example.servicemanagement.repository;

import com.example.servicemanagement.entity.Apartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApartmentRepository extends JpaRepository<Apartment, Integer> {
    Apartment findApartmentById(Integer id);

    @Query(nativeQuery = true, value = "SELECT a.* \n" +
            "FROM APARTMENT a \n" +
            "INNER JOIN SCHEDULE s ON s.apartment_id = a.id \n" +
            "WHERE s.technician_id = :technician_id \n" +
            "AND s.`sequence` IS NOT NULL \n" +
            "ORDER BY s.`sequence`DESC \n" +
            "LIMIT 0, 1")
    Apartment findPreviousApartmentByTechnicianId(@Param("technician_id") Integer technicianId);

    List<Apartment> findApartmentsByIdIsNot(Integer id);

    @Query(nativeQuery = true, value = "SELECT CASE WHEN COUNT(*) > 0 THEN 'true' ELSE 'false' END " +
            "FROM APARTMENT " +
            "WHERE name = :name")
    boolean checkCreateDuplicate(@Param("name") String name);

    @Query(nativeQuery = true, value = "SELECT CASE WHEN COUNT(*) > 0 THEN 'true' ELSE 'false' END " +
            "FROM APARTMENT " +
            "WHERE id != :id AND name = :name")
    boolean checkUpdateDuplicate(@Param("id") Integer id, @Param("name") String name);
}
