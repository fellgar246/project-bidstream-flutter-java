package com.bidstream.infrastructure.persistence.processed;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventJpaRepository
    extends JpaRepository<ProcessedEventEntity, ProcessedEventEntity.ProcessedEventId> {}
