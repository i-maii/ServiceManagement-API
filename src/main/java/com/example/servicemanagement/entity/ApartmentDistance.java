package com.example.servicemanagement.entity;

import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "APARTMENT_DISTANCE")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ApartmentDistance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer start;

    private Integer destination;

    private float distance;
}
