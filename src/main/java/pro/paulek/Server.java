package pro.paulek;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server implements Runnable {

    private final Logger logger = Logger.getGlobal();

    private final String                     address;
    private final int                        port;
    private final int                        backlog;

    private ServerSocket                     serverSocket;

    private Thread                           mainThread;
    private Map<SocketAddress, ClientThread> clientThreads = new HashMap<>();

    public Server(String address, String port, int backlog) {
        this.address = Objects.requireNonNull(address);
        this.port = Integer.parseInt(Objects.requireNonNull(port));
        this.backlog = backlog;
    }

    public void init() {
        try {
            serverSocket = new ServerSocket(port, backlog, InetAddress.getByName(address));
            logger.log(Level.INFO, String.format("Server started listening on: %s:%s", address, port));
        } catch (IOException exception) {
            logger.log(Level.SEVERE, String.format("Failed to create server connection cause: %s, exception: %s", exception.getMessage(), exception.fillInStackTrace()));
        }
        mainThread = new Thread(this);
        mainThread.start();
    }

    public void handlePacket(SocketAddress socketAddress, String message) {
        System.out.println(socketAddress + " > " + message);
        clientThreads.values().forEach(client -> {
            if(client.socket.getRemoteSocketAddress().equals(socketAddress)) {
                return;
            }
            client.sendMessage(socketAddress + " > " + message);
        });
    }

    @Override
    public void run() {
        while (mainThread != null && mainThread.isAlive() && !mainThread.isInterrupted()) {
            try {
                Socket socket = serverSocket.accept();
                ClientThread clientThread = new ClientThread(this, socket);
                clientThreads.put(socket.getRemoteSocketAddress(), clientThread);
                clientThread.start();
                logger.log(Level.INFO, String.format("New client connecting to server: %s", socket.getRemoteSocketAddress()));
            } catch (IOException exception) {
                logger.log(Level.SEVERE, String.format("Failed to create user thread connection cause: %s, exception: %s", exception.getMessage(), exception.fillInStackTrace()));
            }
        }
    }

    public void stop() {
        mainThread.interrupt();
    }

    private static class ClientThread extends Thread {

        private Server server;
        private Socket socket;

        private DataOutputStream dataOutputStream;
        private DataInputStream dataInputStream;

        public ClientThread(Server server, Socket socket) {
            this.server = Objects.requireNonNull(server);
            this.socket = Objects.requireNonNull(socket);
        }

        public void init() {
            this.start();
        }

        public void sendMessage(String message){
            try {
                dataOutputStream.writeUTF(message);
                dataOutputStream.flush();
            } catch (IOException exception) {
                server.logger.log(Level.SEVERE, String.format("Fail to flush message via server cause: %s, exception: %s", exception.getMessage(), exception.fillInStackTrace()));
            }
        }

        public void close() {
            try {
                dataOutputStream.close();
                dataInputStream.close();
            } catch (IOException exception) {
                server.logger.log(Level.SEVERE, String.format("Fail to close client connection cause: %s, exception: %s", exception.getMessage(), exception.fillInStackTrace()));
            }
        }

        @Override
        public void run() {
            try {
                BufferedInputStream bufferedInputStream = new BufferedInputStream(socket.getInputStream());
                dataInputStream = new DataInputStream(bufferedInputStream);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());
                dataOutputStream = new DataOutputStream(bufferedOutputStream);

                while (true) {
                    server.handlePacket(socket.getRemoteSocketAddress(), dataInputStream.readUTF());
                }
            } catch (IOException exception) {
                server.logger.log(Level.SEVERE, String.format("Fail to handle client packet cause: %s, exception: %s", exception.getMessage(), exception.fillInStackTrace()));
                this.close();
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server("127.0.0.1", "3000", 1000);
        server.init();
    }

}
