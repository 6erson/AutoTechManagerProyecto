# Manual técnico y de uso

## Arquitectura

Aplicación Java 26, Swing, JDBC de Microsoft 12.10.1.jre11 y SQL Server. Maven organiza el proyecto. La clase de entrada es com.autotech.AutoTechManager.

| Capa | Clases y responsabilidad |
|---|---|
| Presentación | PanelSistema navega entre recepción, catálogos, órdenes y reportes. PanelTaller conserva Clientes/Vehículos. UI centraliza tablas, diálogos y SwingWorker. |
| Negocio | TallerService valida estados, importes, disponibilidad y compatibilidad; delimita transacciones JDBC. Validaciones revisa campos. Catalogo contiene una lista cerrada de tablas y campos editables. |
| Datos | ConexionDB obtiene conexiones; ClienteDAO y VehiculoDAO implementan Repositorio; Sql ejecuta sentencias parametrizadas y materializa resultados. |
| Modelos | Cliente, Vehiculo, Mecanico y OrdenTrabajo. Servicio es abstracta y ServicioGeneral/ServicioMayor implementan requisitos de entrega distintos. |

FrmPrincipal contiene unicamente la ventana principal y PanelSistema. La interfaz se construye en paneles Java; se elimino el formulario .form antiguo que ya no se mostraba. La entrada del programa es AutoTechManager.

## Instalación

1. Instalar JDK 26, NetBeans con Maven y SQL Server con TCP/IP.
2. En SSMS ejecutar en orden sql/01_clientes_vehiculos.sql y sql/02_taller_completo.sql. Opcionalmente ejecutar sql/03_catalogos_iniciales.sql para cargar cinco servicios, tres mecánicos, un proveedor, tres repuestos y un motor de ejemplo. Este catálogo no crea existencias, clientes ni órdenes. Ejecutar despues sql/04_unidades_repuestos.sql para agregar las unidades de medida; es obligatorio aun si no se cargan los catalogos de ejemplo.
3. Configurar URL, USER y PASSWORD en datos/ConexionDB.java. La base del script es AutoTechManager. La copia de GitHub deja PASSWORD vacío; colocar la contraseña local antes de compilar. No publicar claves reales.
4. Abrir pom.xml como proyecto Maven en NetBeans. Seleccionar JDK 26 y ejecutar AutoTechManager.
5. Para distribuir: ejecutar `mvn clean package` y usar el jar terminado en jar-with-dependencies; o ejecutar `powershell -ExecutionPolicy Bypass -File compilar.ps1` para crear dist/AutoTechManager.jar con dist/lib/mssql-jdbc.jar. Ejecutar iniciar.bat. La carpeta dist requiere Java 26 y SQL Server configurado.

Los scripts de tablas son repetibles: crean objetos faltantes y actualizan procedimientos/vistas/trigger. No migran automáticamente esquemas antiguos con columnas distintas. La base local del proyecto ya recibió los scripts 01 y 02.

## Operación de las pantallas

### Clientes y vehículos

Guardar un cliente, seleccionarlo para editar o desactivarlo conservando historial. Al seleccionar un cliente inactivo aparece Reactivar cliente; la reactivación conserva sus vehículos y órdenes. Registrar sus vehículos con placa y VIN únicos. El propietario debe estar activo. Un cliente puede tener varios vehículos. La tabla se ordena pulsando sus encabezados.

### Catálogos e inventario

El desplegable cambia entre servicios, mecánicos, proveedores, repuestos y motores. Nuevo/Editar administra los campos. Activar/desactivar conserva referencias históricas; los motores se gestionan por disponibilidad, reserva e instalación.

Para aumentar stock elegir Repuestos, seleccionar una fila y pulsar Entrada de inventario. Registrar cantidad y motivo. El stock no es editable directamente. Repuesto de servicio asocia una cantidad sugerida al servicio seleccionado; es una referencia del catálogo y no agrega consumos automáticamente. Cada cantidad se registra explícitamente en la orden. Para modificar una asociación, guardarla nuevamente con la cantidad correcta.

El motor se registra por serie única y compatibilidad de marca, modelo, rango de años y combustible. Sus unidades físicas se contabilizan en el repuesto asociado; antes de reservar, registrar entrada de stock.

### Órdenes y pagos

La pantalla muestra una guía de seis etapas: recepción, diagnóstico, aprobación, reparación, calidad y entrega. La etapa actual aparece en azul y las completadas en verde. Arriba se indica el siguiente paso y se cuentan actividades, consumos y controles pendientes. Solo se muestran acciones de la etapa actual; las acciones secundarias están en **Más opciones...**.

Al crear una orden queda seleccionada automáticamente. Al cambiar de etapa, el detalle inferior muestra la sección correspondiente. Los botones de transición tienen nombres concretos: Iniciar diagnóstico, Enviar a aprobación, Pasar a calidad y Entregar vehículo. En los pasos siguientes, «Avanzar estado» se refiere a ese botón contextual. Completar actividad y Aprobar control aparecen arriba al consultar Actividades o Calidad. Asignar mecánico, agregar actividad/control, pagos durante reparación y cancelación están en Más opciones.

Seleccionar una orden en la tabla superior. La parte inferior permite consultar servicios, repuestos, diagnóstico, presupuesto, mecánicos, actividades, calidad, cambio de motor, pagos y auditoría.

1. Nueva orden: escoger vehículo por marca, modelo, año y placa, y responsable disponible; registrar kilometraje, síntomas y diagnóstico preliminar.
2. Avanzar estado a En diagnóstico. Completar diagnóstico y evidencias; asignar apoyos si hacen falta.
3. Agregar servicios y repuestos por separado. La cantidad del repuesto es el total deseado en la orden: guardarla nuevamente reemplaza ese total, no lo suma. Los servicios copian el costo base del catálogo como importe fijo de mano de obra; las horas son una estimación informativa. No se multiplica el costo base por las horas.
4. Generar presupuesto con descuento autorizado e impuesto configurable. Cualquier modificación posterior de servicios/repuestos durante diagnóstico invalida el presupuesto y obliga a generarlo otra vez.
5. Avanzar a Pendiente de aprobación. Mientras no exista decisión del cliente, Corregir presupuesto permite volver a diagnóstico; invalida el presupuesto anterior y exige generarlo de nuevo. Decisión del cliente registra constancia de aprobación/rechazo. Aprobar pasa a reparación; rechazar cierra la orden.
6. En reparación completar todas las actividades desde la tabla inferior. Trabajo y calidad permite agregar actividades o controles. Procesar consumos descuenta todas las cantidades pendientes en una sola operación.
7. En cambios de motor registrar pruebas y garantía antes de pasar a calidad.
8. Avanzar a En control de calidad; seleccionar cada control obligatorio y Aprobar control con su resultado. Avanzar a Finalizada y entregada.
9. Registrar pagos parciales o totales sin superar el saldo. Se permiten pagos de órdenes aprobadas abiertas o entregadas. La entrega no exige saldo cero; el reporte conserva la cuenta pendiente.
10. Comprobante muestra un documento blanco con texto negro, tablas y resumen de cuenta, listo para imprimir y permite guardarlo en HTML. Incluye importes, abonos, saldo y garantía del motor cuando corresponda. No es una factura fiscal autorizada ni usa JasperReports.

La cancelación se permite si no hay pagos ni consumos procesados. Libera reservas de motores. No se implementan devoluciones de pagos/inventario; por eso una orden con operaciones financieras no se cancela desde esta aplicación.

### Reportes

Consultar saldos, stock bajo, movimientos, ingresos diarios, carga de mecánicos y trazabilidad de motores. Elegir vehículo para consultar su historial, servicios, repuestos o mecánicos. Los vehículos de un mismo cliente conservan historiales separados.

## Reglas y consistencia

- Se permite una orden abierta por vehículo y una orden abierta por mecánico. Varios mecánicos pueden colaborar en una orden, con un responsable principal. Es una regla de disponibilidad deliberadamente simple, sin calendario por horas.
- Importes monetarios usan BigDecimal/DECIMAL y redondeo HALF_UP al calcular el total a dos decimales.
- DetalleOrden y ConsumosOrden guardan copias de precios; actualizar el catálogo no cambia importes históricos.
- La aplicación bloquea cambios de diagnóstico/detalles después de aprobación y protege órdenes cerradas. Los usuarios directos de SQL Server deben limitarse a administración autorizada.
- sp_ConsumirRepuestos usa transacción, bloqueo de orden, actualización condicional por stock, marca Procesado y un índice único de salidas para evitar consumos dobles. Un fallo de cualquier repuesto revierte todos.
- sp_RegistrarPago bloquea la orden al calcular saldo y registrar el pago. No permite saldo negativo ni sobrepago concurrente.
- sp_CambiarEstado valida el flujo y exige actividades terminadas, consumos procesados, datos del cambio de motor y calidad aprobada.
- TallerService agrupa operaciones multitabla con commit/rollback. No comparte una conexión global entre ventanas/hilos.
- Los motores se reservan por serie. Se bloquean incompatibles, instalados, reservados, inactivos y sin stock libre considerando otras reservas. La compatibilidad es por datos del catálogo; no sustituye una certificación técnica del fabricante.
- Evidencias y autorizaciones son registros de texto; no hay carga de fotografías ni firma digital. La garantía se guarda como texto con vigencia/condiciones y se incluye en el comprobante.

## SQL y conceptos del examen

| Requisito | Implementación |
|---|---|
| Tres procedimientos | sp_ConsumirRepuestos, sp_RegistrarPago, sp_CambiarEstado |
| Dos vistas | vw_SaldosOrdenes, vw_StockBajo |
| Trigger | tr_AuditoriaOrdenes: inserciones y cambios de estado, fecha y usuario SQL |
| JOIN y agregaciones | Vistas de saldos, historial por vehículo e ingresos diarios |
| Subconsultas | Disponibilidad de mecánicos, vehículos sin orden abierta y reservas |
| Restricciones | PK, FK, UNIQUE, CHECK, defaults y salida única por orden/repuesto |
| POO y polimorfismo | Servicio abstracta y dos subclases usadas al generar actividades |
| Interfaces | Repositorio de ClienteDAO/VehiculoDAO; FabricaConexion permite pruebas aisladas |
| Hilos | SwingWorker realiza consultas y operaciones JDBC fuera del hilo de eventos |
| Colecciones y excepciones | List/Map, IllegalArgumentException, SQLException y mensajes de validación |

## Pruebas

src/test/java/com/autotech/PruebasIntegracion.java es una prueba ejecutable con main. Crea una base AutoTechManager_Pruebas_<marca de tiempo>, instala el esquema e inserta únicamente allí sus datos. Incluye los dos casos de defensa y pruebas de concurrencia con dos hilos. Conserva la base de pruebas para inspección. No modifica clientes, vehículos, stock ni pagos de la base real.

Ejecutar desde la raíz del proyecto, con target/classes, target/test-classes y el driver JDBC en el classpath. Requiere un usuario SQL que pueda crear una base de pruebas. `mvn test` compila este arnés, pero no lo ejecuta automáticamente; se ejecuta con `probar.ps1`.

## Repositorio

https://github.com/6erson/AutoTechManager

## Presentacion y reportes

La interfaz usa etiquetas sin tildes; los datos ingresados por el usuario se conservan. Importes y cantidades muestran separadores de miles con coma y decimales con punto. Los campos numericos admiten valores como 1,234.50. Anios, placas e identificadores no se agrupan.

El comprobante y el presupuesto muestran subtotal antes del descuento, impuesto en porcentaje y en lempiras, total, abonos y saldo. Los repuestos indican su unidad (Unidad, Litro o Juego). En Reportes se puede elegir Todos los vehiculos o uno especifico; el selector solo se habilita en reportes compatibles. Las consultas vacias muestran un aviso.
