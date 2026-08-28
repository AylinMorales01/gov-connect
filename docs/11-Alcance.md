# Alcance y no-alcance del sistema

## Por qué existe este documento

Gov Connect toca dominios —contratos, presupuesto, recaudo— que en una entidad pública ya están cubiertos por un sistema financiero de registro en operación.

Este documento delimita de forma explícita **qué resuelve Gov Connect, qué deliberadamente no resuelve y por qué**, para que la frontera no quede a interpretación de quien lee el código. Un modelo de datos deliberadamente simple puede confundirse con un modelo de datos incompleto; aquí se distingue uno del otro.

---

## Posicionamiento

Gov Connect es una **capa de observación**, no un **sistema de registro**.

| | Sistema de registro (ERP financiero) | Gov Connect |
|---|---|---|
| Rol frente al dato | Fuente de la verdad | Consumidor |
| Operación | Crea, modifica y certifica actos administrativos | Lee, cruza y vigila |
| Responsabilidad normativa | Produce entregables de obligación legal | Ninguna |
| Pregunta que responde | *¿Cuál es el estado oficial de X?* | *¿Qué necesita atención hoy?* |
| Si deja de funcionar | La entidad no puede operar | Se dejan de recibir alertas |

Esta distinción ordena todas las decisiones de alcance que siguen.

---

## Fuera de alcance (deliberado)

### Cadena de ejecución presupuestal

**No modelado:** CDP (certificado de disponibilidad presupuestal), RP (registro presupuestal de compromiso), órdenes de pago, PAC (plan anual mensualizado de caja), reservas de apropiación, cuentas por pagar de cierre, vigencias futuras, amortización de anticipos, control de consecutivos.

La tabla `budgets` de este proyecto tiene tres columnas de valor —`assigned_budget`, `executed_budget`, `available_budget`— y eso es **intencionalmente insuficiente** para operar un presupuesto público.

La ejecución presupuestal real es una cadena de actos administrativos encadenados (disponibilidad → compromiso → obligación → pago), cada uno con consecutivo propio y efectos jurídicos, más instrumentos de programación de caja y de cierre de vigencia. Modelar eso correctamente es construir un módulo de presupuesto, no una capa analítica.

Gov Connect consume el **resultado agregado** de esa cadena para calcular indicadores de ejecución. No participa en ella.

### Contabilidad y reportes regulatorios

**No modelado:** catálogo de cuentas bajo el marco normativo contable público (NICSP), libros oficiales, centros de costo, terceros por cuenta, CGN 001 y 002, CUIPO, información exógena DIAN, FUT, informes APPUI de la Contraloría, anexos de SETP y BID.

Estos entregables son el núcleo de valor de un sistema financiero público: la entidad los necesita por obligación legal, con periodicidad fija y formato definido por el ente que los exige. Producirlos requiere ser la fuente de la verdad contable, con trazabilidad completa y responsabilidad sobre cada cifra.

**Gov Connect no genera ningún entregable de obligación normativa** y no debe usarse como insumo para ellos.

### Tesorería

**No modelado:** ventanillas de recaudo y pago, comprobantes de egreso, boletín diario de bancos e ingresos, conciliaciones bancarias, control de cuentas y consecutivos de cheques, libro de descuentos tributarios, fuentes y usos.

La tesorería mueve dinero real y su registro debe cuadrar contra extractos bancarios. Es operación transaccional pura, sin componente analítico que justifique una capa externa.

### Nómina e inventario

**No modelado (nómina):** liquidación por tipo de vinculación, novedades, descuentos de ley, prestaciones, parafiscales, generación de planilla PILA.

**No modelado (inventario):** entradas por compra/donación/comodato, movimientos por tercero y dependencia, depreciación automática y su contabilización, bienes muebles e inmuebles, intangibles, impresión de placas.

Ambos dominios tienen reglas de liquidación con efecto legal y laboral directo. Quedan completamente fuera.

### Impuestos y cobro coactivo

**No modelado:** liquidación parametrizable por criterios del municipio, facturación, declaraciones y aperturas de establecimiento, proceso de cobro persuasivo, mandamiento de pago, excepciones, medidas cautelares, cauciones, investigación de bienes.

La tabla `collections` registra un recaudo **ya consumado** (fecha, concepto, contribuyente, monto, medio de pago). No modela la **cartera**, que es el objeto sobre el que operan estos módulos, ni el expediente del proceso de cobro.

> **Extensión identificada, no implementada:** la vigilancia del término de prescripción de la acción de cobro sí encaja en la naturaleza de este sistema —es observación, no operación— y reutilizaría el mismo mecanismo de alerta ya construido para contratos. Requiere acceso al modelo de cartera del sistema de registro.

### Gestión del ciclo contractual

**No modelado:** análisis de conveniencia y etapa precontractual, actas de inicio, actas parciales, acta de liquidación, plantillas por tipo de contrato (obra, suministro, servicios), generación de documentos, amarre con unidades ejecutoras.

La tabla `contracts` guarda nueve campos planos que describen un contrato **ya perfeccionado**. Es una proyección para consulta y vigilancia, no el expediente contractual.

### Operación multi-entidad

**No resuelto:** multi-tenancy, aislamiento de datos por entidad, parametrización por municipio.

El modelo actual asume una sola entidad. Un despliegue real sobre varias exige segregación de datos y parametrización que hoy no existen.

---

## Dentro de alcance

Tres capacidades, elegidas porque son **observación sobre datos existentes** y no operación sobre actos administrativos:

| Capacidad | Qué hace | Estado |
|---|---|---|
| **Vigilancia proactiva** | Detecta contratos activos por vencer dentro de una ventana configurable y notifica por correo (cron o disparo manual). Cada ejecución queda auditada en `automation_logs`. | Implementado |
| **Conciliación con SECOP II** | Ingiere el export real de SECOP II normalizando sus cabeceras propias (`SecopColumnMapper`) para cruzarlo contra el registro interno. | Ingesta implementada y validada contra un export real (~66 000 filas); motor de diferencias pendiente |
| **Tablero ejecutivo** | Consolida indicadores de contratación, ejecución presupuestal y recaudo en una SPA, para perfiles directivos que no operan el sistema transaccional. | Implementado |

El criterio de admisión de nuevas funcionalidades es el mismo: **si la funcionalidad crea o modifica un acto administrativo, está fuera de alcance.**

---

## Dependencia de datos

**Estado actual.** SQL Server almacena tablas propias (`contracts`, `budgets`, `collections`) que se llenan por ingesta de CSV, y DuckDB se alimenta de ellas por ETL (ADR-007). Esto permite que el sistema sea demostrable de forma autónoma.

**Estado objetivo.** Esas tablas deben convertirse en **proyecciones de solo lectura** del sistema de registro de la entidad, alimentadas desde su base de datos o sus exportaciones. El ETL ya construido (`ExportService` → `ImportService`) es el mecanismo previsto: solo cambia el origen.

Mantener indefinidamente tablas propias en paralelo al sistema de registro crearía un segundo origen de la verdad y, con él, discrepancias de cifras. **Es una etapa de desarrollo, no un destino.**

---

## Limitaciones conocidas

- **Volumen probado.** La ingesta de contratos se validó contra un **export real de SECOP II** (~26 MB, del orden de 66 000 filas). Presupuestos y recaudos siguen alimentándose con datos semilla de demostración (~8 y ~70 registros), por lo que los indicadores que los cruzan no están medidos a escala real.
- **Formato de ingesta.** `SecopColumnMapper` cubre los alias del export de SECOP II observados; un formato distinto exige ampliar la tabla de alias.
- **Trazabilidad.** `automation_logs` audita ejecuciones de automatización, no cambios registro a registro. No hay historial de auditoría por dato.
- **Una sola entidad.** Ver *Operación multi-entidad*.

---

## Referencias

- `docs/01-Arquitectura.md` — arquitectura general
- `docs/05-Base-de-Datos.md` — esquema y estrategia de datos
- `docs/09-Architecture-Decision-Records.md` — ADR-003 (vistas), ADR-006 (modularidad), ADR-007 (doble motor de datos)
