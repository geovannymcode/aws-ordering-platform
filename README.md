# AWS Ordering Platform — Java 25

Ejemplo práctico de una API REST serverless para registrar órdenes de compra utilizando Java 25 y servicios de AWS.

Proyecto desarrollado como ejercicio de aprendizaje, basado en el libro *Mastering Event-Driven Microservices in AWS*.

## Tecnologías

- Java 25
- Apache Maven
- AWS Lambda
- Amazon API Gateway (REST API)
- Amazon DynamoDB
- AWS IAM
- Amazon CloudWatch
- AWS SDK for Java 2.x
- Gson

## Arquitectura

Cliente HTTP → Amazon API Gateway → AWS Lambda → Amazon DynamoDB

La función `new-order-service` recibe las órdenes, valida la información y almacena los datos en la tabla `orders`.

## Estructura del proyecto

- `LambdaHandler.java`: punto de entrada de AWS Lambda y lógica de registro de órdenes.
- `OrderRequest.java`: modelo de entrada del pedido.
- `PaymentDetails.java`: información de referencia del pago.
- `ShippingDetails.java`: información de entrega.

## Requisitos

- JDK 25
- Maven 3.9 o superior
- Cuenta AWS para desplegar y probar los recursos

## Compilación

```bash
mvn clean package
```

El archivo generado estará disponible en:

`target/new-order-service.jar`

## Configuración de AWS Lambda

- Runtime: Java 25
- Handler: `com.geovannycode.LambdaHandler`
- Arquitectura: x86_64
- Variable de entorno: `ORDERS_TABLE=orders`

La función necesita permisos para escribir en DynamoDB y generar logs en CloudWatch.

## Endpoint

`POST /dev/orders`

El cuerpo JSON contiene `customerId`, `items`, `total`, `paymentDetails` y `shippingDetails`.

La API devuelve HTTP `201 Created` cuando la orden se almacena correctamente y HTTP `400 Bad Request` cuando los datos no cumplen las validaciones.

## Estado del proyecto

Implementado:
- Registro de órdenes.
- Validación básica de entradas.
- Persistencia en DynamoDB.
- Integración con API Gateway.
- Pruebas manuales con Postman.

Pendiente:
- Pruebas automatizadas.
- Mensajería asíncrona con Amazon SQS.
- Reserva de inventario.
- Procesamiento de pagos.
- Gestión del cumplimiento de pedidos.

## Aviso

Este proyecto es educativo. No procesa pagos reales. Los identificadores de transacciones son referencias simuladas.

La API de demostración no debe exponerse públicamente sin controles de autenticación y protección adecuados.