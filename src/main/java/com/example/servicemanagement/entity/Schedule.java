package com.example.servicemanagement.entity;

import lombok.*;
import org.apache.coyote.RequestInfo;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;

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

    private Date serviceDateTime;

    @OneToOne
    @JoinColumn(name = "apartment_id")
    private Apartment apartment;
}
