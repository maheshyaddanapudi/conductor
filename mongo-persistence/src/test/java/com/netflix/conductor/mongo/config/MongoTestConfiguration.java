package com.netflix.conductor.mongo.config;

import java.time.Duration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;

import com.mongodb.client.MongoClients;

@TestConfiguration
public class MongoTestConfiguration {
	
	@SuppressWarnings("resource")
	public MongoDBContainer mongoDbContainer()
	{
		MongoDBContainer mongoDbContainer = new MongoDBContainer("mongo:4.2.8")
	    		.withExposedPorts(27017).withEnv("MONGO_INITDB_DATABASE", "test").withStartupTimeout(Duration.ofSeconds(900));
		
		return mongoDbContainer;

	}

	@Bean
	public MongoTemplate mongoTemplate() {
		MongoDBContainer mongoDbContainer = mongoDbContainer();
		mongoDbContainer.start();
		MongoTemplate mongoTemplate = new MongoTemplate(MongoClients.create(mongoDbContainer.getReplicaSetUrl()), "test");
		return mongoTemplate;
	}
}
