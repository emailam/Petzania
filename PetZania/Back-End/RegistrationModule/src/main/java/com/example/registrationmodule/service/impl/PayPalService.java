package com.example.registrationmodule.service.impl;

import com.example.registrationmodule.config.PayPalConfig;
import com.example.registrationmodule.exception.payPal.PayPalApprovalUrlNotFound;
import com.example.registrationmodule.exception.payPal.PayPalOrderNotFound;
import com.example.registrationmodule.exception.payPal.PayPalPaymentCapture;
import com.example.registrationmodule.exception.rateLimiting.TooManyPaymentRequests;
import com.example.registrationmodule.model.entity.Payment;
import com.example.registrationmodule.model.enumeration.PaymentStatus;
import com.example.registrationmodule.repository.PaymentRepository;
import com.paypal.core.PayPalHttpClient;
import com.paypal.orders.*;
import com.paypal.payments.CapturesRefundRequest;
import com.paypal.payments.RefundRequest;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.paypal.payments.Refund;


import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayPalService {
    private final PayPalHttpClient client;
    private final PayPalConfig config;
    private final PaymentRepository paymentRepository;

    @RateLimiter(name = "paypalCreateOrderLimiter", fallbackMethod = "createOrderFallback")
    public Order createOrder(String amount, String currency, String description) throws IOException {
        OrdersCreateRequest request = new OrdersCreateRequest();
        request.header("prefer", "return=representation");
        request.requestBody(buildRequestBody(amount, currency, description));

        return client.execute(request).result();
    }

    private OrderRequest buildRequestBody(String amount, String currency, String description) {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent(config.getIntent());

        ApplicationContext applicationContext = new ApplicationContext()
                .brandName(config.getBrandName())
                .landingPage(config.getLandingPage())
                .cancelUrl(config.getCancelUrl())
                .returnUrl(config.getReturnUrl())
                .userAction(config.getUserAction());
        orderRequest.applicationContext(applicationContext);

        List<PurchaseUnitRequest> purchaseUnits = new ArrayList<>();
        PurchaseUnitRequest purchaseUnit = new PurchaseUnitRequest()
                .description(description)
                .amountWithBreakdown(new AmountWithBreakdown()
                        .currencyCode(currency)
                        .value(amount)
                );
        purchaseUnits.add(purchaseUnit);
        orderRequest.purchaseUnits(purchaseUnits);

        return orderRequest;
    }

    @RateLimiter(name = "paypalCaptureOrderLimiter", fallbackMethod = "captureOrderFallback")
    public Order captureOrder(String token) throws IOException {
        OrdersCaptureRequest request = new OrdersCaptureRequest(token);
        request.requestBody(new OrderRequest());

        return client.execute(request).result();
    }

    @RateLimiter(name = "paypalApprovalUrlLimiter", fallbackMethod = "getApprovalUrlFallback")
    public String getApprovalUrl(Order order) {
        for (LinkDescription link : order.links()) {
            if ("approve".equalsIgnoreCase(link.rel())) {
                return link.href();
            }
        }
        throw new PayPalApprovalUrlNotFound("PayPal approval URL not found");
    }

    @RateLimiter(name = "paypalGetOrderDetailsLimiter", fallbackMethod = "getOrderDetailsFallback")
    public Order getOrderDetails(String orderId) throws IOException {
        OrdersGetRequest request = new OrdersGetRequest(orderId);
        return client.execute(request).result();
    }

    @RateLimiter(name = "paypalRefundLimiter", fallbackMethod = "refundPaymentFallback")
    public Refund refundPayment(String captureId, String reason) throws IOException {
        CapturesRefundRequest request = new CapturesRefundRequest(captureId);

        RefundRequest refundRequest = new RefundRequest();
        if (reason != null && !reason.isEmpty()) {
            refundRequest.noteToPayer(reason);
        }

        request.requestBody(refundRequest);
        return client.execute(request).result();
    }

    @RateLimiter(name = "paypalProcessCaptureLimiter", fallbackMethod = "processPaymentCaptureFallback")
    public String processPaymentCapture(String token) throws IOException {
        Order order = getOrderDetails(token);

        if (!"APPROVED".equalsIgnoreCase(order.status())) {
            throw new PayPalPaymentCapture("Order is not approved for capture. Current status: " + order.status());
        }

        order = captureOrder(token);
        if (!"COMPLETED".equalsIgnoreCase(order.status())) {
            throw new PayPalPaymentCapture("Payment capture failed. Order status: " + order.status());
        }

        savePayment(order);

        return formatOrderDetails(order);
    }

    private String formatOrderDetails(Order order) {
        Payer payer = order.payer();
        List<Capture> captures = order.purchaseUnits().get(0).payments().captures();

        if (captures.isEmpty()) {
            throw new PayPalOrderNotFound("No capture details found for the order.");
        }

        Money amount = captures.get(0).amount();
        String transactionId = captures.get(0).id();

        return String.format("""
                        Payment Successful!
                        Order ID: %s
                        Status: %s
                        Payer: %s (%s)
                        Amount: %s %s
                        Transaction ID: %s
                        """,
                order.id(), order.status(),
                payer.name().givenName() + " " + payer.name().surname(), payer.email(),
                amount.currencyCode(), amount.value(),
                transactionId
        );
    }

    public void savePayment(Order order) {
        Payment payment = new Payment();
        payment.setOrderId(order.id());
        payment.setPaymentStatus(PaymentStatus.valueOf(order.status().toUpperCase()));
        payment.setPayerName(order.payer().name().fullName());
        payment.setPayerEmail(order.payer().email());
        payment.setAmount(new BigDecimal(order.purchaseUnits().get(0).amountWithBreakdown().value()));
        payment.setCurrency(order.purchaseUnits().get(0).amountWithBreakdown().currencyCode());
        payment.setPaymentTime(Instant.now());
        payment.setTransactionId(order.purchaseUnits().get(0).payments().captures().get(0).id());
        paymentRepository.save(payment);
    }

    public String getCaptureIdByOrderId(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PayPalPaymentCapture("No payment found for order ID: " + orderId));

        return payment.getTransactionId();
    }

    public Order createOrderFallback(String amount, String currency, String description, RequestNotPermitted ex) {
        throw new TooManyPaymentRequests("Rate limit exceeded for creating PayPal orders. Please try again later.");
    }

    public Order captureOrderFallback(String token, RequestNotPermitted ex) {
        throw new TooManyPaymentRequests("Rate limit exceeded while capturing PayPal order. Try again later.");
    }

    public String getApprovalUrlFallback(Order order, RequestNotPermitted ex) {
        throw new TooManyPaymentRequests("Rate limit exceeded while retrieving approval URL. Try again later.");
    }

    public Order getOrderDetailsFallback(String orderId, RequestNotPermitted ex) {
        throw new TooManyPaymentRequests("Rate limit exceeded while getting order details. Try again later.");
    }

    public Refund refundPaymentFallback(String captureId, String reason, RequestNotPermitted ex) {
        throw new TooManyPaymentRequests("Rate limit exceeded for refunding payments. Try again later.");
    }

    public String processPaymentCaptureFallback(String token, RequestNotPermitted ex) {
        throw new TooManyPaymentRequests("Rate limit exceeded while processing payment capture. Try again later.");
    }
}

