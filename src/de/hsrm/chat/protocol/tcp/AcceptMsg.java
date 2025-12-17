package de.hsrm.chat.protocol.tcp;

public final class AcceptMsg implements TcpMessage {
    public static final String TYPE = "ACCEPT";
    public final String inviterUser;

    public AcceptMsg(String inviterUser) {
        this.inviterUser = inviterUser;
    }

    @Override public String type() { return TYPE; }

    @Override
    public String encode() {
        return TcpCodec.join(TYPE, inviterUser);
    }

    static AcceptMsg decode(String rest) {
        return new AcceptMsg(rest.trim());
    }
}