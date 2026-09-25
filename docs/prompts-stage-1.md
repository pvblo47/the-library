# Registro de prompts — Etapa 1

Formato del Appendix A de `refactor.pdf`, con una columna adicional para los archivos cambiados exigidos por la sección 6.1. Las filas conservan el texto original de los pedidos registrados y su orden cronológico.

**Modelo pendiente de confirmar:** GPT-6 (Codex) es el nombre disponible en esta sesión. El identificador exacto de la variante no está disponible; debe completarse en la columna Model desde el selector o los metadatos de la sesión, sin adivinarlo.

**Alcance del registro:** se conserva el pedido de implementación y el ajuste actual del registro. Se omiten los intercambios administrativos sobre Git y la conversación intermedia sobre presentación del registro; esta tabla no es una transcripción exhaustiva de todos los prompts de la sesión.

| Number | Model | Exact prompt text | Files changed |
| --- | --- | --- | --- |
| 1 | GPT-6 (Codex); ID exacto pendiente | Esto es lo que hay que hacer. necesito avanzar con eun primer commit. lo que ya está hecho dentro del proyecto no lo modifiques ya que yo avancé un poco. aun no he creado el repositorio si | `src/main/java/cl/ucn/disc/arqsist/library/service/LoanPolicy.java`<br>`src/main/java/cl/ucn/disc/arqsist/library/service/NotFoundException.java`<br>`src/test/java/cl/ucn/disc/arqsist/library/service/LoanPolicyTest.java`<br>`docs/prompts-stage-1.md` |
| 2 | GPT-6 (Codex); ID exacto pendiente | ajustalo | `docs/prompts-stage-1.md` |

La fila 2 solicita adaptar este registro al Appendix A, según el contexto de la conversación.

### Estado previo a la asistencia

El proyecto ya contenía la estructura DAO con `CrudDao`, interfaces por entidad y adaptadores ORMLite. También estaban implementadas las validaciones de existencia de miembro y libro en `MemberService.checkout()` y el uso compartido de `LoanService.DUE_DAYS`. Estos elementos estaban presentes antes de esta intervención y no fueron modificados por el asistente.

### Aporte de la asistencia

A partir del PDF, el asistente propuso y agregó exclusivamente:

- `src/main/java/cl/ucn/disc/arqsist/library/service/LoanPolicy.java`: constantes de plazo y tarifa, y cálculo de vencimiento.
- `src/main/java/cl/ucn/disc/arqsist/library/service/NotFoundException.java`: excepción para entidades inexistentes.
- `src/test/java/cl/ucn/disc/arqsist/library/service/LoanPolicyTest.java`: dos pruebas con fechas fijas, para cambio de año y año bisiesto.
- `docs/prompts-stage-1.md`: este registro.

Los archivos Java nuevos incluyen el encabezado de licencia y Javadoc. Ningún archivo de código preexistente fue modificado.

### Validación y pendientes

La ejecución de `gradlew.bat test --offline --console=plain` terminó correctamente: 9 pruebas, sin fallos, incluidas las 2 nuevas. Las posteriores ediciones de este registro no cambiaron el código verificado.

Este avance agrega infraestructura; no completa la Etapa 1. Queda pendiente conectar `LoanPolicy` con los servicios e integrar el uso de `NotFoundException`, además de los demás cambios del plan. El plazo ya estaba compartido mediante `LoanService.DUE_DAYS` antes de este avance.
