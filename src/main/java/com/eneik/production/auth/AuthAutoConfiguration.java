package com.eneik.production.auth;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@ComponentScan(basePackages = {"com.eneik.production.audit", "com.eneik.production.auth", "com.eneik.generated"})
@EnableJpaRepositories(basePackages = {"com.eneik.production.audit", "com.eneik.production.auth.repository", "com.eneik.generated.repository"})
@EntityScan(basePackages = {"com.eneik.production.audit", "com.eneik.production.auth.model", "com.eneik.generated.model"})
public class AuthAutoConfiguration {
}
