package de.hsrm.chat.protocol.tcp;

public final class OkMsg implements TcpMessage {
    public static final String TYPE = "OK";
    public final String info; // optionaler Text

    public OkMsg(String info) {
        this.info = (info == null) ? "" : info;
    }

    @Override public String type() { return TYPE; }

    @Override
    public String encode() {
        return info.isBlank() ? TYPE : TcpCodec.join(TYPE, info);
    }

    static OkMsg decode(String rest) {
        return new OkMsg(rest);
    }
}
