# Registro de asistencia — Etapa 1

Este registro conserva el pedido de implementación original. Se omiten los intercambios administrativos sobre configuración del repositorio y publicación; no se presenta como una transcripción completa de la conversación.

## 1. Primer avance sin modificar archivos existentes

- Modelo: GPT-6 (Codex). La variante exacta no está disponible en esta sesión; pendiente de completar desde el selector de modelo.
- Referencia: `refactor.pdf`, cambios 3 y 4.
- Texto exacto de la solicitud:

```text
Esto es lo que hay que hacer. necesito avanzar con eun primer commit. lo que ya está hecho dentro del proyecto no lo modifiques ya que yo avancé un poco. aun no he creado el repositorio si
```

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
