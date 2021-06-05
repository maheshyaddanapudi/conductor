package com.netflix.conductor.mongo.dao;

import javax.validation.constraints.NotNull;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.testcontainers.containers.MongoDBContainer;

import com.mongodb.client.MongoClients;

@TestConfiguration
@EnableMongoRepositories(basePackages = {"com.netflix.conductor.mongo.repositories"})
@Import({MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
public class TestConfig {
	
	private static final MongoDBContainer MONGO_DB_CONTAINER =
  		  new MongoDBContainer("mongo:4.4.6");
  
    private static final String MONGO_INITDB_DATABASE = "conductor";
	

	@BeforeAll
	public static void setUpAll() {
        MONGO_DB_CONTAINER.withEnv("MONGO_INITDB_DATABASE", MONGO_INITDB_DATABASE).start();
    }
    
    @AfterAll
    public static void tearDownAll() {
      if (!MONGO_DB_CONTAINER.isShouldBeReused()) {
        MONGO_DB_CONTAINER.stop();
      }
    }
    
    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    	  @Override
    	  public void initialize(@NotNull ConfigurableApplicationContext configurableApplicationContext) {
    	    TestPropertyValues.of(
    	      String.format("spring.data.mongodb.uri: %s", MONGO_DB_CONTAINER.getReplicaSetUrl())
    	    ).applyTo(configurableApplicationContext);
    	  }
    	}
    
    @Bean
	  public MongoTemplate mongoTemplate() {
	  	return new MongoTemplate(MongoClients.create(MONGO_DB_CONTAINER.getReplicaSetUrl()), MONGO_INITDB_DATABASE);
	  }
}
