package de.hsrm.chat.protocol.tcp;

public final class LoginMsg implements TcpMessage {
    public static final String TYPE = "LOGIN";
    public final String username;
    public final String password;
    public final int udpPort;

    public LoginMsg(String username, String password, int udpPort) {
        this.username = username;
        this.password = password;
        this.udpPort = udpPort;
    }

    @Override public String type() { return TYPE; }

    @Override
    public String encode() {
        return TcpCodec.join(TYPE, username, password, String.valueOf(udpPort));
    }

    static LoginMsg decode(String rest) {
        String[] p = rest.split("\\s+");
        if (p.length != 3) return new LoginMsg("?", "?", -1);
        return new LoginMsg(p[0], p[1], Integer.parseInt(p[2]));
    }
}
