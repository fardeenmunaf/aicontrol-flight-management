# Database Setup (H2 in-memory and PostgreSQL)

This project supports two persistence modes:

- H2 in-memory for development/testing
- PostgreSQL for persistent storage

## 1) Persistence units

The file `aisafe.persistence.impl/src/main/resources/META-INF/persistence.xml` defines two persistence units:

- `aisafe-pu` (H2 in-memory)
- `aisafe-pu-postgres` (PostgreSQL)

The default is `aisafe-pu` unless you pass a JVM property to select another unit.

## 2) H2 (in-memory) usage

No setup required. Just run the app without extra properties.

Notes:
- The database is created in memory and lost when the app stops.
- Useful for local development and tests.

## 3) PostgreSQL setup

### 3.1 Create user and database

Run these commands in your PostgreSQL instance:

```sql
CREATE USER aisafe_user WITH PASSWORD 'aisafe_password';
CREATE DATABASE aisafe OWNER aisafe_user;
GRANT ALL PRIVILEGES ON DATABASE aisafe TO aisafe_user;
```

### 3.2 Configure connection

The PostgreSQL connection is already declared in `persistence.xml`:

- URL: `jdbc:postgresql://localhost:5432/aisafe`
- User: `aisafe_user`
- Password: `aisafe_password`

If you need to override these without editing XML, use JVM system properties:

```text
-Daisafe.persistence.unit=aisafe-pu-postgres
-Daisafe.db.url=jdbc:postgresql://localhost:5432/aisafe
-Daisafe.db.username=aisafe_user
-Daisafe.db.password=aisafe_password
-Daisafe.db.driver=org.postgresql.Driver
-Daisafe.db.dialect=org.hibernate.dialect.PostgreSQLDialect
```

## 4) Switching between H2 and PostgreSQL

### H2 (default)

Run without extra JVM properties.

### PostgreSQL

Add this JVM property:

```text
-Daisafe.persistence.unit=aisafe-pu-postgres
```

If you are running from an IDE, add the properties to the Run Configuration.

## 5) Schema generation

In `persistence.xml`, the PostgreSQL unit uses:

```text
hibernate.hbm2ddl.auto=update
```

This will create/update tables automatically on startup. You can change it to `validate` for production once the schema is stable.

## 6) Driver dependency

The PostgreSQL driver is declared in the root `pom.xml`:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.3</version>
    <scope>runtime</scope>
</dependency>
```

## 7) Troubleshooting

- Connection refused: confirm the PostgreSQL server is running and the port is correct.
- Authentication failed: check username/password and pg_hba.conf rules.
- Tables not created: ensure `hibernate.hbm2ddl.auto` is `update` or `create`.

