# US090 — Logging Externo dos Acessos Remotos

## Descrição

Como Administrador, quero ter logs de todos os acessos remotos ao sistema.

Esta funcionalidade implementa um servidor dedicado — o **Remote Accesses Logging Server** — que recebe, por **UDP**, eventos de acesso remoto emitidos pelos servidores TCP do sistema (US044, US078, US086) e regista-os em ficheiro. O servidor corre num nó de rede dedicado (potencialmente na cloud), distinto dos servidores que originam os eventos.

---

## Arquitetura

```
[WeatherTcpServer  / US044] ──┐
[AirTransportTcpServer / US078] ──┤──UDP:9999──> [RemoteAccessesLoggingServer]
[PilotTcpServer    / US086] ──┘                          │
                                                          ▼
                                          logs/remote-access-YYYY-MM-DD.log
```

A visualização destes logs num browser (páginas HTTP `/events` e `/active-users`) é a **US091**, implementada na mesma classe — ver `docs/US091`.

### Módulo

**`aisafe.logging`**

| Componente | Classe | Descrição |
|---|---|---|
| Servidor | `RemoteAccessesLoggingServer` | Abre um `DatagramSocket` na porta UDP configurada e processa cada datagrama recebido |
| Emissor UDP (lado cliente) | `UdpLogSender` | Singleton usado pelos servidores TCP (US044/078/086) para enviar eventos; envio assíncrono numa thread daemon |
| Evento | `RemoteAccessEvent` | Aggregate/DTO — `timestamp`, `username`, `clientIp`, `clientPort`, `service`, `eventType`; serializa/desserializa para o formato de wire |
| Tipo de evento | `RemoteAccessEventType` | `SUCCESSFUL_LOGIN`, `FAILED_LOGIN`, `LOGOUT`, `DISCONNECT` |
| Serviço de origem | `RemoteService` | `US044`, `US078`, `US086` |

---

## Critérios de Aceitação

| AC | Critério | Implementação |
|---|---|---|
| AC1 | Eventos transmitidos por **UDP datagrams** para o Remote Accesses Logging Server | `UdpLogSender.send(...)` — `DatagramSocket` efémero, envio numa thread daemon (não bloqueante) |
| AC2 | Registo de logins bem-sucedidos e falhados, logouts e desconexões | `RemoteAccessEventType` cobre os 4 tipos; cada `AttendXxxClient` chama `UdpLogSender.instance().log(...)` em `AUTH`, `DISCONN` e em `IOException` (desconexão inesperada) |
| AC3 | Dados registados incluem timestamp, username, IP, porta do cliente e o serviço de origem (US044, US078 ou US086) | `RemoteAccessEvent` — todos os campos presentes no formato serializado |
| AC4 | A aplicação deve correr num nó de rede dedicado, na cloud | Aplicação standalone (`RemoteAccessesLoggingServer.main`), configurável por variáveis de ambiente; **opção de HTTPS/TLS** na interface de administração (ver secção TLS abaixo) |

---

## Formato de Transmissão (Wire Format)

Cada evento é serializado como uma única linha de texto UTF-8, com `|` como delimitador, enviada num datagrama UDP (até 1024 bytes):

```
<timestamp ISO-8601>|<username>|<clientIp>|<clientPort>|<service>|<eventType>
```

Exemplo:
```
2026-06-14T10:32:05.123Z|pilot@example.com|192.168.1.50|54213|US086|SUCCESSFUL_LOGIN
```

- `RemoteAccessEvent.serialize()` produz esta linha.
- `RemoteAccessEvent.deserialize(raw)` reconstrói o evento no servidor; datagramas malformados (`parts.length != 6`, enum inválido) são descartados com um aviso (`[WARN] Malformed datagram discarded`).

---

## Processamento no Servidor

```
DatagramSocket.receive(packet)
        │
        ▼
handleDatagram(payload)
        │
        ├─ RemoteAccessEvent.deserialize(payload)
        │      └─ falha → "[WARN] Malformed datagram discarded" (ignorado)
        │
        ├─ isDuplicate(payload)?  → "[DUPLICATE] Discarded" (ignorado)
        │
        ├─ recordEvent(event)     → atualiza recentEvents (US091) e activeUsers (US091)
        │
        └─ writeToFile(line)      → logs/remote-access-YYYY-MM-DD.log
```

### Deduplicação

`isDuplicate(payload)` guarda o `payload` exato com o segundo em que foi recebido (`recentEntries`). Se o mesmo payload chegar novamente em menos de 2 segundos, é descartado — protege contra retransmissões duplicadas (ex.: datagramas repetidos por falhas de rede).

### Ficheiro de Log

- Um ficheiro por dia: `logs/remote-access-YYYY-MM-DD.log` (`LocalDate.now()`).
- Cada linha tem o formato de colunas alinhadas:
  ```
  <timestamp> | <username (20)> | <clientIp (15)> | <clientPort (5)> | <service (6)> | <eventType>
  ```
- Escrita com `Files.writeString(..., CREATE, APPEND)`.

---

## Como Executar

### 1. Compilar o módulo

```bash
cd aisafe/
mvn package -pl aisafe.logging -am
```

### 2. Iniciar o servidor

```bash
./run-logging-server.sh
```

Por defeito escuta UDP na porta **9999** e cria `logs/` no diretório atual. A US091 (servidor HTTP de status) arranca automaticamente em simultâneo.

### 3. Configuração

| Variável de Ambiente | Propriedade Java | Padrão | Descrição |
|---|---|---|---|
| `AISAFE_LOGGING_PORT` | `aisafe.logging.port` | `9999` | Porta UDP de receção dos eventos |
| `AISAFE_LOGGING_DIR` | `aisafe.logging.dir` | `logs/` | Diretório dos ficheiros de log |
| `AISAFE_LOGGING_HTTP_PORT` | `aisafe.logging.http.port` | `8080` | Porta HTTP de status (US091) |

Do lado dos emissores (US044/078/086), `UdpLogSender` resolve o destino através de:

| Variável de Ambiente | Propriedade Java | Padrão |
|---|---|---|
| `AISAFE_LOGGING_HOST` | `aisafe.logging.host` | `localhost` |
| `AISAFE_LOGGING_PORT` | `aisafe.logging.port` | `9999` |

---

## TLS para o Nó Dedicado (AC4)

A AC4 exige que o **Remote Accesses Logging Server** corra num nó de rede dedicado na cloud — ou seja, a sua interface de administração (US091, porta HTTP) fica potencialmente acessível através de uma rede não confiável. Para proteger essa interface, o servidor HTTP pode ser promovido a **HTTPS** com um certificado autoassinado (TP11 — SSL/TLS):

```bash
# 1. Gerar o certificado autoassinado (uma vez)
./generate-logging-keystore.sh localhost logging-keystore.p12 changeit

# 2. Arrancar o servidor com TLS ativo
export AISAFE_LOGGING_TLS_KEYSTORE=./logging-keystore.p12
export AISAFE_LOGGING_TLS_KEYSTORE_PASSWORD=changeit
./run-logging-server.sh
```

| Variável de Ambiente | Propriedade Java | Descrição |
|---|---|---|
| `AISAFE_LOGGING_TLS_KEYSTORE` | `aisafe.logging.tls.keystore` | Caminho para o keystore PKCS12 com o certificado autoassinado |
| `AISAFE_LOGGING_TLS_KEYSTORE_PASSWORD` | `aisafe.logging.tls.keystore.password` | Password do keystore (padrão `changeit`) |

Sem estas variáveis, o servidor mantém-se em HTTP simples. O canal **UDP** (receção dos eventos US90) não é afetado — apenas a interface de administração HTTP/HTTPS (US091).

Detalhes de implementação (`createHttpServer`, `buildSslContext`) em `RemoteAccessesLoggingServer.java`.

---

## Princípios EAPLI — Aplicação Prática

### Domain-Driven Design (DDD)

- **Linguagem Ubíqua**: `remote access`, `event type`, `service`, `active session` são consistentes entre o domínio, os emissores TCP e o servidor.
- **Value Objects**: `RemoteAccessEventType` e `RemoteService` são enumerações imutáveis.
- **Aggregate / DTO**: `RemoteAccessEvent` é a unidade atómica transportada via UDP — serialização/desserialização encapsuladas na própria classe.

### Padrões Gang of Four (GoF)

| Padrão | Aplicação | Benefício |
|---|---|---|
| **Singleton** | `UdpLogSender.instance()` | Uma única instância por processo emissor; configuração resolvida uma vez |
| **DTO** | `RemoteAccessEvent.serialize()/deserialize()` | Desacopla o formato de transporte UDP do domínio |

### Princípios SOLID

- **SRP**: `UdpLogSender` apenas envia; `RemoteAccessesLoggingServer` apenas recebe, deduplica e persiste; `RemoteAccessEvent` apenas representa o evento.
- **OCP**: Novos serviços (ex. um futuro US099) podem ser adicionados a `RemoteService` sem alterar o servidor.
- **DIP**: Os servidores TCP (US044/078/086) dependem de `UdpLogSender` (abstração singleton), não da implementação do logging server.

---

## Testes

| Classe de teste | O que verifica |
|---|---|
| `RemoteAccessEventTest` | Criação, serialização, desserialização (round-trip), validação de nulos, todos os serviços e tipos de evento |
| `RemoteAccessesLoggingServerTest` | Receção e processamento de eventos UDP, deduplicação, rejeição de datagramas malformados, escrita do ficheiro de log, leitura de configuração via system properties |

---

## Relação com outras User Stories

- **US044 / US078 / US086** — emitem os eventos via `UdpLogSender`.
- **US091** — expõe `/events` e `/active-users` em HTTP(S) para visualizar os dados recolhidos por esta US.
