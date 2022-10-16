package com.example.servicemanagement.repository;

import com.example.servicemanagement.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {

    @Query(nativeQuery = true, value = "SELECT IFNULL((SELECT s.apartment_id " +
            "FROM SCHEDULE s " +
            "WHERE s.technician_id = :technician_id " +
            "AND s.`sequence` IS NOT NULL " +
            "ORDER BY s.`sequence`DESC " +
            "LIMIT 1), 0)")
    Integer findLatestApartmentIdByTechnicianId(@Param("technician_id") Integer technicianId);

    @Query(nativeQuery = true, value = "SELECT IFNULL(MAX(s.sequence), 0) FROM SCHEDULE s WHERE s.technician_id = :technician_id")
    Integer findLatestSequenceByTechnicianId(@Param("technician_id") Integer technicianId);

    @Query(nativeQuery = true, value = "SELECT technician_id " +
            "FROM SCHEDULE s " +
            "GROUP BY technician_id " +
            "ORDER BY COUNT(DISTINCT apartment_id) DESC " +
            "LIMIT 1")
    Integer findDriver();

    @Query(nativeQuery = true, value = "SELECT DISTINCT s.apartment_id " +
            "FROM SCHEDULE s " +
            "WHERE s.technician_id IN (" +
            "   SELECT s2.technician_id " +
            "   FROM SCHEDULE s2 " +
            "   WHERE s2.`sequence` IS NULL " +
            "   GROUP BY s2.technician_id " +
            "   HAVING COUNT(DISTINCT s2.apartment_id) = 1 " +
            "   AND s2.technician_id != :driver " +
            ") " +
            "AND `sequence` IS NULL")
    List<Integer> findOneApartmentIds(@Param("driver") Integer driver);

    @Query(nativeQuery = true, value = "SELECT * " +
            "FROM SCHEDULE s " +
            "INNER JOIN ( " +
            "   SELECT * " +
            "   FROM APARTMENT_DISTANCE ad " +
            "   WHERE ad.`start` = :start AND ad.destination IN :destination" +
            "   ORDER BY ad.distance ASC " +
            "   LIMIT 1 " +
            ") AS nearest ON s.apartment_id = nearest.destination AND nearest.`start` = :start " +
            "WHERE s.`sequence` IS NULL")
    List<Schedule> findSchedulesNearestOneApartment(@Param("start") Integer start, @Param("destination") List<Integer> destination);

    @Query(nativeQuery = true, value = "SELECT * " +
            "FROM SCHEDULE s " +
            "INNER JOIN ( " +
            "   SELECT * " +
            "   FROM APARTMENT_DISTANCE ad " +
            "   WHERE ad.`start` = :start AND ad.destination IN :destination" +
            "   ORDER BY ad.distance ASC " +
            "   LIMIT 1 " +
            ") AS nearest ON s.apartment_id = nearest.destination AND nearest.`start` = :start " +
            "WHERE s.`sequence` IS NULL " +
            "AND s.technician_id IN :technician_id")
    List<Schedule> findSchedulesNearestOneApartmentByTechnician(@Param("start") Integer start, @Param("destination") List<Integer> destination, @Param("technician_id") List<Integer> technicianId);

    @Query(nativeQuery = true, value = "SELECT apartment_id " +
            "FROM SCHEDULE s " +
            "WHERE `sequence` IS NULL " +
            "GROUP BY apartment_id " +
            "HAVING COUNT(DISTINCT technician_id) = :no_of_technician " +
            "ORDER BY COUNT(DISTINCT technician_id) DESC")
    List<Integer> findSameApartmentIds(@Param("no_of_technician") Integer noOfTechnician);

    @Query(nativeQuery = true, value = "SELECT destination " +
            "FROM APARTMENT_DISTANCE ad " +
            "WHERE `start` = :latest_apartment_id " +
            "AND destination IN ( " +
            "   SELECT DISTINCT apartment_id " +
            "   FROM SCHEDULE s " +
            "   WHERE technician_id <> :driver" +
            "   AND s.`sequence` IS NULL " +
            ") " +
            "ORDER BY distance ASC " +
            "LIMIT 1")
    Integer findNearestApartmentId(@Param("latest_apartment_id") Integer apartmentId, @Param("driver") Integer driver);

    @Query(nativeQuery = true, value = "SELECT destination " +
            "FROM APARTMENT_DISTANCE ad " +
            "WHERE `start` = :latest_apartment_id " +
            "AND destination IN ( " +
            "   SELECT DISTINCT apartment_id " +
            "   FROM SCHEDULE s " +
            "   WHERE technician_id = :technician_id" +
            "   AND s.`sequence` IS NULL " +
            ") " +
            "ORDER BY distance ASC " +
            "LIMIT 1")
    Integer findNearestApartmentIdByTechnicianId(@Param("latest_apartment_id") Integer apartmentId, @Param("technician_id") Integer technicianId);

    List<Schedule> findSchedulesByApartmentIdAndSequenceIsNull(Integer apartmentId);

    List<Schedule> findSchedulesByTechnicianIdAndSequenceIsNull(Integer technicianId);

    List<Schedule> findSchedulesByApartmentIdAndTechnicianIdAndSequenceIsNull(Integer apartmentId, Integer technicianId);

    List<Schedule> findSchedulesByApartmentIdAndTechnicianIdInAndSequenceIsNull(Integer apartmentId, List<Integer> technicianId);

    @Query(nativeQuery = true, value = "SELECT IFNULL(SUM(request_hour), 0) " +
            "FROM SCHEDULE s " +
            "WHERE technician_id = :technician_id " +
            "AND s.`sequence` IS NOT NULL")
    Integer findTotalHourByTechnicianId(@Param("technician_id") Integer technicianId);

    @Query(nativeQuery = true, value = "SELECT CASE WHEN COUNT(*) > 0 THEN 'true' ELSE 'false' END " +
            "FROM SCHEDULE s " +
            "WHERE apartment_id IN :apartment_id " +
            "AND s.`sequence` IS NULL " +
            "AND technician_id = :technician_id")
    boolean checkDriverHaveSameApartment(@Param("apartment_id") List<Integer> apartmentId, @Param("technician_id") Integer technicianId);

    @Query(nativeQuery = true, value = "SELECT CASE WHEN COUNT(*) > 0 THEN 'true' ELSE 'false' END " +
            "FROM SCHEDULE s " +
            "WHERE request_id IN ( " +
            "   SELECT id " +
            "   FROM REQUEST r " +
            "   WHERE estimate_technician = 2 " +
            ")")
    boolean checkHaveRequire2();

    @Query(nativeQuery = true, value = "SELECT DISTINCT technician_id " +
            "FROM SCHEDULE s " +
            "WHERE request_id IN ( " +
            "   SELECT id " +
            "   FROM REQUEST r " +
            "   WHERE estimate_technician = 2 " +
            ")")
    List<Integer> findRequire2TechnicianId();

    @Query(nativeQuery = true, value = "SELECT CASE WHEN COUNT(*) = 0 THEN 'true' ELSE 'false' END " +
            "FROM SCHEDULE s " +
            "WHERE technician_id IN :technician_id " +
            "AND `sequence` IS NULL")
    boolean checkRequire2Finished(@Param("technician_id") List<Integer> technicianId);

    @Query(nativeQuery = true, value = "SELECT CASE WHEN COUNT(DISTINCT apartment_id) = 1 THEN 'true' ELSE 'false' END " +
            "FROM SCHEDULE s " +
            "WHERE `sequence` IS NULL " +
            "AND technician_id = :technician_id " +
            "GROUP BY technician_id")
    boolean checkOtherOneApartment(@Param("technician_id") Integer technicianId);

    @Query(nativeQuery = true, value = "SELECT apartment_id " +
            "FROM SCHEDULE s " +
            "WHERE `sequence` IS NULL " +
            "AND technician_id IN :technician_id " +
            "GROUP BY apartment_id " +
            "HAVING COUNT(DISTINCT technician_id) = 2")
    List<Integer> findDriverAndOtherSameApartment(@Param("technician_id") List<Integer> technicianId);

    @Query(nativeQuery = true, value = "SELECT COUNT(*) " +
            "FROM SCHEDULE s " +
            "GROUP BY technician_id, request_id " +
            "HAVING request_id IS NULL " +
            "ORDER BY COUNT(*) DESC " +
            "LIMIT 1")
    Integer findNumberOfRoute();

    @Query(nativeQuery = true, value = "SELECT CASE WHEN COUNT(*) > 1 THEN 'true' ELSE 'false' END " +
            "FROM SCHEDULE s " +
            "WHERE `sequence` IS NOT NULL")
    boolean checkAlreadyFindRoute();

    List<Schedule> findSchedulesByTechnicianIdOrderBySequence(Integer technicianId);

    List<Schedule> findSchedulesByRequestId(Integer requestId);

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM SCHEDULE WHERE technician_id = :technician_id AND `sequence` <= :sequence")
    int closeTask(@Param("technician_id") Integer technicianId, @Param("sequence") Integer sequence);

    Schedule findScheduleById(Integer requestId);

    @Query(nativeQuery = true, value = "SELECT technician_id " +
            "FROM SCHEDULE s " +
            "WHERE request_id IS NULL " +
            "GROUP BY technician_id " +
            "ORDER BY COUNT(*) DESC " +
            "LIMIT 1")
    Integer findRouteTechnicianId();

    List<Schedule> findSchedulesByTechnicianIdAndRequestIsNullOrderBySequenceAsc(Integer technicianId);
}
