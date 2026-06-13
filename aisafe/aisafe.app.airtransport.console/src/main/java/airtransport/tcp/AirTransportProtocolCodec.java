package airtransport.tcp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

public final class AirTransportProtocolCodec {

    public static void write(final DataOutputStream out, final AirTransportProtocolMessage msg) throws IOException {
        final byte[] data = msg.data();
        out.write(AirTransportProtocolMessage.VERSION);
        out.write(msg.code().code());
        out.write(data.length & 0xFF);
        out.write((data.length >> 8) & 0xFF);
        out.write(data, 0, data.length);
        out.flush();
    }

    public static AirTransportProtocolMessage read(final DataInputStream in) throws IOException {
        final int version = in.read();
        if (version == -1) throw new EOFException("Connection closed");
        final int code    = in.read();
        final int lenLow  = in.read();
        final int lenHigh = in.read();
        final int len     = (lenHigh << 8) | lenLow;

        final byte[] data = new byte[len];
        int total = 0;
        while (total < len) {
            final int r = in.read(data, total, len - total);
            if (r == -1) throw new EOFException("Truncated payload");
            total += r;
        }
        return new AirTransportProtocolMessage(AirTransportMessageCode.from((byte) code), data);
    }

    private AirTransportProtocolCodec() {}
}
