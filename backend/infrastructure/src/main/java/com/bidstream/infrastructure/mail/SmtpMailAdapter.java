package com.bidstream.infrastructure.mail;

import com.bidstream.application.messaging.MailPort;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpMailAdapter implements MailPort {

  private final JavaMailSender mailSender;

  public SmtpMailAdapter(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  @Override
  public void send(String to, String subject, String body) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(to);
    message.setSubject(subject);
    message.setText(body);
    message.setFrom("noreply@bidstream.local");
    mailSender.send(message);
  }
}
