package pro.paulek;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client implements Runnable {

    private final Logger     logger = Logger.getGlobal();

    private final String     address;
    private final int        port;

    private Socket           socket;

    private Thread           mainThread;
    private ClientChatThread chatThread;

    private DataInputStream  dataInputStream;
    private DataOutputStream dataOutputStream;

    public Client(String address, String port) {
        this.address = Objects.requireNonNull(address);
        this.port = Integer.parseInt(Objects.requireNonNull(port));
    }

    public void init() {
        try {
            socket = new Socket(address, port);

            dataInputStream = new DataInputStream(System.in);
            dataOutputStream = new DataOutputStream(socket.getOutputStream());

            mainThread = new Thread(this);
            chatThread = new ClientChatThread(socket, this);

            mainThread.start();
            chatThread.init();

            logger.log(Level.INFO, String.format("Client started on: %s:%s", address, port));
        } catch (IOException exception) {
            logger.log(Level.SEVERE, String.format("Failed to create user connection cause: %s, exception: %s", exception.getMessage(), exception.fillInStackTrace()));
        }
    }

    @Override
    public void run() {
        while (mainThread != null && !mainThread.isInterrupted()){
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(dataInputStream));
            try {
                dataOutputStream.writeUTF(bufferedReader.readLine());
                dataOutputStream.flush();
            } catch (IOException exception) {
                logger.log(Level.WARNING, String.format("Failed to send message cause: %s, exception: %s", exception.getMessage(), exception.fillInStackTrace()));
            }
//            try {
//                mainThread.wait(500);
//            } catch (InterruptedException exception) {
//                logger.log(Level.SEVERE, String.format("Interrupted client sleep cause: %s, exception: %s", exception.getMessage(), exception.fillInStackTrace()));
//            }
        }
    }

    public void showMessage(String message) {
        System.out.println(LocalDateTime.now().toString() + " User > " + message);
    }

    public void close() {
        mainThread.interrupt();
        chatThread.close();

        try {
            dataInputStream.close();
            dataOutputStream.close();
            socket.close();
        } catch (IOException exception) {
            logger.log(Level.SEVERE, String.format("Critical error while closing client cause: %s, exception: %s", exception.getMessage(), exception.fillInStackTrace()));
        }
    }

    private class ClientChatThread extends Thread {

        private final Client client;
        private final Socket socket;

        private DataInputStream dataInputStream;

        public ClientChatThread(Socket socket, Client client) {
            this.client = Objects.requireNonNull(client);
            this.socket = Objects.requireNonNull(socket);
        }

        public void init() {
            this.open();
            this.start();
        }

        public void open() {
            try {
                dataInputStream = new DataInputStream(socket.getInputStream());
            } catch (IOException exception) {
                logger.log(Level.SEVERE, String.format("Error while opening input stream in chat thread cause: %s, exception: %s", exception.getMessage(), exception.fillInStackTrace()));
            }
        }

        public void close() {
            try {
                dataInputStream.close();
            } catch (IOException exception) {
                logger.log(Level.SEVERE, String.format("Error while closing input stream in chat thread cause: %s, exception: %s", exception.getMessage(), exception.fillInStackTrace()));
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    client.showMessage(dataInputStream.readUTF());
                }
            } catch (Exception exception) {
                logger.log(Level.INFO, "Closing client...");
                this.close();
                client.close();
            }
        }

    }

    public static void main(String[] args) {
        Client client = new Client("localhost", "3000");
        client.init();
    }

}
