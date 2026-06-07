package GameBet;

import java.io.Serializable;

// to serializable einai mono an theloyme na stelnoume olokliro ton player meso sockets san antikeimeno
public class Player implements Serializable {

    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private int playerID;

    public Player(String username, String password, int playerID) {
        this.username = username;
        this.password = password;
        this.playerID = playerID;
    }

    public String getUsername() {return username;}
    public String getPassword() {return password;}
    public int getPlayerID() {return playerID;}

    public void setUsername(String username) {this.username = username;}
    public void setPassword(String password) {this.password = password;}
    public void setPlayerID(int playerID) {this.playerID = playerID;}



    @Override
    public String toString() {
        return  "\nUsername: " + username +
                "\nPlayerId: " + playerID +
                "\nPassword: " + password; //isos oxi password
    }

}
