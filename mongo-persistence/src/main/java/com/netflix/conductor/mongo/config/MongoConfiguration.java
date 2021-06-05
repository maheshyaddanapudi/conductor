package com.netflix.conductor.mongo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.dao.ExecutionDAO;
import com.netflix.conductor.dao.MetadataDAO;
import com.netflix.conductor.dao.QueueDAO;
import com.netflix.conductor.mongo.dao.MongoExecutionDAO;
import com.netflix.conductor.mongo.dao.MongoMetadataDAO;
import com.netflix.conductor.mongo.dao.MongoQueueDAO;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "conductor.db.type", havingValue = "mongo")
@EnableMongoRepositories(basePackages = {"com.netflix.conductor.mongo.repositories"})
@Import({MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
public class MongoConfiguration {
	
	@Autowired 
	MongoTemplate mongoTemplate;
	
	@Bean
    public MetadataDAO mongoMetadataDAO(ObjectMapper objectMapper,MongoTemplate mongoTemplate) {
        return new MongoMetadataDAO(objectMapper, mongoTemplate);
    }

    @Bean
    public ExecutionDAO mongoExecutionDAO(ObjectMapper objectMapper,MongoTemplate mongoTemplate) {
        return new MongoExecutionDAO(objectMapper, mongoTemplate);
    }

    @Bean
    public QueueDAO mongoQueueDAO(ObjectMapper objectMapper,MongoTemplate mongoTemplate) {
        return new MongoQueueDAO(objectMapper, mongoTemplate);
    }
}