# Verificación del proyecto

Fecha local: 7 de noviembre de 2026. JDK 26.0.2, driver JDBC 12.10.1.jre11 y SQL Server local.

Resultado del arnés PruebasIntegracion: **34 comprobaciones aprobadas**. Base aislada utilizada: AutoTechManager_Pruebas_1794102307575. Los datos de prueba no se insertaron en AutoTechManager.

| Área | Evidencia |
|---|---|
| Reparación básica | Orden completa con servicio, 6 litros de aceite, filtro, presupuesto L 2127.50, consumos, calidad y dos pagos que completan el saldo. |
| Reparación mayor | Hilux diésel 2015, diagnóstico, motor compatible por serie, autorización, mecánicos, desmontaje/instalación/pruebas, garantía y entrega. |
| Disponibilidad | Bloquea segundo ingreso de vehículo y mecánico ocupado; impide desactivar personal con orden abierta. |
| Precios históricos | Cambiar el costo del catálogo no altera el importe copiado en la orden. |
| Inventario | Stock insuficiente revierte todo el consumo; repetir o ejecutar dos consumos simultáneos produce un solo descuento. |
| Pagos | Dos pagos simultáneos no exceden el saldo; un sobrepago posterior se bloquea. |
| Motores | Exige serie anterior, valida compatibilidad, bloquea motor reservado/instalado y evita reservar más unidades que el stock compartido. |
| Entrega | Bloquea actividades pendientes, falta de pruebas/garantía y controles de calidad sin aprobar. |
| Cierre | Cancelación/rechazo, liberación de motor reservado, stock conservado y saldo cero en orden rechazada. |
| Auditoría | El trigger registra los cambios de estado del flujo. |
| Catálogos | Alta, edición y desactivación; validación de nivel de servicio y mínimo negativo. |

También se verificó compilación Java y renderizado de seis vistas Swing: servicios, inventario, actividades, cambio de motor, presupuesto y reportes. Las capturas del informe usan datos de la base aislada. El precio 9999 del servicio de aceite que puede aparecer en la captura corresponde a la prueba deliberada de conservación de precios históricos; no es el precio inicial del catálogo real.

No se automatizaron los clics de todos los diálogos ni una impresión física. Los comprobantes se validaron por contenido; la impresión depende de la impresora de Windows. El guion de defensa sirve como recorrido manual en NetBeans.
