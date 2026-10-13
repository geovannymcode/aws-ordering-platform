package com.geovannycode;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LambdaHandler implements RequestHandler<
        APIGatewayProxyRequestEvent,
        APIGatewayProxyResponseEvent> {

    // 1. Reutilizamos el cliente de DynamoDB.
    // AWS SDK obtiene las credenciales del rol de Lambda.
    private static final DynamoDbClient DYNAMODB =
            DynamoDbClient.create();

    // 2. Gson convierte el JSON a records Java.
    private static final Gson GSON = new Gson();

    // 3. Nombre de la tabla desde una variable de entorno.
    private static final String TABLE_NAME =
            System.getenv().getOrDefault(
                    "ORDERS_TABLE", "orders");

    // 4. Punto de entrada de nuestra función AWS Lambda.
    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event,
            Context context) {

        try {

            // 5. Verificamos que exista un cuerpo HTTP.
            if (event == null
                    || event.getBody() == null
                    || event.getBody().isBlank()) {

                return errorResponse(
                        400, "El cuerpo es obligatorio");
            }

            // 6. Convertimos el JSON a OrderRequest.
            OrderRequest request = GSON.fromJson(
                    event.getBody(),
                    OrderRequest.class
            );

            // 7. Validamos todos los datos de la orden.
            if (!isValid(request)) {
                return errorResponse(
                        400, "Datos de la orden invalidos");
            }

            // 8. Generamos identificador, fecha y estado.
            String orderId = UUID.randomUUID().toString();
            String orderDate = Instant.now().toString();
            String status = "PLACED";

            // 9. Construimos el elemento de DynamoDB.
            Map<String, AttributeValue> item = new HashMap<>();

            item.put("orderId", stringValue(orderId));

            item.put(
                    "customerId",
                    stringValue(request.customerId())
            );

            item.put(
                    "orderDate",
                    stringValue(orderDate)
            );

            item.put(
                    "status",
                    stringValue(status)
            );

            // 10. Guardamos el total como tipo Number.
            item.put(
                    "total",
                    AttributeValue.builder()
                            .n(request.total().toPlainString())
                            .build()
            );

            // 11. Guardamos los productos como lista DynamoDB.
            List<AttributeValue> products =
                    request.items().stream()
                            .map(LambdaHandler::stringValue)
                            .toList();

            item.put(
                    "items",
                    AttributeValue.builder()
                            .l(products)
                            .build()
            );

            // 12. Guardamos paymentDetails como Map.
            PaymentDetails payment = request.paymentDetails();

            Map<String, AttributeValue> paymentMap = Map.of(
                    "paymentMethod",
                    stringValue(payment.paymentMethod()),

                    "paymentTransactionId",
                    stringValue(payment.paymentTransactionId())
            );

            item.put(
                    "paymentDetails",
                    AttributeValue.builder()
                            .m(paymentMap)
                            .build()
            );

            // 13. Guardamos shippingDetails como Map.
            ShippingDetails shipping = request.shippingDetails();

            Map<String, AttributeValue> shippingMap =
                    new HashMap<>();

            shippingMap.put(
                    "receiverName",
                    stringValue(shipping.receiverName())
            );

            shippingMap.put(
                    "receiverPhoneNumber",
                    stringValue(shipping.receiverPhoneNumber())
            );

            shippingMap.put(
                    "shippingAddressLine1",
                    stringValue(shipping.shippingAddressLine1())
            );

            // Campo opcional.
            if (shipping.shippingAddressLine2() != null) {
                shippingMap.put(
                        "shippingAddressLine2",
                        stringValue(shipping.shippingAddressLine2())
                );
            }

            shippingMap.put(
                    "shippingCity",
                    stringValue(shipping.shippingCity())
            );

            shippingMap.put(
                    "shippingState",
                    stringValue(shipping.shippingState())
            );

            shippingMap.put(
                    "shippingZipCode",
                    stringValue(shipping.shippingZipCode())
            );

            shippingMap.put(
                    "shippingCountry",
                    stringValue(shipping.shippingCountry())
            );

            item.put(
                    "shippingDetails",
                    AttributeValue.builder()
                            .m(shippingMap)
                            .build()
            );

            // 14. Construimos la solicitud de escritura.
            PutItemRequest putRequest =
                    PutItemRequest.builder()
                            .tableName(TABLE_NAME)
                            .item(item)
                            .conditionExpression(
                                    "attribute_not_exists(orderId)")
                            .build();

            // 15. Persistimos la orden.
            DYNAMODB.putItem(putRequest);

            // 16. Construimos la respuesta HTTP 201.
            String responseBody = GSON.toJson(Map.of(
                    "orderId", orderId,
                    "status", status,
                    "message", "Orden creada correctamente"
            ));

            context.getLogger().log(
                    "Orden creada correctamente: " + orderId);

            return response(201, responseBody);

        } catch (JsonParseException e) {

            // 17. El cuerpo no contiene un JSON válido.
            return errorResponse(400, "JSON invalido");

        } catch (Exception e) {

            // 18. Registramos el problema técnico.
            context.getLogger().log(
                    "Error creando orden: " + e);

            return errorResponse(
                    500, "Error interno del servidor");
        }
    }

    // 19. Validamos los campos obligatorios.
    private static boolean isValid(OrderRequest request) {

        if (request == null) {
            return false;
        }

        if (isBlank(request.customerId())
                || request.items() == null
                || request.items().isEmpty()
                || request.items().stream()
                        .anyMatch(LambdaHandler::isBlank)
                || request.total() == null
                || request.total().signum() <= 0) {

            return false;
        }

        PaymentDetails payment = request.paymentDetails();

        if (payment == null
                || isBlank(payment.paymentMethod())
                || isBlank(payment.paymentTransactionId())) {

            return false;
        }

        ShippingDetails shipping = request.shippingDetails();

        return shipping != null
                && !isBlank(shipping.receiverName())
                && !isBlank(shipping.receiverPhoneNumber())
                && !isBlank(shipping.shippingAddressLine1())
                && !isBlank(shipping.shippingCity())
                && !isBlank(shipping.shippingState())
                && !isBlank(shipping.shippingZipCode())
                && !isBlank(shipping.shippingCountry());
    }

    // 20. Utilidad para validar cadenas.
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // 21. Convertimos String a atributo DynamoDB.
    private static AttributeValue stringValue(String value) {
        return AttributeValue.builder()
                .s(value)
                .build();
    }

    // 22. Generamos una respuesta HTTP con JSON.
    private static APIGatewayProxyResponseEvent response(
            int statusCode,
            String body) {

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(Map.of(
                        "Content-Type", "application/json"))
                .withBody(body);
    }

    // 23. Centralizamos las respuestas de error.
    private static APIGatewayProxyResponseEvent errorResponse(
            int statusCode,
            String message) {

        return response(
                statusCode,
                GSON.toJson(Map.of("message", message))
        );
    }
}
