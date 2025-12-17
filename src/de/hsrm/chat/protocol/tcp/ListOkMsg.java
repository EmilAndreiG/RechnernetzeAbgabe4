package de.hsrm.chat.protocol.tcp;

public final class ListOkMsg implements TcpMessage {
    public static final String TYPE = "LIST_OK";
    public final String csvUsers; // "a,b,c"

    public ListOkMsg(String csvUsers) {
        this.csvUsers = (csvUsers == null) ? "" : csvUsers;
    }

    @Override public String type() { return TYPE; }

    @Override
    public String encode() {
        return TcpCodec.join(TYPE, csvUsers);
    }

    static ListOkMsg decode(String rest) {
        return new ListOkMsg(rest.trim());
    }
}
