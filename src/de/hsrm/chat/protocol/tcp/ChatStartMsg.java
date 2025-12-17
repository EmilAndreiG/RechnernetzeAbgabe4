package de.hsrm.chat.protocol.tcp;

public final class ChatStartMsg implements TcpMessage {
    public static final String TYPE = "CHAT_START";
    public final String peerUser;
    public final String peerIp;
    public final int peerUdpPort;

    public ChatStartMsg(String peerUser, String peerIp, int peerUdpPort) {
        this.peerUser = peerUser;
        this.peerIp = peerIp;
        this.peerUdpPort = peerUdpPort;
    }

    @Override public String type() { return TYPE; }

    @Override
    public String encode() {
        return TcpCodec.join(TYPE, peerUser, peerIp, String.valueOf(peerUdpPort));
    }

    static ChatStartMsg decode(String rest) {
        String[] p = rest.split("\\s+");
        if (p.length != 3) return new ChatStartMsg("?", "0.0.0.0", -1);
        return new ChatStartMsg(p[0], p[1], Integer.parseInt(p[2]));
    }
}
