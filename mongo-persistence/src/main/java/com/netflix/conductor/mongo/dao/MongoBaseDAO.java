package com.netflix.conductor.mongo.dao;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.metrics.Monitors;
import com.netflix.conductor.mongo.config.MongoProperties;

public abstract class MongoBaseDAO {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	String DAO_NAME = "mongo";

    private final ObjectMapper objectMapper;
    protected final MongoProperties properties;

    public MongoBaseDAO(ObjectMapper objectMapper, MongoProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }
    
    String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    
    <T> T readValue(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void recordMongoDaoRequests(String action) {
        recordMongoDaoRequests(action, "n/a", "n/a");
    }

    void recordMongoDaoRequests(String action, String taskType, String workflowType) {
        Monitors.recordDaoRequests(DAO_NAME, action, taskType, workflowType);
    }

    void recordMongoDaoEventRequests(String action, String event) {
        Monitors.recordDaoEventRequests(DAO_NAME, action, event);
    }

    void recordMongoDaoPayloadSize(String action, int size, String taskType, String workflowType) {
        Monitors.recordDaoPayloadSize(DAO_NAME, action, StringUtils.defaultIfBlank(taskType, ""),
            StringUtils.defaultIfBlank(workflowType, ""), size);
    }
}
