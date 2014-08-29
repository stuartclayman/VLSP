package usr.net;


public class DatagramPacket extends Size4Datagram {
    public DatagramPacket(byte[] payload) {
        super(payload);
    }

    public DatagramPacket(byte[] payload, int length) {
        super(payload);
    }

    public DatagramPacket(byte[] payload, Address addr, int port) {
        super(payload, addr, port);
    }

    public DatagramPacket(byte[] payload, int length, SocketAddress socketAddress) {
        super(payload, length, socketAddress.getAddress(), socketAddress.getPort());
    }


}
