package de.hsrm.chat.protocol.tcp;

public final class LogoutMsg implements TcpMessage {
    public static final String TYPE = "LOGOUT";
    @Override public String type() { return TYPE; }
    @Override public String encode() { return TYPE; }
}
