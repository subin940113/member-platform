package com.example.auth.infrastructure.login;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LoginLockProperties.class)
public class LoginLockPropertiesConfig {}
