package com.example.servicemanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class RequestDto {
    private Integer requestTypeId;
    private Integer tenantId;
    private String name;
    private String phoneNo;
    private String detail;
    private String image;
    private Date requestDate;
}
