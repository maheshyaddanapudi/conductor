package com.netflix.conductor.mongo.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

@TestConfiguration
public class MongoTestConfiguration {

	@SuppressWarnings("resource")
	@Bean("mongoContainer")
	public MongoDBContainer mongoContainer() {
		
		Map<String, String> envMap = new HashMap<String,String>();
		
		envMap.put("MONGO_INITDB_ROOT_USERNAME", "conductor");
		envMap.put("MONGO_INITDB_ROOT_PASSWORD", "conductor");
		envMap.put("MONGO_INITDB_DATABASE", "conductor");
		
    	MongoDBContainer mongoContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.0.8"))
    			.withStartupTimeout(Duration.ofSeconds(900))
    			.withExposedPorts(27017)
    			.withCommand("--replSet rs0 --bind_ip localhost")
    			.withEnv(envMap);
		
		starMongoContainer(mongoContainer);
		
		return mongoContainer;
    
    }
	
	private void starMongoContainer(MongoDBContainer mongoContainer) {
		mongoContainer.start();
	}
	
	@Bean("mongoClient")
	@DependsOn("mongoContainer")
    public MongoClient mongo() {
		MongoDBContainer mongoContainer = mongoContainer();
		String url = "mongodb://conductor:conductor@"+mongoContainer.getHost()+":"+mongoContainer.getFirstMappedPort()+"/?authSource=admin";
        ConnectionString connectionString = new ConnectionString(url);
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
          .applyConnectionString(connectionString)
          .build();
        return MongoClients.create(mongoClientSettings);
    }

    @Bean
    @DependsOn("mongoClient")
    public MongoTemplate mongoTemplate() throws Exception {
        return new MongoTemplate(mongo(), "conductor");
    }
	
}
