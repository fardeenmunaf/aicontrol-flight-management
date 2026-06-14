# US091 — Visualização dos Logs de Acessos Remotos

## Descrição

Como Administrador, quero visualizar os logs guardados no Remote Accesses Logging Server.

Esta funcionalidade embute um servidor HTTP no próprio Remote Accesses Logging Server, expondo duas páginas web que apresentam em tempo quase real os eventos de acesso remoto registados (US090) e os utilizadores com sessão ativa no sistema.

---

## Arquitetura

```
[Browser]  ──HTTP GET /events──────> [HTTP Server :8080]
           ──HTTP GET /active-users─>       │
           ──AJAX /api/events────────>       │
           ──AJAX /api/active-users──>       │
                                            │
[US044/078/086 servers] ──UDP:9999──> [RemoteAccessesLoggingServer]
                                            │
                                     logs/remote-access-YYYY-MM-DD.log
```

### Módulo

**`aisafe.logging`** — contém o servidor UDP (US090) e o servidor HTTP (US091) na mesma classe.

| Componente | Classe | Descrição |
|---|---|---|
| Servidor principal | `RemoteAccessesLoggingServer` | Recebe eventos UDP e serve as páginas HTTP |
| Domínio do evento | `RemoteAccessEvent` | Representa um evento de acesso remoto |
| Tipo de evento | `RemoteAccessEventType` | `SUCCESSFUL_LOGIN`, `FAILED_LOGIN`, `LOGOUT`, `DISCONNECT` |
| Serviço de origem | `RemoteService` | `US044`, `US078`, `US086` |
| Emissor UDP | `UdpLogSender` | Usado pelos servidores TCP para enviar eventos |

---

## Páginas HTTP

| URL | Descrição |
|---|---|
| `/` | Página inicial com links de navegação |
| `/events` | Últimos N eventos registados (AC2) |
| `/active-users` | Utilizadores com sessão ativa (AC3) |
| `/api/events` | JSON com os eventos (endpoint AJAX) |
| `/api/active-users` | JSON com os utilizadores ativos (endpoint AJAX) |

O servidor HTTP é iniciado automaticamente no arranque do logging server, sem necessidade de Nginx, Apache ou qualquer servidor externo (AC1).

---

## Critérios de Aceitação

| AC | Critério | Implementação |
|---|---|---|
| AC1 | Servidor HTTP embutido na aplicação | `com.sun.net.httpserver.HttpServer` do JDK |
| AC2 | Página com os últimos eventos (máx. 100) | `/events` + `/api/events` |
| AC3 | Página com utilizadores ativos | `/active-users` + `/api/active-users` |
| AC4 | Páginas sempre atualizadas sem reload | AJAX com `setInterval` a cada 3 segundos |
| AC5 | AJAX com endpoints JSON dedicados | `/api/events` e `/api/active-users` |
| AC6 | Acessível em browser standard | HTML + JavaScript puro, sem plugins |

---

## Campos apresentados

### Página de Eventos (`/events`)

| Campo | Fonte |
|---|---|
| Timestamp | `RemoteAccessEvent.timestamp()` |
| Username | `RemoteAccessEvent.username()` |
| IP do cliente | `RemoteAccessEvent.clientIp()` |
| Porta do cliente | `RemoteAccessEvent.clientPort()` |
| Serviço | `RemoteAccessEvent.service()` (US044 / US078 / US086) |
| Tipo de evento | `RemoteAccessEvent.eventType()` |

### Página de Utilizadores Ativos (`/active-users`)

Um utilizador é considerado ativo desde o seu `SUCCESSFUL_LOGIN` até ao `LOGOUT` ou `DISCONNECT`.

| Campo | Fonte |
|---|---|
| Username | `RemoteAccessEvent.username()` |
| IP do cliente | `RemoteAccessEvent.clientIp()` |
| Porta do cliente | `RemoteAccessEvent.clientPort()` |
| Serviço | `RemoteAccessEvent.service()` |
| Hora de login | `RemoteAccessEvent.timestamp()` |

---

## Princípios EAPLI — Aplicação Prática

### Domain-Driven Design (DDD)

- **Linguagem Ubíqua**: Termos como `remote access`, `active session`, `logging`, `event type`, `service` são usados de forma consistente entre o domínio, o servidor e as páginas web.
- **Value Objects**: `RemoteAccessEventType` e `RemoteService` são enumerações imutáveis que representam conceitos do domínio sem identidade própria.
- **Aggregate**: `RemoteAccessEvent` encapsula todos os dados de um evento de acesso; é serializado/desserializado como unidade atómica para transporte via UDP.

### Padrões Gang of Four (GoF)

| Padrão | Aplicação | Benefício |
|---|---|---|
| **Singleton** | `UdpLogSender.instance()` — uma única instância por processo | Evita múltiplas sockets UDP e garante configuração única |
| **Facade** | `RemoteAccessesLoggingServer` esconde a complexidade de dois servidores (UDP + HTTP) atrás de um único método `start()` | Os servidores TCP (US044/086) apenas chamam `UdpLogSender.log(...)` |
| **DTO** | `RemoteAccessEvent` transporta os dados do servidor TCP para o logging server via UDP sem expor internos | A serialização/desserialização é encapsulada na própria classe |

### Princípios SOLID

- **SRP**: `RemoteAccessesLoggingServer` gere os dois servidores; `RemoteAccessEvent` representa apenas o evento; `UdpLogSender` é responsável só pelo envio UDP.
- **OCP**: Novos serviços (ex. US099) podem ser adicionados ao enum `RemoteService` sem alterar o servidor HTTP ou o domínio.
- **DIP**: Os servidores TCP (US044, US086) dependem de `UdpLogSender` (abstração), não de qualquer implementação do logging server.

---

## Fluxo de Atualização AJAX

```
Browser (cada 3 segundos)
      │
      ├─ GET /api/events ──────> servidor Java ──> lista dos últimos 100 eventos (JSON)
      │                                  └──> tbody da tabela atualizado sem reload
      │
      └─ GET /api/active-users ─> servidor Java ──> mapa de sessões ativas (JSON)
                                         └──> tbody da tabela atualizado sem reload
```

---

## Gestão de Estado em Memória

```
recentEvents  : Deque<RemoteAccessEvent>   (máx. 100, FIFO)
activeUsers   : LinkedHashMap<username, event>

SUCCESSFUL_LOGIN  → adiciona a recentEvents + insere em activeUsers
FAILED_LOGIN      → adiciona a recentEvents (não altera activeUsers)
LOGOUT            → adiciona a recentEvents + remove de activeUsers
DISCONNECT        → adiciona a recentEvents + remove de activeUsers
```

O acesso ao estado é protegido com `synchronized` para garantir consistência entre o thread UDP e os threads HTTP.

---

## Configuração

| Variável de ambiente | System property | Default | Descrição |
|---|---|---|---|
| `AISAFE_LOGGING_PORT` | `aisafe.logging.port` | `9999` | Porta UDP |
| `AISAFE_LOGGING_HTTP_PORT` | `aisafe.logging.http.port` | `8080` | Porta HTTP |
| `AISAFE_LOGGING_DIR` | `aisafe.logging.dir` | `logs/` | Diretório dos ficheiros de log |

---

## Como Correr

```bash
# Compilar (se necessário)
./build-all.sh

# Iniciar o servidor
./run-logging-server.sh

# Abrir no browser
http://localhost:8080/          # Página inicial
http://localhost:8080/events    # Últimos eventos
http://localhost:8080/active-users  # Utilizadores ativos
```

---

## Testes

| Classe de teste | O que verifica |
|---|---|
| `RemoteAccessEventTest` | Criação, serialização, desserialização (round-trip), validação de nulos, todos os serviços e tipos de evento |
| `RemoteAccessesLoggingServerTest` | Servidor HTTP (todas as páginas e endpoints), receção e processamento de eventos UDP, gestão de utilizadores ativos, deteção de duplicados, rejeição de datagrams malformados, escrita do ficheiro de log, arranque com porta HTTP ocupada, saída antecipada com diretório inválido, leitura de configuração via system properties |
