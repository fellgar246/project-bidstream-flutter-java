package com.bidstream.application.lot;

import com.bidstream.application.lot.port.ObjectStoragePort;
import com.bidstream.domain.lot.LotImage;
import com.bidstream.domain.lot.LotImageRepository;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ThumbnailService {

  private static final Logger log = LoggerFactory.getLogger(ThumbnailService.class);
  private static final int MAX_THUMBNAIL_SIDE = 400;

  private final LotImageRepository lotImageRepository;
  private final ObjectStoragePort objectStorage;
  private final Clock clock;

  public ThumbnailService(
      LotImageRepository lotImageRepository, ObjectStoragePort objectStorage, Clock clock) {
    this.lotImageRepository = lotImageRepository;
    this.objectStorage = objectStorage;
    this.clock = clock;
  }

  @Async("imageTaskExecutor")
  public void generateAsync(long imageId) {
    try {
      generate(imageId);
    } catch (Exception ex) {
      log.warn("Thumbnail generation failed for imageId={}: {}", imageId, ex.getMessage());
    }
  }

  void generate(long imageId) throws IOException {
    LotImage image =
        lotImageRepository
            .findById(imageId)
            .orElseThrow(() -> new NoSuchElementException("Image not found"));

    byte[] originalBytes = objectStorage.getObject(image.storageKey());
    BufferedImage source = readImage(originalBytes, image.contentType());
    BufferedImage thumbnail = resize(source, MAX_THUMBNAIL_SIDE);
    byte[] webpBytes = writeWebp(thumbnail);

    String thumbnailKey = "lots/" + image.lotId() + "/" + UUID.randomUUID() + "_thumb.webp";
    objectStorage.putObject(thumbnailKey, webpBytes, "image/webp");

    lotImageRepository.save(image.withThumbnail(thumbnailKey, clock.instant()));
  }

  private static BufferedImage readImage(byte[] bytes, String contentType) throws IOException {
    try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
      Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) {
        throw new IOException("No image reader for " + contentType);
      }
      ImageReader reader = readers.next();
      reader.setInput(input);
      return reader.read(0);
    }
  }

  private static BufferedImage resize(BufferedImage source, int maxSide) {
    int width = source.getWidth();
    int height = source.getHeight();
    double scale = Math.min(1.0, (double) maxSide / Math.max(width, height));
    int targetWidth = Math.max(1, (int) Math.round(width * scale));
    int targetHeight = Math.max(1, (int) Math.round(height * scale));

    BufferedImage resized =
        new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = resized.createGraphics();
    graphics.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
    graphics.dispose();
    return resized;
  }

  private static byte[] writeWebp(BufferedImage image) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    if (!ImageIO.write(image, "webp", output)) {
      throw new IOException("WebP writer not available");
    }
    return output.toByteArray();
  }
}
