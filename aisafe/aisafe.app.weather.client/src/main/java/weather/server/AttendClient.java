package weather.server;

import eapli.framework.infrastructure.authz.application.AuthzRegistry;
import eapli.framework.infrastructure.authz.application.UserSession;
import eapli.framework.infrastructure.authz.domain.model.SystemUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import weatherData.application.RegisterWeatherDataController;
import weatherData.dto.WeatherDataDTO;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Optional;

public class AttendClient implements Runnable {

    private final Socket cliSock;
    private SystemUser authenticatedUser;   // sessão da ligação
    private final ObjectMapper objectMapper;

    public AttendClient(Socket s) {
        this.cliSock = s;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void run() {
        try (cliSock;
             DataInputStream  sIn  = new DataInputStream(cliSock.getInputStream());
             DataOutputStream sOut = new DataOutputStream(cliSock.getOutputStream())) {

            System.out.println("Client connected from "
                    + cliSock.getInetAddress().getHostAddress() + ":" + cliSock.getPort());

            boolean running = true;
            while (running) {
                ProtocolMessage msg = ProtocolCodec.read(sIn);
                switch (msg.code()) {
                    case COMMTEST       -> ProtocolCodec.write(sOut, ack("OK"));
                    case AUTH           -> handleAuth(msg, sOut);
                    case UPLOAD_WEATHER -> handleUpload(msg, sOut);
                    case DISCONN        -> { ProtocolCodec.write(sOut, ack("BYE")); running = false; }
                    default             -> ProtocolCodec.write(sOut, err("Unsupported"));
                }
            }
        } catch (IOException e) {
            System.err.println("Connection ended: " + e.getMessage());
        }
    }

    private void handleAuth(ProtocolMessage msg, DataOutputStream sOut) throws IOException {
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
            ProtocolCodec.write(sOut, ack("Authenticated"));
        } else {
            ProtocolCodec.write(sOut, err("Invalid credentials"));
        }
    }

    private void handleUpload(ProtocolMessage msg, DataOutputStream sOut) throws IOException {
        if (authenticatedUser == null) {
            ProtocolCodec.write(sOut, err("Not authenticated"));
            return;
        }
        try {
            String json = msg.dataAsString();
            WeatherDataDTO dto = objectMapper.readValue(json, WeatherDataDTO.class);
            
            RegisterWeatherDataController controller = new RegisterWeatherDataController();
            controller.registerWeatherData(dto);

            System.out.println("Received and saved weather data from " + authenticatedUser.username());
            ProtocolCodec.write(sOut, ack("Data registered successfully"));
        } catch (Exception e) {
            System.err.println("Error processing upload: " + e.getMessage());
            ProtocolCodec.write(sOut, err("Error processing weather data: " + e.getMessage()));
        }
    }

    private ProtocolMessage ack(String text) {
        return new ProtocolMessage(MessageCode.ACK, text);
    }

    private ProtocolMessage err(String text) {
        return new ProtocolMessage(MessageCode.ERR, text);
    }
}