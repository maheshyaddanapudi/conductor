package com.netflix.conductor.mongo.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.netflix.conductor.mongo.entities.EventExecutionDocument;

@Repository
@Transactional
public interface MongoEventExecutionRepository extends MongoRepository<EventExecutionDocument, String> {

}
