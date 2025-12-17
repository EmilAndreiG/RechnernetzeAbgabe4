package de.hsrm.chat.protocol.tcp;

public final class InviteMsg implements TcpMessage {
    public static final String TYPE = "INVITE";
    public final String targetUser;

    public InviteMsg(String targetUser) {
        this.targetUser = targetUser;
    }

    @Override public String type() { return TYPE; }

    @Override
    public String encode() {
        return TcpCodec.join(TYPE, targetUser);
    }

    static InviteMsg decode(String rest) {
        return new InviteMsg(rest.trim());
    }
}
