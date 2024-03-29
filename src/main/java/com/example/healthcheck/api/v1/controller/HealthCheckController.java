package com.example.healthcheck.api.v1.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.healthcheck.api.v1.response.Response;
import com.example.healthcheck.service.health.HealthCheckManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/check")
@RequiredArgsConstructor
public class HealthCheckController {

	private final HealthCheckManager healthCheckManager;

	@PostMapping("/{serverId}")
	public Response<Void> check(@PathVariable final Long serverId) {
		healthCheckManager.check(serverId);

		return Response.success();
	}
}
