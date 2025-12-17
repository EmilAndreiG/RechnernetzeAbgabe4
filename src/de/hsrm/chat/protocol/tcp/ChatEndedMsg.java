package de.hsrm.chat.protocol.tcp;

public final class ChatEndedMsg implements TcpMessage {
    public static final String TYPE = "CHAT_ENDED";
    @Override public String type() { return TYPE; }
    @Override public String encode() { return TYPE; }
}
