# PostgreSQL 18 Database Setup — VS349

## How It Works

The database server does **not run locally** on your machines. It is hosted on a virtual server (VS349) on the ISEP DEI private cloud. Inside that server, PostgreSQL 18 runs in a Docker container managed automatically by the platform — no Docker installation or configuration is required on your machines.

```
Your PC (DataGrip / Spring Boot)
            |
        Internet
            |
vsgate-s1.dei.isep.ipp.pt:10349   <- public external access port
            |
     Virtual Server VS349 (DEI Cloud)
            |
    Docker container postgres:18   <- PostgreSQL runs here
```

---

## Connection Details

| Field        | Value                                  |
|--------------|----------------------------------------|
| **Host**     | `vsgate-s1.dei.isep.ipp.pt`            |
| **Port**     | `10349`                                |
| **User**     | `postgres`                             |
| **Password** | `F3eSM/qlr7NV`                         |
| **Database** | `postgres`                             |
| **JDBC URL** | `jdbc:postgresql://vsgate-s1.dei.isep.ipp.pt:10349/postgres` |

> WARNING: Do not share this password publicly (e.g. in a public GitHub repository).

---

## Connecting with DataGrip

1. Open DataGrip
2. Click the **`+`** in the left panel -> **Data Source** -> **PostgreSQL**
3. If prompted with "Download missing driver files", click to install the JDBC driver
4. Fill in the fields:
   - **Host:** `vsgate-s1.dei.isep.ipp.pt`
   - **Port:** `10349`
   - **User:** `postgres`
   - **Password:** `F3eSM/qlr7NV`
   - **Database:** `postgres`
5. Click **Test Connection** — should return Successful
6. Click **OK** to save

---

## Spring Boot Configuration

In `src/main/resources/application.properties`, add:

```properties
spring.datasource.url=jdbc:postgresql://vsgate-s1.dei.isep.ipp.pt:10349/postgres
spring.datasource.username=postgres
spring.datasource.password=F3eSM/qlr7NV
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
```

Make sure the PostgreSQL driver dependency is present in `pom.xml`:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

No additional `DatabaseConnection` class is needed — Spring Boot manages the connection automatically through the properties above.

---

## Important Notes

- The server must be **running (START)** in the DEI Cloud interface to accept connections. If the connection fails, check that VS349 is in the **Running** state.
- On the **first boot**, PostgreSQL takes a few minutes to initialise — connections will be refused during this time. Wait and try again.
- Data is **persistent** — even if the container is restarted, databases and tables are preserved.
- External access is **enabled** — no VPN or SSH tunnel is required to connect from outside the ISEP network.
