package com.bidstream.application.invoice;

import com.bidstream.domain.lot.ForbiddenLotAccessException;
import com.bidstream.domain.user.Role;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetInvoiceUseCase {

  private final InvoiceRepository invoiceRepository;

  public GetInvoiceUseCase(InvoiceRepository invoiceRepository) {
    this.invoiceRepository = invoiceRepository;
  }

  @Transactional(readOnly = true)
  public Invoice execute(long invoiceId, long userId, Set<Role> roles) {
    Invoice invoice =
        invoiceRepository
            .findById(invoiceId)
            .orElseThrow(() -> new NoSuchElementException("Invoice not found"));
    if (roles.contains(Role.ADMIN)) {
      return invoice;
    }
    if (invoice.buyerId() != userId && invoice.sellerId() != userId) {
      throw new ForbiddenLotAccessException("Not authorized to view this invoice");
    }
    return invoice;
  }
}
