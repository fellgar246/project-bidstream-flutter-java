package com.bidstream.infrastructure.persistence.invoice;

import com.bidstream.application.invoice.Invoice;
import com.bidstream.application.invoice.InvoiceRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InvoiceRepositoryAdapter implements InvoiceRepository {

  private final InvoiceJpaRepository jpaRepository;

  public InvoiceRepositoryAdapter(InvoiceJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public Invoice save(Invoice invoice) {
    InvoiceEntity entity =
        invoice.id() == 0L
            ? new InvoiceEntity()
            : jpaRepository.findById(invoice.id()).orElseThrow();
    entity.setLotId(invoice.lotId());
    entity.setBuyerId(invoice.buyerId());
    entity.setSellerId(invoice.sellerId());
    entity.setAmountCents(invoice.amountCents());
    entity.setStatus(invoice.status());
    entity.setIssuedAt(invoice.issuedAt());
    entity.setCreatedAt(invoice.createdAt());
    return toDomain(jpaRepository.save(entity));
  }

  @Override
  public Optional<Invoice> findById(long id) {
    return jpaRepository.findById(id).map(this::toDomain);
  }

  @Override
  public Optional<Invoice> findByLotId(long lotId) {
    return jpaRepository.findByLotId(lotId).map(this::toDomain);
  }

  private Invoice toDomain(InvoiceEntity entity) {
    return new Invoice(
        entity.getId(),
        entity.getLotId(),
        entity.getBuyerId(),
        entity.getSellerId(),
        entity.getAmountCents(),
        entity.getStatus(),
        entity.getIssuedAt(),
        entity.getCreatedAt());
  }
}
