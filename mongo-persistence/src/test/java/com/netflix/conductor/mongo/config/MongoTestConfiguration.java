package com.netflix.conductor.mongo.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MongoDBContainer;

import com.mongodb.client.MongoClients;

@TestConfiguration
@EnableMongoRepositories(basePackages = {"com.netflix.conductor.mongo.repositories"})
@Import({MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@TestPropertySource(properties = {"spring.main.allow-bean-definition-overriding=true"})
public class MongoTestConfiguration {
	
	private static final MongoDBContainer MONGO_DB_CONTAINER =
  		  new MongoDBContainer("mongo:3.6.23");
	
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
    
    @Bean
      public MongoTemplate mongoTemplate() {
	  	return new MongoTemplate(MongoClients.create(MONGO_DB_CONTAINER.getReplicaSetUrl()), MONGO_INITDB_DATABASE);
	  }
}
