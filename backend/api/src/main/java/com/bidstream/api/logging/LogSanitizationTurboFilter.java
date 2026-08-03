package com.bidstream.api.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import java.util.regex.Pattern;
import org.slf4j.Marker;

public class LogSanitizationTurboFilter extends TurboFilter {

  private static final Pattern BEARER_TOKEN =
      Pattern.compile("Bearer\\s+[A-Za-z0-9\\-._~+/]+=*", Pattern.CASE_INSENSITIVE);
  private static final Pattern JWT_PATTERN =
      Pattern.compile(
          "eyJ[A-Za-z0-9\\-._~+/]+=*\\.[A-Za-z0-9\\-._~+/]+=*\\.[A-Za-z0-9\\-._~+/]+=*");
  private static final Pattern PASSWORD_FIELD =
      Pattern.compile(
          "(password|passwordHash|token_hash)[\"']?\\s*[:=]\\s*[\"']?[^\\s,\"'}]+",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern SIGNED_URL =
      Pattern.compile(
          "https?://[^\\s\"']+[?&](X-Amz-Signature|signature|token)=[^\\s\"'&]+",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern EMAIL =
      Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");

  @Override
  public FilterReply decide(
      Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
    if (format != null && containsSensitive(format)) {
      return FilterReply.DENY;
    }
    if (params != null) {
      for (Object param : params) {
        if (param != null && containsSensitive(param.toString())) {
          return FilterReply.DENY;
        }
      }
    }
    if (t != null && t.getMessage() != null && containsSensitive(t.getMessage())) {
      return FilterReply.DENY;
    }
    return FilterReply.NEUTRAL;
  }

  static boolean containsSensitive(String value) {
    return BEARER_TOKEN.matcher(value).find()
        || JWT_PATTERN.matcher(value).find()
        || PASSWORD_FIELD.matcher(value).find()
        || SIGNED_URL.matcher(value).find()
        || EMAIL.matcher(value).find();
  }
}
