package com.hireconnect.subscription.controller;

import com.hireconnect.subscription.dto.request.RazorpayOrderRequest;
import com.hireconnect.subscription.dto.request.RazorpayVerifyRequest;
import com.hireconnect.subscription.dto.request.SubscribeRequest;
import com.hireconnect.subscription.dto.response.InvoiceResponse;
import com.hireconnect.subscription.dto.response.RazorpayOrderResponse;
import com.hireconnect.subscription.dto.response.SubscriptionPlanResponse;
import com.hireconnect.subscription.dto.response.SubscriptionResponse;
import com.hireconnect.subscription.enums.SubscriptionPlan;
import com.hireconnect.subscription.enums.SubscriptionStatus;
import com.hireconnect.subscription.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock private SubscriptionService subscriptionService;
    @InjectMocks private SubscriptionController controller;

    private final SubscriptionResponse subscription = SubscriptionResponse.builder()
            .subscriptionId(1L)
            .recruiterId(7L)
            .plan(SubscriptionPlan.PROFESSIONAL)
            .status(SubscriptionStatus.ACTIVE)
            .build();
    private final InvoiceResponse invoice = InvoiceResponse.builder().invoiceId(3L).recruiterId(7L).build();

    @Test
    void subscribePaymentAndLifecycleEndpointsDelegate() {
        SubscribeRequest subscribeRequest = new SubscribeRequest();
        subscribeRequest.setPlan(SubscriptionPlan.PROFESSIONAL);
        RazorpayOrderRequest orderRequest = new RazorpayOrderRequest();
        RazorpayVerifyRequest verifyRequest = new RazorpayVerifyRequest();
        RazorpayOrderResponse order = RazorpayOrderResponse.builder().orderId("order-1").build();
        when(subscriptionService.subscribe(7L, subscribeRequest)).thenReturn(subscription);
        when(subscriptionService.createRazorpayOrder(7L, orderRequest)).thenReturn(order);
        when(subscriptionService.verifyRazorpayPayment(7L, verifyRequest)).thenReturn(subscription);
        when(subscriptionService.cancelSubscription(7L)).thenReturn(subscription);
        when(subscriptionService.renewSubscription(7L, subscribeRequest)).thenReturn(subscription);

        assertThat(controller.subscribe(subscribeRequest, 7L).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.createRazorpayOrder(orderRequest, 7L).getBody().getData()).isEqualTo(order);
        assertThat(controller.verifyRazorpayPayment(verifyRequest, 7L).getBody().getData()).isEqualTo(subscription);
        assertThat(controller.cancelSubscription(7L).getBody().getMessage()).isEqualTo("Subscription cancelled");
        assertThat(controller.renewSubscription(subscribeRequest, 7L).getBody().getData()).isEqualTo(subscription);
    }

    @Test
    void planStatusAndLimitEndpointsDelegate() {
        var plan = SubscriptionPlanResponse.builder().plan(SubscriptionPlan.PROFESSIONAL).name("Professional").build();
        when(subscriptionService.getPlans()).thenReturn(List.of(plan));
        when(subscriptionService.getActiveSubscription(7L)).thenReturn(subscription);
        when(subscriptionService.getAllSubscriptionsByRecruiter(7L)).thenReturn(List.of(subscription));
        when(subscriptionService.hasActiveSubscription(7L)).thenReturn(true);
        when(subscriptionService.getMaxJobPosts(7L)).thenReturn(50);

        assertThat(controller.getPlans().getBody().getData()).containsExactly(plan);
        assertThat(controller.getActiveSubscription(7L).getBody().getData()).isEqualTo(subscription);
        assertThat(controller.getActiveSubscriptionByRecruiter(7L).getBody().getData()).isEqualTo(subscription);
        assertThat(controller.getSubscriptionHistory(7L).getBody().getData()).containsExactly(subscription);
        assertThat(controller.hasActiveSubscription(7L).getBody().getData()).isTrue();
        assertThat(controller.getMaxJobPosts(7L).getBody().getData()).isEqualTo(50);
        assertThat(controller.getInternalMaxJobPosts(7L).getBody().getData()).isEqualTo(50);
    }

    @Test
    void invoiceEndpointsDelegate() {
        when(subscriptionService.getInvoicesByRecruiter(eq(7L), any())).thenReturn(new PageImpl<>(List.of(invoice)));
        when(subscriptionService.getAllInvoicesByRecruiter(7L)).thenReturn(List.of(invoice));
        when(subscriptionService.getInvoiceById(3L)).thenReturn(invoice);

        assertThat(controller.getInvoices(7L, 0, 10).getBody().getData().getContent()).containsExactly(invoice);
        assertThat(controller.getAllInvoices(7L).getBody().getData()).containsExactly(invoice);
        assertThat(controller.getInvoiceById(3L).getBody().getData()).isEqualTo(invoice);
    }
}
