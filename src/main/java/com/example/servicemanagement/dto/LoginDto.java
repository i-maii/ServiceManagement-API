package com.example.servicemanagement.dto;

import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class LoginDto {
    private String username;
    private String password;
}
