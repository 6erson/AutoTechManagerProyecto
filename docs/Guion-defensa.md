# Guion para la defensa

La pantalla ahora indica **Siguiente** y muestra pendientes por etapa. Los pasos que mencionan «Avanzar» se realizan con el botón contextual de la guía: Iniciar diagnóstico, Enviar a aprobación, Pasar a calidad o Entregar vehículo. Las acciones de apoyo se encuentran en Más opciones.

## Preparar catálogos

Usar los datos del script 03. En Repuestos registrar entradas de 12 litros de aceite, 2 filtros y 1 motor. Estas son existencias de demostración; ingresarlas solo si se usarán en la defensa. Mantener la marca Toyota, modelo Hilux, año 2015 y combustible Diésel para el caso obligatorio.

Registrar un cliente y dos vehículos: uno para mantenimiento y otro Toyota Hilux 2015. Usar placas y VIN diferentes. Conservar un responsable libre para cada orden abierta.

## Caso 1: reparación básica

1. Nueva orden para el primer vehículo, kilometraje actual, síntomas «Mantenimiento periódico» y diagnóstico preliminar.
2. Avanzar a diagnóstico; registrar «Aceite degradado» y evidencia «Inspección de nivel y estado».
3. Agregar Cambio de aceite y registrar explícitamente 6 litros de aceite y 1 filtro en Repuestos. Son cantidades de ejemplo que deben elegirse según el vehículo.
4. Generar presupuesto con descuento 0 e impuesto 15%. Con precios iniciales: L 350 + L 1320 + L 180 = L 1850; total L 2127.50.
5. Pasar a aprobación; aprobar con el nombre del cliente. Se entra en reparación.
6. Procesar consumos, seleccionar Actividades y completar la actividad.
7. Pasar a calidad. Seleccionar Calidad, aprobar la inspección final y entregar.
8. Registrar dos pagos: L 1000 y L 1127.50. Mostrar saldo cero y comprobante.
9. En Reportes, mostrar historial del primer vehículo y stock actualizado.

## Caso 2: sustitución del motor de Hilux 2015

1. Crear orden del segundo vehículo con síntomas «Sobrecalentamiento, pérdida de potencia y daños internos».
2. Pasar a diagnóstico. Registrar daño interno y evidencias como compresión baja, desgaste y resultados de revisión.
3. Agregar Sustitución de motor, nivel 5, 24 horas estimadas.
4. Cambio de motor → Seleccionar motor compatible. Elegir DEMO-HILUX-001, registrar la serie anterior y el destino del retirado.
5. Asignar mecánico de apoyo disponible y generar presupuesto desglosado. Con datos iniciales y sin descuento: L 12000 + L 58000, más impuesto configurado.
6. Pasar a aprobación y registrar autorización del cliente.
7. Procesar consumos. Completar las actividades de sustitución, desmontaje, instalación y pruebas.
8. Cambio de motor → Registrar pruebas y garantía: temperatura, presión, fugas y ruta; garantía con plazo/condiciones.
9. Pasar a calidad, aprobar todos los controles obligatorios y entregar.
10. Registrar pago, generar comprobante y mostrar trazabilidad, números de serie, destino y garantía.

## Validaciones para mostrar al ingeniero

Intentar placa repetida, asignar mecánico ocupado, motor incompatible, finalizar con actividad pendiente, entregar sin calidad y pagar más del saldo. Repetir Procesar consumos no descuenta nuevamente. Los mensajes explican la causa. Las pruebas automatizadas también verifican rollback y concurrencia en una base separada.
