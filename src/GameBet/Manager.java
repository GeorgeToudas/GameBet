package GameBet;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Manager {
    private static String Master_host = "localhost";
    private static int Master_port = 4445;

    public static void main(String[] args) {
        String configPath = (args.length > 0) ? args[0] : "config.properties";
        AppConfig config = new AppConfig(configPath);

        Master_host = config.getString("master.host", "localhost");
        Master_port = config.getInt("master.port", 4445);
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n--- Manager Console Menu ---");
            System.out.println("1. Add Game");
            System.out.println("2. Add Game from Json");
            System.out.println("3. Remove Game");
            System.out.println("4. Update Game Risk");
            System.out.println("5. Show game/provider stats");
            System.out.println("6. Show player stats");
            System.out.println("0. Exit");
            System.out.print("Select: ");

            int choice = Integer.parseInt(scanner.nextLine());

            if (choice == 0) {
                System.out.println("Exiting...");
                break;
            }

            try {
                switch (choice) {
                    case 1:
                        addGame(scanner);
                        break;

                    case 2:
                        addGameJson(scanner);
                        break;

                    case 3:
                        removeGame(scanner);
                        break;

                    case 4:
                        updateRisk(scanner);
                        break;

                    case 5:
                        Object gameStats = sendReqMaster("aggregateGameStats", null);//στελνει τον master την εντολη για υπολογισμο stats
                        System.out.println("Game Stats: " + gameStats);
                        break;

                    case 6:
                        Object playerStats = sendReqMaster("aggregatePlayerStats", null);//στελνει τον master την εντολη για υπολογισμο stats
                        System.out.println("Player Stats: " + playerStats);
                        break;
                    default:
                        System.out.println("Non valid choice.");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }

        scanner.close();
    }

    private static void addGame(Scanner scanner) {//edw vazw ta json
        System.out.print("Game Name: ");
        String gameName = scanner.nextLine();

        System.out.print("Provider Name: ");
        String providerName = scanner.nextLine();

        System.out.print("Stars (0-5): ");
        int stars = Integer.parseInt(scanner.nextLine());

        System.out.print("No Of Votes: ");
        int noOfVotes = Integer.parseInt(scanner.nextLine());

        System.out.print("Game Logo path: ");
        String gameLogo = scanner.nextLine();

        System.out.print("Min Bet: ");
        double minBet = Double.parseDouble(scanner.nextLine());

        System.out.print("Max Bet: ");
        double maxBet = Double.parseDouble(scanner.nextLine());

        System.out.print("Risk (low/medium/high): ");
        String riskLevel = scanner.nextLine();

        System.out.print("Hash Key: ");
        String hashKey = scanner.nextLine();

        Game game = new Game();
        game.setGameName(gameName);
        game.setProviderName(providerName);
        game.setStars(stars);
        game.setNoOfVotes(noOfVotes);
        game.setGameLogo(gameLogo);
        game.setMinBet(minBet);
        game.setMaxBet(maxBet);
        game.setRiskLevel(riskLevel);
        game.setHashKey(hashKey);
        game.initializeComputedFields();

        Object response = sendReqMaster("addGame", new Object[]{gameName, game});//στελνει το game στον master
        System.out.println("Master Response: " + response);
    }

    private static void addGameJson(Scanner scanner) {
        try {
            System.out.print("JSON file path: ");
            String jsonPath = scanner.nextLine().trim();

            Game game = GameJson.loadGameFromJson(jsonPath);

            Object response = sendReqMaster(
                    "addGame",
                    new Object[]{game.getGameName(), game}
            );

            System.out.println("Master Response: " + response);

        } catch (Exception e) {
            System.out.println("Failed to load game from JSON: " + e.getMessage());
        }
    }

    private static void removeGame(Scanner scanner) {
        System.out.print("Game Name to remove: ");
        String gameName = scanner.nextLine();

        Object response = sendReqMaster("removeGame", gameName);//στελνει το ονομα του παιχνιδου στον master για διαγραφη
        System.out.println("Master Response: " + response);
    }

    private static void updateRisk(Scanner scanner) {
        System.out.print("Game Name: ");
        String gameName = scanner.nextLine();

        System.out.print("New Risk (low/medium/high): ");
        String newRisk = scanner.nextLine();

        Object response = sendReqMaster("updateRisk", new Object[]{gameName, newRisk});//στελνει το ονομα του παιχνιδου στον master για τροποποιηση ρισκου
        System.out.println("Master Response: " + response);
    }

    private static Object sendReqMaster(String rec, Object data) {
        try (Socket socket = new Socket(Master_host, Master_port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());//στελνει δεδομενα στον master
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {//λαμβανει δεδομενα απο τον master

            out.writeObject(rec);//στελνει το ειδος του request
            out.flush();

            out.writeObject(data);//στελνει τα δεδομενα του request
            out.flush();

            return in.readObject();//o master επιστρεφει τα αποτελεσματα 

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}