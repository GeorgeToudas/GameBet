package GameBet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class PlayerApp {

    private static String MASTER_HOST = "localhost";
    private static int MASTER_PORT = 4445;

    private static Player currentPlayer;
    private static List<Game> lastResults = new ArrayList<>();
    // to lastResults ειναι λίστα με τα τελευταία παιχνίδια που έφερε ο client από τον server gia na mhn ton ξαναρωταει

    public static void main(String[] args) {

        // η δημιουργια του παικτη μπορει να μην ειναι απαραιτητη εδω αλλα στο Game η να χρησιμοποιηθει σε καποια απο τις συναρτησεις-play_game-search_games ktl...
        /*    System.out.print("Give player id: ");
        String username = scanner.nextLine();
        String password = scanner.nextLine();
        int playerId = scanner.nextInt();

        currentPlayer = new Player(username, password, playerId);
        */
        String configPath = (args.length > 0) ? args[0] : "config.properties";
        AppConfig config = new AppConfig(configPath);

        MASTER_HOST = config.getString("master.host", "localhost");
        MASTER_PORT = config.getInt("master.port", 4445);

        Scanner scanner = new Scanner(System.in);

        System.out.print("Username: ");
        String username = scanner.nextLine();

        System.out.print("Password: ");
        String password = scanner.nextLine();

        System.out.print("Player ID: ");
        int playerId = Integer.parseInt(scanner.nextLine());

        currentPlayer = new Player(username, password, playerId);

        while (true) {
            System.out.println("\n--- Player App Menu ---");
            System.out.println("1. Search games");
            System.out.println("2. View available games");
            System.out.println("3. Play game");
            System.out.println("4. Rate game");
            System.out.println("5. Add balance");
            System.out.println("6. View last search results");
            System.out.println("0. Exit");
            System.out.print("Select: ");

            String input = scanner.nextLine();
            int choice;
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Give a valid number.");
                continue;
            }
            try {
                switch (choice) {
                    case 1:
                        searchGames(scanner);
                        break;
                    case 2:
                        viewAvailableGames();
                        break;
                    case 3:
                        playGame(scanner);
                        break;
                    case 4:
                        rateGame(scanner);
                        break;
                    case 5:
                        addBalance(scanner);
                        break;
                    case 6:
                        viewLastSearchResults();
                        break;
                    case 0:
                        System.out.println("Goodbye!");
                        scanner.close();
                        return;
                    default:
                        System.out.println("Non valid choice.");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void searchGames(Scanner scanner) {
        System.out.println("Choose game filters");
        // asteria = δημοτικοτητα παιχνιδιου -> 1-5
        System.out.println("Stars (1-5): ");
        int minStars = Integer.parseInt(scanner.nextLine().trim());

        System.out.println("Risk level (low/medium/high/all): ");
        String riskLevel = scanner.nextLine().trim();
        // ορια πονταρισματος απο calculateBetCategory στο Game
        System.out.println("Bet category ($/$$/$$$/all): ");
        String betCategory = scanner.nextLine().trim();

        SearchRequest req = new SearchRequest(minStars, riskLevel, betCategory);
        Object response = sendCommandToMaster("searchGames", req);

        if (response instanceof List<?>) { // sthn λιστα μαλλον βαζουμε παιχνιδια που εχουμε παιξει ηδη-List <Games>
            @SuppressWarnings("unchecked")
            List<Game> games = (List<Game>) response;
            lastResults = games;

            System.out.println("\nSearch results:");
            printGames(games);
        } else {
            System.out.println("Unexpected response: " + response);
        }
    }

    private static void viewAvailableGames() {
        Object response = sendCommandToMaster("getAllGames", null);

        if (response instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<Game> games = (List<Game>) response;
            lastResults = games;

            System.out.println("\nAvailable games:");
            printGames(games);
        } else {
            System.out.println("Unexpected response: " + response);
        }
    }

    private static void playGame(Scanner scanner) {
        if (lastResults.isEmpty()) {
            System.out.println("No games loaded. Search or view games first.");
            return;
        }

        System.out.print("Choose game name: ");
        String gameName = scanner.nextLine().trim();

        Game selectedGame = findGameByName(gameName);
        if (selectedGame == null) {
            System.out.println("Game not found in last shown results.");
            return;
        }

        System.out.print("Bet amount: ");
        double betAmount = Double.parseDouble(scanner.nextLine().trim());

        Object response = sendCommandToMaster(
                "play",
                new Object[]{selectedGame.getGameName(), String.valueOf(currentPlayer.getPlayerID()), betAmount}
        );

        System.out.println("Play response: " + response);
    }

    private static void rateGame(Scanner scanner) {
        if (lastResults.isEmpty()) {
            System.out.println("No games loaded. Search or view games first.");
            return;
        }

        System.out.print("Choose game name: ");
        String gameName = scanner.nextLine().trim();

        Game selectedGame = findGameByName(gameName);
        if (selectedGame == null) {
            System.out.println("Game not found in last shown results.");
            return;
        }

        System.out.print("Rating (1-5): ");
        int rating = Integer.parseInt(scanner.nextLine().trim());

        if (rating < 1 || rating > 5) {
            System.out.println("Rating must be from 1 to 5.");
            return;
        }

        Object response = sendCommandToMaster(
                "rateGame",
                new Object[]{selectedGame.getGameName(), rating}
        );

        System.out.println("Rate response: " + response);
    }

    private static void addBalance(Scanner scanner) {
        System.out.print("Amount to add: ");
        double amount = Double.parseDouble(scanner.nextLine().trim());

        if (amount <= 0) {
            System.out.println("Amount must be positive.");
            return;
        }

        Object response = sendCommandToMaster(
                "addBalance",
                new Object[]{String.valueOf(currentPlayer.getPlayerID()), amount}
        );

        System.out.println("Add balance response: " + response);
    }

    private static void viewLastSearchResults() {
        if (lastResults.isEmpty()) {
            System.out.println("No stored results.");
            return;
        }

        System.out.println("\nLast shown results:");
        printGames(lastResults);
    }

    private static Game findGameByName(String gameName) {
        for (Game g : lastResults) {
            if (g.getGameName() != null && g.getGameName().equalsIgnoreCase(gameName)) {
                return g;
            }
        }
        return null;
    }

    private static void printGames(List<Game> games) {
        if (games == null || games.isEmpty()) {
            System.out.println("No games found.");
            return;
        }

        for (Game g : games) {
            System.out.println("--------------------------------");
            System.out.println("Game: " + g.getGameName());
            System.out.println("Provider: " + g.getProviderName());
            System.out.println("Stars: " + g.getStars());
            System.out.println("Risk: " + g.getRiskLevel());
            System.out.println("Bet category: " + g.getBetCategory());
            System.out.println("Min bet: " + g.getMinBet());
            System.out.println("Max bet: " + g.getMaxBet());
            System.out.println("Jackpot: " + g.getJackpot());
        }
        System.out.println("--------------------------------");
    }

    private static Object sendCommandToMaster(String cmd, Object data) {
        try (Socket socket = new Socket(MASTER_HOST, MASTER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(cmd);
            out.flush();

            out.writeObject(data);
            out.flush();

            return in.readObject();

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error on " + cmd + ": " + e.getMessage());
            return null;
        }
    }
}