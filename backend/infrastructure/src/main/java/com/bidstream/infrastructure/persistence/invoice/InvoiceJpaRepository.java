package com.bidstream.infrastructure.persistence.invoice;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceJpaRepository extends JpaRepository<InvoiceEntity, Long> {

  java.util.Optional<InvoiceEntity> findByLotId(long lotId);
}
