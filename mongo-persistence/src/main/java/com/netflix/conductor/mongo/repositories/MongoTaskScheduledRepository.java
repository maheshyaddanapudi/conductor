package com.netflix.conductor.mongo.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.netflix.conductor.mongo.entities.TaskScheduledDocument;

@Repository
@Transactional
public interface MongoTaskScheduledRepository extends MongoRepository<TaskScheduledDocument, String> {

}
