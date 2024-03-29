package com.example.healthcheck.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestTemplate;

import com.example.healthcheck.service.alarm.AlarmSender;
import com.example.healthcheck.service.alarm.MailAlarmSender;
import com.example.healthcheck.service.alarm.MessageMaker;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class AppConfig {

	private final JavaMailSender javaMailSender;

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public MessageMaker messageMaker() {
		return new MessageMaker(javaMailSender);
	}

	@Bean
	public AlarmSender mailAlarmSender() {
		return new MailAlarmSender(javaMailSender, messageMaker());
	}
}
