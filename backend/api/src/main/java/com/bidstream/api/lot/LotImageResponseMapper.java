package com.bidstream.api.lot;

import com.bidstream.application.lot.port.ObjectStoragePort;
import com.bidstream.domain.lot.LotImage;
import com.bidstream.domain.lot.LotImageStatus;
import org.springframework.stereotype.Component;

@Component
public class LotImageResponseMapper {

  private final ObjectStoragePort objectStorage;

  public LotImageResponseMapper(ObjectStoragePort objectStorage) {
    this.objectStorage = objectStorage;
  }

  public LotImageResponse toResponse(LotImage image) {
    String url =
        image.status() == LotImageStatus.READY ? objectStorage.publicUrl(image.storageKey()) : null;
    String thumbnailUrl =
        image.thumbnailKey() != null ? objectStorage.publicUrl(image.thumbnailKey()) : null;
    return new LotImageResponse(
        image.id(),
        url,
        thumbnailUrl,
        image.position(),
        image.contentType(),
        image.status().name());
  }
}
