# aisafe.app.weather.client

**Weather Data TCP Client** — for Weather Persons / Providers.

## Purpose

This module is a **standalone utility application** that connects to the AISafe server over **TCP/IP** to upload atmospheric weather data affecting air control areas:

### Weather Person / Provider (US041–US044)
- Register individual weather data entries (US041)
- **Bulk import** weather data from files via TCP (US042–US044)
- Specify wind speed, temperature, and other atmospheric conditions per air control area

## How it works

1. The user launches this client app with connection parameters (server host/port)
2. The client establishes a TCP connection to the server running in `aisafe.app.aircontrol.console`
3. Weather data (single entries or bulk files) is serialized and sent over the socket
4. The server persists the data and makes it available for the simulation engine

## Structure

```
tcp/
└── WeatherTcpClient.java   ← manages socket connection + protocol
presentation/
└── BulkImportWeatherUI.java
WeatherClientApp.java       ← main() entry point
```

## Dependencies

- `aisafe.core` — domain model (WeatherData value objects)
- Java standard library (`java.net.Socket`)
