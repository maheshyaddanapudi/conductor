package com.netflix.conductor.mongo.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@EnableConfigurationProperties(MongoProperties.class)
@ConditionalOnProperty(name = "conductor.db.type", havingValue = "mongo")
//@EnableJpaRepositories(basePackages = {"com.netflix.conductor.mongo.repositories"})
//@EntityScan("com.netflix.conductor.mongo.entities")
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
    public MetadataDAO mongoMetadataDAO(ObjectMapper objectMapper, MongoProperties properties) {
        return new MongoMetadataDAO(objectMapper, properties);
    }

    @Bean
    public ExecutionDAO mongoExecutionDAO(ObjectMapper objectMapper, MongoProperties properties) {
        return new MongoExecutionDAO(objectMapper, properties);
    }

    @Bean
    public QueueDAO mongoQueueDAO(ObjectMapper objectMapper, MongoProperties properties) {
        return new MongoQueueDAO(objectMapper, properties);
    }
}