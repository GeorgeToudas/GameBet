package GameBet;

import java.io.Serializable;
import java.util.Locale;
//stelnei mazemena filtra tou palyer ston master san ena object
public class SearchRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private int minStars;
    private String riskLevel;     // low, medium, high, all
    private String betCategory;   // $, $$, $$$, all

    public SearchRequest() {
    }

    public SearchRequest(int minStars, String riskLevel, String betCategory) {
        this.minStars = minStars;
        this.riskLevel = riskLevel;
        this.betCategory = betCategory;
    }

    public int getMinStars() {
        return minStars;
    }

    public void setMinStars(int minStars) {
        this.minStars = minStars;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getBetCategory() {
        return betCategory;
    }

    public void setBetCategory(String betCategory) {
        this.betCategory = betCategory;
    }

    public boolean matches(Game game) {
        if (game == null) {
            return false;
        }

        if (game.getStars() < minStars) {
            return false;
        }

        if (riskLevel != null && !riskLevel.trim().equalsIgnoreCase("all")) {
            String reqRisk = riskLevel.trim().toLowerCase(Locale.ROOT);
            String gameRisk = game.getRiskLevel().trim().toLowerCase(Locale.ROOT);

            if (!reqRisk.equals(gameRisk)) {
                return false;
            }
        }

        if (betCategory != null && !betCategory.trim().equalsIgnoreCase("all")) {
            String reqBetCat = betCategory.trim();
            String gameBetCat = game.getBetCategory().trim();

            if (!reqBetCat.equals(gameBetCat)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        return "SearchRequest{" +
                "minStars=" + minStars +
                ", riskLevel='" + riskLevel + '\'' +
                ", betCategory='" + betCategory + '\'' +
                '}';
    }
}
