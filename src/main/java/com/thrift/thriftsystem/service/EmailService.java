package com.thrift.thriftsystem.service;

import com.thrift.thriftsystem.model.ThriftGroup;
import com.thrift.thriftsystem.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendWelcomeEmail(User user) {

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(user.getEmail());
            mailMessage.setSubject("Welcome to Thrift System");
            mailMessage.setText(
                    "Hi "
                            + user.getFirstName()
                            + ",\n\n"
                            + "Welcome to Thrift System! your account has been successfully created.\n\n"
                            + "You can now join or create thrift groups.\n\n"
                            + "Best regards,\n"
                            + "Thrift System Team");

            javaMailSender.send(mailMessage);
            log.info("Welcome email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}:{}", user.getEmail(), e.getMessage());
        }
    }

    public void sendContributionReminder(User user, ThriftGroup group, String dueDate) {

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(user.getEmail());
            mailMessage.setSubject("Contribution Reminder - " + group.getName());
            mailMessage.setText(
                    "Hi "
                            + user.getFirstName()
                            + ",\n\n"
                            + "This is a reminder that your contribution for "
                            + group.getName()
                            + "is  due on "
                            + dueDate
                            + "\n\n"
                            + "Amount: "
                            + group.getContributionAmount()
                            + " "
                            + group.getCurrency()
                            + "\n\n"
                            + "Please ensure you make your payment on time.\n\n"
                            + "Best regards,\n"
                            + "Thrift System Team");
            javaMailSender.send(mailMessage);
            log.info("Contribution Reminder sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error(
                    "Failed to send contribution reminder to {}:{}",
                    user.getEmail(),
                    e.getMessage());
        }
    }

    public void sendPayoutNotification(User user, ThriftGroup group, String amount) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(user.getEmail());
            mailMessage.setSubject("Payout Notification - " + group.getName());
            mailMessage.setText(
                    "Hi "
                            + user.getFirstName()
                            + ",\n\n"
                            + "Congratulations! Your payout of "
                            + amount
                            + " from "
                            + group.getName()
                            + " has been processed successfully.\n\n"
                            + "Best regards,\n"
                            + "Thrift System Team");
            javaMailSender.send(mailMessage);
            log.info("Payout notification sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error(
                    "Failed to send payout notification to {}:{}", user.getEmail(), e.getMessage());
        }
    }

    public void sendGroupJointedNotification(User user, ThriftGroup group) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(user.getEmail());
            mailMessage.setSubject("You've Joined " + group.getName());
            mailMessage.setText(
                    "Hi "
                            + user.getFirstName()
                            + ",\n\n"
                            + "You've successfully joined the thrift group "
                            + group.getName()
                            + "\n\n"
                            + "Contribution amount: "
                            + group.getContributionAmount()
                            + " "
                            + group.getCurrency()
                            + "\n"
                            + "Frequency: "
                            + group.getFrequency()
                            + "\n\n"
                            + "\"Best regards,\\n\"+\"Thrift System Team\"");
            javaMailSender.send(mailMessage);
            log.info("Group jointed notification sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error(
                    "Failed to send group jointed notification to {}: {}",
                    user.getEmail(),
                    e.getMessage());
        }
    }

    public void sendHolidayNotification(User user, String holidayName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Public Holiday Notice - " + holidayName);
            message.setText(
                    "Hi "
                            + user.getFirstName()
                            + ",\n\n"
                            + "Today is a public holiday — "
                            + holidayName
                            + "!\n\n"
                            + "Your thrift contributions scheduled for today have been "
                            + "automatically moved to the next working day.\n\n"
                            + "Best regards,\n"
                            + "Thrift System Team");
            javaMailSender.send(message);
            log.info("Holiday notification sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error(
                    "Failed to send holiday notification to {}: {}",
                    user.getEmail(),
                    e.getMessage());
        }
    }
}
