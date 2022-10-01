package com.example.servicemanagement.service;

import com.example.servicemanagement.dto.TechnicianPlanDto;
import com.example.servicemanagement.entity.Schedule;
import com.example.servicemanagement.entity.Technician;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

import static com.example.servicemanagement.constant.Constant.STATUS_READY_TO_SERVICE;

@Service
@Transactional
public class StgScheduleService {

    @PersistenceContext
    private EntityManager entityManager;

    public void prepareSchedule(List<Integer> technicianIds) {
        Query insertQuery = this.entityManager.createNativeQuery(
                "INSERT INTO STG_SCHEDULE " +
                        "SELECT * " +
                        "FROM SCHEDULE s " +
                        "WHERE request_id NOT IN (" +
                        "   SELECT id " +
                        "   FROM REQUEST r " +
                        "   WHERE estimate_technician = 2 " +
                        ") " +
                        "AND technician_id IN :technician_id"
        );
        insertQuery.setParameter("technician_id", technicianIds);
        insertQuery.executeUpdate();

        Query deleteQuery = this.entityManager.createNativeQuery(
                "DELETE FROM SCHEDULE " +
                        "WHERE request_id IN (" +
                        "   SELECT request_id " +
                        "   FROM STG_SCHEDULE stg)"
        );
        deleteQuery.executeUpdate();
    }

    public List<Schedule> prepareRequire1Schedule() {
        Query selectQuery = this.entityManager.createNativeQuery(
                "SELECT * " +
                        "FROM STG_SCHEDULE"
        );
        List<Schedule> schedules = selectQuery.getResultList();

        Query insertQuery = this.entityManager.createNativeQuery(
                "INSERT INTO SCHEDULE " +
                        "SELECT * " +
                        "FROM STG_SCHEDULE"
        );
        insertQuery.executeUpdate();

        Query deleteQuery = this.entityManager.createNativeQuery("TRUNCATE TABLE STG_SCHEDULE");
        deleteQuery.executeUpdate();

        return schedules;
    }

    public void saveToTemp() {
        Query insertQuery = this.entityManager.createNativeQuery(
                "INSERT INTO STG_SCHEDULE " +
                        "SELECT * " +
                        "FROM SCHEDULE"
        );
        insertQuery.executeUpdate();
    }

    public void saveBestRequest() {
        Query insertQuery = this.entityManager.createNativeQuery(
                "INSERT INTO SCHEDULE " +
                        "SELECT * " +
                        "FROM STG_SCHEDULE"
        );
        insertQuery.executeUpdate();

        Query updateQuery = this.entityManager.createNativeQuery(
                "UPDATE REQUEST " +
                        "SET `STATUS` = :status " +
                        "WHERE ID IN ( " +
                        "   SELECT DISTINCT REQUEST_ID FROM SCHEDULE WHERE REQUEST_ID IS NOT NULL" +
                        ")"
        );
        updateQuery.setParameter("status", STATUS_READY_TO_SERVICE);
        updateQuery.executeUpdate();

        Query deleteQuery = this.entityManager.createNativeQuery("TRUNCATE TABLE STG_SCHEDULE");
        deleteQuery.executeUpdate();
    }

    public void truncateSchedule() {
        Query deleteQuery = this.entityManager.createNativeQuery("TRUNCATE TABLE SCHEDULE");
        deleteQuery.executeUpdate();
    }

    public void truncateStgSchedule() {
        Query deleteQuery = this.entityManager.createNativeQuery("TRUNCATE TABLE STG_SCHEDULE");
        deleteQuery.executeUpdate();
    }

    public void deleteSchedule() {
        Query deleteQuery = this.entityManager.createNativeQuery("DELETE FROM SCHEDULE");
        deleteQuery.executeUpdate();
    }
}
