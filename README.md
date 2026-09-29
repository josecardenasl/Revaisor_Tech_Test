# BS — Meeting Notes to Action Items

Una aplicación web que convierte las notas de una reunión en una lista estructurada de
tareas accionables (responsable y fecha límite incluidos), usando IA generativa.

Proyecto desarrollado como parte de la prueba técnica de Internship / Software Engineering
de RevAIsor.

## Índice

- [Cómo funciona](#cómo-funciona)
- [Stack técnico](#stack-técnico)
- [Por qué Groq en vez de OpenAI](#por-qué-groq-en-vez-de-openai)
- [Requisitos previos](#requisitos-previos)
- [Variables de entorno](#variables-de-entorno)
- [Cómo correrlo en local (sin Docker)](#cómo-correrlo-en-local-sin-docker)
- [Cómo correrlo con Docker](#cómo-correrlo-con-docker)
- [Endpoint de la API](#endpoint-de-la-api)
- [Tests](#tests)
- [Despliegue en la nube](#despliegue-en-la-nube)
- [Decisiones de diseño](#decisiones-de-diseño)
- [Dificultades y cómo se resolvieron](#dificultades-y-cómo-se-resolvieron)

## Cómo funciona

El usuario pega el texto de una reunión en el frontend, y la aplicación le devuelve una
lista de tareas extraídas de ese texto, cada una con:

- **Tarea**: qué hay que hacer.
- **Responsable**: quién la tiene a cargo (o "No especificado" si el texto no lo menciona).
- **Fecha límite**: cuándo debería estar lista (o "No especificado" si no se menciona).

Flujo de datos:

Usuario
│ pega notas + clic en "Extraer tareas"

Next.js (frontend)
│ POST /api/action-items { meetingNotes }

Spring Boot (backend)
│ arma el prompt + pide salida en JSON Schema estricto

Groq API (modelo openai/gpt-oss-20b)
│ devuelve JSON validado contra el schema

Spring Boot (backend)
│ parsea y devuelve { tasks: [...] }

Next.js (frontend)
│ renderiza la lista de tareas

Usuario


La API key del proveedor de IA nunca llega al navegador: solo vive en el backend.

## Stack técnico

| Capa       | Tecnología                                  |
|------------|----------------------------------------------|
| Frontend   | Next.js (App Router, TypeScript, Tailwind CSS) |
| Backend    | Spring Boot 4 (Java 25, Maven)                |
| IA generativa | Groq API (modelo `openai/gpt-oss-20b`, Structured Outputs) |
| Contenedores | Docker + Docker Compose (multi-stage, usuario no-root) |
| Tests      | JUnit 5, MockMvc, Mockito                     |

## Por qué Groq en vez de OpenAI

La prueba permite elegir cualquier proveedor de IA generativa. Se evaluaron dos:

- **OpenAI**: excelente soporte de Structured Outputs, pero desde que se discontinuaron
  los créditos gratuitos para cuentas nuevas, requiere un saldo mínimo prepago para usarse.
- **Groq**: no pide tarjeta de crédito, tiene un nivel gratuito generoso (30 solicitudes
  por minuto, 1.000 por día), su API es **compatible con el formato de OpenAI** (mismos
  endpoints y estructura de request/response), y soporta **Structured Outputs en modo
  estricto** en modelos como `openai/gpt-oss-20b` — la misma garantía de JSON válido que
  ofrece OpenAI, sin costo.

Por eso se eligió Groq: mismo nivel de robustez técnica, sin la barrera de pago.

## Requisitos previos

- Java 25 (o el JDK indicado en `backend/pom.xml`, tag `<java.version>`)
- Maven (o usar el wrapper `./mvnw` incluido en el proyecto)
- Node.js 20+
- Una API key de Groq, gratuita, generada en [console.groq.com/keys](https://console.groq.com/keys)
- (Opcional) Docker y Docker Compose, si se prefiere correr todo containerizado

## Variables de entorno

### Backend

Crear el archivo `backend/secrets.properties` (este archivo está en `.gitignore` y
**nunca** debe subirse al repositorio):

```properties
GROQ_API_KEY=tu-api-key-de-groq
```

Spring Boot lo carga automáticamente gracias a esta línea en
`backend/src/main/resources/application.properties`:

```properties
spring.config.import=optional:file:./secrets.properties
```

En un entorno de despliegue real, en vez de este archivo simplemente se configura
`GROQ_API_KEY` como variable de entorno real de la plataforma (ver sección de despliegue).

### Frontend

Crear el archivo `frontend/.env.local` (también en `.gitignore`):

NEXT_PUBLIC_API_URL=http://localhost:8080


## Cómo correrlo en local (sin Docker)

**Backend** (desde `backend/`):

```bash
mvn spring-boot:run
```

Queda escuchando en `http://localhost:8080`.

**Frontend** (desde `frontend/`, en otra terminal):

```bash
npm install
npm run dev
```

Queda disponible en `http://localhost:3000`.

## Cómo correrlo con Docker

Con `backend/secrets.properties` y `frontend/.env.local` ya creados (ver sección
anterior), desde la raíz del repositorio:

```bash
docker compose up --build
```

Esto construye y levanta ambos servicios: frontend en `http://localhost:3000` y backend
en `http://localhost:8080`. Ambas imágenes usan builds multi-stage (para no incluir
herramientas de compilación en la imagen final) y corren como usuario no-root.

Para bajar los servicios:

```bash
docker compose down
```

## Endpoint de la API

**`POST /api/action-items`**

Request:
```json
{
  "meetingNotes": "Juan va a enviar la propuesta al cliente antes del viernes. Maria queda pendiente de revisar el presupuesto."
}
```

Response (200 OK):
```json
{
  "tasks": [
    { "task": "Enviar la propuesta al cliente", "assignee": "Juan", "deadline": "antes del viernes" },
    { "task": "Revisar el presupuesto", "assignee": "Maria", "deadline": null }
  ]
}
```

Errores:
- `400 Bad Request` si `meetingNotes` viene vacío.
- `502 Bad Gateway` si falla la comunicación con Groq.

## Tests

Desde `backend/`:

```bash
mvn test
```

Cubren: validación de entrada (rechaza notas vacías sin llamar a la IA), el camino feliz
del controller (usando un `GroqService` simulado con Mockito, para no depender de la red
ni de la API key real), y el manejo de errores cuando el servicio de IA falla.

## Despliegue en la nube

La aplicación está preparada para desplegarse como dos contenedores independientes en
cualquier plataforma que soporte Docker (por ejemplo Render, Railway o Fly.io):

1. **Backend**: desplegar `backend/Dockerfile` como servicio web, configurando
   `GROQ_API_KEY` como variable de entorno en el panel de la plataforma (no se sube
   ningún archivo de secretos, la plataforma la inyecta directamente).
2. **Frontend**: desplegar `frontend/Dockerfile`, configurando `NEXT_PUBLIC_API_URL`
   con la URL pública que la plataforma le asignó al backend.
3. Habilitar CORS en el backend (`@CrossOrigin`) para el dominio real del frontend en
   producción, en vez de `http://localhost:3000`.

## Decisiones de diseño

- **Feature elegido**: extractor de tareas de notas de reunión, en vez de las opciones
  sugeridas (chatbot, generador de contenido, resumidor, tone checker), buscando algo con
  aplicación práctica real y que permitiera demostrar manejo de salida estructurada.
- **Un solo endpoint**: para un MVP de este alcance, separar en varios endpoints hubiera
  agregado complejidad sin beneficio real.
- **`spring.config.import` en vez de una librería de terceros para variables de entorno**:
  se evaluó `spring-dotenv`, pero presentó problemas de compatibilidad con Spring Boot 4.
  La función nativa de Spring logra lo mismo sin depender de una librería externa.
- **`Map<String, Object>` para el request a Groq**: al ser un JSON anidado usado en un
  solo lugar, crear varias clases DTO solo para esto hubiera sido complejidad innecesaria.

## Dificultades y cómo se resolvieron

- **`spring-dotenv` no cargaba las variables de entorno** en Spring Boot 4.1.1, fallando
  silenciosamente sin indicar la causa real. Se resolvió reemplazándolo por
  `spring.config.import=optional:file:./secrets.properties`, una función nativa de Spring
  Boot sin dependencias externas.
- **`ObjectMapper` no se inyectaba automáticamente**: en esta versión de Spring Boot la
  autoconfiguración de Jackson no lo registró como bean disponible para inyección directa.
  Se resolvió instanciándolo manualmente dentro del servicio, ya que no se necesitaba
  ninguna configuración especial de serialización para este caso.
