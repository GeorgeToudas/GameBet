package GameBet;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

public class Game implements Serializable {
    private static final long serialVersionUID = 1L;

    private String gameName;
    private String providerName;
    private int stars;
    private int noOfVotes;
    private String gameLogo;
    private double minBet;
    private double maxBet;
    private String riskLevel;
    private String hashKey;

    private String betCategory;
    private double jackpot;
    private boolean active = true;

    private int totalPlays;
    private double totalBetAmount;
    private double totalPayoutAmount;
    private double totalProfitLoss;

    public Game() {}

    public void initializeComputedFields() {
        normalizeRiskLevel();
        calculateBetCategory();
        calculateJackpot();
    }

    private void normalizeRiskLevel() {
        if (riskLevel == null) {
            throw new IllegalArgumentException("RiskLevel cannot be null.");
        }

        riskLevel = riskLevel.trim().toLowerCase(Locale.ROOT);

        if (!riskLevel.equals("low") &&
                !riskLevel.equals("medium") &&
                !riskLevel.equals("high")) {
            throw new IllegalArgumentException("RiskLevel must be low, medium or high.");
        }
    }

    public void calculateBetCategory() {
        if (minBet >= 5.0) betCategory = "$$$";
        else if (minBet >= 1.0) betCategory = "$$";
        else betCategory = "$";
    }

    public void calculateJackpot() {
        switch (riskLevel) {
            case "low":
                jackpot = 10.0;
                break;
            case "medium":
                jackpot = 20.0;
                break;
            case "high":
                jackpot = 40.0;
                break;
            default:
                throw new IllegalStateException("Unexpected risk level: " + riskLevel);
        }
    }

    public double[] getRiskMultipliers() {
        switch (riskLevel) {
            case "low":
                return new double[]{0.0, 0.0, 0.0, 0.1, 0.5, 1.0, 1.1, 1.3, 2.0, 2.5};
            case "medium":
                return new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.5, 1.0, 1.5, 2.5, 3.5};
            case "high":
                return new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 2.0, 6.5};
            default:
                throw new IllegalStateException("Unexpected risk level: " + riskLevel);
        }
    }

    public synchronized void registerPlay(double betAmount, double payout) {
        totalPlays++;
        totalBetAmount += betAmount;
        totalPayoutAmount += payout;
        totalProfitLoss = totalBetAmount - totalPayoutAmount;
    }

    public String getGameName() {
        return gameName;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public int getStars() {
        return stars;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    public int getNoOfVotes() {
        return noOfVotes;
    }

    public void setNoOfVotes(int noOfVotes) {
        this.noOfVotes = noOfVotes;
    }

    public String getGameLogo() {
        return gameLogo;
    }

    public void setGameLogo(String gameLogo) {
        this.gameLogo = gameLogo;
    }

    public double getMinBet() {
        return minBet;
    }

    public void setMinBet(double minBet) {
        this.minBet = minBet;
    }

    public double getMaxBet() {
        return maxBet;
    }

    public void setMaxBet(double maxBet) {
        this.maxBet = maxBet;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getHashKey() {
        return hashKey;
    }

    public void setHashKey(String hashKey) {
        this.hashKey = hashKey;
    }

    public String getBetCategory() {
        return betCategory;
    }

    public double getJackpot() {
        return jackpot;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getTotalPlays() {
        return totalPlays;
    }

    public double getTotalBetAmount() {
        return totalBetAmount;
    }

    public double getTotalPayoutAmount() {
        return totalPayoutAmount;
    }

    public double getTotalProfitLoss() {
        return totalProfitLoss;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Game)) return false;
        Game game = (Game) o;
        return Objects.equals(gameName, game.gameName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameName);
    }

    @Override
    public String toString() {
        return "Game{" +
                "gameName='" + gameName + '\'' +
                ", providerName='" + providerName + '\'' +
                ", stars=" + stars +
                ", noOfVotes=" + noOfVotes +
                ", gameLogo='" + gameLogo + '\'' +
                ", minBet=" + minBet +
                ", maxBet=" + maxBet +
                ", riskLevel='" + riskLevel + '\'' +
                ", betCategory='" + betCategory + '\'' +
                ", jackpot=" + jackpot +
                ", active=" + active +
                ", totalProfitLoss=" + totalProfitLoss +
                '}';
    }
}