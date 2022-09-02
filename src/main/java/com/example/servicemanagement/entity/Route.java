package com.example.servicemanagement.entity;

import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "ROUTE")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "apartment_id")
    private Apartment apartment;

    private Integer sequence;
}
