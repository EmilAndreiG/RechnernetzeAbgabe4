package de.hsrm.chat.protocol.udp;

public final class UdpChatCodec {
    private UdpChatCodec() {}

    // Format:
    // CHAT <fromUser> <text...>
    // BYE  <fromUser>
    public static String encodeChat(String from, String text) {
        return "CHAT " + from + " " + text;
    }

    public static String encodeBye(String from) {
        return "BYE " + from;
    }

    public static UdpMsg decode(String line) {
        if (line == null) return null;
        line = line.trim();
        if (line.isEmpty()) return null;

        String[] p = line.split(" ", 3);
        String t = p[0].toUpperCase();

        if ("CHAT".equals(t) && p.length >= 3) {
            return new UdpMsg.Chat(p[1], p[2]);
        }
        if ("BYE".equals(t) && p.length >= 2) {
            return new UdpMsg.Bye(p[1]);
        }
        return new UdpMsg.Unknown(line);
    }
}
