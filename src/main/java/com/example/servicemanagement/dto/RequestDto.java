package com.example.servicemanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class RequestDto {
    private Integer requestTypeId;
    private Integer userId;
    private String detail;
    private Integer apartmentId;
}
