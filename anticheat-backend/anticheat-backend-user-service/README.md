# User Service — migración a Spring Boot (en curso)

Migración de aprendizaje de `anticheat-backend-user-service` (microservicio Java plano, gRPC + JDBC
a mano) a Spring Boot, hecha a mano paso a paso. Este documento refleja el estado real del código,
no un plan — actualízalo según avances.

## Stack actual

- Spring Boot 3.5.4 (`spring-boot-starter-parent`)
- `spring-boot-starter-web` (Tomcat embebido, `@RestController` bajo `/users`)
- `spring-boot-starter-data-jdbc` (repositorio de usuarios)
- `spring-boot-starter-mail` (`JavaMailSender`, sustituye al `jakarta.mail` manual)
- `spring-boot-starter-actuator` (expone `/actuator/health`)
- H2 en fichero (`./data/anticheat`), driver `com.h2database:h2`
- `jbcrypt`
- Empaquetado con `spring-boot-maven-plugin` (no `maven-shade-plugin`)

## Estructura de paquetes

Paquete raíz `com.chessfraud.userservice`, organizado **por tipo y, dentro de cada tipo, por
funcionalidad**:

```
com.chessfraud.userservice/
  UserServiceApplication.java        @SpringBootApplication, @EnableConfigurationProperties
  config/
    UserServiceConfig.java           @ConfigurationProperties(prefix = "user-service"): smtpUser/smtpPassword
    MailConfig.java                  @Bean JavaMailSender (Gmail SMTP/STARTTLS), lee UserServiceConfig
  model/
    user/
      User.java                     entidad Spring Data JDBC, @Table("USERS")
  repository/
    user/
      UserRepository.java           interface extends CrudRepository<User, Long>
  service/
    user/
      UserAccountService.java       @Service, depende de UserRepository
    EmailNotificationService.java   @Service, usa JavaMailSender + MimeMessageHelper
  dto/
    RegisterRequest.java / LoginRequest.java / ChangePasswordRequest.java /
    ChangePasswordByEmailRequest.java / PasswordResetRequest.java   DTOs de entrada del controller
    RegisterResponse.java / LoginResponse.java / UserInfoResponse.java /
    ChangePasswordResponse.java / PasswordResetResponse.java / ErrorResponse.java   DTOs de salida
    user/
      LoginResult.java / RegisterResult.java / UserInfoResult.java   resultados internos del service
  web/
    UserController.java            @RestController @RequestMapping("/users"), JSON vía Jackson
```

## Qué está hecho

- **Bootstrap Spring Boot**: `UserServiceApplication` con `@SpringBootApplication` +
  `SpringApplication.run(...)`. Arranca limpio con Tomcat embebido.
- **Configuración externalizada**: `UserServiceConfig` con `@ConfigurationProperties(prefix =
  "user-service")`, registrado vía `@EnableConfigurationProperties` en la clase principal. Las env
  vars `SMTP_USER`/`SMTP_PASSWORD` se mapean explícitamente en `application.properties`
  (`user-service.smtp-user=${SMTP_USER:}`, etc.) — antes esta conexión no existía y el envío de
  email fallaba siempre en silencio (ver "Bugs encontrados y arreglados" más abajo).
- **Inyección de dependencias**: `UserAccountService`, `EmailNotificationService` y `UserController`
  son beans Spring, conectados por constructor.
- **Capa de datos con Spring Data JDBC**: `application.properties` define `spring.datasource.*` (con
  placeholders `${DB_USER:sa}` etc.). `schema.sql` crea la tabla `users` al arrancar. `User` lleva
  `@Table("USERS")` para que Spring Data JDBC apunte a la tabla real (H2 guarda los nombres sin
  comillas en mayúsculas). `UserRepository` con métodos derivados (`findByName`, `findByEmail`,
  `existsByName`, `existsByEmail`) — sin una sola línea de SQL escrita a mano.
- **Capa web con Spring MVC**: `web/UserController.java`, `@RestController` bajo `/users`, con DTOs
  de request/response propios y serialización JSON automática vía Jackson. Sustituye por completo al
  `HttpServer` de JDK hecho a mano (`http/UserHttpServer`, `http/UserHttpHandler`, `utils/Json.java`
  — los tres eliminados).
- **Health check con Actuator**: `spring-boot-starter-actuator` expone `/actuator/health`.
  `utils/HealthServer.java` (JDK `HttpServer` a mano, dead code) eliminado.
- **Envío de e-mail con `JavaMailSender`**: `EmailNotificationService` ya no usa
  `Session`/`Transport`/`Authenticator` a mano; usa `JavaMailSender` + `MimeMessageHelper`, con el
  `Bean` construido en `MailConfig` (Gmail SMTP relay, STARTTLS, mismos parámetros que antes).
- **`smoke-test.ps1` actualizado**: habla JSON contra el `@RestController` en el puerto 8080 y
  comprueba `/actuator/health`. 23/23 tests en verde contra el jar empaquetado.

## Bugs encontrados y arreglados durante la migración

- `UserAccountService.register(...)` tenía la comprobación de `nameExists`/`emailExists` invertida
  — ya corregido, un usuario nuevo registra `OK` y uno duplicado devuelve `UNV`/`ENV`.
- `changePassword` y `changePasswordByEmail` eran inconsistentes entre sí (uno comparaba hashes
  directamente, dead code que nunca era `true`; el otro usaba `BCrypt.checkpw` de verdad) — ya
  unificado: los dos usan `BCrypt.checkpw` y bloquean reutilizar la contraseña anterior.
- **Mapeo de tabla roto**: la entidad `User` no tenía `@Table`, así que Spring Data JDBC buscaba la
  tabla `USER` (nombre derivado de la clase) mientras `schema.sql` crea `users` — toda query fallaba
  con 500. No se detectó hasta tener el `@RestController` real haciendo peticiones (el arranque solo
  prueba la conexión, no las queries). Arreglado con `@Table("USERS")`.
- **SMTP nunca configurado**: `EmailNotificationService` leía `UserServiceConfig.getSmtpUser()` /
  `getSmtpPassword()`, pero nada mapeaba las env vars `SMTP_USER`/`SMTP_PASSWORD` a las propiedades
  `user-service.smtp-user`/`user-service.smtp-password` — el envío de email fallaba siempre,
  independientemente del entorno. Arreglado en `application.properties`.
- `UserServiceConfig` tenía `dbHost`/`dbUser`/`dbPassword` sin usar en ningún sitio (la conexión real
  va por `spring.datasource.*`) — eliminados.
- `smoke-test.ps1` original: colisión de nombre entre el parámetro `[hashtable]$Body` y la variable
  local `$body` del bloque catch (PowerShell no distingue mayúsculas/minúsculas en nombres de
  variable), y `Invoke-WebRequest -UseBasicParsing` devolviendo `.Content` como `byte[]` en vez de
  `string` para respuestas JSON sin charset explícito — ambos corregidos.

## Qué falta (roadmap)

Todos los puntos del roadmap anterior están resueltos. Próximos pasos razonables, sin urgencia:

1. Tests automatizados de verdad (JUnit + `@SpringBootTest` o `@WebMvcTest`) — hoy la única
   cobertura es `smoke-test.ps1`, manual y contra el jar empaquetado.
2. Validación de entrada con Bean Validation (`spring-boot-starter-validation` + `@Valid` /
   `@NotBlank` en los DTOs) en vez de los `isBlank(...)` a mano en `UserController` — no es urgente,
   es una cuestión de gusto/consistencia con el resto del proyecto.

## Cómo ejecutar

```bash
mvn package
java -jar target/user-service-lite-1.0-SNAPSHOT.jar
```

Requiere JDK 21 (Temurin) y Maven en el PATH — instalados en esta máquina en
`C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot` y `C:\Tools\apache-maven-3.9.16`.

La app escucha en el puerto 8080. `smoke-test.ps1` en la raíz del proyecto habla JSON contra
`http://localhost:8080/users/...` y `http://localhost:8080/actuator/health`:

```powershell
.\smoke-test.ps1
```

## Variables de entorno

| Variable | Descripción | Por defecto |
|----------|-------------|-------------|
| `DB_NAME` | Nombre del fichero de base de datos H2 | `anticheat` |
| `DB_USER` | Usuario de la base de datos | `sa` |
| `DB_PASSWORD` | Contraseña de la base de datos | *(vacía)* |
| `SMTP_USER` / `SMTP_PASSWORD` | Credenciales Gmail para `EmailNotificationService` (mapeadas a `user-service.smtp-user`/`user-service.smtp-password` en `application.properties`) | *(sin valor: falla de forma controlada)* |
