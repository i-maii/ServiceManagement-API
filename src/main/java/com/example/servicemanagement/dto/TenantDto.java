package com.example.servicemanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TenantDto {
    private Integer tenantId;
    private String roomNo;
    private String name;
    private String phoneNo;
    private String password;
}
