package com.netflix.conductor.mongo.dao;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.netflix.conductor.annotations.Trace;
import com.netflix.conductor.common.metadata.events.EventExecution;
import com.netflix.conductor.common.metadata.tasks.PollData;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.core.exception.ApplicationException;
import com.netflix.conductor.dao.ExecutionDAO;
import com.netflix.conductor.dao.PollDataDAO;
import com.netflix.conductor.dao.RateLimitingDAO;
import com.netflix.conductor.metrics.Monitors;
import com.netflix.conductor.mongo.entities.EventExecutionDocument;
import com.netflix.conductor.mongo.entities.PollDataDocument;
import com.netflix.conductor.mongo.entities.TaskDataDocument;
import com.netflix.conductor.mongo.entities.TaskInProgressDocument;
import com.netflix.conductor.mongo.entities.TaskScheduledDocument;
import com.netflix.conductor.mongo.entities.WorkflowDefToWorkflowDocument;
import com.netflix.conductor.mongo.entities.WorkflowDocument;
import com.netflix.conductor.mongo.entities.WorkflowPendingDocument;
import com.netflix.conductor.mongo.entities.WorkflowToTaskDocument;
import com.netflix.conductor.mongo.repositories.MongoEventExecutionRepository;
import com.netflix.conductor.mongo.repositories.MongoPollDataRepository;
import com.netflix.conductor.mongo.repositories.MongoTaskInProgressRepository;
import com.netflix.conductor.mongo.repositories.MongoTaskScheduledRepository;
import com.netflix.conductor.mongo.repositories.MongoWorkflowDefToWorkflowRepository;
import com.netflix.conductor.mongo.repositories.MongoWorkflowPendingRepository;
import com.netflix.conductor.mongo.repositories.MongoWorkflowRepository;
import com.netflix.conductor.mongo.repositories.MongoWorkflowToTaskRepository;

@Trace
public class MongoExecutionDAO extends MongoBaseDAO implements ExecutionDAO, RateLimitingDAO, PollDataDAO {
	
	@Autowired
	MongoTaskScheduledRepository mongoTaskScheduledRepository;
	
	@Autowired
	MongoWorkflowToTaskRepository mongoWorkflowToTaskRepository;
	
	@Autowired
	MongoTaskInProgressRepository mongoTaskInProgressRepository;
	
	@Autowired
	MongoWorkflowRepository mongoWorkflowRepository;  
	
	@Autowired
	MongoWorkflowDefToWorkflowRepository workflowDefToWorkflowRepository;
	
	@Autowired
	MongoWorkflowPendingRepository mongoWorkflowPendingRepository;
	
	@Autowired
	MongoEventExecutionRepository mongoEventExecutionRepository;
	
	@Autowired
	MongoPollDataRepository mongoPollDataRepository;

	public MongoExecutionDAO(ObjectMapper objectMapper, MongoTemplate mongoTemplate) {
		super(objectMapper, mongoTemplate);
	}
	
	private static String dateStr(Long timeInMs) {
        Date date = new Date(timeInMs);
        return dateStr(date);
    }

    private static String dateStr(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        return format.format(date);
    }

	@Override
	public List<Task> getPendingTasksByWorkflow(String taskName, String workflowId) {
		
		List<Task> result = new ArrayList<Task>();
		
		Query searchQuery = new Query();
		searchQuery.addCriteria(Criteria.where("task_name").is(taskName).andOperator(Criteria.where("workflow_id").is(workflowId)));
        
		List<TaskInProgressDocument> inProgress = mongoTemplate.find(searchQuery, TaskInProgressDocument.class);
		
		if(!inProgress.isEmpty()) {
			Criteria mainCriteria = Criteria.where("task_id").is(inProgress.get(0).getTask_id());
			
			int total = inProgress.size();
			
			for(int counter = 1; counter < total ; counter++) {
				mainCriteria.orOperator(Criteria.where("task_id").is(inProgress.get(counter).getTask_id()));
			}
			
			mongoTemplate.find(new Query().addCriteria(mainCriteria), TaskDataDocument.class).forEach(tdd -> {
				result.add(readValue(tdd.getJson_data(), Task.class));
			});;
		}
		
    	return result;
    }

	@Override
	public List<Task> getTasks(String taskDefName, String startKey, int count) {
        List<Task> tasks = new ArrayList<>(count);

        List<Task> pendingTasks = getPendingTasksForTaskType(taskDefName);
        boolean startKeyFound = startKey == null;
        int found = 0;
        for (Task pendingTask : pendingTasks) {
            if (!startKeyFound) {
                if (pendingTask.getTaskId().equals(startKey)) {
                    startKeyFound = true;
                    // noinspection ConstantConditions
                    if (startKey != null) {
                        continue;
                    }
                }
            }
            if (startKeyFound && found < count) {
                tasks.add(pendingTask);
                found++;
            }
        }

        return tasks;
    }
	
	private static String taskKey(Task task) {
        return task.getReferenceTaskName() + "_" + task.getRetryCount();
    }

	@Override
	public List<Task> createTasks(List<Task> tasks) {
		
        List<Task> created = Lists.newArrayListWithCapacity(tasks.size());


        for (Task task : tasks) {
            validate(task);

            task.setScheduledTime(System.currentTimeMillis());

            final String taskKey = taskKey(task);

            boolean scheduledTaskAdded = addScheduledTask(task, taskKey);

            if (!scheduledTaskAdded) {
                logger.trace("Task already scheduled, skipping the run " + task.getTaskId() + ", ref="
                    + task.getReferenceTaskName() + ", key=" + taskKey);
                continue;
            }

            insertOrUpdateTaskData(task);
            addWorkflowToTaskMapping(task);
            addTaskInProgress(task);
            updateTask(task);

            created.add(task);
        }
    

        return created;
    }

	@Override
	public void updateTask(Task task) {
        Optional<TaskDef> taskDefinition = task.getTaskDefinition();

        if (taskDefinition.isPresent() && taskDefinition.get().concurrencyLimit() > 0) {
            boolean inProgress = task.getStatus() != null && task.getStatus().equals(Task.Status.IN_PROGRESS);
            updateInProgressStatus(task, inProgress);
        }

        insertOrUpdateTaskData(task);

        if (task.getStatus() != null && task.getStatus().isTerminal()) {
            removeTaskInProgress(task);
        }

        addWorkflowToTaskMapping(task);
    }

	@Override
	public boolean exceedsInProgressLimit(Task task) {

        Optional<TaskDef> taskDefinition = task.getTaskDefinition();
        if (!taskDefinition.isPresent()) {
            return false;
        }

        TaskDef taskDef = taskDefinition.get();

        int limit = taskDef.concurrencyLimit();
        if (limit <= 0) {
            return false;
        }

        long current = getInProgressTaskCount(task.getTaskDefName());

        if (current >= limit) {
            Monitors.recordTaskConcurrentExecutionLimited(task.getTaskDefName(), limit);
            return true;
        }

        logger.info("Task execution count for {}: limit={}, current={}", task.getTaskDefName(), limit,
            getInProgressTaskCount(task.getTaskDefName()));

        String taskId = task.getTaskId();

        List<String> tasksInProgressInOrderOfArrival = findAllTasksInProgressInOrderOfArrival(task, limit);

        boolean rateLimited = !tasksInProgressInOrderOfArrival.contains(taskId);

        if (rateLimited) {
            logger.info("Task execution count limited. {}, limit {}, current {}", task.getTaskDefName(), limit,
                getInProgressTaskCount(task.getTaskDefName()));
            Monitors.recordTaskConcurrentExecutionLimited(task.getTaskDefName(), limit);
        }

        return rateLimited;
    }

	@Override
	public boolean removeTask(String taskId) {
        Task task = getTask(taskId);

        if (task == null) {
            logger.warn("No such task found by id {}", taskId);
            return false;
        }

        final String taskKey = taskKey(task);

        removeScheduledTask(task, taskKey);
        removeWorkflowToTaskMapping(task);
        removeTaskInProgress(task);
        removeTaskData(task);
    
        return true;
    }

	@Override
	public Task getTask(String taskId) {
        Query searchQuery = new Query();
        searchQuery.addCriteria(Criteria.where("task_id").is(taskId));
        TaskDataDocument taskDocument =  mongoTemplate.findOne(searchQuery, TaskDataDocument.class);
        return null!=taskDocument && null!=taskDocument.getJson_data() ? readValue(mongoTemplate.findOne(searchQuery, TaskDataDocument.class).getJson_data(), Task.class) : null;
    }

	@Override
	public List<Task> getTasks(List<String> taskIds) {
		Query searchQuery = new Query();
		
		List<Task> result = new ArrayList<Task>();
        
        try {
        	System.out.println("get Tasks taskIds --> "+taskIds.toString());
        	 if(!taskIds.isEmpty()) {
        		 Criteria mainCriteria = new Criteria();
        		 
        		 for(int taskCounter = 0; taskCounter<taskIds.size() ; taskCounter++) {
        			 mainCriteria.orOperator(Criteria.where("task_id").is(taskIds.get(taskCounter)));
        		 }
        		 
        		 searchQuery.addCriteria(mainCriteria);
        		 
        		 
        		 System.out.println("get Tasks searchQuery --> "+searchQuery.toString());
        		 
             	 List<TaskDataDocument> taskDataList = mongoTemplate.find(searchQuery, TaskDataDocument.class);
             	 
             	 System.out.println("taskDataList --> "+taskDataList.isEmpty()+" "+taskDataList.size()+ " "+taskDataList.toString());
             	 
                 if(taskDataList.isEmpty())
                 	return result;
                 else
                 	taskDataList.forEach(taskDoc -> {
                 		if(taskDoc.getJson_data()!=null)
                 			result.add(readValue(taskDoc.getJson_data(), Task.class));
                 	});
             }
        }
        catch(Exception e) {
        	e.printStackTrace();
        	taskIds.forEach(taskId -> {
            	result.add(getTask(taskId));
            });
        }
        System.out.println("result--> "+result.toString());
        return result;
	}

	

	@Override
	public List<Task> getTasksForWorkflow(String workflowId) {
		
		Query searchQuery = new Query();
		searchQuery.addCriteria(Criteria.where("workflow_id").is(workflowId));
		
		List<String> taskIds = new ArrayList<String>();
		
		System.out.println("searchQuery --> "+searchQuery.toString());
		mongoTemplate.find(searchQuery, WorkflowToTaskDocument.class).forEach(wttd -> taskIds.add(wttd.getTask_id()));
        System.out.println("taskIds --> "+taskIds.toString());
		return getTasks(taskIds);
    }
	
	@Override
	public String createWorkflow(Workflow workflow) {
		 return insertOrUpdateWorkflow(workflow, false);
	}

	@Override
	public String updateWorkflow(Workflow workflow) {
		return insertOrUpdateWorkflow(workflow, true);
	}
	
	private void updateWorkflowToDB(Workflow workflow) {
        Query searchQuery = new Query();
        searchQuery .addCriteria(Criteria.where("workflow_id").is(workflow.getWorkflowId()));
        Update update = new Update();
        update.set("json_data", toJson(workflow));
        
        mongoTemplate.updateMulti(searchQuery, update, WorkflowDocument.class);
    }

	@Override
	public boolean removeWorkflow(String workflowId) {
        boolean removed = false;
        Workflow workflow = getWorkflow(workflowId, true);
        if (workflow != null) {

            removeWorkflowDefToWorkflowMapping(workflow);
            removeWorkflow(workflowId);
            removePendingWorkflow(workflow.getWorkflowName(), workflowId);
        
            removed = true;

            for (Task task : workflow.getTasks()) {
                if (!removeTask(task.getTaskId())) {
                    removed = false;
                }
            }
        }
        return removed;
    }

	@Override
	public boolean removeWorkflowWithExpiry(String workflowId, int ttlSeconds) {
        throw new UnsupportedOperationException(
                "This method is not implemented in MongoExecutionDAO. Please use RedisDAO mode instead for using TTLs.");
        }

	@Override
	public void removeFromPendingWorkflow(String workflowType, String workflowId) {
		removePendingWorkflow(workflowType, workflowId);
	}

	@Override
	public Workflow getWorkflow(String workflowId) {
		return getWorkflow(workflowId, true);
    }

	@Override
	public Workflow getWorkflow(String workflowId, boolean includeTasks) {
		
        Workflow workflow = readWorkflow(workflowId);

        if (workflow != null) {
            if (includeTasks) {
                List<Task> tasks = getTasksForWorkflow(workflowId);
                tasks.sort(Comparator.comparingLong(Task::getScheduledTime).thenComparingInt(Task::getSeq));
                workflow.setTasks(tasks);
            }
        }
        return workflow;
    }

	@Override
	public List<String> getRunningWorkflowIds(String workflowName, int version) {
        Preconditions.checkNotNull(workflowName, "workflowName cannot be null");
        
        List<String> workflowIds = new ArrayList<String>();
        Query searchQuery = new Query();
        searchQuery.addCriteria(Criteria.where("workflow_type").is(workflowName));
        
        mongoTemplate.find(searchQuery, WorkflowPendingDocument.class).forEach(wpd -> workflowIds.add(wpd.getWorkflow_id()));
        
        return workflowIds;
    }

	@Override
	public List<Workflow> getPendingWorkflowsByType(String workflowName, int version) {
        Preconditions.checkNotNull(workflowName, "workflowName cannot be null");
        return getRunningWorkflowIds(workflowName, version).stream()
            .map(this::getWorkflow)
            .filter(workflow -> workflow.getWorkflowVersion() == version)
            .collect(Collectors.toList());
    }

	@Override
	public long getPendingWorkflowCount(String workflowName) {
        Preconditions.checkNotNull(workflowName, "workflowName cannot be null");
        
        Query searchQuery = new Query();
        searchQuery.addCriteria(Criteria.where("workflow_type").is(workflowName));

        return mongoTemplate.count(searchQuery, WorkflowPendingDocument.class);
    }

	@Override
	public long getInProgressTaskCount(String taskDefName) {
       
        Query searchQuery = new Query();
        searchQuery.addCriteria(Criteria.where("task_def_name").is(taskDefName).and("in_progress_status").is(true));

        return mongoTemplate.count(searchQuery, TaskInProgressDocument.class);
    }

	@Override
	public List<Workflow> getWorkflowsByType(String workflowName, Long startTime, Long endTime) {
        Preconditions.checkNotNull(workflowName, "workflowName cannot be null");
        Preconditions.checkNotNull(startTime, "startTime cannot be null");
        Preconditions.checkNotNull(endTime, "endTime cannot be null");

        List<Workflow> workflows = new LinkedList<>();
 
        Query searchQuery = new Query();
        searchQuery.addCriteria(Criteria.where("workflow_def").is(workflowName).and("date_str").gte(startTime).and("date_str").lte(endTime));

        List<String> workflowIds = new ArrayList<String>();
        mongoTemplate.find(searchQuery, WorkflowDefToWorkflowDocument.class).forEach(wdtw -> workflowIds.add(wdtw.getWorkflow_id()));
        
        workflowIds.forEach(workflowId -> {
            try {
                Workflow wf = getWorkflow(workflowId);
                if (wf.getCreateTime() >= startTime && wf.getCreateTime() <= endTime) {
                    workflows.add(wf);
                }
            } catch (Exception e) {
                logger.error("Unable to load workflow id {} with name {}", workflowId, workflowName, e);
            }
        });

        return workflows;
    }
	
	@Override
	public List<Task> getPendingTasksForTaskType(String taskName) {
		
		List<Task> result = new ArrayList<Task>();
		
		Query searchQuery = new Query();
		searchQuery.addCriteria(Criteria.where("task_name").is(taskName));
        
		List<TaskInProgressDocument> inProgress = mongoTemplate.find(searchQuery, TaskInProgressDocument.class);
		
		if(!inProgress.isEmpty()) {
			Criteria mainCriteria = new Criteria();
   		 
   		 	int total = inProgress.size();
			
			for(int counter = 0; counter < total ; counter++) {
				mainCriteria.orOperator(Criteria.where("task_id").is(inProgress.get(counter).getTask_id()));
			}
			
			mongoTemplate.find(new Query().addCriteria(mainCriteria), TaskDataDocument.class).forEach(tdd -> {
				result.add(readValue(tdd.getJson_data(), Task.class));
			});;
		}
		
    	return result;
    
	}
	
	@Override
	public List<Workflow> getWorkflowsByCorrelationId(String workflowName, String correlationId, boolean includeTasks) {
		
		Preconditions.checkNotNull(correlationId, "correlationId cannot be null");
        List<Workflow> result = new ArrayList<Workflow>();
		
		Query searchQuery = new Query();
		searchQuery.addCriteria(Criteria.where("workflow_def").is(workflowName));
        
		List<WorkflowDefToWorkflowDocument> inProgress = mongoTemplate.find(searchQuery, WorkflowDefToWorkflowDocument.class);
		
		if(!inProgress.isEmpty()) {
			
			Criteria mainCriteria = new Criteria();
			
			int total = inProgress.size();
			
			for(int counter = 0; counter < total ; counter++) {
				mainCriteria.orOperator(Criteria.where("workflow_id").is(inProgress.get(counter).getWorkflow_id()).and("correlation_id").is(correlationId));
			}
			
			mongoTemplate.find(new Query().addCriteria(mainCriteria), WorkflowDocument.class).forEach(wd -> {
				result.add(readValue(wd.getJson_data(), Workflow.class));
			});;
		}
		
    	return result;
    }

	@Override
	public boolean canSearchAcrossWorkflows() {
		return true;
	}

	@Override
	public boolean addEventExecution(EventExecution eventExecution) {
        try {
            return insertEventExecution(eventExecution);
        } catch (Exception e) {
            throw new ApplicationException(ApplicationException.Code.BACKEND_ERROR,
                "Unable to add event execution " + eventExecution.getId(), e);
        }
    }

	@Override
	public void updateEventExecution(EventExecution eventExecution) {
	       Query updateQuery = new Query();
	       updateQuery.addCriteria(Criteria.where("event_handler_name").is(eventExecution.getName()).and("event_name").is(eventExecution.getEvent()).and("message_id").is(eventExecution.getMessageId()).and("execution_id").is(eventExecution.getId()));

	       Update update = new Update();
	       update.set("json_data", toJson(eventExecution));
	       mongoTemplate.updateMulti(updateQuery, update, EventExecutionDocument.class);
	       
	       
	   }

	@Override
	public void removeEventExecution(EventExecution eventExecution) {
	      
	       Query deleteQuery = new Query();
	       deleteQuery.addCriteria(Criteria.where("event_handler_name").is(eventExecution.getName()).and("event_name").is(eventExecution.getEvent()).and("message_id").is(eventExecution.getMessageId()).and("execution_id").is(eventExecution.getId()));

	       mongoTemplate.remove(deleteQuery, EventExecutionDocument.class);

	       }

	@Override
	public void updateLastPollData(String taskDefName, String domain, String workerId) {
        Preconditions.checkNotNull(taskDefName, "taskDefName name cannot be null");
        PollData pollData = new PollData(taskDefName, domain, workerId, System.currentTimeMillis());
        String effectiveDomain = (domain == null) ? "DEFAULT" : domain;
        insertOrUpdatePollData(pollData, effectiveDomain);
    }

	@Override
	public PollData getPollData(String taskDefName, String domain) {
        Preconditions.checkNotNull(taskDefName, "taskDefName name cannot be null");
        String effectiveDomain = (domain == null) ? "DEFAULT" : domain;
        return readPollData(taskDefName, effectiveDomain);
    }

	@Override
	public List<PollData> getPollData(String taskDefName) {
        Preconditions.checkNotNull(taskDefName, "taskDefName name cannot be null");
        return readAllPollData(taskDefName);
    }

	@Override
	public boolean exceedsRateLimitPerFrequency(Task task, TaskDef taskDef) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@VisibleForTesting
    boolean addScheduledTask(Task task, String taskKey) {
		
		Query checkExistsQuery = new Query();
		
		checkExistsQuery.addCriteria(Criteria.where("workflow_id").is(task.getWorkflowInstanceId()).and("task_key").is(taskKey));
		
		List<TaskScheduledDocument> taskScheduledDocument = mongoTemplate.find(checkExistsQuery, TaskScheduledDocument.class);

        boolean exists = !taskScheduledDocument.isEmpty();

        if (!exists) {
           
        	TaskScheduledDocument newTaskScheduledDocument = new TaskScheduledDocument();
            newTaskScheduledDocument.setWorkflow_id(task.getWorkflowInstanceId());
            newTaskScheduledDocument.setTask_key(taskKey);
            newTaskScheduledDocument.setTask_id(newTaskScheduledDocument.getTask_id());
            
            newTaskScheduledDocument = mongoTaskScheduledRepository.save(newTaskScheduledDocument);
            
            return null!=newTaskScheduledDocument;
        } else {
            return false;
        }

    }
	
	private void insertOrUpdateTaskData(Task task) {
       
		Query updateQuery = new Query();
		updateQuery.addCriteria(Criteria.where("task_id").is(task.getTaskId()));
		Update update = new Update();
		update.set("json_data", toJson(task));
		update.set("task_id", task.getTaskId());
		
		mongoTemplate.upsert(updateQuery, update, TaskDataDocument.class).getModifiedCount();
    }
	
	private void removeTaskData(Task task) {
		Query query = new Query();
		query.addCriteria(Criteria.where("task_id").is(task.getTaskId()));
		mongoTemplate.remove(query, TaskDataDocument.class);
	}
	
	 private void addWorkflowToTaskMapping(Task task) {

	        Query searchQuery = new Query();
	        
	        searchQuery.addCriteria(Criteria.where("workflow_id").is(task.getWorkflowInstanceId()).and("task_id").is(task.getTaskId()));
	        
	        boolean exists = mongoTemplate.exists(searchQuery, WorkflowToTaskDocument.class);

	        if (!exists) {
	            WorkflowToTaskDocument newWorkflowToTaskDocument = new WorkflowToTaskDocument();
	            newWorkflowToTaskDocument.setWorkflow_id(task.getWorkflowInstanceId());
	            newWorkflowToTaskDocument.setTask_id(task.getTaskId());
	            mongoWorkflowToTaskRepository.save(newWorkflowToTaskDocument);
	        }
	    }
	
	 private void validate(Task task) {
	        Preconditions.checkNotNull(task, "task object cannot be null");
	        Preconditions.checkNotNull(task.getTaskId(), "Task id cannot be null");
	        Preconditions.checkNotNull(task.getWorkflowInstanceId(), "Workflow instance id cannot be null");
	        Preconditions.checkNotNull(task.getReferenceTaskName(), "Task reference name cannot be null");
	    }
	 
	 private void addTaskInProgress(Task task) {
	        
		 	Query searchQuery = new Query();
		 	searchQuery.addCriteria(Criteria.where("task_def_name").is(task.getTaskDefName()).and("task_id").is(task.getTaskId()));
		 	
		 	mongoTemplate.count(searchQuery, TaskInProgressDocument.class);
		 
		 	boolean exists = mongoTemplate.exists(searchQuery, TaskInProgressDocument.class);

	        if (!exists) {
	           
	            TaskInProgressDocument newTaskInProgressDocument = new TaskInProgressDocument();
	            
	            newTaskInProgressDocument.setTask_def_name(task.getTaskDefName());
	            newTaskInProgressDocument.setTask_id(task.getTaskId());
	            newTaskInProgressDocument.setWorkflow_id(task.getWorkflowInstanceId());
	            
	            newTaskInProgressDocument = mongoTaskInProgressRepository.save(newTaskInProgressDocument);
	        }
	    }
	 
	 private void updateInProgressStatus(Task task, boolean inProgress) {
	        Query updateQuery = new Query();
	        
	        updateQuery.addCriteria(Criteria.where("task_def_name").is(task.getTaskDefName()).and("task_id").is(task.getTaskId()));
	        
	        Update update = new Update();
	        
	        update.set("in_progress_status", inProgress);

	        mongoTemplate.updateFirst(updateQuery, update, TaskInProgressDocument.class);
	        }
	 
	 private void removeTaskInProgress(Task task) {
		 
		 	Query searchQuery = new Query();
		 	searchQuery.addCriteria(Criteria.where("task_def_name").is(task.getTaskDefName()).and("task_id").is(task.getTaskId()));
		 	
		 	mongoTemplate.remove(searchQuery, TaskInProgressDocument.class);
	    }
	 
	 private List<String> findAllTasksInProgressInOrderOfArrival(Task task, int limit) {
		 
		 	Query searchQuery= new Query();
		 	searchQuery.addCriteria(Criteria.where("task_def_name").is(task.getTaskDefName()));
		 	searchQuery.with(Sort.by(new Order(Direction.ASC, "created_on")));
		 	searchQuery.limit(limit);
		 	
		 	List<String> tips= new ArrayList<String>(); 
		 	mongoTemplate.find(searchQuery, TaskInProgressDocument.class).forEach(tip -> tips.add(tip.getTask_id()));
	        return tips;
	    }
	 
	 private void removeScheduledTask(Task task, String taskKey) {
	        Query searchQuery= new Query();
		 	searchQuery.addCriteria(Criteria.where("workflow_id").is(task.getWorkflowInstanceId()).and("task_key").is(taskKey));
		 	
		 	System.out.println("Remove Query -->"+searchQuery.toString());
		 	
	        mongoTemplate.remove(searchQuery, TaskScheduledDocument.class);
	    }
	 
	 private void removeWorkflowToTaskMapping(Task task) {
		 	Query searchQuery = new Query();
	        
	        searchQuery.addCriteria(Criteria.where("workflow_id").is(task.getWorkflowInstanceId()).and("task_id").is(task.getTaskId()));
	        mongoTemplate.remove(searchQuery, WorkflowToTaskDocument.class);
	    }
	 
	 private String insertOrUpdateWorkflow(Workflow workflow, boolean update) {
	        Preconditions.checkNotNull(workflow, "workflow object cannot be null");

	        boolean terminal = workflow.getStatus().isTerminal();

	        List<Task> tasks = workflow.getTasks();
	        
	        workflow.setTasks(Lists.newLinkedList());
            
	        if (!update) {
                addWorkflow(workflow);
                addWorkflowDefToWorkflowMapping(workflow);
            } else {
            	updateWorkflowToDB(workflow);
            }

            if (terminal) {
                removePendingWorkflow(workflow.getWorkflowName(), workflow.getWorkflowId());
            } else {
                addPendingWorkflow(workflow.getWorkflowName(), workflow.getWorkflowId());
            }

	        workflow.setTasks(tasks);
	        return workflow.getWorkflowId();
	    }
	 
	 private void addWorkflow(Workflow workflow) {
	       
	        WorkflowDocument workflowDocument = new WorkflowDocument();
	        workflowDocument.setWorkflow_id(workflow.getWorkflowId());
	        workflowDocument.setCorrelation_id(workflow.getCorrelationId());
	        workflowDocument.setJson_data(toJson(workflow));
	        
	        workflowDocument = mongoWorkflowRepository.save(workflowDocument);
	    }
	 
	 private void addWorkflowDefToWorkflowMapping(Workflow workflow) {
	       
	        WorkflowDefToWorkflowDocument workflowDefToWorkflowDocument = new WorkflowDefToWorkflowDocument();
	        
	        workflowDefToWorkflowDocument.setDate_str(dateStr(workflow.getCreateTime()));
	        workflowDefToWorkflowDocument.setWorkflow_def(workflow.getWorkflowName());
	        workflowDefToWorkflowDocument.setWorkflow_id(workflow.getWorkflowId());
	        
	        
	        workflowDefToWorkflowDocument = workflowDefToWorkflowRepository.save(workflowDefToWorkflowDocument);
	    }
	 
	 private void removePendingWorkflow(String workflowType, String workflowId) {
	        
	        Query deleteQuery = new Query();
	        deleteQuery.addCriteria(Criteria.where("workflow_type").is(workflowType).and("workflow_id").is(workflowId));

	        mongoTemplate.remove(deleteQuery, WorkflowPendingDocument.class);
	    }
	 
	 private void addPendingWorkflow(String workflowType, String workflowId) {

	        Query searchQuery = new Query();
	        
	        searchQuery.addCriteria(Criteria.where("workflow_type").is(workflowType).and("workflow_id").is(workflowId));
	        
	        boolean exists = mongoTemplate.exists(searchQuery, WorkflowPendingDocument.class);

	        if (!exists) {
	        	WorkflowPendingDocument workflowPendingDocument = new WorkflowPendingDocument();
	            workflowPendingDocument.setWorkflow_id(workflowId);
	            workflowPendingDocument.setWorkflow_type(workflowType);
	            
	            mongoWorkflowPendingRepository.save(workflowPendingDocument);
	        }
	    }
	 
   private void removeWorkflowDefToWorkflowMapping(Workflow workflow) {
       
        Query deleteQuery = new Query();
        deleteQuery.addCriteria(Criteria.where("workflow_def").is(workflow.getWorkflowName()).and("date_str").is(dateStr(workflow.getCreateTime())).and("workflow_id").is(workflow.getWorkflowId()));
        mongoTemplate.remove(deleteQuery, WorkflowDefToWorkflowDocument.class);
   }
   
   private Workflow readWorkflow(String workflowId) {
       
       Query searchQuery = new Query();
       searchQuery.addCriteria(Criteria.where("workflow_id").is(workflowId));
       
       WorkflowDocument workflowDocument = mongoTemplate.findOne(searchQuery, WorkflowDocument.class);
       
       return null!=workflowDocument ? readValue(workflowDocument.getJson_data(), Workflow.class) : null;
   }
   
   private boolean insertEventExecution(EventExecution eventExecution) {

       EventExecutionDocument eventExecutionDocument = new EventExecutionDocument();
       eventExecutionDocument.setEvent_handler_name(eventExecution.getName());
       eventExecutionDocument.setEvent_name(eventExecution.getEvent());
       eventExecutionDocument.setMessage_id(eventExecution.getMessageId());
       eventExecutionDocument.setExecution_id(eventExecution.getId());
       eventExecutionDocument.setJson_data(toJson(eventExecution));
       
       eventExecutionDocument = mongoEventExecutionRepository.save(eventExecutionDocument);
       return eventExecutionDocument!=null;
   }
   
   public List<EventExecution> getEventExecutions(String eventHandlerName, String eventName, String messageId,
	        int max) {
	        try {
	            List<EventExecution> executions = Lists.newLinkedList();

                for (int i = 0; i < max; i++) {
                    String executionId = messageId + "_" + i; // see SimpleEventProcessor.handle to understand how the
                    // execution id is set
                    EventExecution ee = readEventExecution(eventHandlerName, eventName, messageId, executionId);
                    if (ee == null) {
                        break;
                    }
                    executions.add(ee);
                }
            
	            return executions;
	        } catch (Exception e) {
	            String message = String.format(
	                "Unable to get event executions for eventHandlerName=%s, eventName=%s, messageId=%s",
	                eventHandlerName, eventName, messageId);
	            throw new ApplicationException(ApplicationException.Code.BACKEND_ERROR, message, e);
	        }
	    }

   private EventExecution readEventExecution(String eventHandlerName, String eventName,
       String messageId, String executionId) {
          
       Query searchQuery = new Query();
       searchQuery.addCriteria(Criteria.where("event_handler_name").is(eventHandlerName).and("event_name").is(eventName).and("message_id").is(messageId).and("execution_id").is(executionId));

       /*Criteria criteria = new Criteria();
       criteria
       .andOperator(Criteria.where("event_handler_name").is(eventHandlerName).and("event_name").is(eventName).and("message_id").is(messageId).and("execution_id").is(executionId));
       
       Query searchQuery = new Query();
       searchQuery.addCriteria(criteria);*/
       
       EventExecutionDocument eventExecutionDocument = mongoTemplate.findOne(searchQuery, EventExecutionDocument.class);
       return null!=eventExecutionDocument ? readValue(eventExecutionDocument.getJson_data(), EventExecution.class) : null;
   }

   private void insertOrUpdatePollData(PollData pollData, String domain) {

       Query updateQuery = new Query();
       updateQuery.addCriteria(Criteria.where("queue_name").is(pollData.getQueueName()).and("domain").is(domain));
       
       Update update = new Update();
       
       update.set("json_data", toJson(pollData));
       
       mongoTemplate.updateMulti(updateQuery, update, PollDataDocument.class).getModifiedCount();

       long rowsUpdated = mongoTemplate.updateMulti(updateQuery, update, PollDataDocument.class).getModifiedCount();
    		   
       if (rowsUpdated == 0) {
    	   
    	   PollDataDocument pollDataDocument = new PollDataDocument();
    	   pollDataDocument.setDomain(domain);
    	   pollDataDocument.setQueue_name(pollData.getQueueName());
    	   pollDataDocument.setJson_data(toJson(pollData));
    	   
    	   pollDataDocument = mongoPollDataRepository.save(pollDataDocument);
       }
   }

   private PollData readPollData(String queueName, String domain) {
       
       Query searchQuery = new Query();
       searchQuery.addCriteria(Criteria.where("queue_name").is(queueName).and("domain").is(domain));
       
       PollDataDocument pollDataDocument = mongoTemplate.findOne(searchQuery, PollDataDocument.class);
       
       return null!=pollDataDocument ? readValue(pollDataDocument.getJson_data(), PollData.class) : null;
   }

   private List<PollData> readAllPollData(String queueName) {
       
       List<PollData> pollDataList = new ArrayList<PollData>();
       
       Query searchQuery = new Query();
       searchQuery.addCriteria(Criteria.where("queue_name").is(queueName));
       
       List<PollDataDocument> pollDataDocuments = mongoTemplate.find(searchQuery, PollDataDocument.class);
       
       pollDataDocuments.forEach(pdd -> pollDataList.add(readValue(pdd.getJson_data(), PollData.class)));
       
       return pollDataList;
   }
}
