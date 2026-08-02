package com.bidstream.application.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@ComponentScan(basePackages = "com.bidstream.application")
@EnableMethodSecurity
public class ApplicationConfig {}
