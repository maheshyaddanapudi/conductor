package com.netflix.conductor.mongo.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.netflix.conductor.mongo.entities.TaskInProgressDocument;

@Repository
@Transactional
public interface MongoTaskInProgressRepository extends MongoRepository<TaskInProgressDocument, String> {

}
