package com.example.member.infrastructure.register;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RegisterProperties.class)
public class RegisterPropertiesConfig {}
