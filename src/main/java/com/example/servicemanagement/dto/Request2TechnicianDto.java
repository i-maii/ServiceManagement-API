package com.example.servicemanagement.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Request2TechnicianDto {
    private Integer requestTypeId;
    private boolean technician1;
    private boolean technician2;
    private boolean technician3;
}
