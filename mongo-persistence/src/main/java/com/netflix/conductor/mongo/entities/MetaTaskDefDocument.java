package com.netflix.conductor.mongo.entities;

import java.util.Date;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Data;

@Document(collection = "meta_task_def")
@Data
public class MetaTaskDefDocument {
	
	@Field("name")
	@Indexed(name = "meta_task_def_name_idx", unique = true)
    private String name;
    @Field("json_data")
	private String json_data;
    @Field("created_on")
	@Temporal(TemporalType.TIMESTAMP)
	private Date created_on;
    @Field("modified_on")
	@Temporal(TemporalType.TIMESTAMP)
	private Date modified_on;
	
	public MetaTaskDefDocument() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public MetaTaskDefDocument(String name, String json_data, Date created_on, Date modified_on) {
		super();
		this.name = name;
		this.json_data = json_data;
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	@Override
	public String toString() {
		return "MetaTaskDef [ name=" + name + ", json_data=" + json_data + ", created_on=" + created_on
				+ ", modified_on=" + modified_on + "]";
	}

}