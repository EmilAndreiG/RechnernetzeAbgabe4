package de.hsrm.chat.protocol.tcp;

public final class DeclineMsg implements TcpMessage {
    public static final String TYPE = "DECLINE";
    public final String inviterUser;

    public DeclineMsg(String inviterUser) {
        this.inviterUser = inviterUser;
    }

    @Override public String type() { return TYPE; }

    @Override
    public String encode() {
        return TcpCodec.join(TYPE, inviterUser);
    }

    static DeclineMsg decode(String rest) {
        return new DeclineMsg(rest.trim());
    }
}
