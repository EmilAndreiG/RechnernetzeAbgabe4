package de.hsrm.chat.protocol.tcp;

import java.util.StringJoiner;

public final class TcpCodec {
    private TcpCodec() {}

    public static TcpMessage decode(String line) {
        if (line == null) return null;
        line = line.trim();
        if (line.isEmpty()) return null;

        String[] parts = line.split(" ", 2); // 2 => Rest behalten (z.B. ERROR Text)
        String t = parts[0].toUpperCase();
        String rest = (parts.length == 2) ? parts[1].trim() : "";

        switch (t) {
            case RegisterMsg.TYPE: return RegisterMsg.decode(rest);
            case LoginMsg.TYPE:    return LoginMsg.decode(rest);
            case ListMsg.TYPE:     return new ListMsg();
            case InviteMsg.TYPE:   return InviteMsg.decode(rest);
            case AcceptMsg.TYPE:   return AcceptMsg.decode(rest);
            case DeclineMsg.TYPE:  return DeclineMsg.decode(rest);
            case LogoutMsg.TYPE:   return new LogoutMsg();

            // Server->Client
            case OkMsg.TYPE:       return OkMsg.decode(rest);
            case ErrorMsg.TYPE:    return ErrorMsg.decode(rest);
            case ListOkMsg.TYPE:   return ListOkMsg.decode(rest);
            case InviteFromMsg.TYPE:return InviteFromMsg.decode(rest);
            case ChatStartMsg.TYPE:return ChatStartMsg.decode(rest);
            case ChatEndedMsg.TYPE:return new ChatEndedMsg();

            default: return new ErrorMsg("UNKNOWN_MESSAGE " + line);
        }
    }

    static String join(String... parts) {
        StringJoiner sj = new StringJoiner(" ");
        for (String p : parts) sj.add(p);
        return sj.toString();
    }
}
