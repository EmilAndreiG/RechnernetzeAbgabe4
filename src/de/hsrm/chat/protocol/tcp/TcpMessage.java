package de.hsrm.chat.protocol.tcp;

public interface TcpMessage {
    String type();
    String encode(); // eine Zeile, endet beim Senden mit \n
}
