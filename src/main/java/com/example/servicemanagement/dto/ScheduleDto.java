package com.example.servicemanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ScheduleDto {
    private Integer id;
    private Integer requestId;
    private String apartmentName;
    private String roomNo;
    private String requestType;
}
