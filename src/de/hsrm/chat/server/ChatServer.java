package de.hsrm.chat.server;

import de.hsrm.chat.protocol.tcp.*;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.*;

public final class ChatServer {
    private final int port;
    private final ExecutorService pool = Executors.newCachedThreadPool();

    // "Datenbank" im Speicher: username -> password
    private final ConcurrentHashMap<String, String> users = new ConcurrentHashMap<>();

    // aktive Sessions: username -> Session
    private final ConcurrentHashMap<String, Session> active = new ConcurrentHashMap<>();

    // pending invites: invitee -> inviter
    private final ConcurrentHashMap<String, String> pendingInvite = new ConcurrentHashMap<>();

    public ChatServer(int port) {
        this.port = port;
    }

    public void runForever() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[SERVER] TCP listening on port " + port);

            while (true) {
                Socket s = serverSocket.accept();
                pool.submit(() -> handleClient(s));
            }
        }
    }

    private void handleClient(Socket socket) {
        String loggedInUser = null;

        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            outLine(out, new OkMsg("CONNECTED"));

            String line;
            while ((line = in.readLine()) != null) {
                TcpMessage msg = TcpCodec.decode(line);

                // Falls Client komische Sachen schickt
                if (msg == null) {
                    outLine(out, new ErrorMsg("EMPTY"));
                    continue;
                }

                // Routing nach Typ
                if (msg instanceof RegisterMsg) {
                    RegisterMsg m = (RegisterMsg) msg;
                    // Minimal check
                    if (m.udpPort < 1 || m.udpPort > 65535) {
                        outLine(out, new ErrorMsg("BAD_UDP_PORT"));
                        continue;
                    }
                    if (users.putIfAbsent(m.username, m.password) == null) {
                        outLine(out, new OkMsg("REGISTER_OK"));
                    } else {
                        outLine(out, new ErrorMsg("USER_EXISTS"));
                    }
                }
                else if (msg instanceof LoginMsg) {
                    LoginMsg m = (LoginMsg) msg;
                    if (m.udpPort < 1 || m.udpPort > 65535) {
                        outLine(out, new ErrorMsg("BAD_UDP_PORT"));
                        continue;
                    }

                    String pw = users.get(m.username);
                    if (pw == null || !pw.equals(m.password)) {
                        outLine(out, new ErrorMsg("LOGIN_BAD_CREDENTIALS"));
                        continue;
                    }

                    // Nur ein Login pro User
                    if (active.containsKey(m.username)) {
                        outLine(out, new ErrorMsg("ALREADY_LOGGED_IN"));
                        continue;
                    }

                    Session sess = new Session(m.username, socket.getInetAddress(), m.udpPort, out);
                    active.put(m.username, sess);
                    loggedInUser = m.username;

                    outLine(out, new OkMsg("LOGIN_OK"));
                }
                else if (msg instanceof ListMsg) {
                    if (loggedInUser == null) {
                        outLine(out, new ErrorMsg("NOT_LOGGED_IN"));
                        continue;
                    }
                    // Einfach: alle aktiven User als CSV
                    String csv = String.join(",", active.keySet());
                    outLine(out, new ListOkMsg(csv));
                }
                else if (msg instanceof InviteMsg) {
                    if (loggedInUser == null) {
                        outLine(out, new ErrorMsg("NOT_LOGGED_IN"));
                        continue;
                    }
                    InviteMsg m = (InviteMsg) msg;

                    synchronized (this) {
                        Session inviter = active.get(loggedInUser);
                        Session invitee = active.get(m.targetUser);

                        if (invitee == null) {
                            outLine(out, new ErrorMsg("TARGET_NOT_ACTIVE"));
                            continue;
                        }
                        if (inviter.inChat || invitee.inChat) {
                            outLine(out, new ErrorMsg("SOMEONE_IN_CHAT"));
                            continue;
                        }
                        if (pendingInvite.containsKey(m.targetUser)) {
                            outLine(out, new ErrorMsg("TARGET_HAS_PENDING_INVITE"));
                            continue;
                        }

                        pendingInvite.put(m.targetUser, loggedInUser);
                        // Invitee bekommt asynchron eine Nachricht
                        invitee.send(new InviteFromMsg(loggedInUser));
                        outLine(out, new OkMsg("INVITE_SENT"));
                    }
                }
                else if (msg instanceof AcceptMsg) {
                    if (loggedInUser == null) {
                        outLine(out, new ErrorMsg("NOT_LOGGED_IN"));
                        continue;
                    }
                    AcceptMsg m = (AcceptMsg) msg;

                    synchronized (this) {
                        String expectedInviter = pendingInvite.get(loggedInUser);
                        if (expectedInviter == null || !expectedInviter.equals(m.inviterUser)) {
                            outLine(out, new ErrorMsg("NO_SUCH_INVITE"));
                            continue;
                        }

                        Session inviter = active.get(m.inviterUser);
                        Session invitee = active.get(loggedInUser);

                        if (inviter == null || invitee == null) {
                            pendingInvite.remove(loggedInUser);
                            outLine(out, new ErrorMsg("INVITER_NOT_ACTIVE"));
                            continue;
                        }
                        if (inviter.inChat || invitee.inChat) {
                            pendingInvite.remove(loggedInUser);
                            outLine(out, new ErrorMsg("SOMEONE_IN_CHAT"));
                            continue;
                        }

                        // Chat starten: beide bekommen Peer-IP + Peer-UDP-Port
                        inviter.inChat = true;
                        invitee.inChat = true;
                        inviter.chatPeer = invitee.username;
                        invitee.chatPeer = inviter.username;

                        pendingInvite.remove(loggedInUser);

                        inviter.send(new ChatStartMsg(invitee.username, invitee.ip.getHostAddress(), invitee.udpPort));
                        invitee.send(new ChatStartMsg(inviter.username, inviter.ip.getHostAddress(), inviter.udpPort));

                        outLine(out, new OkMsg("ACCEPT_OK"));
                    }
                }
                else if (msg instanceof DeclineMsg) {
                    if (loggedInUser == null) {
                        outLine(out, new ErrorMsg("NOT_LOGGED_IN"));
                        continue;
                    }
                    DeclineMsg m = (DeclineMsg) msg;

                    synchronized (this) {
                        String expectedInviter = pendingInvite.get(loggedInUser);
                        if (expectedInviter == null || !expectedInviter.equals(m.inviterUser)) {
                            outLine(out, new ErrorMsg("NO_SUCH_INVITE"));
                            continue;
                        }
                        pendingInvite.remove(loggedInUser);

                        Session inviter = active.get(m.inviterUser);
                        if (inviter != null) inviter.send(new OkMsg("INVITE_DECLINED_BY " + loggedInUser));

                        outLine(out, new OkMsg("DECLINE_OK"));
                    }
                }
                else if (msg instanceof LogoutMsg) {
                    if (loggedInUser != null) {
                        cleanupUser(loggedInUser);
                        loggedInUser = null;
                    }
                    outLine(out, new OkMsg("LOGOUT_OK"));
                }
                else {
                    outLine(out, new ErrorMsg("UNSUPPORTED " + msg.type()));
                }
            }

        } catch (Exception e) {
            // Verbindung weg, normal bei Client-Ende
        } finally {
            if (loggedInUser != null) cleanupUser(loggedInUser);
        }
    }

    private void cleanupUser(String username) {
        synchronized (this) {
            Session s = active.remove(username);
            pendingInvite.remove(username);

            // Wenn jemand im Chat war: Peer wieder freigeben
            if (s != null && s.inChat && s.chatPeer != null) {
                Session peer = active.get(s.chatPeer);
                if (peer != null) {
                    peer.inChat = false;
                    peer.chatPeer = null;
                    peer.send(new ChatEndedMsg());
                }
            }
        }
    }

    private static void outLine(BufferedWriter out, TcpMessage msg) throws IOException {
        out.write(msg.encode());
        out.write("\n");
        out.flush();
    }

    // Session hält OutputStreamWriter, damit Server dem Client jederzeit Nachrichten senden kann (INVITE_FROM, CHAT_START)
    static final class Session {
        final String username;
        final InetAddress ip;
        final int udpPort;
        final BufferedWriter out;

        volatile boolean inChat = false;
        volatile String chatPeer = null;

        Session(String username, InetAddress ip, int udpPort, BufferedWriter out) {
            this.username = username;
            this.ip = ip;
            this.udpPort = udpPort;
            this.out = out;
        }

        void send(TcpMessage msg) {
            try {
                synchronized (out) {
                    out.write(msg.encode());
                    out.write("\n");
                    out.flush();
                }
            } catch (IOException ignored) {
            }
        }
    }
}
