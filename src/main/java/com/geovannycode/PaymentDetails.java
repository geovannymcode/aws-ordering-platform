package com.geovannycode;

public record PaymentDetails(
        String paymentMethod,
        String paymentTransactionId
) {
}
