package com.netflix.conductor.mongo.util;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;

public class TestUtil {

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;

    public TestUtil(MongoDBContainer mongoContainer, ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;

        mongoContainer = new MongoDBContainer(DockerImageName.parse("mongo"));
    	mongoContainer.addEnv("MONGO_INITDB_ROOT_USERNAME", "conductor");
    	mongoContainer.addEnv("MONGO_INITDB_ROOT_PASSWORD", "conductor");
    	mongoContainer.addEnv("MONGO_INITDB_DATABASE", "conductor");
    	
    	mongoContainer.start();
    	
		String url = "mongodb://conductor:conductor@"+mongoContainer.getContainerIpAddress()+":"+mongoContainer.getFirstMappedPort()+"/conductor";
        ConnectionString connectionString = new ConnectionString(url);
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
          .applyConnectionString(connectionString)
          .build();
        
        this.mongoTemplate = new MongoTemplate(MongoClients.create(mongoClientSettings), "conductor");
    }
    
    public MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }


}
