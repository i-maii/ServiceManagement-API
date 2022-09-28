package com.example.servicemanagement.repository;

import com.example.servicemanagement.entity.ApartmentDistance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApartmentDistanceRepository extends JpaRepository<ApartmentDistance, Integer> {
    @Query(nativeQuery = true, value = "SELECT * FROM APARTMENT_DISTANCE a WHERE a.start = 0 AND a.destination IN ?")
    List<ApartmentDistance> findDistanceByDestination(@Param("destination") List<Integer> destination);

    List<ApartmentDistance> findByStartAndDestinationIn(Integer start, List<Integer> destination);

    @Query(nativeQuery = true, value = "SELECT destination \n" +
            "FROM APARTMENT_DISTANCE ad \n" +
            "WHERE `start` = :start \n" +
            "AND destination IN (\n" +
            "\tSELECT apartment_id \n" +
            "\tFROM SCHEDULE s \n" +
            "\tWHERE `sequence` IS NULL \n" +
            "\tGROUP BY apartment_id \n" +
            "\tHAVING COUNT(DISTINCT technician_id) = :no_of_technician - 1 \n" +
            "\tORDER BY COUNT(DISTINCT technician_id) DESC \n" +
            ") \n" +
            "ORDER BY distance ASC \n" +
            "LIMIT 0, 1")
    Integer findNearestSameApartment(@Param("start") Integer start, @Param("no_of_technician") Integer noOfTechnician);

    @Query(nativeQuery = true, value = "SELECT destination " +
            "FROM APARTMENT_DISTANCE ad " +
            "WHERE `start` = :start " +
            "AND destination IN :destination " +
            "ORDER BY distance ASC " +
            "LIMIT 1")
    Integer findNearest(@Param("start") Integer start, @Param("destination") List<Integer> destination);
}
