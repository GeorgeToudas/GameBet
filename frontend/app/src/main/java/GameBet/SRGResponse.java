package GameBet;

import java.io.Serializable;

public class SRGResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private int randomNumber;
    private String hash;

    public SRGResponse(int randomNumber, String hash) {
        this.randomNumber = randomNumber;
        this.hash = hash;
    }

    public int getRandomNumber() {
        return randomNumber;
    }

    public String getHash() {
        return hash;
    }

    public void setRandomNumber(int randomNumber) {
        this.randomNumber = randomNumber;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    @Override
    public String toString() {
        return "SRGResponse{" +
                "randomNumber=" + randomNumber +
                ", hash='" + hash + '\'' +
                '}';
    }
}
