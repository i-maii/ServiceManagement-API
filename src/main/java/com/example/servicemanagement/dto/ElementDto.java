package com.example.servicemanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ElementDto {
    private ObjectDto distance;
    private ObjectDto duration;
    private String status;
}
