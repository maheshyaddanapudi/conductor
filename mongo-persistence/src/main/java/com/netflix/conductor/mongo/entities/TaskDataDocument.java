package com.netflix.conductor.mongo.entities;

import java.util.Date;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "task")
public class TaskDataDocument {

	@Indexed(name = "task_task_id_uk", unique = true)
	@Field(name = "task_id")
	private String task_id;
	
	@Field(name = "json_data")
	private String json_data;
	
	@Field("created_on")
	@Temporal(TemporalType.TIMESTAMP)
	private Date created_on;
    @Field("modified_on")
	@Temporal(TemporalType.TIMESTAMP)
	private Date modified_on;
    
    @PrePersist
	protected void onCreate() {
		created_on = modified_on = new Date();
	}

	@PreUpdate
	protected void onUpdate() {
		modified_on = new Date();
	}

	public TaskDataDocument() {
		super();
		// TODO Auto-generated constructor stub
	}

	public TaskDataDocument(String task_id, String json_data, Date created_on, Date modified_on) {
		super();
		this.task_id = task_id;
		this.json_data = json_data;
		this.created_on = created_on;
		this.modified_on = modified_on;
	}

	public String getTask_id() {
		return task_id;
	}

	public void setTask_id(String task_id) {
		this.task_id = task_id;
	}

	public String getJson_data() {
		return json_data;
	}

	public void setJson_data(String json_data) {
		this.json_data = json_data;
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
