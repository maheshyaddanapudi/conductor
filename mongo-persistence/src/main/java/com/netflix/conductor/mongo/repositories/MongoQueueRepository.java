package com.netflix.conductor.mongo.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.netflix.conductor.mongo.entities.QueueDocument;

@Repository
@Transactional
public interface MongoQueueRepository extends MongoRepository<QueueDocument, String> {
	Optional<QueueDocument> findByName(String queueName);
}
