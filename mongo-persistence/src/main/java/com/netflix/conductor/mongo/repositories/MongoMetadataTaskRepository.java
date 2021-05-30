package com.netflix.conductor.mongo.repositories;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.netflix.conductor.mongo.entities.MetaTaskDefDocument;

@Repository
@Transactional
public interface MongoMetadataTaskRepository extends MongoRepository<MetaTaskDefDocument, String> {
    @Modifying
    void deleteByName(String name);
}