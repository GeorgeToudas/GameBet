package GameBet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameJson {

    public static Game loadGameFromJson(String filePath) throws IOException {
        String json = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);

        Game game = new Game();
        game.setGameName(extractString(json, "GameName"));
        game.setProviderName(extractString(json, "ProviderName"));
        game.setStars(extractInt(json, "Stars"));
        game.setNoOfVotes(extractInt(json, "NoOfVotes"));
        game.setGameLogo(extractString(json, "GameLogo"));
        game.setMinBet(extractDouble(json, "MinBet"));
        game.setMaxBet(extractDouble(json, "MaxBet"));
        game.setRiskLevel(extractString(json, "RiskLevel"));
        game.setHashKey(extractString(json, "HashKey"));

        game.initializeComputedFields();
        return game;
    }

    private static String extractString(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(json);
        if (!m.find()) {
            throw new IllegalArgumentException("Missing string field: " + key);
        }
        return m.group(1);
    }

    private static int extractInt(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+)");
        Matcher m = p.matcher(json);
        if (!m.find()) {
            throw new IllegalArgumentException("Missing int field: " + key);
        }
        return Integer.parseInt(m.group(1));
    }

    private static double extractDouble(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+(\\.\\d+)?)");
        Matcher m = p.matcher(json);
        if (!m.find()) {
            throw new IllegalArgumentException("Missing double field: " + key);
        }
        return Double.parseDouble(m.group(1));
    }
}