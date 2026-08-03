package com.bidstream.application.messaging;

public interface MailPort {

  void send(String to, String subject, String body);
}
