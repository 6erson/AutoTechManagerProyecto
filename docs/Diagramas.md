# Diagramas de AutoTech Manager

Los diagramas se visualizan directamente en GitHub. Se corresponden con los scripts SQL 01 y 02 y las clases Java del proyecto.

## Entidad-relación

[Imagen PNG](diagrama-er.png) · [Versión SVG ampliable](diagrama-er.svg) · [Fuente Graphviz](diagrama-er.dot)

![Diagrama entidad-relación](diagrama-er.png)

```mermaid
erDiagram
    Clientes ||--o{ Vehiculos : posee
    Clientes ||--o{ OrdenesTrabajo : solicita
    Vehiculos ||--o{ OrdenesTrabajo : ingresa
    Mecanicos ||--o{ OrdenesTrabajo : responsable
    OrdenesTrabajo ||--|{ OrdenMecanicos : asigna
    Mecanicos ||--o{ OrdenMecanicos : participa
    OrdenesTrabajo ||--o{ DetalleOrden : incluye
    Servicios ||--o{ DetalleOrden : origina
    Servicios ||--o{ ServicioRepuesto : sugiere
    Repuestos ||--o{ ServicioRepuesto : utiliza
    Proveedores ||--o{ Repuestos : suministra
    OrdenesTrabajo ||--o| Diagnosticos : evalua
    OrdenesTrabajo ||--o| Presupuestos : cotiza
    OrdenesTrabajo ||--o{ ConsumosOrden : planifica
    Repuestos ||--o{ ConsumosOrden : consume
    OrdenesTrabajo |o--o{ MovimientosInventario : registra
    Repuestos ||--o{ MovimientosInventario : mueve
    OrdenesTrabajo ||--o{ Pagos : recibe
    OrdenesTrabajo ||--o{ ActividadesTrabajo : organiza
    Mecanicos ||--o{ ActividadesTrabajo : ejecuta
    OrdenesTrabajo ||--|{ ControlesCalidad : verifica
    Repuestos ||--o{ Motores : identifica
    Motores ||--o| CambiosMotor : instala
    OrdenesTrabajo ||--o| CambiosMotor : sustituye
    OrdenesTrabajo ||--o{ AuditoriaOrdenes : audita
    Clientes {
        int ClienteId PK
        string Nombre
        string Telefono
        string Correo
        bit Activo
    }
    Vehiculos {
        int VehiculoId PK
        int ClienteId FK
        string Placa UK
        string VIN UK
        string Marca
        string Modelo
        int Anio
        string Color
        int Kilometraje
        string Combustible
    }
    Servicios {
        int ServicioId PK
        string Codigo UK
        string Nombre
        string Descripcion
        int Nivel
        decimal Costo
        decimal Horas
        bit Activo
    }
    Mecanicos {
        int MecanicoId PK
        string Nombre
        string Especialidad
        bit Activo
    }
    Proveedores {
        int ProveedorId PK
        string Nombre
        string Telefono
        bit Activo
    }
    Repuestos {
        int RepuestoId PK
        int ProveedorId FK
        string Codigo UK
        string Nombre
        int Stock
        int Minimo
        decimal Costo
        decimal Precio
        bit Activo
    }
    ServicioRepuesto {
        int ServicioId PK,FK
        int RepuestoId PK,FK
        int Cantidad
    }
    OrdenesTrabajo {
        int OrdenId PK
        int ClienteId FK
        int VehiculoId FK
        int ResponsableId FK
        datetime Fecha
        int Kilometraje
        string Sintomas
        string Estado
        datetime FechaEntrega
    }
    OrdenMecanicos {
        int OrdenId PK,FK
        int MecanicoId PK,FK
    }
    DetalleOrden {
        int DetalleId PK
        int OrdenId FK
        int ServicioId FK
        string Nombre
        int Nivel
        decimal Horas
        decimal Importe
    }
    Diagnosticos {
        int DiagnosticoId PK
        int OrdenId FK,UK
        string Evaluacion
        string Evidencias
        datetime Fecha
    }
    ConsumosOrden {
        int ConsumoId PK
        int OrdenId FK
        int RepuestoId FK
        int Cantidad
        decimal Precio
        bit Procesado
    }
    MovimientosInventario {
        int MovimientoId PK
        int RepuestoId FK
        int OrdenId FK
        string Tipo
        int Cantidad
        datetime Fecha
        string Motivo
    }
    Presupuestos {
        int PresupuestoId PK
        int OrdenId FK,UK
        decimal ManoObra
        decimal Repuestos
        decimal Descuento
        string AutorizoDescuento
        decimal Impuesto
        decimal Total
        string Decision
        string Autorizacion
    }
    Pagos {
        int PagoId PK
        int OrdenId FK
        datetime Fecha
        decimal Monto
        string Metodo
        string Referencia
    }
    Motores {
        int MotorId PK
        int RepuestoId FK
        string Serie UK
        string Marca
        string Modelo
        int AnioDesde
        int AnioHasta
        string Combustible
        string Estado
    }
    CambiosMotor {
        int CambioId PK
        int OrdenId FK,UK
        int MotorId FK,UK
        string SerieAnterior
        string DestinoRetirado
        string Pruebas
        string Garantia
    }
    ActividadesTrabajo {
        int ActividadId PK
        int OrdenId FK
        int MecanicoId FK
        string Descripcion
        bit Completada
    }
    ControlesCalidad {
        int ControlId PK
        int OrdenId FK
        string Descripcion
        bit Obligatorio
        bit Aprobado
        string Resultado
    }
    AuditoriaOrdenes {
        int AuditoriaId PK
        int OrdenId
        string EstadoAnterior
        string EstadoNuevo
        datetime Fecha
        string Usuario
    }
```

AuditoriaOrdenes conserva el identificador de la orden como dato de auditoría, sin clave foránea. Las parejas (OrdenId, ServicioId) y (OrdenId, RepuestoId) son únicas en DetalleOrden y ConsumosOrden. Las cardinalidades mínimas describen los registros creados por el programa; las tablas puente tienen claves compuestas.

## UML de clases

[Imagen PNG](diagrama-uml.png) · [Versión SVG ampliable](diagrama-uml.svg) · [Fuente Graphviz](diagrama-uml.dot)

![Diagrama UML](diagrama-uml.png)

```mermaid
classDiagram
    class AutoTechManager { +main(String[] args) void }
    class FrmPrincipal { +FrmPrincipal() }
    class PanelSistema
    class PanelTaller
    class PanelCatalogos
    class PanelOrdenes
    class PanelReportes
    class TallerService {
        +crearOrden(vehiculo, responsable, km, sintomas, preliminar) int
        +diagnostico(orden, evaluacion, evidencias) void
        +agregarServicio(orden, servicio, horas) void
        +agregarRepuesto(orden, repuesto, cantidad) void
        +presupuesto(orden, descuento, impuesto, autorizo) BigDecimal
        +decidir(orden, aprobar, autorizacion) void
        +reservarMotor(orden, motor, serieAnterior, destino) void
        +consumir(orden) void
        +estado(orden, nuevo) void
        +pagar(orden, monto, metodo, referencia) void
        +comprobante(orden) String
    }
    class Sql { +consultar(connection, sql, args) Tabla }
    class ConexionDB { +getConexion() Connection }
    class Repositorio~T~ {
        <<interface>>
        +listar() List~T~
        +guardar(T entidad) void
    }
    class ClienteDAO
    class VehiculoDAO
    class Cliente { <<record>> }
    class Vehiculo { <<record>> }
    class Mecanico { <<record>> }
    class OrdenTrabajo {
        <<record>>
        +cerrada() boolean
    }
    class Servicio {
        <<abstract>>
        +nombre() String
        +costoManoObra() BigDecimal
        +requisitoEntrega()* String
        +crear(nombre, costo, nivel)$ Servicio
    }
    class ServicioGeneral { +requisitoEntrega() String }
    class ServicioMayor { +requisitoEntrega() String }
    AutoTechManager --> FrmPrincipal
    FrmPrincipal *-- PanelSistema
    PanelSistema *-- PanelTaller
    PanelSistema *-- PanelCatalogos
    PanelSistema *-- PanelOrdenes
    PanelSistema *-- PanelReportes
    PanelTaller --> ClienteDAO
    PanelTaller --> VehiculoDAO
    PanelCatalogos --> TallerService
    PanelOrdenes --> TallerService
    PanelReportes --> TallerService
    Repositorio <|.. ClienteDAO
    Repositorio <|.. VehiculoDAO
    ClienteDAO --> Cliente
    VehiculoDAO --> Vehiculo
    ClienteDAO --> ConexionDB
    VehiculoDAO --> ConexionDB
    TallerService --> Sql
    TallerService --> ConexionDB
    TallerService --> OrdenTrabajo
    TallerService --> Servicio
    Servicio <|-- ServicioGeneral
    Servicio <|-- ServicioMayor
```

Mecanico se utiliza al validar personal activo y disponible. Los catálogos actuales muestran resultados tabulares de JDBC mediante Sql.Tabla. La herencia de Servicio sí se utiliza al agregar servicios: cada subtipo genera requisitos de entrega diferentes en la actividad de trabajo.
