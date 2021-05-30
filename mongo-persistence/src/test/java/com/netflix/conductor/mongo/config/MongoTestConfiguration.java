package com.netflix.conductor.mongo.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
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
		
    	MongoDBContainer mongoContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.2.8"))
    			.withNetwork(Network.newNetwork())
    			.withNetworkAliases("mongo")
    			.withStartupTimeout(Duration.ofSeconds(900))
    			.withExposedPorts(27017)
    			//.withCommand("--replSet rs0 --bind_ip localhost,mongo")
    			.withEnv(envMap);
		
		starMongoContainer(mongoContainer);
		
		/*GenericContainer genericContainer = new GenericContainer("mongo:4.0.8")
                .withExposedPorts(27017)
                .withCommand("--replSet rs0 --bind_ip localhost,M1");
		
		starGenericContainer(genericContainer);*/
		
		return mongoContainer;
    
    }
	
	private void starMongoContainer(MongoDBContainer mongoContainer) {
		mongoContainer.start();
		/*try {
			mongoContainer.execInContainer("/bin/bash", "-c",
                    "mongo --eval 'printjson(rs.initiate({_id:\"rs0\","
                    + "members:[{_id:0,host:\"mongo:27017\"}]}))' "
                    + "--quiet");
			mongoContainer.execInContainer("/bin/bash", "-c",
                    "until mongo --eval \"printjson(rs.isMaster())\" | grep ismaster | grep true > /dev/null 2>&1;"
                    + "do sleep 1;done");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initiate rs.", e);
        }*/
	}
	
	private void starGenericContainer(GenericContainer genericContainer) {
		genericContainer.start();
	}
	
	@Bean("mongoClient")
	@DependsOn("mongoContainer")
    public MongoClient mongo() {
		MongoDBContainer mongoContainer = mongoContainer();
		String url = "mongodb://conductor:conductor@"+mongoContainer.getContainerIpAddress()+":"+mongoContainer.getFirstMappedPort()+"/?authSource=admin";
        return MongoClients.create(mongoContainer.getReplicaSetUrl("conductor"));
    }

    @Bean
    @DependsOn("mongoClient")
    public MongoTemplate mongoTemplate() throws Exception {
        return new MongoTemplate(mongo(), "conductor");
    }
	
}
