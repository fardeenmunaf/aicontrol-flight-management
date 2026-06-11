package weather.server;

import auth.domain.AISafeRoles;
import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserSession;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import remotelogging.domain.RemoteAccessEventType;
import remotelogging.domain.RemoteService;
import remotelogging.udp.UdpLogSender;
import weatherData.application.ListWeatherDataController;
import weatherData.application.RegisterWeatherDataController;
import weatherData.dto.WeatherDataDTO;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.StringJoiner;

public class AttendClient implements Runnable {
    private final Socket cliSock;
    private SystemUser authenticatedUser;   // sessão da ligação

    public AttendClient(Socket s) {
        this.cliSock = s;
    }

    @Override
    public void run() {
        String clientIp = cliSock.getInetAddress().getHostAddress();
        int clientPort = cliSock.getPort();
        try (cliSock;
             DataInputStream  sIn  = new DataInputStream(cliSock.getInputStream());
             DataOutputStream sOut = new DataOutputStream(cliSock.getOutputStream())) {

            System.out.println("Client connected from " + clientIp + ":" + clientPort);

            boolean running = true;
            while (running) {
                ProtocolMessage msg = ProtocolCodec.read(sIn);
                switch (msg.code()) {
                    case COMMTEST       -> ProtocolCodec.write(sOut, ack("OK"));
                    case AUTH           -> handleAuth(msg, sOut, clientIp, clientPort);
                    case UPLOAD_WEATHER -> handleUpload(msg, sOut);
                    case LIST_WEATHER   -> handleList(msg, sOut);
                    case DISCONN        -> {
                        ProtocolCodec.write(sOut, ack("BYE"));
                        running = false;
                        if (authenticatedUser != null) {
                            UdpLogSender.instance().log(authenticatedUser.username().toString(), clientIp, clientPort, RemoteService.US044, RemoteAccessEventType.LOGOUT);
                        }
                    }
                    default             -> ProtocolCodec.write(sOut, err("Unsupported"));
                }
            }
        } catch (IOException e) {
            System.err.println("Connection ended: " + e.getMessage());
            if (authenticatedUser != null) {
                UdpLogSender.instance().log(authenticatedUser.username().toString(), clientIp, clientPort, RemoteService.US044, RemoteAccessEventType.DISCONNECT);
            }
        }
    }

    private void handleAuth(ProtocolMessage msg, DataOutputStream sOut, String ip, int port) throws IOException {
        String data = msg.dataAsString();
        String[] parts = data.split("\0");
        if (parts.length != 2) {
            ProtocolCodec.write(sOut, err("Invalid auth format"));
            return;
        }
        String user = parts[0];
        String pass = parts[1];

        Optional<UserSession> session = AuthzRegistry.authenticationService().authenticate(user, pass);
        if (session.isPresent()) {
            this.authenticatedUser = session.get().authenticatedUser();
            // Validar se tem o role de Weather Person (AC4)
            if (this.authenticatedUser.roleTypes().contains(AISafeRoles.WEATHER_PERSON)) {
                UdpLogSender.instance().log(user, ip, port, RemoteService.US044, RemoteAccessEventType.SUCCESSFUL_LOGIN);
                ProtocolCodec.write(sOut, ack("Authenticated"));
            } else {
                UdpLogSender.instance().log(user, ip, port, RemoteService.US044, RemoteAccessEventType.FAILED_LOGIN);
                this.authenticatedUser = null;
                ProtocolCodec.write(sOut, err("Not authorized: Weather Person role required"));
            }
        } else {
            UdpLogSender.instance().log(user, ip, port, RemoteService.US044, RemoteAccessEventType.FAILED_LOGIN);
            ProtocolCodec.write(sOut, err("Invalid credentials"));
        }
    }

    private void handleUpload(ProtocolMessage msg, DataOutputStream sOut) throws IOException {
        if (authenticatedUser == null) {
            ProtocolCodec.write(sOut, err("Not authenticated"));
            return;
        }
        try {
            String payload = msg.dataAsString();
            RegisterWeatherDataController controller = new RegisterWeatherDataController();
            
            // Se o payload contiver '\n', é um bulk import (múltiplas linhas)
            if (payload.contains("\n")) {
                String[] lines = payload.split("\n");
                int successCount = 0;
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    try {
                        WeatherDataDTO dto = deserializeWeatherDataDTO(line);
                        controller.registerWeatherData(dto);
                        successCount++;
                    } catch (Exception ex) {
                        System.err.println("Error in bulk item: " + ex.getMessage());
                    }
                }
                ProtocolCodec.write(sOut, ack("Bulk import completed. Imported: " + successCount + "/" + lines.length));
            } else {
                WeatherDataDTO dto = deserializeWeatherDataDTO(payload);
                controller.registerWeatherData(dto);
                ProtocolCodec.write(sOut, ack("Data registered successfully"));
            }
            System.out.println("Processed weather data from " + authenticatedUser.username());
        } catch (Exception e) {
            System.err.println("Error processing upload: " + e.getMessage());
            ProtocolCodec.write(sOut, err("Error processing weather data: " + e.getMessage()));
        }
    }

    private void handleList(ProtocolMessage msg, DataOutputStream sOut) throws IOException {
        if (authenticatedUser == null) {
            ProtocolCodec.write(sOut, err("Not authenticated"));
            return;
        }
        try {
            // Payload: "area;YYYY-MM-DD" ou "area;" ou ";YYYY-MM-DD" ou ";"
            String payload = msg.dataAsString();
            String[] parts = payload.split(";", -1);
            String area = parts.length > 0 && !parts[0].isEmpty() ? parts[0] : null;
            LocalDate date = parts.length > 1 && !parts[1].isEmpty() ? LocalDate.parse(parts[1]) : null;

            ListWeatherDataController controller = new ListWeatherDataController();
            Iterable<WeatherDataDTO> results;
            if (date == null) {
                if (area == null) results = controller.listAll();
                else results = controller.listByDateAndArea(area, null);
            } else {
                results = controller.listByDateAndArea(area, date);
            }

            StringJoiner response = new StringJoiner("\n");
            for (WeatherDataDTO dto : results) {
                response.add(serializeWeatherDataDTO(dto));
            }
            ProtocolCodec.write(sOut, ack(response.toString()));
        } catch (Exception e) {
            ProtocolCodec.write(sOut, err("Error listing weather data: " + e.getMessage()));
        }
    }

    private String serializeWeatherDataDTO(WeatherDataDTO dto) {
        return new StringJoiner(";")
                .add(dto.acaCode())
                .add(String.valueOf(dto.latitude1()))
                .add(String.valueOf(dto.longitude1()))
                .add(String.valueOf(dto.latitude2()))
                .add(String.valueOf(dto.longitude2()))
                .add(String.valueOf(dto.altitudeInf()))
                .add(String.valueOf(dto.altitudeSup()))
                .add(String.valueOf(dto.direction()))
                .add(String.valueOf(dto.windSpeed()))
                .add(dto.date().toString())
                .add(dto.startHour().toString())
                .add(dto.endHour().toString())
                .toString();
    }

    private WeatherDataDTO deserializeWeatherDataDTO(String line) {
        String[] p = line.split(";", -1);
        return new WeatherDataDTO(
                p[0],
                Double.parseDouble(p[1]),
                Double.parseDouble(p[2]),
                Double.parseDouble(p[3]),
                Double.parseDouble(p[4]),
                Double.parseDouble(p[5]),
                Double.parseDouble(p[6]),
                Double.parseDouble(p[7]),
                Double.parseDouble(p[8]),
                LocalDate.parse(p[9]),
                LocalTime.parse(p[10]),
                LocalTime.parse(p[11])
        );
    }

    private ProtocolMessage ack(String text) {
        return new ProtocolMessage(MessageCode.ACK, text);
    }

    private ProtocolMessage err(String text) {
        return new ProtocolMessage(MessageCode.ERR, text);
    }
}