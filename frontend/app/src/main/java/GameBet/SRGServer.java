package GameBet;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class SRGServer implements Serializable {

    private static final int DEFAULT_BUFFER_SIZE = 10;

    // ena queue ana game/secret
    private static final Map<String, RandomBuffer> buffers = new HashMap<>();

    public static void main(String[] args) {
        String configPath = (args.length > 0) ? args[0] : "config.properties";
        AppConfig config = new AppConfig(configPath);

        int PORT = config.getInt("srg.port", 3000);
        int BUFFER_SIZE = config.getInt("srg.buffer.size", DEFAULT_BUFFER_SIZE);

        System.out.println("SRG Server started on port " + PORT);
        System.out.println("SRG buffer size = " + BUFFER_SIZE);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new SRGHandler(socket, BUFFER_SIZE)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class SRGHandler implements Runnable {
        private final Socket socket;
        private final int bufferSize;

        public SRGHandler(Socket socket, int bufferSize) {
            this.socket = socket;
            this.bufferSize = bufferSize;
        }

        @Override
        public void run() {
            try (
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())
            ) {
                String secret = (String) in.readObject();

                RandomBuffer buffer = getOrCreateBuffer(secret, bufferSize);

                int randomNumber = buffer.take();   // consumer
                String hash = sha256(randomNumber + secret);

                SRGResponse response = new SRGResponse(randomNumber, hash);
                out.writeObject(response);
                out.flush();

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static RandomBuffer getOrCreateBuffer(String secret, int bufferSize) {
        synchronized (buffers) {
            RandomBuffer buffer = buffers.get(secret);

            if (buffer == null) {
                buffer = new RandomBuffer(bufferSize);
                buffers.put(secret, buffer);

                Thread producerThread = new Thread(new RandomProducer(buffer, secret));
                producerThread.setDaemon(true);
                producerThread.start();

                System.out.println("Created SRG buffer for secret: " + secret);
            }

            return buffer;
        }
    }

    static class RandomBuffer {
        private final LinkedList<Integer> queue = new LinkedList<>();
        private final int capacity;

        public RandomBuffer(int capacity) {
            this.capacity = capacity;
        }

        public synchronized void put(int number) throws InterruptedException {
            while (queue.size() >= capacity) {
                wait();
            }
            queue.addLast(number);
            notifyAll();
        }

        public synchronized int take() throws InterruptedException {
            while (queue.isEmpty()) {
                wait();
            }
            int number = queue.removeFirst();
            notifyAll();
            return number;
        }

        public synchronized int size() {
            return queue.size();
        }
    }

    static class RandomProducer implements Runnable {
        private final RandomBuffer buffer;
        private final String secret;
        private final SecureRandom random = new SecureRandom();

        public RandomProducer(RandomBuffer buffer, String secret) {
            this.buffer = buffer;
            this.secret = secret;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    int randomNumber = random.nextInt(100);

                    // mikri kathysterisi gia na fainetai to producer-consumer model
                    Thread.sleep(100);

                    buffer.put(randomNumber);

                    // optional log
                    // System.out.println("Produced for secret " + secret + ": " + randomNumber + " | size=" + buffer.size());

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private static String sha256(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(input.getBytes("UTF-8"));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}