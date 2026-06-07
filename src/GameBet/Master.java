package GameBet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Master {

    private static int master_port = 4445;
    private static String REDUCER_HOST = "localhost";
    private static int REDUCER_PORT = 6000;
    private static int EXPECTED_WORKERS = 0;

    public static void main(String[] args) {
        String configPath = (args.length > 0) ? args[0] : "config.properties";
        AppConfig config = new AppConfig(configPath);

        master_port = config.getInt("master.port", 4445);
        REDUCER_HOST = config.getString("reducer.host", "localhost");
        REDUCER_PORT = config.getInt("reducer.port", 6000);
        EXPECTED_WORKERS = config.getInt("workers.count", 0);
        System.out.println("Expected workers from config: " + EXPECTED_WORKERS);

        try (ServerSocket masterSocket = new ServerSocket(master_port)) {
            System.out.println("Master listening on port: " + master_port);
            System.out.println("Reducer configured at: " + REDUCER_HOST + ":" + REDUCER_PORT);

            while (true) {
                Socket socket = masterSocket.accept();
                new Thread(() -> connectionHandlingMaster(socket)).start();
            }
        } catch (IOException e) {
            System.err.println("Master error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ----------------------------Worker---------------------------------
    static class WorkerInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        String host;
        int port;

        public WorkerInfo(String host, int port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public String toString() {
            return host + " is listening to :" + port;
        }
    }

    private static final List<WorkerInfo> workerList =
            Collections.synchronizedList(new ArrayList<>());

    private static void availableWorkers() throws InterruptedException {
        synchronized (workerList) {
            while (workerList.size() < EXPECTED_WORKERS) {
                System.out.println("Waiting for workers... current=" + workerList.size()
                        + ", expected=" + EXPECTED_WORKERS);
                workerList.wait();
            }
        }
    }

    private static void registerWorker(Socket socket,
                                       ObjectInputStream in,
                                       ObjectOutputStream out) throws IOException, ClassNotFoundException {
        Object portObj = in.readObject();
        if (!(portObj instanceof Integer)) {
            sendResponse(out, "Expected worker port as Integer");
            return;
        }

        int workerPort = (Integer) portObj;
        String workerHost = socket.getInetAddress().getHostAddress();
        WorkerInfo newWorker = new WorkerInfo(workerHost, workerPort);

        synchronized (workerList) {
            for (WorkerInfo w : workerList) {
                if (w.host.equals(workerHost) && w.port == workerPort) {
                    sendResponse(out, "Worker already registered: " + newWorker);
                    return;
                }
            }

            workerList.add(newWorker);
            workerList.notifyAll();

            System.out.println("Registered worker: " + newWorker);
            System.out.println("Total workers: " + workerList.size());

            sendResponse(out, "Worker registered successfully.");
        }
    }

    private static List<WorkerInfo> getWorkerSnap() {
        synchronized (workerList) {
            return new ArrayList<>(workerList);
        }
    }

    private static void routeToWorker(String command,
                                      Object payload,
                                      String routingKey,
                                      ObjectOutputStream out) throws IOException {
        List<WorkerInfo> workerSnap = getWorkerSnap();
        if (workerSnap.isEmpty()) {
            sendResponse(out, "No available workers");
            return;
        }

        int workerIndex = Math.floorMod(routingKey.hashCode(), workerSnap.size());
        WorkerInfo selectedWorker = workerSnap.get(workerIndex);

        System.out.println("Routing command " + command +
                " with key " + routingKey +
                " to worker " + selectedWorker.host + ":" + selectedWorker.port);

        try (Socket workerSocket = new Socket(selectedWorker.host, selectedWorker.port);
             ObjectOutputStream workerOut = new ObjectOutputStream(workerSocket.getOutputStream());
             ObjectInputStream workerIn = new ObjectInputStream(workerSocket.getInputStream())) {

            workerOut.writeObject(command);
            workerOut.flush();

            workerOut.writeObject(payload);
            workerOut.flush();

            Object workerResponse = workerIn.readObject();
            sendResponse(out, workerResponse);

        } catch (Exception e) {
            System.err.println("Error communicating with worker: " + e.getMessage());
            sendResponse(out, "Error communicating with worker.");
        }
    }

    // ----------------------------Reducer---------------------------------

    private static Object getReducedResultFromReducer(String reduceType,
                                                      String jobId,
                                                      int expectedWorkers) {
        try (Socket reducerSocket = new Socket(REDUCER_HOST, REDUCER_PORT);
             ObjectOutputStream rOut = new ObjectOutputStream(reducerSocket.getOutputStream());
             ObjectInputStream rIn = new ObjectInputStream(reducerSocket.getInputStream())) {

            rOut.writeObject(new Object[]{"GET_RESULT", reduceType, jobId, expectedWorkers});
            rOut.flush();

            return rIn.readObject();

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Reducer communication error: " + e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------

    private static void sendResponse(ObjectOutputStream out, Object response) throws IOException {
        out.writeObject(response);
        out.flush();
    }

    private static void connectionHandlingMaster(Socket socket) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            Object firstObj = in.readObject();
            String command = (String) firstObj;

            System.out.println("Received command: " + command);

            if (command.equals("registerWorker")) {
                registerWorker(socket, in, out);
                return;
            }

            availableWorkers();

            switch (command) {
                case "searchGames":
                case "getAllGames":
                    handleSearch(command, in, out);
                    break;

                case "addGame":
                case "removeGame":
                case "updateRisk":
                case "play":
                case "rateGame":
                case "addBalance":
                    handleRoutedCommand(command, in, out);
                    break;

                case "aggregateGameStats":
                case "aggregatePlayerStats":
                    handleAggregation(command, out);
                    break;

                default:
                    sendResponse(out, "Unknown command: " + command);
                    break;
            }

        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // ----------------------------Handling---------------------------------

    private static void handleSearch(String command,
                                     ObjectInputStream in,
                                     ObjectOutputStream out) throws IOException, ClassNotFoundException {
        Object filters = in.readObject();
        List<WorkerInfo> workerSnap = getWorkerSnap();
        String jobId = UUID.randomUUID().toString();

        for (WorkerInfo worker : workerSnap) {
            try (Socket workerSocket = new Socket(worker.host, worker.port);
                 ObjectOutputStream workerOut = new ObjectOutputStream(workerSocket.getOutputStream());
                 ObjectInputStream workerIn = new ObjectInputStream(workerSocket.getInputStream())) {

                workerOut.writeObject("searchGamesMap");
                workerOut.flush();

                Object actualFilters = command.equals("getAllGames") ? null : filters;
                workerOut.writeObject(new Object[]{jobId, actualFilters});
                workerOut.flush();

                Object ack = workerIn.readObject();
                System.out.println("Worker map ack: " + ack);

            } catch (Exception e) {
                System.err.println("Search map error on worker " + worker + ": " + e.getMessage());
            }
        }

        Object reduced = getReducedResultFromReducer("SEARCH", jobId, workerSnap.size());
        sendResponse(out, reduced);
    }

    private static void handleRoutedCommand(String command,
                                            ObjectInputStream in,
                                            ObjectOutputStream out) throws IOException, ClassNotFoundException {
        Object payload = in.readObject();
        String routingKey = extractRoutingKey(command, payload);
        routeToWorker(command, payload, routingKey, out);
    }

    private static void handleAggregation(String command,
                                          ObjectOutputStream out) throws IOException {
        List<WorkerInfo> workerSnap = getWorkerSnap();
        String jobId = UUID.randomUUID().toString();

        String workerCommand;
        if (command.equals("aggregateGameStats")) {
            workerCommand = "aggregateGameStatsMap";
        } else {
            workerCommand = "aggregatePlayerStatsMap";
        }

        for (WorkerInfo worker : workerSnap) {
            try (Socket workerSocket = new Socket(worker.host, worker.port);
                 ObjectOutputStream workerOut = new ObjectOutputStream(workerSocket.getOutputStream());
                 ObjectInputStream workerIn = new ObjectInputStream(workerSocket.getInputStream())) {

                workerOut.writeObject(workerCommand);
                workerOut.flush();

                workerOut.writeObject(jobId);
                workerOut.flush();

                Object ack = workerIn.readObject();
                System.out.println("Worker aggregation ack: " + ack);

            } catch (Exception e) {
                System.err.println("Aggregation map error on worker " + worker + ": " + e.getMessage());
            }
        }

        Object reduced = getReducedResultFromReducer("SUM_DOUBLE", jobId, workerSnap.size());
        sendResponse(out, reduced);
    }

    private static String extractRoutingKey(String command, Object payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Null payload for command: " + command);
        }

        if (payload instanceof String) {
            return (String) payload;
        }

        if (payload instanceof Object[]) {
            Object[] arr = (Object[]) payload;
            if (arr.length == 0 || arr[0] == null) {
                throw new IllegalArgumentException("Invalid payload for command: " + command);
            }
            return String.valueOf(arr[0]);
        }

        throw new IllegalArgumentException(
                "Unsupported payload type for command: " + command + ". Use String or Object[]."
        );
    }
}