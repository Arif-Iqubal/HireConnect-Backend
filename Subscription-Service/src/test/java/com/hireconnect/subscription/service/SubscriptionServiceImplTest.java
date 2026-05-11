package com.hireconnect.subscription.service;

import com.hireconnect.subscription.dto.request.SubscribeRequest;
import com.hireconnect.subscription.dto.response.InvoiceResponse;
import com.hireconnect.subscription.dto.response.SubscriptionResponse;
import com.hireconnect.subscription.entity.Invoice;
import com.hireconnect.subscription.entity.Subscription;
import com.hireconnect.subscription.enums.PaymentMode;
import com.hireconnect.subscription.enums.SubscriptionPlan;
import com.hireconnect.subscription.enums.SubscriptionStatus;
import com.hireconnect.subscription.exception.ResourceNotFoundException;
import com.hireconnect.subscription.mapper.SubscriptionMapper;
import com.hireconnect.subscription.repository.InvoiceRepository;
import com.hireconnect.subscription.repository.SubscriptionRepository;
import com.hireconnect.subscription.service.impl.SubscriptionServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import com.hireconnect.subscription.dto.request.RazorpayOrderRequest;
import com.hireconnect.subscription.dto.request.RazorpayVerifyRequest;
import com.hireconnect.subscription.dto.response.RazorpayOrderResponse;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionServiceImpl Tests")
class SubscriptionServiceImplTest {

	@Mock
	private SubscriptionRepository subscriptionRepository;
	@Mock
	private InvoiceRepository invoiceRepository;
	@Mock
	private SubscriptionMapper subscriptionMapper;

	@Mock
	private RestTemplate restTemplate;
	@Mock
	private RabbitTemplate rabbitTemplate;

	@InjectMocks
	private SubscriptionServiceImpl subscriptionService;

	// ─── Fixtures ──────────────────────────────────────────────────────────────

	private Subscription buildSubscription(Long id, SubscriptionPlan plan, SubscriptionStatus status) {
		return Subscription.builder().subscriptionId(id).recruiterId(1L).plan(plan).startDate(LocalDate.now())
				.endDate(plan == SubscriptionPlan.FREE ? null : LocalDate.now().plusDays(30)).status(status)
				.amountPaid(planPrice(plan)).maxJobPosts(planMaxPosts(plan)).autoRenew(false).build();
	}

	private SubscriptionResponse buildSubResponse(Subscription s) {
		return SubscriptionResponse.builder().subscriptionId(s.getSubscriptionId()).recruiterId(s.getRecruiterId())
				.plan(s.getPlan()).status(s.getStatus()).amountPaid(s.getAmountPaid()).maxJobPosts(s.getMaxJobPosts())
				.isActive(s.isActive()).build();
	}

	private InvoiceResponse buildInvoiceResponse(Long subId) {
		return InvoiceResponse.builder().invoiceId(1L).subscriptionId(subId).recruiterId(1L).amount(1999.0)
				.gstAmount(359.82).totalAmount(2358.82).paymentMode(PaymentMode.UPI).paymentDate(LocalDateTime.now())
				.invoiceNumber("HC-INV-20260101-ABCD1234").planName("PROFESSIONAL").build();
	}

	private double planPrice(SubscriptionPlan plan) {
		return switch (plan) {
		case FREE -> 0.0;
		case PROFESSIONAL -> 1999.0;
		case ENTERPRISE -> 4999.0;
		default -> throw new IllegalArgumentException("Unsupported plan: " + plan);
		};
	}

	private int planMaxPosts(SubscriptionPlan plan) {
		return switch (plan) {
		case FREE -> 3;
		case PROFESSIONAL -> 50;
		case ENTERPRISE -> 999;
		default -> throw new IllegalArgumentException("Unsupported plan: " + plan);
		};
	}

	// ─── subscribe() ───────────────────────────────────────────────────────────

	@Nested
	@DisplayName("subscribe()")
	class SubscribeTests {

		@Test
		@DisplayName("should create PROFESSIONAL subscription and generate invoice")
		void shouldCreateProfessionalSubscription() {
			SubscribeRequest request = new SubscribeRequest();
			request.setPlan(SubscriptionPlan.PROFESSIONAL);
			request.setPaymentMode(PaymentMode.UPI);

			Subscription saved = buildSubscription(1L, SubscriptionPlan.PROFESSIONAL, SubscriptionStatus.ACTIVE);
			SubscriptionResponse response = buildSubResponse(saved);

			when(subscriptionRepository.findActiveByRecruiterId(eq(1L), any(LocalDate.class)))
					.thenReturn(Optional.empty());
			when(subscriptionRepository.save(any(Subscription.class))).thenReturn(saved);
			when(invoiceRepository.save(any(Invoice.class))).thenReturn(Invoice.builder().build());
			when(subscriptionMapper.toResponse(saved)).thenReturn(response);

			SubscriptionResponse result = subscriptionService.subscribe(1L, request);

			assertThat(result).isNotNull();
			assertThat(result.getPlan()).isEqualTo(SubscriptionPlan.PROFESSIONAL);
			assertThat(result.getMaxJobPosts()).isEqualTo(50);
			assertThat(result.getAmountPaid()).isEqualTo(1999.0);

			// Invoice must be generated for paid plan
			verify(invoiceRepository).save(any(Invoice.class));
		}

		@Test
		@DisplayName("should create FREE subscription without generating invoice")
		void shouldCreateFreeSubscriptionWithoutInvoice() {
			SubscribeRequest request = new SubscribeRequest();
			request.setPlan(SubscriptionPlan.FREE);
			request.setPaymentMode(PaymentMode.WALLET);

			Subscription saved = buildSubscription(1L, SubscriptionPlan.FREE, SubscriptionStatus.ACTIVE);
			SubscriptionResponse response = buildSubResponse(saved);

			when(subscriptionRepository.findActiveByRecruiterId(eq(1L), any(LocalDate.class)))
					.thenReturn(Optional.empty());
			when(subscriptionRepository.save(any(Subscription.class))).thenReturn(saved);
			when(subscriptionMapper.toResponse(saved)).thenReturn(response);

			SubscriptionResponse result = subscriptionService.subscribe(1L, request);

			assertThat(result.getAmountPaid()).isEqualTo(0.0);
			// No invoice for free plan
			verify(invoiceRepository, never()).save(any(Invoice.class));
		}

		@Test
		@DisplayName("should cancel existing subscription before creating new one")
		void shouldCancelExistingBeforeNewSubscription() {
			SubscribeRequest request = new SubscribeRequest();
			request.setPlan(SubscriptionPlan.ENTERPRISE);
			request.setPaymentMode(PaymentMode.CREDIT_CARD);

			Subscription existing = buildSubscription(1L, SubscriptionPlan.PROFESSIONAL, SubscriptionStatus.ACTIVE);
			Subscription newSub = buildSubscription(2L, SubscriptionPlan.ENTERPRISE, SubscriptionStatus.ACTIVE);
			SubscriptionResponse response = buildSubResponse(newSub);

			when(subscriptionRepository.findActiveByRecruiterId(eq(1L), any(LocalDate.class)))
					.thenReturn(Optional.of(existing));
			when(subscriptionRepository.save(any(Subscription.class))).thenReturn(newSub);
			when(invoiceRepository.save(any(Invoice.class))).thenReturn(Invoice.builder().build());
			when(subscriptionMapper.toResponse(newSub)).thenReturn(response);

			subscriptionService.subscribe(1L, request);

			// existing must be cancelled — save called at least twice (cancel + new)
			verify(subscriptionRepository, atLeast(2)).save(any(Subscription.class));
		}
	}

	// ─── cancelSubscription() ──────────────────────────────────────────────────

	@Nested
	@DisplayName("cancelSubscription()")
	class CancelTests {

		@Test
		@DisplayName("should cancel active subscription")
		void shouldCancelActiveSubscription() {
			Subscription active = buildSubscription(1L, SubscriptionPlan.PROFESSIONAL, SubscriptionStatus.ACTIVE);
			SubscriptionResponse response = buildSubResponse(active);
			response.setStatus(SubscriptionStatus.CANCELLED);

			when(subscriptionRepository.findActiveByRecruiterId(eq(1L), any(LocalDate.class)))
					.thenReturn(Optional.of(active));
			when(subscriptionRepository.save(any(Subscription.class))).thenReturn(active);
			when(subscriptionMapper.toResponse(any())).thenReturn(response);

			SubscriptionResponse result = subscriptionService.cancelSubscription(1L);

			assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
			verify(subscriptionRepository).save(argThat(s -> s.getStatus() == SubscriptionStatus.CANCELLED));
		}

		@Test
		@DisplayName("should throw ResourceNotFoundException when no active subscription")
		void shouldThrowWhenNoActiveSubscription() {
			when(subscriptionRepository.findActiveByRecruiterId(eq(1L), any(LocalDate.class)))
					.thenReturn(Optional.empty());

			assertThatThrownBy(() -> subscriptionService.cancelSubscription(1L))
					.isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("No active subscription");
		}
	}

	// ─── getActiveSubscription() ───────────────────────────────────────────────

	@Nested
	@DisplayName("getActiveSubscription()")
	class GetActiveTests {

		@Test
		@DisplayName("should return active subscription when it exists")
		void shouldReturnActiveSubscription() {
			Subscription active = buildSubscription(1L, SubscriptionPlan.PROFESSIONAL, SubscriptionStatus.ACTIVE);
			SubscriptionResponse response = buildSubResponse(active);

			when(subscriptionRepository.findActiveByRecruiterId(eq(1L), any(LocalDate.class)))
					.thenReturn(Optional.of(active));
			when(subscriptionMapper.toResponse(active)).thenReturn(response);

			SubscriptionResponse result = subscriptionService.getActiveSubscription(1L);

			assertThat(result.getPlan()).isEqualTo(SubscriptionPlan.PROFESSIONAL);
		}

		@Test
		@DisplayName("should return FREE plan when no active subscription exists")
		void shouldReturnFreePlanWhenNoActiveSubscription() {
			when(subscriptionRepository.findActiveByRecruiterId(eq(1L), any(LocalDate.class)))
					.thenReturn(Optional.empty());
			when(subscriptionMapper.toResponse(any(Subscription.class))).thenAnswer(inv -> {
				Subscription s = inv.getArgument(0);
				return buildSubResponse(s);
			});

			SubscriptionResponse result = subscriptionService.getActiveSubscription(1L);

			assertThat(result.getPlan()).isEqualTo(SubscriptionPlan.FREE);
		}
	}

	// ─── hasActiveSubscription() ───────────────────────────────────────────────

	@Test
	@DisplayName("hasActiveSubscription() should return true when active subscription exists")
	void shouldReturnTrueWhenActiveSubscriptionExists() {
		Subscription active = buildSubscription(1L, SubscriptionPlan.PROFESSIONAL, SubscriptionStatus.ACTIVE);
		when(subscriptionRepository.findActiveByRecruiterId(eq(1L), any(LocalDate.class)))
				.thenReturn(Optional.of(active));

		assertThat(subscriptionService.hasActiveSubscription(1L)).isTrue();
	}

	@Test
	@DisplayName("hasActiveSubscription() should return false when no subscription")
	void shouldReturnFalseWhenNoActiveSubscription() {
		when(subscriptionRepository.findActiveByRecruiterId(eq(1L), any(LocalDate.class))).thenReturn(Optional.empty());

		assertThat(subscriptionService.hasActiveSubscription(1L)).isFalse();
	}

	// ─── getMaxJobPosts() ──────────────────────────────────────────────────────

	@Test
	@DisplayName("getMaxJobPosts() should return 50 for PROFESSIONAL plan")
	void shouldReturn50ForProfessionalPlan() {
		Subscription sub = buildSubscription(1L, SubscriptionPlan.PROFESSIONAL, SubscriptionStatus.ACTIVE);
		when(subscriptionRepository.findActiveByRecruiterId(eq(1L), any(LocalDate.class))).thenReturn(Optional.of(sub));

		assertThat(subscriptionService.getMaxJobPosts(1L)).isEqualTo(50);
	}

	@Test
	@DisplayName("getMaxJobPosts() should return 3 (FREE) when no subscription exists")
	void shouldReturn3WhenNoSubscription() {
		when(subscriptionRepository.findActiveByRecruiterId(eq(1L), any(LocalDate.class))).thenReturn(Optional.empty());

		assertThat(subscriptionService.getMaxJobPosts(1L)).isEqualTo(3);
	}

	// ─── getInvoiceById() ──────────────────────────────────────────────────────

	@Test
	@DisplayName("getInvoiceById() should return invoice response")
	void shouldReturnInvoiceById() {
		Invoice invoice = Invoice.builder().invoiceId(1L).subscriptionId(1L).recruiterId(1L).amount(1999.0)
				.gstAmount(359.82).totalAmount(2358.82).paymentMode(PaymentMode.UPI).paymentDate(LocalDateTime.now())
				.invoiceNumber("HC-INV-20260101-ABCD1234").planName("PROFESSIONAL").build();

		InvoiceResponse response = buildInvoiceResponse(1L);

		when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
		when(subscriptionMapper.toInvoiceResponse(invoice)).thenReturn(response);

		InvoiceResponse result = subscriptionService.getInvoiceById(1L);

		assertThat(result.getInvoiceId()).isEqualTo(1L);
		assertThat(result.getPlanName()).isEqualTo("PROFESSIONAL");
	}

	@Test
	@DisplayName("getInvoiceById() should throw ResourceNotFoundException when not found")
	void shouldThrowWhenInvoiceNotFound() {
		when(invoiceRepository.findById(999L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> subscriptionService.getInvoiceById(999L)).isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("999");
	}

	@Test
	@DisplayName("renewSubscription should delegate to subscribe")
	void shouldRenewSubscription() {
		SubscribeRequest request = new SubscribeRequest();
		request.setPlan(SubscriptionPlan.PROFESSIONAL);
		request.setPaymentMode(PaymentMode.UPI);

		Subscription saved = buildSubscription(10L, SubscriptionPlan.PROFESSIONAL, SubscriptionStatus.ACTIVE);
		SubscriptionResponse response = buildSubResponse(saved);

		when(subscriptionRepository.findActiveByRecruiterId(eq(1L), any(LocalDate.class))).thenReturn(Optional.empty());

		when(subscriptionRepository.save(any(Subscription.class))).thenReturn(saved);

		when(invoiceRepository.save(any(Invoice.class))).thenReturn(Invoice.builder().build());

		when(subscriptionMapper.toResponse(saved)).thenReturn(response);

		SubscriptionResponse result = subscriptionService.renewSubscription(1L, request);

		assertThat(result).isNotNull();
		assertThat(result.getPlan()).isEqualTo(SubscriptionPlan.PROFESSIONAL);
	}

	@Test
	@DisplayName("getPlans should return all subscription plans")
	void shouldReturnAllPlans() {
		var plans = subscriptionService.getPlans();

		assertThat(plans).hasSize(3);

		assertThat(plans).extracting("plan").contains(SubscriptionPlan.FREE, SubscriptionPlan.PROFESSIONAL,
				SubscriptionPlan.ENTERPRISE);
	}

	@Test
	@DisplayName("generateInvoice should return latest invoice")
	void shouldGenerateInvoice() {
		Subscription subscription = buildSubscription(1L, SubscriptionPlan.PROFESSIONAL, SubscriptionStatus.ACTIVE);

		Invoice invoice = Invoice.builder().invoiceId(1L).subscriptionId(1L).invoiceNumber("INV-001").build();

		InvoiceResponse response = buildInvoiceResponse(1L);

		when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));

		when(invoiceRepository.findFirstBySubscriptionIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.of(invoice));

		when(subscriptionMapper.toInvoiceResponse(invoice)).thenReturn(response);

		InvoiceResponse result = subscriptionService.generateInvoice(1L);

		assertThat(result).isNotNull();
		assertThat(result.getInvoiceId()).isEqualTo(1L);
	}

	@Test
	@DisplayName("generateInvoice should throw when subscription not found")
	void shouldThrowWhenSubscriptionNotFound() {

		when(subscriptionRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> subscriptionService.generateInvoice(99L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("generateInvoice should throw when invoice not found")
	void shouldThrowWhenInvoiceNotFoundForSubscription() {

		Subscription subscription = buildSubscription(1L, SubscriptionPlan.PROFESSIONAL, SubscriptionStatus.ACTIVE);

		when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(subscription));

		when(invoiceRepository.findFirstBySubscriptionIdOrderByCreatedAtDesc(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> subscriptionService.generateInvoice(1L)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("getAllSubscriptionsByRecruiter should return mapped subscriptions")
	void shouldReturnAllSubscriptionsByRecruiter() {

		Subscription s1 = buildSubscription(1L, SubscriptionPlan.PROFESSIONAL, SubscriptionStatus.ACTIVE);

		Subscription s2 = buildSubscription(2L, SubscriptionPlan.ENTERPRISE, SubscriptionStatus.CANCELLED);

		when(subscriptionRepository.findByRecruiterId(1L)).thenReturn(List.of(s1, s2));

		when(subscriptionMapper.toResponse(any())).thenAnswer(invocation -> {
			Subscription sub = invocation.getArgument(0);
			return buildSubResponse(sub);
		});

		List<SubscriptionResponse> result = subscriptionService.getAllSubscriptionsByRecruiter(1L);

		assertThat(result).hasSize(2);
	}

	@Test
	@DisplayName("getAllInvoicesByRecruiter should return mapped invoices")
	void shouldReturnAllInvoicesByRecruiter() {

		Invoice invoice = Invoice.builder().invoiceId(1L).subscriptionId(1L).build();

		when(invoiceRepository.findByRecruiterIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(invoice));

		when(subscriptionMapper.toInvoiceResponse(invoice)).thenReturn(buildInvoiceResponse(1L));

		List<InvoiceResponse> result = subscriptionService.getAllInvoicesByRecruiter(1L);

		assertThat(result).hasSize(1);
	}

	@Test
	@DisplayName("getMaxJobPosts should fallback to FREE when max posts null")
	void shouldFallbackToFreePostsWhenNull() {

		Subscription subscription = buildSubscription(1L, SubscriptionPlan.PROFESSIONAL, SubscriptionStatus.ACTIVE);

		subscription.setMaxJobPosts(null);

		when(subscriptionRepository.findActiveByRecruiterId(eq(1L), any(LocalDate.class)))
				.thenReturn(Optional.of(subscription));

		int result = subscriptionService.getMaxJobPosts(1L);

		assertThat(result).isEqualTo(3);
	}

	@Test
	@DisplayName("createRazorpayOrder should create order successfully")
	void shouldCreateRazorpayOrder() {

		ReflectionTestUtils.setField(subscriptionService, "razorpayKeyId", "rzp_test");
		ReflectionTestUtils.setField(subscriptionService, "razorpayKeySecret", "secret");

		RazorpayOrderRequest request = new RazorpayOrderRequest();
		request.setPlan(SubscriptionPlan.PROFESSIONAL);

		Map<String, Object> body = new HashMap<>();
		body.put("id", "order_123");

		when(restTemplate.postForEntity(anyString(), any(), eq(Map.class))).thenReturn(ResponseEntity.ok(body));

		RazorpayOrderResponse response = subscriptionService.createRazorpayOrder(1L, request);

		assertThat(response).isNotNull();
		assertThat(response.getOrderId()).isEqualTo("order_123");
	}

	@Test
	@DisplayName("createRazorpayOrder should fail for FREE plan")
	void shouldFailForFreePlanOrder() {

		RazorpayOrderRequest request = new RazorpayOrderRequest();
		request.setPlan(SubscriptionPlan.FREE);

		assertThatThrownBy(() -> subscriptionService.createRazorpayOrder(1L, request))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("createRazorpayOrder should fail when credentials missing")
	void shouldFailWhenRazorpayCredentialsMissing() {

		ReflectionTestUtils.setField(subscriptionService, "razorpayKeyId", "");
		ReflectionTestUtils.setField(subscriptionService, "razorpayKeySecret", "");

		RazorpayOrderRequest request = new RazorpayOrderRequest();
		request.setPlan(SubscriptionPlan.PROFESSIONAL);

		assertThatThrownBy(() -> subscriptionService.createRazorpayOrder(1L, request))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("verifyRazorpayPayment should fail for invalid signature")
	void shouldFailForInvalidSignature() {

		ReflectionTestUtils.setField(subscriptionService, "razorpayKeyId", "rzp_test");
		ReflectionTestUtils.setField(subscriptionService, "razorpayKeySecret", "secret");

		RazorpayVerifyRequest request = new RazorpayVerifyRequest();
		request.setPlan(SubscriptionPlan.PROFESSIONAL);
		request.setRazorpayOrderId("order1");
		request.setRazorpayPaymentId("payment1");
		request.setRazorpaySignature("invalid");

		assertThatThrownBy(() -> subscriptionService.verifyRazorpayPayment(1L, request))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("verifyRazorpayPayment should fail for FREE plan")
	void shouldFailVerificationForFreePlan() {

		RazorpayVerifyRequest request = new RazorpayVerifyRequest();
		request.setPlan(SubscriptionPlan.FREE);

		assertThatThrownBy(() -> subscriptionService.verifyRazorpayPayment(1L, request))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	@DisplayName("subscribe should continue when notification publishing fails")
	void shouldContinueWhenNotificationFails() {

		SubscribeRequest request = new SubscribeRequest();
		request.setPlan(SubscriptionPlan.PROFESSIONAL);
		request.setPaymentMode(PaymentMode.UPI);

		Subscription saved = buildSubscription(1L, SubscriptionPlan.PROFESSIONAL, SubscriptionStatus.ACTIVE);

		SubscriptionResponse response = buildSubResponse(saved);

		when(subscriptionRepository.findActiveByRecruiterId(eq(1L), any(LocalDate.class))).thenReturn(Optional.empty());

		when(subscriptionRepository.save(any())).thenReturn(saved);

		when(invoiceRepository.save(any())).thenReturn(Invoice.builder().build());

		when(subscriptionMapper.toResponse(saved)).thenReturn(response);

		when(restTemplate.getForEntity(anyString(), eq(Map.class))).thenThrow(new RuntimeException("Rabbit failure"));

		SubscriptionResponse result = subscriptionService.subscribe(1L, request);

		assertThat(result).isNotNull();
	}
}
