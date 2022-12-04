package com.example.servicemanagement.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;

import static com.example.servicemanagement.constant.Constant.STATUS_READY_TO_SERVICE;

@Service
@Transactional
public class StgScheduleService {

    @PersistenceContext
    private EntityManager entityManager;

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
