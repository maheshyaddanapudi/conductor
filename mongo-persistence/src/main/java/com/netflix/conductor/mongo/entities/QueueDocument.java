package com.netflix.conductor.mongo.entities;

import java.util.Date;

import javax.persistence.PrePersist;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Data;

@Document(collection = "queue")
@Data
public class QueueDocument {

	@Field("name")
	@Indexed(name = "queue_queue_name_idx", unique=true)
	private String name;
 	@Field("created_on")
	@Temporal(TemporalType.TIMESTAMP)
	private Date created_on;
 	
	public QueueDocument() {
		super();
		// TODO Auto-generated constructor stub
	}

	public QueueDocument(String queueName, Date created_on) {
		super();
		this.name = queueName;
		this.created_on = created_on;
	}
	
	@PrePersist
	protected void onCreate() {
		created_on = new Date();
	}

	public String getQueueName() {
		return name;
	}

	public void setQueueName(String queueName) {
		this.name = queueName;
	}

	public Date getCreated_on() {
		return created_on;
	}

	public void setCreated_on(Date created_on) {
		this.created_on = created_on;
	}
 	
 	
}
