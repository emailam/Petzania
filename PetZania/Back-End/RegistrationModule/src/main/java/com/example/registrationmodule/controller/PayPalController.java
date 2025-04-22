package com.example.registrationmodule.controller;


import com.example.registrationmodule.exception.payPal.PayPalOrderCreation;
import com.example.registrationmodule.exception.payPal.PayPalOrderNotFound;
import com.example.registrationmodule.exception.payPal.PayPalPaymentCapture;
import com.example.registrationmodule.exception.payPal.PayPalRefund;
import com.example.registrationmodule.model.dto.PaymentRequestDTO;
import com.example.registrationmodule.service.impl.PayPalService;
import com.example.registrationmodule.util.PayPalOrderFormatter;
import com.paypal.orders.*;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.paypal.payments.Refund;


import java.io.IOException;

@AllArgsConstructor
@RestController
@RequestMapping("/api/payment")
public class PayPalController {
    private final PayPalService payPalService;

    @PostMapping("/create")
    public ResponseEntity<String> createOrder(@RequestBody PaymentRequestDTO paymentRequest) {
        try {
            Order order = payPalService.createOrder(paymentRequest.getAmount(), paymentRequest.getCurrency(), paymentRequest.getDescription());
            String approvalUrl = payPalService.getApprovalUrl(order);
            return ResponseEntity.ok(approvalUrl);
        } catch (IOException e) {
            throw new PayPalOrderCreation("Error creating PayPal order: " + e.getMessage());
        }
    }

    @GetMapping("/success")
    public ResponseEntity<String> capturePayment(@RequestParam String token) {
        try {
            String responseMessage = payPalService.processPaymentCapture(token);
            return ResponseEntity.ok(responseMessage);
        } catch (IllegalStateException e) {
            throw new PayPalPaymentCapture("Error during payment capture: " + e.getMessage());
        } catch (IOException e) {
            throw new PayPalPaymentCapture("Payment Capture Failed: " + e.getMessage());
        }
    }

    @GetMapping("/cancel")
    public ResponseEntity<String> cancelPayment() {
        return ResponseEntity.ok("Payment was canceled.");
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<String> getOrderDetails(@PathVariable String orderId) {
        try {
            Order order = payPalService.getOrderDetails(orderId);
            return ResponseEntity.ok(PayPalOrderFormatter.formatOrderDetails(order));
        } catch (IOException e) {
            throw new PayPalOrderNotFound("Error retrieving order details: " + e.getMessage());
        }
    }

    @PostMapping("/refund/{orderId}")
    public ResponseEntity<String> refundPayment(@PathVariable String orderId,
                                                @RequestParam(required = false) String reason) {
        try {
            String captureId = payPalService.getCaptureIdByOrderId(orderId);
            Refund refund = payPalService.refundPayment(captureId, reason);
            return ResponseEntity.ok("Refund Successful! Refund ID: " + refund.id());
        } catch (IOException e) {
            throw new PayPalRefund("Refund Failed: " + e.getMessage());
        } catch (RuntimeException e) {
            throw new PayPalOrderNotFound("Order not found: " + e.getMessage());
        }
    }
}
