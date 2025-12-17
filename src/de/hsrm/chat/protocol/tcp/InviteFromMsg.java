package de.hsrm.chat.protocol.tcp;

public final class InviteFromMsg implements TcpMessage {
    public static final String TYPE = "INVITE_FROM";
    public final String inviterUser;

    public InviteFromMsg(String inviterUser) {
        this.inviterUser = inviterUser;
    }

    @Override public String type() { return TYPE; }

    @Override
    public String encode() {
        return TcpCodec.join(TYPE, inviterUser);
    }

    static InviteFromMsg decode(String rest) {
        return new InviteFromMsg(rest.trim());
    }
}
