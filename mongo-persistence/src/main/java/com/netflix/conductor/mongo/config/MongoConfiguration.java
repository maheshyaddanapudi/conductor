package com.netflix.conductor.mongo.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
 
	/*@Bean
    public MongoDbFactory mongoDbFactory(MongoProperties properties) throws MongoException, UnknownHostException {
        return new SimpleMongoDbFactory(new MongoClientURI("mongodb://"+properties.getUsername()+":"+properties.getPassword()+"@"+properties.getHost()+":"+properties.getPort()+"/"+properties.getDatabase()));
    }
 
    @Bean
    public MongoTemplate mongoTemplate(MongoProperties properties) throws MongoException, UnknownHostException {
        return new MongoTemplate(mongoDbFactory(properties));
    }*/
 
   	@Bean
    public MetadataDAO mongoMetadataDAO(ObjectMapper objectMapper) {
        return new MongoMetadataDAO(objectMapper);
    }

    @Bean
    public ExecutionDAO mongoExecutionDAO(ObjectMapper objectMapper) {
        return new MongoExecutionDAO(objectMapper);
    }

    @Bean
    public QueueDAO mongoQueueDAO(ObjectMapper objectMapper) {
        return new MongoQueueDAO(objectMapper);
    }
}