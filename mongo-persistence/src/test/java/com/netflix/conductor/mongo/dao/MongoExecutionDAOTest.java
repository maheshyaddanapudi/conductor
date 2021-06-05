/*
 *  Copyright 2021 Netflix, Inc.
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *  <p>
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.mongo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.MongoDBContainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClients;
import com.netflix.conductor.common.config.TestObjectMapperConfiguration;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.dao.ExecutionDAO;
import com.netflix.conductor.dao.ExecutionDAOTest;

@ContextConfiguration(classes = {TestObjectMapperConfiguration.class})
@RunWith(SpringRunner.class)
@SpringBootTest
public class MongoExecutionDAOTest extends ExecutionDAOTest {

    private MongoExecutionDAO executionDAO;

    @Autowired
    public ObjectMapper objectMapper;

    @Rule
    public TestName name = new TestName();

    @Rule
    public ExpectedException expected = ExpectedException.none();
    
    private static final MongoDBContainer MONGO_DB_CONTAINER =
    		  new MongoDBContainer("mongo:4.2.8");
    
    private static final String MONGO_INITDB_DATABASE = "conductor";
  		  

    @BeforeAll
    static void setUpAll() {
        MONGO_DB_CONTAINER.withEnv("MONGO_INITDB_DATABASE", MONGO_INITDB_DATABASE).start();
    }
    
    @Bean
    public MongoTemplate mongoTemplate() {
    	return new MongoTemplate(MongoClients.create(MONGO_DB_CONTAINER.getReplicaSetUrl()), MONGO_INITDB_DATABASE);
    }

    @AfterAll
    static void tearDownAll() {
      if (!MONGO_DB_CONTAINER.isShouldBeReused()) {
        MONGO_DB_CONTAINER.stop();
      }
    }
    
    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    	  @Override
    	  public void initialize(@NotNull ConfigurableApplicationContext configurableApplicationContext) {
    	    TestPropertyValues.of(
    	      String.format("spring.data.mongodb.uri: %s", MONGO_DB_CONTAINER.getReplicaSetUrl())
    	    ).applyTo(configurableApplicationContext);
    	  }
    	}
    
    @Before
    public void setup() {
    	
    	executionDAO = new MongoExecutionDAO(objectMapper);
    }
    
    @Test
    public void testPendingByCorrelationId() {

        WorkflowDef def = new WorkflowDef();
        def.setName("pending_count_correlation_jtest");

        Workflow workflow = createTestWorkflow();
        workflow.setWorkflowDefinition(def);

        generateWorkflows(workflow, 10);

        List<Workflow> bycorrelationId = getExecutionDAO()
            .getWorkflowsByCorrelationId("pending_count_correlation_jtest", "corr001", true);
        assertNotNull(bycorrelationId);
        assertEquals(10, bycorrelationId.size());
    }

    @Test
    public void testRemoveWorkflow() {
        WorkflowDef def = new WorkflowDef();
        def.setName("workflow");

        Workflow workflow = createTestWorkflow();
        workflow.setWorkflowDefinition(def);

        List<String> ids = generateWorkflows(workflow, 1);

        assertEquals(1, getExecutionDAO().getPendingWorkflowCount("workflow"));
        ids.forEach(wfId -> getExecutionDAO().removeWorkflow(wfId));
        assertEquals(0, getExecutionDAO().getPendingWorkflowCount("workflow"));
    }

    @Override
    public ExecutionDAO getExecutionDAO() {
        return executionDAO;
    }
}
