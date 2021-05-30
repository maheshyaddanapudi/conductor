package com.netflix.conductor.mongo.entities;

import java.util.Date;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Data;

@Document(collection = "task_scheduled")
@Data
@CompoundIndexes({
    @CompoundIndex(name = "task_scheduled_pk_uk", def = "{'workflow_id' : 1, 'task_key': 1}", unique = true)
})
public class TaskScheduledDocument {

	@Field("workflow_id")
	private String workflow_id;
 	@Field("task_key")
	private String task_key;
    @Field("task_id")
	private String task_id;
    @Field("created_on")
	@Temporal(TemporalType.TIMESTAMP)
	private Date created_on;
    @Field("modified_on")
	@Temporal(TemporalType.TIMESTAMP)
	private Date modified_on;
    
	public TaskScheduledDocument() {
		super();
		// TODO Auto-generated constructor stub
	}
	public TaskScheduledDocument(String workflow_id, String task_key, String task_id, Date created_on, Date modified_on) {
		super();
		this.workflow_id = workflow_id;
		this.task_key = task_key;
		this.task_id = task_id;
		this.created_on = created_on;
		this.modified_on = modified_on;
	}
	
	@PrePersist
	protected void onCreate() {
		created_on = modified_on = new Date();
	}

	@PreUpdate
	protected void onUpdate() {
		modified_on = new Date();
	}
	public String getWorkflow_id() {
		return workflow_id;
	}
	public void setWorkflow_id(String workflow_id) {
		this.workflow_id = workflow_id;
	}
	public String getTask_key() {
		return task_key;
	}
	public void setTask_key(String task_key) {
		this.task_key = task_key;
	}
	public String getTask_id() {
		return task_id;
	}
	public void setTask_id(String task_id) {
		this.task_id = task_id;
	}
	public Date getCreated_on() {
		return created_on;
	}
	public void setCreated_on(Date created_on) {
		this.created_on = created_on;
	}
	public Date getModified_on() {
		return modified_on;
	}
	public void setModified_on(Date modified_on) {
		this.modified_on = modified_on;
	}
	
}
