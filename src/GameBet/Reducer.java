package GameBet;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Reducer {

    // apothikeusi partial αποτελεσματων ana job
    private static final Map<String, List<Object>> partialStore = new HashMap<>();
    private static final Map<String, Integer> partialCount = new HashMap<>();
    private static final Object reducerLock = new Object();

    public static void main(String[] args) {
        String configPath = (args.length > 0) ? args[0] : "config.properties";
        AppConfig config = new AppConfig(configPath);

        int REDUCER_PORT = config.getInt("reducer.port", 6000);

        try (ServerSocket serverSocket = new ServerSocket(REDUCER_PORT)) {
            System.out.println("Reducer listening on port " + REDUCER_PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handleReducerRequest(socket)).start();
            }

        } catch (IOException e) {
            System.err.println("Reducer error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleReducerRequest(Socket socket) {
        try (
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            out.flush();

            Object obj = in.readObject();
            if (!(obj instanceof Object[])) {
                out.writeObject("Invalid reducer request.");
                out.flush();
                return;
            }

            Object[] req = (Object[]) obj;
            if (req.length == 0 || !(req[0] instanceof String)) {
                out.writeObject("Invalid reducer request format.");
                out.flush();
                return;
            }

            String command = (String) req[0];

            switch (command) {
                case "STORE_PARTIAL":
                    handleStorePartial(req, out);
                    break;

                case "GET_RESULT":
                    handleGetResult(req, out);
                    break;

                // krataw kai ta palia an theleis na kaneis metavaτικά test
                case "SEARCH_REDUCE":
                    if (req.length >= 2) {
                        handleSearchReduce(req[1], out);
                    } else {
                        out.writeObject(Collections.emptyList());
                        out.flush();
                    }
                    break;

                case "SUM_DOUBLE":
                    if (req.length >= 2) {
                        handleSumDouble(req[1], out);
                    } else {
                        out.writeObject(Collections.emptyMap());
                        out.flush();
                    }
                    break;

                default:
                    out.writeObject("Unknown reducer request type: " + command);
                    out.flush();
                    break;
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Reducer request error: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    // req = ["STORE_PARTIAL", reduceType, jobId, payload]
    private static void handleStorePartial(Object[] req, ObjectOutputStream out) throws IOException {
        if (req.length < 4 || !(req[1] instanceof String) || !(req[2] instanceof String)) {
            out.writeObject("Invalid STORE_PARTIAL request.");
            out.flush();
            return;
        }

        String reduceType = (String) req[1];
        String jobId = (String) req[2];
        Object payload = req[3];

        String key = buildStoreKey(reduceType, jobId);

        synchronized (reducerLock) {
            List<Object> bucket = partialStore.get(key);
            if (bucket == null) {
                bucket = new ArrayList<>();
                partialStore.put(key, bucket);
            }

            if (payload instanceof List<?>) {
                bucket.addAll((List<?>) payload);
            } else if (payload != null) {
                bucket.add(payload);
            }

            int count = partialCount.getOrDefault(key, 0);
            partialCount.put(key, count + 1);

            reducerLock.notifyAll();
        }

        out.writeObject("PARTIAL_STORED");
        out.flush();
    }

    // req = ["GET_RESULT", reduceType, jobId, expectedWorkers]
    private static void handleGetResult(Object[] req, ObjectOutputStream out) throws IOException {
        if (req.length < 4 ||
                !(req[1] instanceof String) ||
                !(req[2] instanceof String) ||
                !(req[3] instanceof Integer)) {
            out.writeObject("Invalid GET_RESULT request.");
            out.flush();
            return;
        }

        String reduceType = (String) req[1];
        String jobId = (String) req[2];
        int expectedWorkers = (Integer) req[3];

        String key = buildStoreKey(reduceType, jobId);
        List<Object> dataToReduce;

        synchronized (reducerLock) {
            while (partialCount.getOrDefault(key, 0) < expectedWorkers) {
                try {
                    reducerLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    out.writeObject("Reducer interrupted while waiting.");
                    out.flush();
                    return;
                }
            }

            dataToReduce = new ArrayList<>(partialStore.getOrDefault(key, Collections.emptyList()));

            partialStore.remove(key);
            partialCount.remove(key);
        }

        if (reduceType.equals("SEARCH")) {
            handleSearchReduce(dataToReduce, out);
        } else if (reduceType.equals("SUM_DOUBLE")) {
            handleSumDouble(dataToReduce, out);
        } else {
            out.writeObject("Unknown reduce type: " + reduceType);
            out.flush();
        }
    }

    private static String buildStoreKey(String reduceType, String jobId) {
        return reduceType + "::" + jobId;
    }

    // Reduce search results -> krataei monadika games
    private static void handleSearchReduce(Object payload, ObjectOutputStream out) throws IOException {
        if (!(payload instanceof List<?>)) {
            out.writeObject(Collections.emptyList());
            out.flush();
            return;
        }

        List<?> list = (List<?>) payload;

        if (list.isEmpty()) {
            out.writeObject(Collections.emptyList());
            out.flush();
            return;
        }

        Map<String, Game> uniqueGames = new LinkedHashMap<>();

        for (Object obj : list) {
            if (obj instanceof Game) {
                Game game = (Game) obj;
                uniqueGames.putIfAbsent(game.getGameName(), game);
            }
        }

        List<Game> result = new ArrayList<>(uniqueGames.values());
        out.writeObject(result);
        out.flush();
    }

    // Reduce aggregations -> athroisma ana key
    private static void handleSumDouble(Object payload, ObjectOutputStream out) throws IOException {
        if (!(payload instanceof List<?>)) {
            out.writeObject(Collections.emptyMap());
            out.flush();
            return;
        }

        List<?> list = (List<?>) payload;
        Map<String, Double> totals = new LinkedHashMap<>();

        for (Object obj : list) {
            if (obj instanceof KeyValue<?, ?>) {
                KeyValue<?, ?> kv = (KeyValue<?, ?>) obj;

                String key = String.valueOf(kv.getKey());
                Object valueObj = kv.getValue();

                double value;
                if (valueObj instanceof Double) {
                    value = (Double) valueObj;
                } else if (valueObj instanceof Integer) {
                    value = ((Integer) valueObj).doubleValue();
                } else if (valueObj instanceof Float) {
                    value = ((Float) valueObj).doubleValue();
                } else if (valueObj instanceof Long) {
                    value = ((Long) valueObj).doubleValue();
                } else {
                    continue;
                }

                totals.merge(key, value, Double::sum);
            }
        }

        out.writeObject(totals);
        out.flush();
    }
}