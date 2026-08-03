package com.bidstream.api.invoice;

import com.bidstream.application.invoice.GetInvoiceUseCase;
import com.bidstream.application.invoice.Invoice;
import com.bidstream.domain.user.Role;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

  private final GetInvoiceUseCase getInvoiceUseCase;

  public InvoiceController(GetInvoiceUseCase getInvoiceUseCase) {
    this.getInvoiceUseCase = getInvoiceUseCase;
  }

  @GetMapping("/{id}")
  public InvoiceResponse get(Authentication authentication, @PathVariable long id) {
    long userId = (long) authentication.getPrincipal();
    Set<Role> roles = extractRoles(authentication);
    Invoice invoice = getInvoiceUseCase.execute(id, userId, roles);
    return InvoiceResponse.from(invoice);
  }

  private Set<Role> extractRoles(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .map(auth -> auth.replace("ROLE_", ""))
        .map(Role::valueOf)
        .collect(Collectors.toCollection(() -> EnumSet.noneOf(Role.class)));
  }

  public record InvoiceResponse(
      long id,
      long lotId,
      long buyerId,
      long sellerId,
      long amountCents,
      String status,
      String issuedAt,
      String createdAt) {

    static InvoiceResponse from(Invoice invoice) {
      return new InvoiceResponse(
          invoice.id(),
          invoice.lotId(),
          invoice.buyerId(),
          invoice.sellerId(),
          invoice.amountCents(),
          invoice.status(),
          invoice.issuedAt().toString(),
          invoice.createdAt().toString());
    }
  }
}
