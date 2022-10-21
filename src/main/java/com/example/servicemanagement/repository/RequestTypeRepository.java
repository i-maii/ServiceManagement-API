package com.example.servicemanagement.repository;

import com.example.servicemanagement.entity.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RequestTypeRepository extends JpaRepository<RequestType, Integer> {
    RequestType findRequestTypeById(Integer id);

    List<RequestType> findRequestTypeByRoleName(String roleName);

    @Query(nativeQuery = true, value = "SELECT CASE WHEN COUNT(*) > 0 THEN 'true' ELSE 'false' END " +
            "FROM REQUEST_TYPE " +
            "WHERE name = :name")
    boolean checkCreateDuplicate(@Param("name") String name);

    @Query(nativeQuery = true, value = "SELECT CASE WHEN COUNT(*) > 0 THEN 'true' ELSE 'false' END " +
            "FROM REQUEST_TYPE " +
            "WHERE id != :id AND name = :name")
    boolean checkUpdateDuplicate(@Param("id") Integer id, @Param("name") String name);
}
