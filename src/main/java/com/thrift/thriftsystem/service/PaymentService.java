package com.thrift.thriftsystem.service;

import com.thrift.thriftsystem.enums.ContributionStatus;
import com.thrift.thriftsystem.exception.BadRequestException;
import com.thrift.thriftsystem.exception.ResourceNotFoundException;
import com.thrift.thriftsystem.model.Contribution;
import com.thrift.thriftsystem.model.User;
import com.thrift.thriftsystem.repository.ContributionRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaystackService paystackService;
    private final ContributionRepository contributionRepository;
    private final UserService userService;
    private final EmailService emailService;

    public Map<String, Object> initializeContributionPayment(String contributionId) {
        User currentUser = userService.getCurrentUser();

        Contribution contribution =
                contributionRepository
                        .findById(contributionId)
                        .orElseThrow(() -> new ResourceNotFoundException("Contribution not found"));

        if (!contribution.getUserId().equals(currentUser.getId())) {
            throw new BadRequestException("You can only pay for your own contributions");
        }

        if (contribution.getStatus() != ContributionStatus.PENDING) {
            throw new BadRequestException("Contribution is not in pending state");
        }

        String reference = paystackService.generateReference();
        contribution.setPaystackReference(reference);
        contribution.setStatus(ContributionStatus.PROCESSING);
        contributionRepository.save(contribution);

        return paystackService.initializePayment(
                currentUser.getEmail(), contribution.getAmount(), reference);
    }

    public void handlePaymentSuccess(String reference) {
        Contribution contribution =
                contributionRepository
                        .findByPaystackReference(reference)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Contribution not found for reference: "
                                                        + reference));

        boolean verified = paystackService.verifyPayment(reference);

        if (verified) {
            contribution.setStatus(ContributionStatus.SUCCESSFUL);
            contribution.setPaidAt(java.time.LocalDateTime.now());
            contributionRepository.save(contribution);
            log.info("Payment successful for reference: {}", reference);
        } else {
            contribution.setStatus(ContributionStatus.FAILED);
            contributionRepository.save(contribution);
            log.error("Payment verification failed for reference: {}", reference);
        }
    }
}
