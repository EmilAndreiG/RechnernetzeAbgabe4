package de.hsrm.chat.client;

import de.hsrm.chat.protocol.tcp.*;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ChatClientMain {
    public static void main(String[] args) throws Exception {
        String host = (args.length >= 1) ? args[0] : "127.0.0.1";
        int port = (args.length >= 2) ? Integer.parseInt(args[1]) : 5000;
        int udpPort = (args.length >= 3) ? Integer.parseInt(args[2]) : 6000;

        System.out.println("[CLIENT] connect TCP " + host + ":" + port + " | local UDP port=" + udpPort);

        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            AtomicBoolean inChat = new AtomicBoolean(false);
            UdpChatSession chat = new UdpChatSession(udpPort);

            // Reader-Thread: Server kann INVITE_FROM / CHAT_START jederzeit schicken
            Thread reader = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        TcpMessage m = TcpCodec.decode(line);
                        if (m instanceof InviteFromMsg) {
                            InviteFromMsg im = (InviteFromMsg) m;
                            System.out.println("\n[SERVER] Invite von: " + im.inviterUser
                                    + " | tippe: /accept " + im.inviterUser + " oder /decline " + im.inviterUser);
                        } else if (m instanceof ChatStartMsg) {
                            ChatStartMsg cs = (ChatStartMsg) m;
                            System.out.println("\n[SERVER] Chat gestartet mit " + cs.peerUser
                                    + " (" + cs.peerIp + ":" + cs.peerUdpPort + ")");
                            inChat.set(true);
                            chat.start(cs.peerUser, cs.peerIp, cs.peerUdpPort);
                            System.out.println("[CHAT] tippe Nachricht oder /leave");
                        } else if (m instanceof ChatEndedMsg) {
                            System.out.println("\n[SERVER] Chat beendet.");
                            inChat.set(false);
                            chat.stop();
                        } else if (m != null) {
                            System.out.println("\n[SERVER] " + m.encode());
                        }
                        System.out.print("> ");
                    }
                } catch (Exception ignored) {
                }
            });
            reader.setDaemon(true);
            reader.start();

            System.out.println("Commands:");
            System.out.println("  /register <user> <pw>");
            System.out.println("  /login <user> <pw>");
            System.out.println("  /list");
            System.out.println("  /invite <user>");
            System.out.println("  /accept <user>");
            System.out.println("  /decline <user>");
            System.out.println("  /logout");
            System.out.println("  /quit");
            System.out.println();

            Scanner sc = new Scanner(System.in);
            while (true) {
                System.out.print("> ");
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue;

                // Wenn Chat aktiv ist: normale Texte gehen per UDP raus
                if (inChat.get() && !line.startsWith("/")) {
                    chat.sendText(line);
                    continue;
                }
                if (inChat.get() && line.equalsIgnoreCase("/leave")) {
                    chat.sendBye();
                    chat.stop();
                    inChat.set(false);
                    // optional: Server informieren (hier minimal nicht nötig)
                    continue;
                }

                TcpMessage msg = parseUserCommand(line, udpPort);
                if (msg == null) {
                    System.out.println("Unbekannter Befehl.");
                    continue;
                }

                send(out, msg);

                if (line.equalsIgnoreCase("/quit")) {
                    break;
                }
            }
        }
    }

    private static TcpMessage parseUserCommand(String line, int udpPort) {
        String[] p = line.split("\\s+");
        switch (p[0].toLowerCase()) {
            case "/register":
                if (p.length != 3) return null;
                return new RegisterMsg(p[1], p[2], udpPort);
            case "/login":
                if (p.length != 3) return null;
                return new LoginMsg(p[1], p[2], udpPort);
            case "/list":
                return new ListMsg();
            case "/invite":
                if (p.length != 2) return null;
                return new InviteMsg(p[1]);
            case "/accept":
                if (p.length != 2) return null;
                return new AcceptMsg(p[1]);
            case "/decline":
                if (p.length != 2) return null;
                return new DeclineMsg(p[1]);
            case "/logout":
                return new LogoutMsg();
            case "/quit":
                return new LogoutMsg(); // einfach: logout und Verbindung schließen
            default:
                return null;
        }
    }

    private static void send(BufferedWriter out, TcpMessage msg) throws IOException {
        out.write(msg.encode());
        out.write("\n");
        out.flush();
    }
}
