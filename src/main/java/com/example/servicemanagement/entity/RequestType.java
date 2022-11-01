package com.example.servicemanagement.entity;

import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "REQUEST_TYPE")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class RequestType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    private Integer priority;

    @OneToOne
    @JoinColumn(name = "role_id")
    private Role role;

    @Column(name = "is_common_area")
    private boolean commonArea;
}
