package de.hsrm.chat.server;

public final class ChatServerMain {
    public static void main(String[] args) throws Exception {
        int port = (args.length >= 1) ? Integer.parseInt(args[0]) : 5000;
        new ChatServer(port).runForever();
    }
}
