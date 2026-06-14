# US078 — Acesso Remoto do Air Transport Company Collaborator via TCP

## Descrição

Como Air Transport Company Collaborator (ATCC), quero aceder remotamente ao sistema utilizando a aplicação da Air Transport Company.

Esta funcionalidade implementa um servidor TCP dedicado que permite ao ATCC executar todas as suas operações remotamente, sem qualquer acesso direto à base de dados. A autenticação e autorização são sempre verificadas no lado do servidor.

---

## Arquitetura

```
[ATCC] ──TCP:9002──> [AirTransportTcpServer] ──JPA──> [Base de Dados]
                              │
                              └──UDP:9999──> [RemoteAccessesLoggingServer]
```

### Módulo

**`aisafe.app.airtransport.console`** — contém tanto o servidor TCP como o cliente de consola (modos local e remoto na mesma aplicação).

| Componente | Classe | Descrição |
|---|---|---|
| Ponto de entrada | `AirtransportApp` | `--server <porta>` arranca o servidor; `--remote <host> <porta>` arranca o cliente remoto; sem argumentos corre em modo local |
| Servidor TCP | `AirTransportTcpServer` | Escuta na porta 9002; aceita ligações de ATCCs |
| Gestor de ligação | `AttendAirTransportClient` | Trata cada sessão TCP num thread separado (`setDaemon(true)`) |
| Protocolo | `AirTransportMessageCode`, `AirTransportProtocolMessage`, `AirTransportProtocolCodec` | Protocolo binário idêntico ao do WeatherClient/PilotClient |
| Cliente TCP | `AirTransportTcpClient` | Stub usado pela camada de apresentação em modo `--remote` |
| Login remoto | `TcpLoginUI` | Autentica via TCP e mantém a sessão para o `MainMenu` |
| Serviço remoto | `LocalAirTransportService` | Executa as operações de negócio do lado do servidor, à escala da companhia do ATCC autenticado |

---

## Protocolo de Comunicação

O protocolo binário usa o mesmo formato do WeatherClient/PilotClient:

```
| VERSION (1 byte) | CODE (1 byte) | LENGTH_LOW (1 byte) | LENGTH_HIGH (1 byte) | DATA (N bytes) |
```

### Códigos de mensagem

| Código | Byte | Direção | Descrição |
|---|---|---|---|
| `COMMTEST` | 0 | C→S | Teste de conectividade |
| `DISCONN` | 1 | C→S | Desligar sessão |
| `ACK` | 2 | S→C | Resposta de sucesso |
| `ERR` | 3 | S→C | Resposta de erro |
| `AUTH` | 4 | C→S | Autenticação (`email\0password`) |
| `LIST_AIRCRAFT_MODELS` | 20 | C→S | Listar modelos de aeronave disponíveis |
| `LIST_FLEET` | 21 | C→S | Listar a frota da companhia |
| `REGISTER_AIRCRAFT` | 22 | C→S | Registar uma aeronave na frota (US070) |
| `DECOMMISSION_AIRCRAFT` | 23 | C→S | Abater (decomissionar) uma aeronave (US071) |
| `LIST_AIRPORTS` | 24 | C→S | Listar aeroportos |
| `LIST_FLIGHT_ROUTES` | 25 | C→S | Listar rotas da companhia |
| `CREATE_FLIGHT_ROUTE` | 26 | C→S | Criar uma rota de voo (US073) |
| `DELETE_FLIGHT_ROUTE` | 27 | C→S | Desativar uma rota de voo a partir de uma data (US074) |
| `LIST_FLIGHT_PLANS` | 28 | C→S | Listar planos de voo da companhia |
| `CREATE_FLIGHT_PLAN` | 29 | C→S | Criar plano de voo a partir de texto DSL |
| `VALIDATE_FLIGHT_PLAN` | 30 | C→S | Validar/testar plano por designador |
| `ADD_WEATHER_DATA` | 31 | C→S | Associar dados meteorológicos ao plano |
| `LIST_COMPANY_PILOTS` | 32 | C→S | Listar pilotos da companhia (US076) |
| `LIST_AVAILABLE_PILOTS` | 33 | C→S | Listar pilotos disponíveis para adicionar |
| `ADD_PILOT` | 34 | C→S | Adicionar piloto à companhia (US075) |
| `REMOVE_PILOT` | 35 | C→S | Remover/inativar piloto da companhia (US077) |
| `ACTIVATE_PILOT` | 36 | C→S | Reativar piloto da companhia |
| `DEACTIVATE_PILOT` | 37 | C→S | Desativar piloto da companhia |

---

## Funcionalidades Disponíveis Remotamente

| User Story | Operação | Código TCP |
|---|---|---|
| US070 — Adicionar aeronave à frota | `REGISTER_AIRCRAFT` |
| US071 — Decomissionar aeronave | `DECOMMISSION_AIRCRAFT` |
| US072 — Listar frota da companhia | `LIST_FLEET` |
| US073 — Criar rota de voo | `CREATE_FLIGHT_ROUTE` |
| US074 — Desativar rota de voo | `DELETE_FLIGHT_ROUTE` |
| US075 — Adicionar piloto | `ADD_PILOT` |
| US076 — Listar pilotos da companhia | `LIST_COMPANY_PILOTS` |
| US077 — Remover piloto | `REMOVE_PILOT` |
| US080/US081 — Criar plano de voo | `CREATE_FLIGHT_PLAN` |
| US085 — Validar/testar plano de voo | `VALIDATE_FLIGHT_PLAN` |
| US082 — Adicionar dados meteorológicos a um plano | `ADD_WEATHER_DATA` |

---

## Como Executar

### Pré-requisitos

1. A base de dados deve estar a correr (H2 ou PostgreSQL)
2. O servidor de logging UDP deve estar a correr (opcional, mas recomendado)
3. Deve existir pelo menos um utilizador com papel **AIR_TRANSPORT_COMPANY_COLLABORATOR**, associado a um `CustomerCollaborator` do tipo `AIR_TRANSPORT_COMPANY`

### 1. Compilar o módulo

```bash
cd aisafe/
mvn package -pl aisafe.app.airtransport.console -am
```

### 2. Iniciar o Servidor TCP do ATCC

```bash
./run-airtransport-server.sh
# ou numa porta diferente:
./run-airtransport-server.sh 9100
```

O servidor arranca na porta **9002** por defeito.

### 3. Iniciar o Cliente Remoto do ATCC

```bash
# padrão: localhost:9002
./run-airtransport-client.sh

# servidor remoto
./run-airtransport-client.sh 192.168.1.10 9002
```

---

## Fluxo de Autenticação

1. O cliente conecta-se ao servidor via TCP na porta 9002.
2. O cliente envia `AUTH` com `email\0password` (null-byte como separador).
3. O servidor verifica as credenciais através do `AuthzRegistry.authenticationService()`.
4. O servidor verifica se o utilizador tem o papel **AIR_TRANSPORT_COMPANY_COLLABORATOR**.
5. O servidor resolve o `CustomerCollaborator` associado ao utilizador e confirma que é do tipo `AIR_TRANSPORT_COMPANY`, obtendo o `AirTransportCompanyId` e o código ICAO da companhia.
6. Se todos os passos forem bem-sucedidos:
   - Cria um `LocalAirTransportService` à escala da companhia do colaborador.
   - Responde `ACK("Authenticated as ATCC")`.
   - Regista `SUCCESSFUL_LOGIN` no servidor de logging via UDP.
7. Se qualquer passo falhar (credenciais, papel, ou perfil de colaborador):
   - Responde `ERR(...)`.
   - Regista `FAILED_LOGIN` no servidor de logging via UDP.

Todas as operações subsequentes (listar/criar/gerir frota, rotas, pilotos e planos de voo) atuam apenas sobre os dados da companhia do colaborador autenticado.

---

## Logging Remoto

Cada sessão TCP envia eventos UDP ao `RemoteAccessesLoggingServer` (porta 9999):

| Evento | Quando |
|---|---|
| `SUCCESSFUL_LOGIN` | Autenticação bem-sucedida como ATCC |
| `FAILED_LOGIN` | Credenciais erradas, papel insuficiente, ou perfil de colaborador inválido |
| `LOGOUT` | ATCC enviou `DISCONN` explicitamente |
| `DISCONNECT` | Ligação TCP fechada inesperadamente |

O serviço associado é sempre **`US078`** (`RemoteService.US078`).

---

## Configuração

| Variável de Ambiente | Propriedade Java | Padrão | Descrição |
|---|---|---|---|
| `AISAFE_PERSISTENCE_UNIT` | `aisafe.persistence.unit` | (definido em `persistence.xml`) | Unidade de persistência JPA |
| `AISAFE_DB_DIALECT` | `aisafe.db.dialect` | — | Dialeto Hibernate (para RDBMS remota) |
| `AISAFE_DB_DRIVER` | `aisafe.db.driver` | — | Driver JDBC |
| `AISAFE_DB_URL` | `aisafe.db.url` | — | URL JDBC |
| `AISAFE_DB_USERNAME` | `aisafe.db.username` | — | Utilizador da base de dados |
| `AISAFE_DB_PASSWORD` | `aisafe.db.password` | — | Password da base de dados |

A porta TCP é passada como argumento (`./run-airtransport-server.sh [porta]`, default `9002`), tal como o host/porta do cliente (`./run-airtransport-client.sh [host] [porta]`).

---

## Restrições de Segurança

- **Nenhum acesso direto à base de dados** é feito pelo cliente. Toda a interação passa pelo servidor TCP, que valida cada operação através de `LocalAirTransportService`.
- **Apenas utilizadores com o papel `AIR_TRANSPORT_COMPANY_COLLABORATOR`** ligados a um `CustomerCollaborator` do tipo `AIR_TRANSPORT_COMPANY` conseguem autenticar.
- **As operações estão restritas à companhia do colaborador autenticado** — `LocalAirTransportService` é construído com o `AirTransportCompanyId` resolvido durante a autenticação.
- **Cada ligação corre num thread separado (daemon)**, garantindo isolamento entre sessões.
- **Timeout de 30 segundos** no socket do cliente para evitar bloqueios indefinidos.

---

## Estrutura de Ficheiros

```
aisafe.app.airtransport.console/
├── pom.xml
└── src/main/java/
    └── airtransport/
        ├── AirtransportApp.java              ← ponto de entrada (local | --server | --remote)
        ├── tcp/
        │   ├── AirTransportMessageCode.java  ← códigos do protocolo
        │   ├── AirTransportProtocolMessage.java
        │   ├── AirTransportProtocolCodec.java ← serialização/deserialização
        │   ├── AirTransportTcpClient.java    ← stub TCP usado pela UI em modo remoto
        │   ├── AirTransportTcpServer.java    ← servidor TCP (porta 9002)
        │   └── AttendAirTransportClient.java ← handler por ligação
        ├── service/
        │   └── LocalAirTransportService.java ← operações de negócio à escala da companhia
        └── presentation/
            ├── MainMenu.java
            ├── authz/
            │   ├── LoginUI.java               ← login local
            │   └── TcpLoginUI.java            ← login remoto (US078)
            ├── aircraft/   (US070, US071)
            ├── fleet/      (US072 e variantes)
            ├── flightroute/ (US073, US074)
            ├── pilot/      (US075, US076, US077)
            └── flightplan/ (US080/081, US082, US085)
```
