package com.example.servicemanagement.dto;

import com.example.servicemanagement.entity.Apartment;
import com.example.servicemanagement.entity.Request;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TechnicianPlanDto {
    private Integer requestId;
    private Integer requestTypeId;
    private Integer apartmentId;
    private Integer tenantId;
    private Integer priority;
    private Integer estimateTime;

    private Request request;
    private Apartment apartment;
}
