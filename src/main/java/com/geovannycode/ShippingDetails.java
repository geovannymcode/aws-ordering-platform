package com.geovannycode;

public record ShippingDetails(
        String receiverName,
        String receiverPhoneNumber,
        String shippingAddressLine1,
        String shippingAddressLine2,
        String shippingCity,
        String shippingState,
        String shippingZipCode,
        String shippingCountry
) {
}
