package com.example.servicemanagement.repository;

import com.example.servicemanagement.entity.Config;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfigRepository extends JpaRepository<Config, Integer> {
    Config findConfigByKey(String key);
}
