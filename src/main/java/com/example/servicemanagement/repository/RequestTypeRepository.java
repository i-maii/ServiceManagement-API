package com.example.servicemanagement.repository;

import com.example.servicemanagement.entity.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RequestTypeRepository extends JpaRepository<RequestType, Integer> {
    RequestType findRequestTypeById(Integer id);

    List<RequestType> findRequestTypeByRoleName(String roleName);

    List<RequestType> findRequestTypeByRoleNameIsNot(String roleName);

    @Query(nativeQuery = true, value = "SELECT CASE WHEN COUNT(*) > 0 THEN 'true' ELSE 'false' END " +
            "FROM REQUEST_TYPE " +
            "WHERE name = :name " +
            "AND role_id = :role_id " +
            "AND is_common_area = :common_area")
    boolean checkCreateDuplicate(@Param("name") String name, @Param("role_id") Integer roleId, @Param("common_area") boolean commonArea);

    @Query(nativeQuery = true, value = "SELECT CASE WHEN COUNT(*) > 0 THEN 'true' ELSE 'false' END " +
            "FROM REQUEST_TYPE " +
            "WHERE id != :id " +
            "AND name = :name " +
            "AND role_id = :role_id " +
            "AND is_common_area = :common_area")
    boolean checkUpdateDuplicate(@Param("id") Integer id, @Param("name") String name, @Param("role_id") Integer roleId, @Param("common_area") boolean commonArea);

    List<RequestType> findRequestTypesByIdIn(List<Integer> id);

    @Query(nativeQuery = true, value = "SELECT CASE WHEN COUNT(*) > 0 THEN 'false' ELSE 'true' END " +
            "FROM REQUEST " +
            "WHERE request_type_id = :request_type_id " +
            "AND STATUS != 'DONE'")
    boolean checkCanDelete(@Param("request_type_id") Integer requestTypeId);

    List<RequestType> findRequestTypesByCommonArea(boolean isCommonArea);

    List<RequestType> findRequestTypesByOrderByCommonAreaAscNameAsc();
}
