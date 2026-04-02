package com.azadi.config;

import com.google.cloud.spring.data.datastore.repository.config.EnableDatastoreRepositories;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableDatastoreRepositories(basePackages = "com.azadi")
public class DatastoreConfig {
}
