package com.netflix.conductor.mongo.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.netflix.conductor.mongo.entities.MetaEventHandlerDocument;


@Repository
@Transactional
public interface MongoMetaEventHandlerRepository extends MongoRepository<MetaEventHandlerDocument, String> {
	List<MetaEventHandlerDocument> findAllByEvent(String event);
	List<MetaEventHandlerDocument> findAllByEventAndActive(String event, boolean active);
	@Modifying
    void deleteByName(String name);
}
