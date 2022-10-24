package com.example.servicemanagement.repository;

import com.example.servicemanagement.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TenantRepository extends JpaRepository<Tenant, Integer> {
    Tenant findTenantById(Integer id);

    Tenant findTenantByUserId(Integer userId);

    List<Tenant> findTenantsByApartmentIdOrderByRoomNoAsc(Integer apartmentId);

    @Query(nativeQuery = true, value = "SELECT CASE WHEN COUNT(*) > 0 THEN 'true' ELSE 'false' END " +
            "FROM TENANT t " +
            "INNER JOIN `USER` u ON t.user_id = u.id " +
            "WHERE t.id != :tenant_id " +
            "AND ((room_no = :room_no AND apartment_id = :apartment_id) " +
            "OR username = :username)")
    boolean checkUpdateDuplicate(@Param("tenant_id") Integer tenantId, @Param("room_no") String roomNo, @Param("apartment_id") Integer apartmentId, @Param("username") String username);

    @Query(nativeQuery = true, value = "SELECT CASE WHEN COUNT(*) > 0 THEN 'true' ELSE 'false' END " +
            "FROM TENANT t " +
            "INNER JOIN `USER` u ON t.user_id = u.id " +
            "WHERE (room_no = :room_no AND apartment_id = :apartment_id) " +
            "OR username = :username")
    boolean checkCreateDuplicate(@Param("room_no") String roomNo, @Param("apartment_id") Integer apartmentId, @Param("username") String username);

    @Query(nativeQuery = true, value = "SELECT CASE WHEN COUNT(*) > 0 THEN 'false' ELSE 'true' END " +
            "FROM REQUEST " +
            "WHERE user_id = :user_id " +
            "AND STATUS != 'DONE'")
    boolean checkCanDelete(@Param("user_id") Integer userId);
}
