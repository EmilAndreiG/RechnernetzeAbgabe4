package de.hsrm.chat.client;

import de.hsrm.chat.protocol.udp.UdpChatCodec;
import de.hsrm.chat.protocol.udp.UdpMsg;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public final class UdpChatSession {
    private final int localPort;

    private DatagramSocket socket;
    private InetAddress peerIp;
    private int peerPort;
    private String myUser = "me";
    private String peerUser = "?";

    private Thread recvThread;

    public UdpChatSession(int localPort) {
        this.localPort = localPort;
    }

    public void start(String peerUser, String peerIp, int peerPort) throws Exception {
        this.peerUser = peerUser;
        this.peerIp = InetAddress.getByName(peerIp);
        this.peerPort = peerPort;

        // UDP-Socket an lokalen Port binden, damit der Peer dich erreichen kann
        if (socket == null || socket.isClosed()) {
            socket = new DatagramSocket(localPort);
        }

        // Receiver-Thread
        recvThread = new Thread(() -> {
            byte[] buf = new byte[2048];
            while (!socket.isClosed()) {
                try {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    socket.receive(p);

                    String s = new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8);
                    UdpMsg m = UdpChatCodec.decode(s);

                    if (m instanceof UdpMsg.Chat) {
                        UdpMsg.Chat c = (UdpMsg.Chat) m;
                        System.out.println("\n[" + c.from() + "] " + c.text());
                        System.out.print("> ");
                    } else if (m instanceof UdpMsg.Bye) {
                        System.out.println("\n[CHAT] " + ((UdpMsg.Bye) m).from() + " hat den Chat verlassen.");
                        System.out.print("> ");
                        break;
                    }
                } catch (Exception e) {
                    break;
                }
            }
        });
        recvThread.setDaemon(true);
        recvThread.start();
    }

    public void setMyUser(String myUser) {
        this.myUser = myUser;
    }

    public void sendText(String text) throws Exception {
        if (socket == null || peerIp == null) return;
        String msg = UdpChatCodec.encodeChat(myUser, text);
        sendRaw(msg);
    }

    public void sendBye() throws Exception {
        if (socket == null || peerIp == null) return;
        String msg = UdpChatCodec.encodeBye(myUser);
        sendRaw(msg);
    }

    private void sendRaw(String s) throws Exception {
        byte[] data = s.getBytes(StandardCharsets.UTF_8);
        DatagramPacket p = new DatagramPacket(data, data.length, peerIp, peerPort);
        socket.send(p);
    }

    public void stop() {
        if (socket != null) socket.close();
        socket = null;
        peerIp = null;
        peerPort = 0;
    }
}
