package com.hireconnect.subscription.service.impl;

import com.hireconnect.subscription.dto.request.SubscribeRequest;
import com.hireconnect.subscription.dto.response.InvoiceResponse;
import com.hireconnect.subscription.dto.response.SubscriptionResponse;
import com.hireconnect.subscription.entity.Invoice;
import com.hireconnect.subscription.entity.Subscription;
import com.hireconnect.subscription.enums.SubscriptionPlan;
import com.hireconnect.subscription.enums.SubscriptionStatus;
import com.hireconnect.subscription.exception.ResourceNotFoundException;
import com.hireconnect.subscription.mapper.SubscriptionMapper;
import com.hireconnect.subscription.repository.InvoiceRepository;
import com.hireconnect.subscription.repository.SubscriptionRepository;
import com.hireconnect.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final SubscriptionMapper subscriptionMapper;

    // Plan configs
    private static final int FREE_MAX_POSTS         = 3;
    private static final int PROFESSIONAL_MAX_POSTS  = 50;
    private static final int ENTERPRISE_MAX_POSTS    = 999;
    private static final double PROFESSIONAL_PRICE   = 1999.0;
    private static final double ENTERPRISE_PRICE     = 4999.0;
    private static final double GST_RATE             = 0.18;
    private static final int PLAN_DURATION_DAYS      = 30;

    @Override
    public SubscriptionResponse subscribe(Long recruiterId, SubscribeRequest request) {
        log.info("Recruiter {} subscribing to plan: {}", recruiterId, request.getPlan());

        // Cancel any existing active subscription
        subscriptionRepository.findActiveByRecruiterId(recruiterId, LocalDate.now())
                .ifPresent(existing -> {
                    existing.setStatus(SubscriptionStatus.CANCELLED);
                    subscriptionRepository.save(existing);
                    log.info("Cancelled existing subscription {} for recruiter {}", existing.getSubscriptionId(), recruiterId);
                });

        double amount = getPlanPrice(request.getPlan());
        int maxPosts  = getMaxPosts(request.getPlan());
        LocalDate startDate = LocalDate.now();
        LocalDate endDate   = request.getPlan() == SubscriptionPlan.FREE ? null : startDate.plusDays(PLAN_DURATION_DAYS);

        Subscription subscription = Subscription.builder()
                .recruiterId(recruiterId)
                .plan(request.getPlan())
                .startDate(startDate)
                .endDate(endDate)
                .status(SubscriptionStatus.ACTIVE)
                .amountPaid(amount)
                .maxJobPosts(maxPosts)
                .autoRenew(request.getAutoRenew())
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Subscription {} created for recruiter {}", saved.getSubscriptionId(), recruiterId);

        // Generate invoice for paid plans
        if (amount > 0) {
            generateInvoiceInternal(saved, request);
        }

        return subscriptionMapper.toResponse(saved);
    }

    @Override
    public SubscriptionResponse cancelSubscription(Long recruiterId) {
        Subscription subscription = subscriptionRepository
                .findActiveByRecruiterId(recruiterId, LocalDate.now())
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription found for recruiter: " + recruiterId));

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setAutoRenew(false);
        Subscription updated = subscriptionRepository.save(subscription);
        log.info("Subscription {} cancelled for recruiter {}", subscription.getSubscriptionId(), recruiterId);

        return subscriptionMapper.toResponse(updated);
    }

    @Override
    public SubscriptionResponse renewSubscription(Long recruiterId, SubscribeRequest request) {
        log.info("Renewing subscription for recruiter {}", recruiterId);
        return subscribe(recruiterId, request);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse getActiveSubscription(Long recruiterId) {
        Subscription subscription = subscriptionRepository
                .findActiveByRecruiterId(recruiterId, LocalDate.now())
                .orElseGet(() -> Subscription.builder()
                        .recruiterId(recruiterId)
                        .plan(SubscriptionPlan.FREE)
                        .status(SubscriptionStatus.ACTIVE)
                        .amountPaid(0.0)
                        .maxJobPosts(FREE_MAX_POSTS)
                        .build());
        return subscriptionMapper.toResponse(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getAllSubscriptionsByRecruiter(Long recruiterId) {
        return subscriptionRepository.findByRecruiterId(recruiterId)
                .stream().map(subscriptionMapper::toResponse).toList();
    }

    @Override
    public InvoiceResponse generateInvoice(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", subscriptionId));
        return subscriptionMapper.toInvoiceResponse(
                invoiceRepository.findFirstBySubscriptionIdOrderByCreatedAtDesc(subscriptionId)
                        .orElseThrow(() -> new ResourceNotFoundException("Invoice not found for subscription: " + subscriptionId))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getInvoicesByRecruiter(Long recruiterId, Pageable pageable) {
        return invoiceRepository.findByRecruiterIdOrderByCreatedAtDesc(recruiterId, pageable)
                .map(subscriptionMapper::toInvoiceResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAllInvoicesByRecruiter(Long recruiterId) {
        return invoiceRepository.findByRecruiterIdOrderByCreatedAtDesc(recruiterId)
                .stream().map(subscriptionMapper::toInvoiceResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));
        return subscriptionMapper.toInvoiceResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveSubscription(Long recruiterId) {
        return subscriptionRepository.findActiveByRecruiterId(recruiterId, LocalDate.now()).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public int getMaxJobPosts(Long recruiterId) {
        return subscriptionRepository.findActiveByRecruiterId(recruiterId, LocalDate.now())
                .map(s -> s.getMaxJobPosts() != null ? s.getMaxJobPosts() : FREE_MAX_POSTS)
                .orElse(FREE_MAX_POSTS);
    }

    // ---- private helpers ----

    private void generateInvoiceInternal(Subscription subscription, SubscribeRequest request) {
        double baseAmount = subscription.getAmountPaid();
        double gstAmount  = baseAmount * GST_RATE;
        double total      = baseAmount + gstAmount;

        Invoice invoice = Invoice.builder()
                .subscriptionId(subscription.getSubscriptionId())
                .recruiterId(subscription.getRecruiterId())
                .amount(baseAmount)
                .gstAmount(gstAmount)
                .totalAmount(total)
                .paymentDate(LocalDateTime.now())
                .paymentMode(request.getPaymentMode())
                .transactionId(UUID.randomUUID().toString().replace("-", "").toUpperCase())
                .invoiceNumber(generateInvoiceNumber())
                .planName(subscription.getPlan().name())
                .build();

        invoiceRepository.save(invoice);
        log.info("Invoice {} generated for subscription {}", invoice.getInvoiceNumber(), subscription.getSubscriptionId());
    }

    private String generateInvoiceNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "HC-INV-" + date + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private double getPlanPrice(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE         -> 0.0;
            case PROFESSIONAL -> PROFESSIONAL_PRICE;
            case ENTERPRISE   -> ENTERPRISE_PRICE;
        };
    }

    private int getMaxPosts(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE         -> FREE_MAX_POSTS;
            case PROFESSIONAL -> PROFESSIONAL_MAX_POSTS;
            case ENTERPRISE   -> ENTERPRISE_MAX_POSTS;
        };
    }
}
