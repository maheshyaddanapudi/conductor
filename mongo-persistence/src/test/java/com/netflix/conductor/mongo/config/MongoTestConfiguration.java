/*
 * Copyright 2020 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.netflix.conductor.mongo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MongoDBContainer;

import com.mongodb.client.MongoClients;

@TestConfiguration
@TestPropertySource(properties = {"spring.main.allow-bean-definition-overriding=true"})
public class MongoTestConfiguration {
	
	private static final MongoDBContainer MONGO_DB_CONTAINER =
  		  new MongoDBContainer("mongo:3.6.23");
	
    private static final String MONGO_INITDB_DATABASE = "conductor";
	
      @Bean
      public MongoTemplate mongoTemplate() {
    	
		if(!MONGO_DB_CONTAINER.isRunning())
			MONGO_DB_CONTAINER.withEnv("MONGO_INITDB_DATABASE", MONGO_INITDB_DATABASE).start();
		
	  	return new MongoTemplate(MongoClients.create(MONGO_DB_CONTAINER.getReplicaSetUrl()), MONGO_INITDB_DATABASE);
	  }
}
