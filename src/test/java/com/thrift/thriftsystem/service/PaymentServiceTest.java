package com.thrift.thriftsystem.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.thrift.thriftsystem.enums.ContributionStatus;
import com.thrift.thriftsystem.exception.BadRequestException;
import com.thrift.thriftsystem.model.Contribution;
import com.thrift.thriftsystem.model.User;
import com.thrift.thriftsystem.repository.ContributionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaystackService paystackService;

    @Mock private ContributionRepository contributionRepository;

    @Mock private UserService userService;

    @Mock private EmailService emailService;

    @InjectMocks private PaymentService paymentService;

    private User currentUser;
    private Contribution contribution;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id("user123").email("john@example.com").build();

        contribution =
                Contribution.builder()
                        .id("contribution123")
                        .userId("user123")
                        .groupId("group123")
                        .amount(new BigDecimal("10000"))
                        .status(ContributionStatus.PENDING)
                        .build();
    }

    @Test
    void initializeContributionPayment_Success() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(contributionRepository.findById("contribution123"))
                .thenReturn(Optional.of(contribution));
        when(paystackService.generateReference()).thenReturn("THRIFT-123456");
        when(contributionRepository.save(any())).thenReturn(contribution);
        when(paystackService.initializePayment(any(), any(), any()))
                .thenReturn(
                        Map.of(
                                "status",
                                true,
                                "data",
                                Map.of("authorization_url", "https://paystack.com/pay/test")));

        Map<String, Object> result =
                paymentService.initializeContributionPayment("contribution123");

        assertNotNull(result);
        verify(contributionRepository, times(1)).save(any());
        verify(paystackService, times(1)).initializePayment(any(), any(), any());
    }

    @Test
    void initializeContributionPayment_WrongUser_ThrowsException() {
        contribution.setUserId("differentUser");
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(contributionRepository.findById(any())).thenReturn(Optional.of(contribution));

        assertThrows(
                BadRequestException.class,
                () -> paymentService.initializeContributionPayment("contribution123"));
    }

    @Test
    void initializeContributionPayment_NotPending_ThrowsException() {
        contribution.setStatus(ContributionStatus.SUCCESSFUL);
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(contributionRepository.findById(any())).thenReturn(Optional.of(contribution));

        assertThrows(
                BadRequestException.class,
                () -> paymentService.initializeContributionPayment("contribution123"));
    }

    @Test
    void handlePaymentSuccess_Verified() {
        contribution.setPaystackReference("THRIFT-123");
        when(contributionRepository.findByAnyPaystackReference("THRIFT-123"))
                .thenReturn(Optional.of(contribution));
        when(paystackService.verifyPayment("THRIFT-123")).thenReturn(true);
        when(contributionRepository.save(any())).thenReturn(contribution);

        paymentService.handlePaymentSuccess("THRIFT-123");

        verify(contributionRepository, times(1)).save(any());
        assertEquals(ContributionStatus.SUCCESSFUL, contribution.getStatus());
    }

    @Test
    void handlePaymentSuccess_NotVerified() {
        contribution.setPaystackReference("THRIFT-123");
        when(contributionRepository.findByAnyPaystackReference("THRIFT-123"))
                .thenReturn(Optional.of(contribution));
        when(paystackService.verifyPayment("THRIFT-123")).thenReturn(false);
        when(contributionRepository.save(any())).thenReturn(contribution);

        paymentService.handlePaymentSuccess("THRIFT-123");

        verify(contributionRepository, times(1)).save(any());
        assertEquals(ContributionStatus.FAILED, contribution.getStatus());
    }

    @Test
    void initializeContributionPayment_AbandonedCheckout_StartsNewCheckout() {
        contribution.setStatus(ContributionStatus.PROCESSING);
        contribution.setPaystackReference("THRIFT-old");
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(contributionRepository.findById("contribution123"))
                .thenReturn(Optional.of(contribution));
        when(paystackService.verifyPayment("THRIFT-old")).thenReturn(false);
        when(paystackService.generateReference()).thenReturn("THRIFT-new");
        when(paystackService.initializePayment(any(), any(), any()))
                .thenReturn(Map.of("status", true));

        paymentService.initializeContributionPayment("contribution123");

        assertEquals("THRIFT-new", contribution.getPaystackReference());
        assertEquals(List.of("THRIFT-old"), contribution.getPreviousPaystackReferences());
        assertEquals(ContributionStatus.PROCESSING, contribution.getStatus());
        verify(paystackService)
                .initializePayment("john@example.com", new BigDecimal("10000"), "THRIFT-new");
    }

    @Test
    void initializeContributionPayment_FailedPayment_CanRetry() {
        contribution.setStatus(ContributionStatus.FAILED);
        contribution.setPaystackReference("THRIFT-old");
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(contributionRepository.findById("contribution123"))
                .thenReturn(Optional.of(contribution));
        when(paystackService.verifyPayment("THRIFT-old")).thenReturn(false);
        when(paystackService.generateReference()).thenReturn("THRIFT-new");
        when(paystackService.initializePayment(any(), any(), any()))
                .thenReturn(Map.of("status", true));

        paymentService.initializeContributionPayment("contribution123");

        verify(paystackService).initializePayment(any(), any(), eq("THRIFT-new"));
    }

    @Test
    void initializeContributionPayment_PreviousCheckoutWasPaid_MarksPaidInsteadOfCharging() {
        contribution.setStatus(ContributionStatus.PROCESSING);
        contribution.setPaystackReference("THRIFT-old");
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(contributionRepository.findById("contribution123"))
                .thenReturn(Optional.of(contribution));
        when(paystackService.verifyPayment("THRIFT-old")).thenReturn(true);

        assertThrows(
                BadRequestException.class,
                () -> paymentService.initializeContributionPayment("contribution123"));

        assertEquals(ContributionStatus.SUCCESSFUL, contribution.getStatus());
        assertNotNull(contribution.getPaidAt());
        verify(paystackService, never()).initializePayment(any(), any(), any());
    }

    @Test
    void handlePaymentSuccess_AlreadyRecorded_Ignored() {
        contribution.setPaystackReference("THRIFT-123");
        contribution.setStatus(ContributionStatus.SUCCESSFUL);
        when(contributionRepository.findByAnyPaystackReference("THRIFT-123"))
                .thenReturn(Optional.of(contribution));

        paymentService.handlePaymentSuccess("THRIFT-123");

        verify(paystackService, never()).verifyPayment(any());
        verify(contributionRepository, never()).save(any());
    }

    private void givenStaleCheckout() {
        contribution.setStatus(ContributionStatus.PROCESSING);
        contribution.setPaystackReference("THRIFT-new");
        contribution.getPreviousPaystackReferences().add("THRIFT-old");
        when(contributionRepository.findByAnyPaystackReference("THRIFT-old"))
                .thenReturn(Optional.of(contribution));
    }

    @Test
    void handlePaymentSuccess_StaleCheckoutPaid_RecordsPayment() {
        givenStaleCheckout();
        when(paystackService.verifyPayment("THRIFT-old")).thenReturn(true);

        paymentService.handlePaymentSuccess("THRIFT-old");

        assertEquals(ContributionStatus.SUCCESSFUL, contribution.getStatus());
        assertEquals("THRIFT-old", contribution.getPaystackReference());
        assertEquals(List.of("THRIFT-new"), contribution.getPreviousPaystackReferences());
        verify(paystackService, never()).refundPayment(any());
    }

    @Test
    void handlePaymentSuccess_StaleCheckoutNotVerified_LeavesContributionAlone() {
        givenStaleCheckout();
        when(paystackService.verifyPayment("THRIFT-old")).thenReturn(false);

        paymentService.handlePaymentSuccess("THRIFT-old");

        assertEquals(ContributionStatus.PROCESSING, contribution.getStatus());
        assertEquals("THRIFT-new", contribution.getPaystackReference());
        verify(contributionRepository, never()).save(any());
    }

    @Test
    void handlePaymentSuccess_PaidTwice_RefundsExtraPayment() {
        givenStaleCheckout();
        contribution.setStatus(ContributionStatus.SUCCESSFUL);
        when(paystackService.verifyPayment("THRIFT-old")).thenReturn(true);
        when(paystackService.refundPayment("THRIFT-old")).thenReturn(true);

        paymentService.handlePaymentSuccess("THRIFT-old");

        assertEquals(List.of("THRIFT-old"), contribution.getRefundedPaystackReferences());
        assertEquals("THRIFT-new", contribution.getPaystackReference());
        assertEquals(ContributionStatus.SUCCESSFUL, contribution.getStatus());
    }

    @Test
    void handlePaymentSuccess_RefundFails_ThrowsSoPaystackRetries() {
        givenStaleCheckout();
        contribution.setStatus(ContributionStatus.SUCCESSFUL);
        when(paystackService.verifyPayment("THRIFT-old")).thenReturn(true);
        when(paystackService.refundPayment("THRIFT-old")).thenReturn(false);

        assertThrows(
                BadRequestException.class, () -> paymentService.handlePaymentSuccess("THRIFT-old"));
        assertTrue(contribution.getRefundedPaystackReferences().isEmpty());
    }

    @Test
    void handlePaymentSuccess_AlreadyRefunded_Ignored() {
        givenStaleCheckout();
        contribution.setStatus(ContributionStatus.SUCCESSFUL);
        contribution.getRefundedPaystackReferences().add("THRIFT-old");

        paymentService.handlePaymentSuccess("THRIFT-old");

        verify(paystackService, never()).verifyPayment(any());
        verify(paystackService, never()).refundPayment(any());
    }
}
