package com.example.servicemanagement.repository;

import com.example.servicemanagement.entity.ApartmentDistance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApartmentDistanceRepository extends JpaRepository<ApartmentDistance, Integer> {

    @Query(nativeQuery = true, value = "SELECT destination " +
            "FROM APARTMENT_DISTANCE ad " +
            "WHERE `start` = :start " +
            "AND destination IN :destination " +
            "ORDER BY distance ASC " +
            "LIMIT 1")
    Integer findNearest(@Param("start") Integer start, @Param("destination") List<Integer> destination);

    @Query(nativeQuery = true, value = "SELECT CASE WHEN distance <= 1 THEN 'true' ELSE 'false' END " +
            "FROM APARTMENT_DISTANCE ad " +
            "WHERE `start` = :start " +
            "AND destination = :destination")
    boolean checkCanWalk(@Param("start") Integer start, @Param("destination") Integer destination);

    ApartmentDistance findApartmentDistanceByStartAndDestination(Integer start, Integer destination);
}
