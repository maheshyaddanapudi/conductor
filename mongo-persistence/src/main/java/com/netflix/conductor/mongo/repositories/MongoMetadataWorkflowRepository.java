package com.netflix.conductor.mongo.repositories;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.netflix.conductor.mongo.entities.MetaWorkflowDefDocument;

@Repository
@Transactional
public interface MongoMetadataWorkflowRepository extends MongoRepository<MetaWorkflowDefDocument, String> {
    Optional<MetaWorkflowDefDocument> findByNameAndVersion(String name, int version);
    Optional<MetaWorkflowDefDocument> findFirstByNameOrderByVersionDesc(String name);
    @Modifying
    void deleteByNameAndVersion(String name, int version);
}