package com.netflix.conductor.mongo.config;

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

	@Bean("mongoContainer")
	public MongoDBContainer mongoContainer() {
		
    	
    	MongoDBContainer mongoContainer = new MongoDBContainer(DockerImageName.parse("mongo"));
    	mongoContainer.addEnv("MONGO_INITDB_ROOT_USERNAME", "conductor");
    	mongoContainer.addEnv("MONGO_INITDB_ROOT_PASSWORD", "conductor");
    	mongoContainer.addEnv("MONGO_INITDB_DATABASE", "conductor");
		
		//starMongoContainer(mongoContainer);
		
		return mongoContainer;
    
    }
	
	private void starMongoContainer(MongoDBContainer mongoContainer) {
		mongoContainer.start();
	}
	
	/*@Bean
	@DependsOn("mongoContainer")
    public MongoClient mongo() {
		MongoDBContainer mongoContainer = mongoContainer();
		String url = "mongodb://conductor:conductor@"+mongoContainer.getHost()+":"+mongoContainer.getMappedPort(27017)+"/conductor";
        ConnectionString connectionString = new ConnectionString(url);
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
          .applyConnectionString(connectionString)
          .build();
        return MongoClients.create(mongoClientSettings);
    }

    @Bean
    public MongoTemplate mongoTemplate() throws Exception {
        return new MongoTemplate(mongo(), "conductor");
    }*/
	
}
