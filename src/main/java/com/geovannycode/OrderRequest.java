package com.geovannycode;

import java.math.BigDecimal;
import java.util.List;

public record OrderRequest(
        String customerId,
        List<String> items,
        BigDecimal total,
        PaymentDetails paymentDetails,
        ShippingDetails shippingDetails) {
}
