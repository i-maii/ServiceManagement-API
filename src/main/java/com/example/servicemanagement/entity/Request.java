package com.example.servicemanagement.entity;

import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "REQUEST")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "request_type_id")
    private RequestType requestType;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @OneToOne
    @JoinColumn(name = "apartment_id")
    private Apartment apartment;

    private String status;

    private String detail;

    private Integer estimateTime;

    private Date requestDate;

    private Integer estimateTechnician;

    private Integer priority;

    private Date serviceDate;
}
