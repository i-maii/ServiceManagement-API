package com.example.servicemanagement.dto;

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
}
