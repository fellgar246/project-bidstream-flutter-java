package com.bidstream.application.lot;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class CreateLotUseCase {

  @PreAuthorize("hasRole('SELLER')")
  public void execute() {
    // Stub for SPEC-03 — method security demonstration (CA-02.6).
  }
}
