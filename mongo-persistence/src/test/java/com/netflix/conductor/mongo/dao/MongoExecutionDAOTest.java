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
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.MongoDBContainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClients;
import com.netflix.conductor.common.config.TestObjectMapperConfiguration;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.dao.ExecutionDAO;
import com.netflix.conductor.dao.ExecutionDAOTest;

@ContextConfiguration(classes = {TestObjectMapperConfiguration.class})
@RunWith(SpringRunner.class)
@SpringBootTest(properties = {"spring.main.allow-bean-definition-overriding=true"})
@Import({MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
public class MongoExecutionDAOTest extends ExecutionDAOTest {

    private MongoExecutionDAO executionDAO;

    @Autowired
    public ObjectMapper objectMapper;

    @Rule
    public TestName name = new TestName();

    @Rule
    public ExpectedException expected = ExpectedException.none();
    
    private static final MongoDBContainer MONGO_DB_CONTAINER =
    		  new MongoDBContainer("mongo:3.6.23");
  	
  	  private static final String MONGO_INITDB_DATABASE = "conductor";
  	  
  	public MongoTemplate mongoTemplate;
	  
	  @Before
	  public void setup() {
		if(!MONGO_DB_CONTAINER.isRunning())
				MONGO_DB_CONTAINER.withEnv("MONGO_INITDB_DATABASE", MONGO_INITDB_DATABASE).start();
	  	
	  	mongoTemplate = new MongoTemplate(MongoClients.create(MONGO_DB_CONTAINER.getReplicaSetUrl()), MONGO_INITDB_DATABASE);
    	executionDAO = new MongoExecutionDAO(objectMapper, mongoTemplate);
    }
    
	  @Test
	    @Override
	  public void testTaskOps() {
	        List<Task> tasks = new LinkedList<>();
	        String workflowId = UUID.randomUUID().toString();

	        for (int i = 0; i < 3; i++) {
	            Task task = new Task();
	            task.setScheduledTime(1L);
	            task.setSeq(1);
	            task.setTaskId(workflowId + "_t" + i);
	            task.setReferenceTaskName("testTaskOps" + i);
	            task.setRetryCount(0);
	            task.setWorkflowInstanceId(workflowId);
	            task.setTaskDefName("testTaskOps" + i);
	            task.setStatus(Task.Status.IN_PROGRESS);
	            tasks.add(task);
	        }

	        for (int i = 0; i < 3; i++) {
	            Task task = new Task();
	            task.setScheduledTime(1L);
	            task.setSeq(1);
	            task.setTaskId("x" + workflowId + "_t" + i);
	            task.setReferenceTaskName("testTaskOps" + i);
	            task.setRetryCount(0);
	            task.setWorkflowInstanceId("x" + workflowId);
	            task.setTaskDefName("testTaskOps" + i);
	            task.setStatus(Task.Status.IN_PROGRESS);
	            getExecutionDAO().createTasks(Collections.singletonList(task));
	        }

	        List<Task> created = getExecutionDAO().createTasks(tasks);
	        assertEquals(tasks.size(), created.size());

	        List<Task> pending = getExecutionDAO().getPendingTasksForTaskType(tasks.get(0).getTaskDefName());
	        assertNotNull(pending);
	        assertEquals(2, pending.size());
	        //Pending list can come in any order.  finding the one we are looking for and then comparing
	        Task matching = pending.stream().filter(task -> task.getTaskId().equals(tasks.get(0).getTaskId())).findAny()
	            .get();
	        assertTrue(EqualsBuilder.reflectionEquals(matching, tasks.get(0)));

	        for (int i = 0; i < 3; i++) {
	            Task found = getExecutionDAO().getTask(workflowId + "_t" + i);
	            assertNotNull(found);
	            found.getOutputData().put("updated", true);
	            found.setStatus(Task.Status.COMPLETED);
	            getExecutionDAO().updateTask(found);
	        }

	        List<String> taskIds = tasks.stream().map(Task::getTaskId).collect(Collectors.toList());
	        List<Task> found = getExecutionDAO().getTasks(taskIds);
	        assertEquals(taskIds.size(), found.size());
	        found.forEach(task -> {
	            assertTrue(task.getOutputData().containsKey("updated"));
	            assertEquals(true, task.getOutputData().get("updated"));
	            boolean removed = getExecutionDAO().removeTask(task.getTaskId());
	            assertTrue(removed);
	        });

	        found = getExecutionDAO().getTasks(taskIds);
	        assertTrue(found.isEmpty());
	    }

	    @Test
	    @Override
	    public void complexExecutionTest() {
	        Workflow workflow = createTestWorkflow();
	        int numTasks = workflow.getTasks().size();

	        String workflowId = getExecutionDAO().createWorkflow(workflow);
	        assertEquals(workflow.getWorkflowId(), workflowId);

	        List<Task> created = getExecutionDAO().createTasks(workflow.getTasks());
	        assertEquals(workflow.getTasks().size(), created.size());

	        Workflow workflowWithTasks = getExecutionDAO().getWorkflow(workflow.getWorkflowId(), true);
	        assertEquals(workflowId, workflowWithTasks.getWorkflowId());
	        assertEquals(numTasks, workflowWithTasks.getTasks().size());

	        Workflow found = getExecutionDAO().getWorkflow(workflowId, false);
	        assertTrue(found.getTasks().isEmpty());

	        workflow.getTasks().clear();
	        assertEquals(workflow, found);

	        workflow.getInput().put("updated", true);
	        getExecutionDAO().updateWorkflow(workflow);
	        found = getExecutionDAO().getWorkflow(workflowId);
	        assertNotNull(found);
	        assertTrue(found.getInput().containsKey("updated"));
	        assertEquals(true, found.getInput().get("updated"));

	        List<String> running = getExecutionDAO()
	            .getRunningWorkflowIds(workflow.getWorkflowName(), workflow.getWorkflowVersion());
	        assertNotNull(running);
	        assertTrue(running.isEmpty());

	        workflow.setStatus(Workflow.WorkflowStatus.RUNNING);
	        getExecutionDAO().updateWorkflow(workflow);

	        running = getExecutionDAO().getRunningWorkflowIds(workflow.getWorkflowName(), workflow.getWorkflowVersion());
	        assertNotNull(running);
	        assertEquals(1, running.size());
	        assertEquals(workflow.getWorkflowId(), running.get(0));

	        List<Workflow> pending = getExecutionDAO()
	            .getPendingWorkflowsByType(workflow.getWorkflowName(), workflow.getWorkflowVersion());
	        assertNotNull(pending);
	        assertEquals(1, pending.size());
	        assertEquals(3, pending.get(0).getTasks().size());
	        pending.get(0).getTasks().clear();
	        assertEquals(workflow, pending.get(0));

	        workflow.setStatus(Workflow.WorkflowStatus.COMPLETED);
	        getExecutionDAO().updateWorkflow(workflow);
	        running = getExecutionDAO().getRunningWorkflowIds(workflow.getWorkflowName(), workflow.getWorkflowVersion());
	        assertNotNull(running);
	        assertTrue(running.isEmpty());

	        List<Workflow> bytime = getExecutionDAO()
	            .getWorkflowsByType(workflow.getWorkflowName(), System.currentTimeMillis(),
	                System.currentTimeMillis() + 100);
	        assertNotNull(bytime);
	        assertTrue(bytime.isEmpty());

	        bytime = getExecutionDAO().getWorkflowsByType(workflow.getWorkflowName(), workflow.getCreateTime() - 10,
	            workflow.getCreateTime() + 10);
	        assertNotNull(bytime);
	        assertEquals(1, bytime.size());
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

     @Override
     public ExecutionDAO getExecutionDAO() {
         return executionDAO;
     }
 }
