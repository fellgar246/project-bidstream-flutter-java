package com.bidstream.application.invoice;

import java.util.Optional;

public interface InvoiceRepository {

  Invoice save(Invoice invoice);

  Optional<Invoice> findById(long id);

  Optional<Invoice> findByLotId(long lotId);
}
