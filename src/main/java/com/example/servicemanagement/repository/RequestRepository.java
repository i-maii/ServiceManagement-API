package com.example.servicemanagement.repository;

import com.example.servicemanagement.entity.Request;
import com.example.servicemanagement.entity.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Integer> {
    Request findRequestById(Integer id);
    List<Request> findRequestsByStatus(String status);
    List<Request> findRequestsByStatusInOrderByEstimateTimeAscEstimateTechnicianAsc(List<String> status);
    List<Request> findRequestsByUserIdOrderByRequestDateDesc(Integer userId);
    List<Request> findRequestsByStatusAndRequestTypeIn(String status, List<RequestType> requestType);

    @Query(nativeQuery = true, value = "SELECT CASE WHEN COUNT(*) > 0 THEN 'true' ELSE 'false' END " +
            "FROM REQUEST " +
            "WHERE status <> 'DONE' " +
            "AND apartment_id = :apartment_id " +
            "AND request_type_id = :request_type_id")
    boolean checkCreateDuplicate(@Param("apartment_id") Integer apartmentId, @Param("request_type_id") Integer requestTypeId);
}
