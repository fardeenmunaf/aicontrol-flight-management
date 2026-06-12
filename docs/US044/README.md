# US044 — Acesso Remoto do Weather Person via TCP

## Descrição

Como Weather Person, quero aceder remotamente ao sistema para fazer upload de dados meteorológicos.

Esta funcionalidade implementa um cliente TCP dedicado que permite ao Weather Person executar todas as suas operações remotamente, sem qualquer acesso direto à base de dados. A autenticação e autorização são sempre verificadas no lado do servidor.

---

## Arquitetura

```
[Weather Person] ──TCP:9000──> [WeatherTcpServer] ──JPA──> [Base de Dados]
                                       │
                                       └──UDP:9999──> [RemoteAccessesLoggingServer]
```

### Módulo criado

**`aisafe.app.weather.client`** — contém tanto o servidor TCP como o cliente de consola.

| Componente | Classe | Descrição |
|---|---|---|
| Servidor TCP | `WeatherTcpServer` | Escuta na porta 9000; aceita ligações de Weather Persons |
| Gestor de ligação | `AttendClient` | Trata cada sessão TCP num thread separado |
| Protocolo | `MessageCode`, `ProtocolMessage`, `ProtocolCodec` | Protocolo binário com cabeçalho de 4 bytes |
| Cliente TCP | `WeatherTcpClient` | Stub utilizado pela camada de apresentação |
| Sessão do cliente | `ClientSession` | Singleton com a ligação ativa e modo de conexão |
| Aplicação cliente | `WeatherClientApp` | Ponto de entrada do cliente remoto |
| Menu do cliente | `WeatherMenu` | Menu principal do Weather Person |

---

## Protocolo de Comunicação

O protocolo binário usa o seguinte formato:

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
| `UPLOAD_WEATHER` | 10 | C→S | Registar dados meteorológicos (individual ou bulk CSV) |
| `LIST_WEATHER` | 11 | C→S | Listar dados meteorológicos com filtros opcionais |

---

## Funcionalidades Disponíveis Remotamente

| User Story | Operação | Código TCP |
|---|---|---|
| Registar dados meteorológicos (US041) | Enviar um registo individual | `UPLOAD_WEATHER` |
| Bulk import de dados meteorológicos (US042) | Enviar ficheiro CSV com múltiplos registos | `UPLOAD_WEATHER` (payload multi-linha) |
| Consultar dados meteorológicos | Listar por área e/ou data | `LIST_WEATHER` |

---

## Como Executar

### Pré-requisitos

1. A base de dados deve estar a correr (H2 ou PostgreSQL)
2. O servidor de logging UDP deve estar a correr (opcional, mas recomendado)
3. Deve existir pelo menos um utilizador com papel **WEATHER_PERSON** registado

### 1. Compilar o módulo

```bash
# Linux/macOS
cd aisafe/
mvn package -pl aisafe.app.weather.client -am

# Windows
cd aisafe\
mvn package -pl aisafe.app.weather.client -am
```

### 2. Iniciar o Servidor TCP do Weather Person

```bash
# Linux/macOS
./run-weatherserver.sh

# Windows
run-weatherserver.bat
```

O servidor arranca na porta **9000** por defeito.

### 3. Iniciar o Cliente Remoto do Weather Person

```bash
# Linux/macOS
./run-weatherclient.sh

# Windows
run-weatherclient.bat
```

O cliente pergunta o modo de ligação ao arrancar:

```
Select Connection Mode:
1. TCP (Remote)
2. Direct Database (Local)
Choice:
```

Para acesso remoto (US044), selecionar a opção **1**.

---

## Fluxo de Autenticação

1. O cliente conecta-se ao servidor via TCP na porta 9000.
2. O cliente envia `AUTH` com `email\0password` (null-byte como separador).
3. O servidor verifica as credenciais através do `AuthzRegistry.authenticationService()`.
4. O servidor verifica se o utilizador tem o papel **WEATHER_PERSON**.
5. Se autenticado:
   - Responde `ACK("Authenticated")`.
   - Regista `SUCCESSFUL_LOGIN` no servidor de logging via UDP.
6. Se falhar:
   - Responde `ERR(...)`.
   - Regista `FAILED_LOGIN` no servidor de logging via UDP.

---

## Formato do CSV para Bulk Import

O ficheiro CSV deve ter 12 campos separados por ponto e vírgula:

```
ACA;Lat1;Long1;Lat2;Long2;Alt_inf;Alt_sup;Direction;WindSpeed;Date;Start_hour;End_hour
```

**Exemplo:**

```
121;43.84;-9.79;40.22;-7.95;0;1000;90;28.75;2026-06-22;05:00;08:15
SCPA;38.5;-9.0;39.0;-8.5;0;10000;180;15.5;2024-01-15;08:00;18:00
```

Ficheiros de exemplo estão disponíveis em `aisafe/aisafe.app.weather.client/docs/`.

---

## Logging Remoto

Cada sessão TCP envia eventos UDP ao `RemoteAccessesLoggingServer` (porta 9999):

| Evento | Quando |
|---|---|
| `SUCCESSFUL_LOGIN` | Autenticação bem-sucedida como Weather Person |
| `FAILED_LOGIN` | Credenciais erradas ou papel insuficiente |
| `LOGOUT` | Weather Person enviou `DISCONN` explicitamente |
| `DISCONNECT` | Ligação TCP fechada inesperadamente |

O serviço associado é sempre **`US044`**.

---

## Configuração

| Propriedade Java | Padrão | Descrição |
|---|---|---|
| `aisafe.weather.host` | `localhost` | Host do servidor (lado cliente) |
| `aisafe.weather.port` | `9000` | Porta TCP |
| `aisafe.logging.host` | `localhost` | Host do servidor de logging |
| `aisafe.logging.port` | `9999` | Porta UDP do servidor de logging |

---

## Restrições de Segurança

- **Nenhum acesso direto à base de dados** é feito pelo cliente no modo TCP. Todo o acesso a dados passa pelo servidor TCP.
- **Apenas utilizadores com o papel `WEATHER_PERSON`** conseguem autenticar. Outros papéis recebem erro de autorização.
- **Cada ligação corre num thread separado**, garantindo isolamento entre sessões.
- **Operações de upload e listagem** são bloqueadas se a sessão não estiver autenticada.

---

## Estrutura de Ficheiros

```
aisafe.app.weather.client/
├── pom.xml
├── docs/
│   ├── CW_WeatherData_v0.csv        ← dados de exemplo (formato CW)
│   ├── HP_WeatherData_v0.csv        ← dados de exemplo (formato HP)
│   └── convert_xlsx_to_csv.ps1      ← script de conversão Excel→CSV
└── src/main/java/
    └── weather/
        ├── WeatherClientApp.java               ← ponto de entrada do cliente
        ├── ClientSession.java                  ← singleton da sessão e modo de conexão
        ├── server/
        │   ├── MessageCode.java                ← códigos do protocolo
        │   ├── ProtocolMessage.java
        │   ├── ProtocolCodec.java              ← serialização/deserialização
        │   ├── WeatherTcpClient.java           ← stub TCP usado pela UI
        │   ├── WeatherTcpServer.java           ← servidor TCP (porta 9000)
        │   └── AttendClient.java               ← handler por ligação
        └── presentation/
            ├── WeatherMenu.java
            ├── authz/
            │   └── WeatherLoginUI.java
            └── weatherData/
                ├── RegisterWeatherDataUI.java
                ├── ViewWeatherDataUI.java
                └── BulkImportWeatherUI.java
```
