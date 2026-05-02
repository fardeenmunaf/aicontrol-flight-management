# Database Setup (H2 in-memory and PostgreSQL)

This project supports two persistence modes:

- H2 in-memory for development/testing
- PostgreSQL for persistent storage

## 1) Persistence units

The file `aisafe.persistence.impl/src/main/resources/META-INF/persistence.xml` defines two persistence units:

- `aisafe-pu` (H2 in-memory)
- `aisafe-pu-postgres` (PostgreSQL)

The default is `aisafe-pu` (H2) used during development and tests.

## 2) H2 (in-memory) usage

No setup required. Just run the app — the H2 database is created automatically in memory.

Notes:
- The database is created in memory and lost when the app stops.
- Useful for local development and unit tests.
- Schema is created and dropped automatically (`create-drop`).

## 3) PostgreSQL setup

### 3.1 Create the database

Connect to your PostgreSQL instance (via psql or DataGrip) and run:

```sql
CREATE DATABASE aisafe;
```

The default superuser `postgres` is used. No extra user creation is needed for local development.

### 3.2 Connection details

The PostgreSQL connection is declared in `persistence.xml`:

- URL: `jdbc:postgresql://localhost:5432/aisafe`
- User: `postgres`
- Password: `postgres`

If your local PostgreSQL uses a different password, update it directly in `persistence.xml`:

```xml
<property name="jakarta.persistence.jdbc.user"     value="postgres"/>
<property name="jakarta.persistence.jdbc.password" value="YOUR_PASSWORD_HERE"/>
```

### 3.3 DataGrip configuration

To inspect the database using DataGrip:

1. Open DataGrip → `+` → `Data Source` → `PostgreSQL`
2. Fill in:
   - Host: `localhost`
   - Port: `5432`
   - Database: `aisafe`
   - User: `postgres`
   - Password: `postgres`
3. Click `Test Connection` → should show green
4. Click `OK`

Tables are created automatically by Hibernate on first run.
To inspect data, open a Query Console and run:

```sql
SELECT * FROM user_entity;
SELECT * FROM aircraft;
SELECT * FROM flight;
```

## 4) Switching between H2 and PostgreSQL

To switch persistence unit, change the constant in `JpaRepositoryFactory.java`:

```java
// H2 in-memory (development/tests)
private static final String PERSISTENCE_UNIT = "aisafe-pu";

// PostgreSQL (production/integration)
private static final String PERSISTENCE_UNIT = "aisafe-pu-postgres";
```

If running from an IDE (e.g. IntelliJ), you can also set the persistence unit via the Run Configuration
by adding a JVM property:

```text
-Daisafe.persistence.unit=aisafe-pu-postgres
```

## 5) Schema generation

| Persistence unit   | `hbm2ddl.auto` | Behaviour                              |
|--------------------|----------------|----------------------------------------|
| `aisafe-pu`        | `create-drop`  | Schema created on start, dropped on stop |
| `aisafe-pu-postgres` | `update`     | Tables created/updated automatically   |

Once the schema is stable, change PostgreSQL to `validate` for production:

```xml
<property name="hibernate.hbm2ddl.auto" value="validate"/>
```

## 6) Driver dependency

The PostgreSQL driver is declared in `aisafe.persistence.impl/pom.xml`:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.3</version>
</dependency>
```

The H2 driver is declared with test scope:

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <version>2.2.224</version>
    <scope>test</scope>
</dependency>
```

## 7) Troubleshooting

- **Connection refused**: confirm PostgreSQL is running on port 5432.
- **Authentication failed**: check username/password in `persistence.xml` match your local PostgreSQL setup.
- **Tables not created**: ensure `hibernate.hbm2ddl.auto` is set to `update` or `create`.
- **Class not found — org.postgresql.Driver**: confirm the PostgreSQL dependency is present in `pom.xml`.
- **H2 data lost between runs**: expected behaviour — H2 is in-memory only. Use PostgreSQL for persistent data.