package de.hsrm.chat.protocol.udp;

public sealed interface UdpMsg {
    record Chat(String from, String text) implements UdpMsg {}
    record Bye(String from) implements UdpMsg {}
    record Unknown(String raw) implements UdpMsg {}
}
