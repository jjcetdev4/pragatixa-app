# SPDMS Modularization Strategy

## Current Architecture
The current SPDMS backend uses a flat, functional package structure under `jjcet.PragatiX` (e.g., all controllers in one package, all entities in another). While functional, this monolithic structure makes the codebase harder to maintain, scale, and understand as the domain grows.

## Target Architecture
The system will be refactored into a **Modular Monolith** architecture. This divides the application into distinct, cohesive business modules (Authentication, Student, Activity, XP, Badge, Team, Faculty, Leaderboard, Notification, Admin).
Each module will contain its own layered architecture (controller, service, repository, dto, mapper, validator, exception) ensuring high cohesion and low coupling.
Cross-cutting concerns (config, exceptions, security, etc.) will reside in `common`.
Domain core components (entities, enums, global specifications) will reside in `domain`.
Infrastructure components (database, logging, mail, swagger) will reside in `infrastructure`.

## Migration Rules
1. **No Logic Changes:** Business logic must remain exactly the same.
2. **No API Breakage:** Endpoints and URLs must not change.
3. **No Schema Changes:** The database schema remains unchanged.
4. **No Security Modification:** The JWT flow and security configurations must be preserved as-is.
5. **Incremental Approach:** Move one module at a time. Do not attempt a "big bang" migration.
6. **Maintain Compilation:** The codebase must compile successfully after each atomic migration step.

## Migration Order
1. **Infrastructure & Common:** Extract configurations, security, utilities, and global exceptions.
2. **Domain Layer:** Move entities, enums, and global repositories/specifications to the domain package.
3. **Core Modules (Independent):** Migrate low-dependency modules like Authentication and Notification.
4. **Dependent Modules:** Migrate modules that depend on core modules (Student, Faculty, Admin).
5. **Complex Modules:** Migrate feature-heavy modules like Activity, XP, Badge, Team, Leaderboard.

## Module Boundaries
Modules should encapsulate their own:
- **Presentation:** Controllers and request/response DTOs.
- **Business Logic:** Services and validators.
- **Data Access:** Specific repositories.
Cross-module communication should ideally happen via Services or dedicated interfaces, avoiding tight coupling between repositories of different modules.

## Folder Purpose
- **common:** Application-wide cross-cutting concerns (security, config, utilities).
- **domain:** Centralized business domain models, entities, and base repository interfaces.
- **infrastructure:** Technical details, external integrations, database migrations, and scheduling.
- **modules:** Feature-based packages containing their own vertical slices (controller to repository).

## Dependency Rules
- Modules can depend on `common` and `domain`.
- Modules can depend on `infrastructure` only when absolutely necessary (e.g., storage, mail).
- Modules should minimize dependencies on other modules.
- `common`, `domain`, and `infrastructure` must NOT depend on any specific `modules`.

## Refactoring Strategy
1. **Prepare:** Create the target package structure (Completed).
2. **Move:** Relocate classes to their new packages. Update imports.
3. **Verify:** Compile the project and run existing tests.
4. **Commit:** Commit the changes for that specific step.
5. **Repeat:** Move to the next module/layer until the flat structure is empty.
6. **Cleanup:** Remove empty legacy packages.
