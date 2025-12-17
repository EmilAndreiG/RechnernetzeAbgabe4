package de.hsrm.chat.protocol.tcp;

public final class ListMsg implements TcpMessage {
    public static final String TYPE = "LIST";
    @Override public String type() { return TYPE; }
    @Override public String encode() { return TYPE; }
}
