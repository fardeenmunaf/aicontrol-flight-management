package weather.tcp;

import java.io.*;
import java.net.Socket;

public class WeatherTcpClient {

    private static final String SERVER_HOST = "localhost"; // ip do
    private static final int SERVER_PORT = 12345; // porta do servidor

    public String sendWeatherData(String payload) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(payload);
            return in.readLine(); // waits

        } catch (IOException e) {
            return "ERROR: " + e.getMessage();
        }
    }
}