package de.hsrm.chat.protocol.tcp;

public final class ErrorMsg implements TcpMessage {
    public static final String TYPE = "ERROR";
    public final String reason;

    public ErrorMsg(String reason) {
        this.reason = (reason == null) ? "UNKNOWN" : reason;
    }

    @Override public String type() { return TYPE; }

    @Override
    public String encode() {
        return TcpCodec.join(TYPE, reason);
    }

    static ErrorMsg decode(String rest) {
        return new ErrorMsg(rest.isBlank() ? "UNKNOWN" : rest);
    }
}
