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

        this.mongoTemplate = new MongoTemplate(MongoClients.create(mongoContainer.getReplicaSetUrl()), "test");
    }
    
    public MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }


}
