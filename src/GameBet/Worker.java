package GameBet;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.*;

public class Worker {

    private static final Map<String, Game> gameMap = new HashMap<>();
    private static final Map<String, Double> playerBalances = new HashMap<>();
    private static final Map<String, Double> playerProfitLoss = new HashMap<>();

    private static String MASTER_HOST = "localhost";
    private static int MASTER_PORT = 4445;

    private static String SRG_HOST = "localhost";
    private static int SRG_PORT = 3000;

    private static String REDUCER_HOST = "localhost";
    private static int REDUCER_PORT = 6000;

    public static void main(String[] args) {
        String configPath = (args.length > 0) ? args[0] : "config.properties";
        AppConfig config = new AppConfig(configPath);

        int workerPort = config.getInt("worker.port", 5555);
        MASTER_HOST = config.getString("master.host", "localhost");
        MASTER_PORT = config.getInt("master.port", 4445);

        SRG_HOST = config.getString("srg.host", "localhost");
        SRG_PORT = config.getInt("srg.port", 3000);

        REDUCER_HOST = config.getString("reducer.host", "localhost");
        REDUCER_PORT = config.getInt("reducer.port", 6000);
        System.out.println("Worker config loaded. Port = " + workerPort);
        registerToMaster(workerPort);

        try {
            ServerSocket workerSocket = new ServerSocket(workerPort);
            System.out.println("Worker listening on port: " + workerPort);
            System.out.println("Master: " + MASTER_HOST + ":" + MASTER_PORT);
            System.out.println("Reducer: " + REDUCER_HOST + ":" + REDUCER_PORT);
            System.out.println("SRG: " + SRG_HOST + ":" + SRG_PORT);

            while (true) {
                Socket socket = workerSocket.accept();
                new Thread(() -> handleConnection(socket)).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void registerToMaster(int workerPort) {
        try (
                Socket masterSocket = new Socket(MASTER_HOST, MASTER_PORT);
                ObjectOutputStream out = new ObjectOutputStream(masterSocket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(masterSocket.getInputStream())
        ) {
            out.writeObject("registerWorker");
            out.flush();

            out.writeObject(workerPort);
            out.flush();

            Object response = in.readObject();
            System.out.println("Master response: " + response);

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Worker registration error: " + e.getMessage());
        }
    }

    private static void handleConnection(Socket socket) {
        try (
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            out.flush();

            Object cmdObj = in.readObject();
            if (!(cmdObj instanceof String)) {
                out.writeObject("Invalid command type.");
                out.flush();
                return;
            }

            String command = (String) cmdObj;
            Object data = in.readObject();

            System.out.println("Worker " + socket.getLocalPort() + " received command: " + command);

            switch (command) {
                case "addGame":
                    handleAddGame(data, out);
                    break;

                case "removeGame":
                    handleRemoveGame(data, out);
                    break;

                case "updateRisk":
                    handleUpdateRisk(data, out);
                    break;

                case "getAllGames":
                    handleGetAllGames(out);
                    break;

                case "rateGame":
                    handleRateGame(data, out);
                    break;

                case "addBalance":
                    handleAddBalance(data, out);
                    break;

                case "play":
                    handlePlay(data, out);
                    break;

                // ---------------------------------
                case "searchGamesMap":
                    handleSearchGamesMap(data, out);
                    break;

                case "aggregateGameStatsMap":
                    handleAggregateGameStatsMap(data, out);
                    break;

                case "aggregatePlayerStatsMap":
                    handleAggregatePlayerStatsMap(data, out);
                    break;

                default:
                    out.writeObject("Unknown worker command.");
                    out.flush();
                    break;
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Worker connection error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    // --------------------------------------------------
    // BASIC COMMANDS
    // --------------------------------------------------

    private static void handleAddGame(Object data, ObjectOutputStream out) throws IOException {
        if (!(data instanceof Object[])) {
            out.writeObject("Expected [gameName, gameObject].");
            out.flush();
            return;
        }

        Object[] arr = (Object[]) data;
        if (arr.length < 2 || !(arr[0] instanceof String) || !(arr[1] instanceof Game)) {
            out.writeObject("Invalid addGame payload.");
            out.flush();
            return;
        }

        String gameName = (String) arr[0];
        Game game = (Game) arr[1];
        game.initializeComputedFields();

        synchronized (gameMap) {
            gameMap.put(gameName, game);
        }

        out.writeObject("Game '" + gameName + "' added successfully.");
        out.flush();
    }

    private static void handleRemoveGame(Object data, ObjectOutputStream out) throws IOException {
        String gameName;

        if (data instanceof String) {
            gameName = (String) data;
        } else {
            out.writeObject("Expected gameName as String.");
            out.flush();
            return;
        }

        synchronized (gameMap) {
            Game game = gameMap.get(gameName);
            if (game == null) {
                out.writeObject("Game not found.");
                out.flush();
                return;
            }
            game.setActive(false);
        }

        out.writeObject("Game '" + gameName + "' deactivated.");
        out.flush();
    }

    private static void handleUpdateRisk(Object data, ObjectOutputStream out) throws IOException {
        if (!(data instanceof Object[])) {
            out.writeObject("Expected [gameName, newRisk].");
            out.flush();
            return;
        }

        Object[] arr = (Object[]) data;
        if (arr.length < 2 || !(arr[0] instanceof String) || !(arr[1] instanceof String)) {
            out.writeObject("Invalid updateRisk payload.");
            out.flush();
            return;
        }

        String gameName = (String) arr[0];
        String newRisk = (String) arr[1];

        synchronized (gameMap) {
            Game game = gameMap.get(gameName);
            if (game == null) {
                out.writeObject("Game not found.");
                out.flush();
                return;
            }

            game.setRiskLevel(newRisk);
            game.initializeComputedFields();
        }

        out.writeObject("Risk updated for game '" + gameName + "'.");
        out.flush();
    }

    private static void handleGetAllGames(ObjectOutputStream out) throws IOException {
        List<Game> results = new ArrayList<>();

        synchronized (gameMap) {
            for (Game game : gameMap.values()) {
                if (game.isActive()) {
                    results.add(game);
                }
            }
        }

        out.writeObject(results);
        out.flush();
    }

    private static void handleRateGame(Object data, ObjectOutputStream out) throws IOException {
        if (!(data instanceof Object[])) {
            out.writeObject("Expected [gameName, rating].");
            out.flush();
            return;
        }

        Object[] arr = (Object[]) data;
        if (arr.length < 2 || !(arr[0] instanceof String) || !(arr[1] instanceof Integer)) {
            out.writeObject("Invalid rateGame payload.");
            out.flush();
            return;
        }

        String gameName = (String) arr[0];
        int rating = (Integer) arr[1];

        synchronized (gameMap) {
            Game game = gameMap.get(gameName);
            if (game == null) {
                out.writeObject("Game not found.");
                out.flush();
                return;
            }

            int oldVotes = game.getNoOfVotes();
            int oldStars = game.getStars();
            int newVotes = oldVotes + 1;
            int newStars = (int) Math.round(((oldStars * oldVotes) + rating) / (double) newVotes);

            game.setNoOfVotes(newVotes);
            game.setStars(newStars);
        }

        out.writeObject("Rating saved for game '" + gameName + "'.");
        out.flush();
    }

    private static void handleAddBalance(Object data, ObjectOutputStream out) throws IOException {
        if (!(data instanceof Object[])) {
            out.writeObject("Expected [playerId, amount].");
            out.flush();
            return;
        }

        Object[] arr = (Object[]) data;
        if (arr.length < 2 || !(arr[0] instanceof String) || !(arr[1] instanceof Double)) {
            out.writeObject("Invalid addBalance payload.");
            out.flush();
            return;
        }

        String playerId = (String) arr[0];
        double amount = (Double) arr[1];

        synchronized (playerBalances) {
            double oldBalance = playerBalances.getOrDefault(playerId, 0.0);
            playerBalances.put(playerId, oldBalance + amount);
        }

        out.writeObject("Balance added successfully for player '" + playerId + "'.");
        out.flush();
    }

    // --------------------------------------------------
    // SRG
    // --------------------------------------------------

    private static int getVerifiedRandomFromSRG(String secret) throws Exception {
        try (
                Socket srgSocket = new Socket(SRG_HOST, SRG_PORT);
                ObjectOutputStream out = new ObjectOutputStream(srgSocket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(srgSocket.getInputStream())
        ) {
            out.flush();

            out.writeObject(secret);
            out.flush();

            Object obj = in.readObject();
            if (!(obj instanceof SRGResponse)) {
                throw new IllegalStateException("Invalid response from SRG server.");
            }

            SRGResponse response = (SRGResponse) obj;

            String expectedHash = sha256(response.getRandomNumber() + secret);
            if (!expectedHash.equals(response.getHash())) {
                throw new SecurityException("Hash verification failed for SRG response.");
            }

            return response.getRandomNumber();
        }
    }

    private static String sha256(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(input.getBytes("UTF-8"));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // --------------------------------------------------
    // PLAY
    // --------------------------------------------------

    private static void handlePlay(Object data, ObjectOutputStream out) throws IOException {
        if (!(data instanceof Object[])) {
            out.writeObject("Expected [gameName, playerId, betAmount].");
            out.flush();
            return;
        }

        Object[] arr = (Object[]) data;
        if (arr.length < 3 ||
                !(arr[0] instanceof String) ||
                !(arr[1] instanceof String) ||
                !(arr[2] instanceof Double)) {
            out.writeObject("Invalid play payload.");
            out.flush();
            return;
        }

        String gameName = (String) arr[0];
        String playerId = (String) arr[1];
        double betAmount = (Double) arr[2];

        Game game;
        synchronized (gameMap) {
            game = gameMap.get(gameName);
        }

        if (game == null) {
            out.writeObject("Game not found.");
            out.flush();
            return;
        }

        synchronized (game) {
            if (!game.isActive()) {
                out.writeObject("Game is not active.");
                out.flush();
                return;
            }

            if (betAmount < game.getMinBet() || betAmount > game.getMaxBet()) {
                out.writeObject("Bet amount is out of range.");
                out.flush();
                return;
            }

            synchronized (playerBalances) {
                double balance = playerBalances.getOrDefault(playerId, 0.0);
                if (balance < betAmount) {
                    out.writeObject("Insufficient balance.");
                    out.flush();
                    return;
                }

                playerBalances.put(playerId, balance - betAmount);
            }

            int rnd;
            try {
                rnd = getVerifiedRandomFromSRG(game.getHashKey());
            } catch (Exception e) {
                out.writeObject("SRG error: " + e.getMessage());
                out.flush();
                return;
            }

            double payout;
            if (rnd % 100 == 0) {
                payout = betAmount * game.getJackpot();
            } else {
                int idx = rnd % 10;
                double[] multipliers = game.getRiskMultipliers();
                payout = betAmount * multipliers[idx];
            }

            synchronized (playerBalances) {
                double newBalance = playerBalances.getOrDefault(playerId, 0.0) + payout;
                playerBalances.put(playerId, newBalance);
            }

            double playerNet = payout - betAmount;
            synchronized (playerProfitLoss) {
                double old = playerProfitLoss.getOrDefault(playerId, 0.0);
                playerProfitLoss.put(playerId, old + playerNet);
            }

            game.registerPlay(betAmount, payout);

            String msg = "Play completed. Random=" + rnd +
                    ", payout=" + payout +
                    ", new balance=" + playerBalances.get(playerId);

            out.writeObject(msg);
            out.flush();
        }
    }

    // --------------------------------------------------
    // REDUCER COMMUNICATION
    // --------------------------------------------------

    private static void sendPartialToReducer(String type, String jobId, Object payload) {
        try (Socket reducerSocket = new Socket(REDUCER_HOST, REDUCER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(reducerSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(reducerSocket.getInputStream())) {

            out.writeObject(new Object[]{"STORE_PARTIAL", type, jobId, payload});
            out.flush();

            Object ack = in.readObject();
            System.out.println("Reducer ack: " + ack);

        } catch (Exception e) {
            System.err.println("Reducer partial send error: " + e.getMessage());
        }
    }

    // --------------------------------------------------
    // NEW MAPREDUCE MAP HANDLERS
    // data format:
    // [jobId, realPayload]
    // --------------------------------------------------

    private static void handleSearchGamesMap(Object data, ObjectOutputStream out) throws IOException {
        if (!(data instanceof Object[])) {
            out.writeObject("Expected [jobId, filters].");
            out.flush();
            return;
        }

        Object[] arr = (Object[]) data;
        if (arr.length < 2 || !(arr[0] instanceof String)) {
            out.writeObject("Invalid searchGamesMap payload.");
            out.flush();
            return;
        }

        String jobId = (String) arr[0];
        Object filters = arr[1];

        List<Game> results = new ArrayList<>();
        /*backEnd*/
        synchronized (gameMap) {
            for (Game game : gameMap.values()) {
                if (!game.isActive()) continue;

                if (filters == null) {
                    results.add(game);
                } else if (filters instanceof SearchRequest) {
                    SearchRequest req = (SearchRequest) filters;
                    if (req.matches(game)) {
                        results.add(game);
                    }
                }
            }
        }
        /*AndroidApp*/
        /*
        synchronized (gameMap) {
            for (Game game : gameMap.values()) {
                if (!game.isActive()) continue;

                if (filters == null) {
                    results.add(game);
                } else if (filters instanceof Object[]) {
                    Object[] f = (Object[]) filters;

                    int minStars = (Integer) f[0];
                    String riskLevel = (String) f[1];
                    String betCategory = (String) f[2];

                    boolean matches = true;

                    if (game.getStars() < minStars) {
                        matches = false;
                    }

                    if (!"all".equalsIgnoreCase(riskLevel)
                            && !game.getRiskLevel().equalsIgnoreCase(riskLevel)) {
                        matches = false;
                    }

                    if (!"all".equalsIgnoreCase(betCategory)
                            && !game.getBetCategory().equalsIgnoreCase(betCategory)) {
                        matches = false;
                    }

                    if (matches) {
                        results.add(game);
                    }
                }
            }
        }*/
        sendPartialToReducer("SEARCH", jobId, results);

        out.writeObject("SEARCH_PARTIAL_SENT");
        out.flush();
    }

    private static void handleAggregateGameStatsMap(Object data, ObjectOutputStream out) throws IOException {
        if (!(data instanceof String)) {
            out.writeObject("Expected jobId as String.");
            out.flush();
            return;
        }

        String jobId = (String) data;

        List<KeyValue<String, Double>> partial = new ArrayList<>();

        synchronized (gameMap) {
            for (Game game : gameMap.values()) {
                String key = game.getProviderName() + "::" + game.getGameName();
                partial.add(new KeyValue<>(key, game.getTotalProfitLoss()));
            }
        }

        sendPartialToReducer("SUM_DOUBLE", jobId, partial);

        out.writeObject("GAME_STATS_PARTIAL_SENT");
        out.flush();
    }

    private static void handleAggregatePlayerStatsMap(Object data, ObjectOutputStream out) throws IOException {
        if (!(data instanceof String)) {
            out.writeObject("Expected jobId as String.");
            out.flush();
            return;
        }

        String jobId = (String) data;

        List<KeyValue<String, Double>> partial = new ArrayList<>();

        synchronized (playerProfitLoss) {
            for (Map.Entry<String, Double> entry : playerProfitLoss.entrySet()) {
                partial.add(new KeyValue<>(entry.getKey(), entry.getValue()));
            }
        }

        sendPartialToReducer("SUM_DOUBLE", jobId, partial);

        out.writeObject("PLAYER_STATS_PARTIAL_SENT");
        out.flush();
    }
}