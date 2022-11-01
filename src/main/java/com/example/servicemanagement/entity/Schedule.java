package com.example.servicemanagement.entity;

import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.sql.Time;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "SCHEDULE")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "technician_id")
    private Technician technician;

    @OneToOne
    @JoinColumn(name = "request_id")
    private Request request;

    @OneToOne
    @JoinColumn(name = "apartment_id")
    private Apartment apartment;

    private Integer sequence;

    private Integer requestHour;

    private Time serviceStartTime;
    private Time serviceEndTime;
}
