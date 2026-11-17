DEVELOPER & AI AGENT DOCUMENTATION (Agent.md)

This document provides the complete context for the social networking application project (similar to Locket). Please read this document carefully before proposing code, fixing bugs, or adding new features.

REMEMBER: KEEP THING SIMPLE AND EFFECTIVE NO overengineering  

1. PROJECT OVERVIEW

Goal: Develop an instant photo-sharing application with friends.

The system consists of 2 main parts:

Backend: RESTful API providing data, business logic, and authentication.

Android: Mobile client application for users.

2. TECH STACK

2.1. Backend (Spring Boot)

Language: Java 

Framework: Spring Boot 4.x (Java 21, Jackson 3)

Database: MySQL

ORM: Spring Data JPA / Hibernate

Security: Spring Security + JWT (JSON Web Token)

Tools: Lombok (boilerplate code generation), hand-written mappers (Mapper/UserMapper), SLF4J + Logback (logging), Firebase Admin (push).

API Documentation: Springdoc OpenAPI (Swagger 3).

2.2. Android (Client)

Language: Kotlin

UI: Jetpack Compose (single Activity, Navigation Compose with type-safe routes), Glance for the home screen widget.

Architecture: MVVM + Repository, offline-first. There is NO domain/UseCase layer on purpose (keep it simple): ViewModel -> Repository -> Api/Dao.

API Calls: Retrofit + OkHttp.

Asynchronous Processing: Kotlin Coroutines & Flow.

Dependency Injection: Hilt / Dagger.

Image Loading: Coil 3.

Local data: Room (cache + offline outbox, with Migrations and exported schemas), DataStore (session, settings), WorkManager (OutboxWorker).

3. ARCHITECTURE AND DIRECTORY STRUCTURE

3.1. Backend Structure (LokiBE/LokiBE/src/main/java/com/nihongu/lokibe). See LokiBE/LokiBE/README.md for config and the API list.

controller/: REST API endpoints (receive requests, return responses).

service/: Business Logic (interfaces and implementations). Do not call Controllers directly.

repository/: Interfaces extending JpaRepository to communicate with the Database.

entity/: Classes mapping to Database tables (using @Entity).

dto/: Data Transfer Objects (Request/Response payloads).

exception/: Global Exception Handling (@ControllerAdvice) and Custom Exceptions (e.g., UnauthorizedException).

Jwt/: Spring Security configuration and the JWT filter. Util/: JwtUtils, SecurityUtils, ImageUtils.

Config/: JacksonConfig (dates are written with the server's UTC offset).

Service/Device/: DeviceTokenService (one user, many phones for push).

3.2. Android Structure (Loki/app/src/main/java/com/example/loki). See Loki/README.md for the full tree, features and config.
Use single ACTIVITY 

ui/: Screens, UI Components, and ViewModels.

data/:

remote/: Retrofit API interfaces, DTOs.

local/: Room Database (for offline caching), DataStore/SharedPreferences.

repository/: Where data from remote and local sources are combined. ViewModels only talk to repositories, never to Retrofit APIs.

worker/: OutboxWorker (sends what was queued offline).

di/: Hilt configuration modules for dependency injection.

core/util/: Small shared helpers (image compression, Cloudinary thumbnails, emoji check).

push/: FCM service, PushType, PushEvents.

4. CODING CONVENTIONS

4.1. General

Variables, functions: camelCase.

Classes, interfaces: PascalCase.

Constants: UPPER_SNAKE_CASE.

API error responses must follow a standard JSON format. Example:
{ "status": 400, "message": "Invalid format", "data": null }

4.2. Backend

Always use DTOs to exchange data between Controllers and Services. NEVER return Entities directly from the API.

Use @Slf4j to log important processing steps.

Paginate large lists using Pageable.

Fully document APIs using Swagger (@Operation, @Tag).

Never expose another user's email: use UserMapper.toProfile for others, toSelf only for the current user.

Create endpoints that the app may retry (sending photos/messages) must accept a clientId and return the stored row on a repeat.

Secrets come from environment variables (or the git-ignored application-dev.properties on a dev machine), never from application.properties.

4.3. Android

Do not write API calling logic or complex data processing in the UI (Activity/Fragment/Composable). Put it in the ViewModel.

Store JWT Tokens securely in EncryptedSharedPreferences or DataStore, and keep them out of Auto Backup (res/xml/backup_rules.xml, data_extraction_rules.xml).

Use OkHttp Interceptors to automatically attach the Bearer Token to the Header of every request.

Offline-first: screens read Room; API results are written to Room first.

Send photos/messages only through OutboxRepository (sendPhoto/sendMessage): it queues on a lost connection and keeps the clientId so a retry never duplicates.

Changing a Room entity: bump the version, add a Migration in LokiDatabase.kt, commit the schema in app/schemas/.

Every Kotlin file starts with a "TỔNG QUAN FILE" comment (role, main parts, caveats, in Vietnamese). Update it when the file's behaviour changes.

Before a release: run ./gradlew assembleRelease (R8 is on) and try it on a real phone.

5. AI AGENT INSTRUCTIONS (PROMPTS)

When interacting with this project, the AI must strictly follow:

Check imports: Pay attention to imported libraries (e.g., use io.jsonwebtoken for JWT, avoid accidentally importing java.security).

Context-aware: If generating a new Android screen, automatically suggest the corresponding DTO and Controller for the Backend if they don't exist.

Security: Never hardcode sensitive information (JWT Secret, DB Password) into the code. Always read from application.properties or .env.

Responses: Provide concise code, specifying exactly which files need to be modified and where.