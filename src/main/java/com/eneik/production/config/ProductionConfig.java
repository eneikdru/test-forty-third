package com.eneik.production.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"com.eneik.generated", "com.eneik.production"})
public class ProductionConfig {
}
