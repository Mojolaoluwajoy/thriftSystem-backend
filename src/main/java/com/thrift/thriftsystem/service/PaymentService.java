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

        if (contribution.getStatus() == ContributionStatus.SUCCESSFUL
                || contribution.getStatus() == ContributionStatus.REFUNDED) {
            throw new BadRequestException("Contribution has already been paid");
        }

        // A previous checkout may have been abandoned, or paid without the webhook arriving
        // yet; check it with Paystack before starting a new one so the member isn't charged twice
        if (contribution.getPaystackReference() != null
                && paystackService.verifyPayment(contribution.getPaystackReference())) {
            markPaid(contribution);
            throw new BadRequestException("Contribution has already been paid");
        }

        String reference = paystackService.generateReference();
        if (contribution.getPaystackReference() != null) {
            contribution.getPreviousPaystackReferences().add(contribution.getPaystackReference());
        }
        contribution.setPaystackReference(reference);
        contribution.setStatus(ContributionStatus.PROCESSING);
        contributionRepository.save(contribution);

        return paystackService.initializePayment(
                currentUser.getEmail(), contribution.getAmount(), reference);
    }

    public void handlePaymentSuccess(String reference) {
        Contribution contribution =
                contributionRepository
                        .findByAnyPaystackReference(reference)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Contribution not found for reference: "
                                                        + reference));

        boolean isCurrentReference = reference.equals(contribution.getPaystackReference());
        boolean alreadyPaid = contribution.getStatus() == ContributionStatus.SUCCESSFUL;

        // Paystack can deliver the same event more than once
        if ((alreadyPaid && isCurrentReference)
                || contribution.getRefundedPaystackReferences().contains(reference)) {
            log.info("Payment already recorded for reference: {}", reference);
            return;
        }

        boolean verified = paystackService.verifyPayment(reference);

        if (!verified) {
            // Only the live checkout decides the contribution's state; a stale one failing
            // doesn't change anything
            if (isCurrentReference && !alreadyPaid) {
                contribution.setStatus(ContributionStatus.FAILED);
                contributionRepository.save(contribution);
            }
            log.error("Payment verification failed for reference: {}", reference);
            return;
        }

        if (alreadyPaid) {
            // The member paid through two checkouts; give the extra payment back
            if (paystackService.refundPayment(reference)) {
                contribution.getRefundedPaystackReferences().add(reference);
                contributionRepository.save(contribution);
                log.warn("Duplicate payment {} refunded", reference);
                return;
            }
            // Throwing makes Paystack redeliver the webhook, which retries the refund
            throw new BadRequestException("Refund failed for duplicate payment " + reference);
        }

        if (!isCurrentReference) {
            // An older checkout was paid; make it the contribution's reference of record
            contribution.getPreviousPaystackReferences().remove(reference);
            contribution.getPreviousPaystackReferences().add(contribution.getPaystackReference());
            contribution.setPaystackReference(reference);
        }
        markPaid(contribution);
        log.info("Payment successful for reference: {}", reference);
    }

    private void markPaid(Contribution contribution) {
        contribution.setStatus(ContributionStatus.SUCCESSFUL);
        contribution.setPaidAt(java.time.LocalDateTime.now());
        contributionRepository.save(contribution);
    }
}
