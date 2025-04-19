package com.example.registrationmodule.util;

import com.paypal.orders.Order;

public final class PayPalOrderFormatter {
    private PayPalOrderFormatter() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }
    public static String formatOrderDetails(Order order) {
        return String.format("""
            Order ID: %s
            Status: %s
            Payer: %s (%s)
            Amount: %s %s
            """,
                order.id(),
                order.status(),
                order.payer().name().givenName() + " " + order.payer().name().surname(),
                order.payer().email(),
                order.purchaseUnits().get(0).amountWithBreakdown().currencyCode(),
                order.purchaseUnits().get(0).amountWithBreakdown().value()
        );
    }
}
