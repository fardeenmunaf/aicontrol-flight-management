package weather.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

public class ProtocolCodec {

    public static void write(DataOutputStream out, ProtocolMessage msg) throws IOException {
        byte[] data = msg.data();
        out.write(ProtocolMessage.VERSION);              // 1 byte
        out.write(msg.code().code());                    // 1 byte
        out.write(data.length & 0xFF);                   // 2 bytes little-endian
        out.write((data.length >> 8) & 0xFF);
        out.write(data, 0, data.length);                 // slide 17 write(b, off, len)
        out.flush();
    }

    public static ProtocolMessage read(DataInputStream in) throws IOException {
        int version = in.read();                          // slide 17 read()
        if (version == -1) throw new EOFException("Connection closed");
        int code    = in.read();
        int lenLow  = in.read();
        int lenHigh = in.read();
        int len = (lenHigh << 8) | lenLow;

        byte[] data = new byte[len];
        // IMPORTANTE: read pode devolver menos bytes que pedidos (slide 14!)
        // por isso usa readFully ou ciclo até ter os bytes todos
        int total = 0;
        while (total < len) {
            int r = in.read(data, total, len - total);    // slide 17 read(b, off, len)
            if (r == -1) throw new EOFException("Truncated payload");
            total += r;
        }
        return new ProtocolMessage(MessageCode.from((byte) code), data);
    }
}