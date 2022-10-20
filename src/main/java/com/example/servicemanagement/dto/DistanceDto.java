package com.example.servicemanagement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class DistanceDto {
    @JsonProperty("destination_addresses")
    private String[] destinationAddresses;
    @JsonProperty("origin_addresses")
    private String[] originAddresses;
    private List<RowDto> rows;
    private String status;
}
