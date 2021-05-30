package com.netflix.conductor.mongo.dao;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.common.config.TestObjectMapperConfiguration;
import com.netflix.conductor.mongo.config.MongoTestConfiguration;

@ContextConfiguration(classes = {TestObjectMapperConfiguration.class, MongoTestConfiguration.class})
@RunWith(SpringRunner.class)
@DataMongoTest
public class MongoExecutionDAOTest {
	
	private MongoExecutionDAO dao;
	
	@Autowired
    private ObjectMapper objectMapper;
	
	@Autowired
    public MongoTemplate mongoTemplate;
	
	@Before
    public void setup() {
		dao = new MongoExecutionDAO(objectMapper);
    }
	
}
