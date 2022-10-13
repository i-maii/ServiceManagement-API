package com.example.servicemanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class EstimateRequestDto {
    private Integer requestId;
    private String roomNo;
    private String requestType;
    private Integer priority;
    private Integer estimateTechnician;
    private Integer estimateTime;
}
