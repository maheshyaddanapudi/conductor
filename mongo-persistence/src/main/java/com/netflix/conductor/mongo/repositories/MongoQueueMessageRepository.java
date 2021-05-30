package com.netflix.conductor.mongo.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.netflix.conductor.mongo.entities.QueueMessageDocument;

@Repository
@Transactional
public interface MongoQueueMessageRepository extends MongoRepository<QueueMessageDocument, String> {
	Optional<QueueMessageDocument> findByQueueNameAndMessageId(String queueName, String messageId);
	List<QueueMessageDocument> findAllByPopped(boolean popped);
	
	@Modifying
	void deleteByQueueNameAndMessageId(String queueName, String messageId);
	@Modifying
	void deleteAllByQueueName(String queueName);
}
