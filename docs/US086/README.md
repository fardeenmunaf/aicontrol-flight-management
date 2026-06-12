# US086 — Acesso Remoto do Piloto via TCP

## Descrição

Como Piloto, quero aceder remotamente ao sistema utilizando a aplicação da Air Transport Company.

Esta funcionalidade implementa um cliente TCP dedicado que permite ao Piloto executar todas as suas operações remotamente, sem qualquer acesso direto à base de dados. A autenticação e autorização são sempre verificadas no lado do servidor.

---

## Arquitetura

```
[Piloto] ──TCP:9001──> [PilotTcpServer] ──JPA──> [Base de Dados]
                              │
                              └──UDP:9999──> [RemoteAccessesLoggingServer]
```

### Módulo criado

**`aisafe.app.pilot.client`** — contém tanto o servidor TCP como o cliente de consola.

| Componente | Classe | Descrição |
|---|---|---|
| Servidor TCP | `PilotTcpServer` | Escuta na porta 9001; aceita ligações de Pilotos |
| Gestor de ligação | `AttendPilotClient` | Trata cada sessão TCP num thread separado |
| Protocolo | `PilotMessageCode`, `PilotProtocolMessage`, `PilotProtocolCodec` | Protocolo binário idêntico ao do WeatherClient |
| Cliente TCP | `PilotTcpClient` | Stub utilizado pela camada de apresentação |
| Sessão do cliente | `PilotClientSession` | Singleton com a ligação ativa |
| Aplicação cliente | `PilotClientApp` | Ponto de entrada do cliente remoto |
| Menu do cliente | `PilotMenu` | Menu principal do Piloto |

---

## Protocolo de Comunicação

O protocolo binário usa o mesmo formato do WeatherClient:

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
| `LIST_MY_PLANS` | 20 | C→S | Listar planos de voo do Piloto |
| `CREATE_PLAN` | 21 | C→S | Criar plano a partir de texto DSL |
| `VALIDATE_PLAN` | 22 | C→S | Validar/testar plano por designador |
| `ADD_WEATHER` | 23 | C→S | Associar dados meteorológicos ao plano |
| `LIST_FLEET` | 24 | C→S | Listar frota certificada do Piloto |
| `LIST_ROUTES` | 25 | C→S | Listar rotas da empresa do Piloto |

---

## Funcionalidades Disponíveis Remotamente

| User Story | Operação | Código TCP |
|---|---|---|
| Visualizar planos de voo | Listar planos com estado/rota/partida | `LIST_MY_PLANS` |
| Criar plano de voo | Enviar DSL (ficheiro ou texto) | `CREATE_PLAN` |
| Validar/testar plano | Validação DSL + física pelo ID | `VALIDATE_PLAN` |
| Adicionar dados meteorológicos | Associar registos de weather ao plano | `ADD_WEATHER` |
| Consultar frota certificada | Lista de aeronaves disponíveis | `LIST_FLEET` |
| Consultar rotas da empresa | Lista de rotas ativas | `LIST_ROUTES` |

---

## Como Executar

### Pré-requisitos

1. A base de dados deve estar a correr (H2 ou PostgreSQL)
2. O servidor de logging UDP deve estar a correr (opcional, mas recomendado)
3. Deve existir pelo menos um utilizador com papel **PILOT** registado

### 1. Compilar o módulo

```bash
# Linux/macOS
cd aisafe/
mvn package -pl aisafe.app.pilot.client -am

# Windows
cd aisafe\
mvn package -pl aisafe.app.pilot.client -am
```

### 2. Iniciar o Servidor TCP do Piloto

```bash
# Linux/macOS
./run-pilot-server.sh

# Windows
run-pilot-server.bat
```

O servidor arranca na porta **9001** por defeito.  
Para usar outra porta: `-Daisafe.pilot.port=XXXX`

### 3. Iniciar o Cliente Remoto do Piloto

```bash
# Linux/macOS (padrão: localhost:9001)
./run-pilot-client.sh

# Linux/macOS (servidor remoto)
./run-pilot-client.sh 192.168.1.10 9001

# Windows
run-pilot-client.bat
```

O cliente pergunta o endereço IP e porta do servidor se não forem passados como argumento.

---

## Fluxo de Autenticação

1. O cliente conecta-se ao servidor via TCP na porta 9001.
2. O cliente envia `AUTH` com `email\0password` (null-byte como separador).
3. O servidor verifica as credenciais através do `AuthzRegistry.authenticationService()`.
4. O servidor verifica se o utilizador tem o papel **PILOT**.
5. Se autenticado:
   - Responde `ACK("Authenticated as Pilot")`.
   - Regista `SUCCESSFUL_LOGIN` no servidor de logging via UDP.
6. Se falhar:
   - Responde `ERR(...)`.
   - Regista `FAILED_LOGIN` no servidor de logging via UDP.

---

## Logging Remoto

Cada sessão TCP envia eventos UDP ao `RemoteAccessesLoggingServer` (porta 9999):

| Evento | Quando |
|---|---|
| `SUCCESSFUL_LOGIN` | Autenticação bem-sucedida como Piloto |
| `FAILED_LOGIN` | Credenciais erradas ou papel insuficiente |
| `LOGOUT` | Piloto enviou `DISCONN` explicitamente |
| `DISCONNECT` | Ligação TCP fechada inesperadamente |

O serviço associado é sempre **`US086`**.

---

## Configuração

| Variável de Ambiente | Propriedade Java | Padrão | Descrição |
|---|---|---|---|
| `AISAFE_PILOT_HOST` | `aisafe.pilot.host` | `localhost` | Host do servidor (lado cliente) |
| `AISAFE_PILOT_PORT` | `aisafe.pilot.port` | `9001` | Porta TCP |
| `AISAFE_LOGGING_HOST` | `aisafe.logging.host` | `localhost` | Host do servidor de logging |
| `AISAFE_LOGGING_PORT` | `aisafe.logging.port` | `9999` | Porta UDP do servidor de logging |

---

## Restrições de Segurança

- **Nenhum acesso direto à base de dados** é feito pelo cliente. Todo o acesso a dados passa pelo servidor TCP.
- **Apenas utilizadores com o papel `PILOT`** conseguem autenticar. Outros papéis (ADMIN, ATCC, etc.) recebem erro de autorização.
- **Cada ligação corre num thread separado**, garantindo isolamento entre sessões.
- **Timeout de 30 segundos** no socket do cliente para evitar bloqueios indefinidos.

---

## Estrutura de Ficheiros

```
aisafe.app.pilot.client/
├── pom.xml
└── src/main/java/
    └── pilot/
        ├── PilotClientApp.java          ← ponto de entrada do cliente
        ├── PilotClientSession.java      ← singleton da sessão ativa
        ├── server/
        │   ├── PilotMessageCode.java    ← códigos do protocolo
        │   ├── PilotProtocolMessage.java
        │   ├── PilotProtocolCodec.java  ← serialização/deserialização
        │   ├── PilotTcpClient.java      ← stub TCP usado pela UI
        │   ├── PilotTcpServer.java      ← servidor TCP (porta 9001)
        │   └── AttendPilotClient.java   ← handler por ligação
        └── presentation/
            ├── PilotMenu.java
            ├── authz/
            │   └── PilotLoginUI.java
            ├── flightplan/
            │   ├── ListFlightPlansUI.java
            │   ├── CreateFlightPlanRemoteUI.java
            │   ├── ValidateFlightPlanRemoteUI.java
            │   └── AddWeatherDataRemoteUI.java
            ├── fleet/
            │   └── ListFleetRemoteUI.java
            └── flightroute/
                └── ListFlightRoutesRemoteUI.java
```
