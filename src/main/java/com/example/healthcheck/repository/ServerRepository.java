package com.example.healthcheck.repository;

import com.example.healthcheck.entity.server.Server;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerRepository extends JpaRepository<Server,Long> {
}
