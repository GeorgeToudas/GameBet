package com.example.GameBetApp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;

import GameBet.Game;
//emfanizei ola ta available paixnidia se ListView se ListView gia na mhn xreiazetai AndroidX kai na paizei me minSdk 8
public class GamesActivity extends Activity {

    private ListView listGames;
    //10.0.2.2 xrisimopoiei to o emulator gia na sundethei me ton master
    private static final String MASTER_HOST = "10.0.2.2"; // emulator -> local pc
    private static final int MASTER_PORT = 4445;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_games);
        //arxikopoihsh tou ListView kai tropos emfanishs
        //kratame to id recyclerGames gia na mhn xreiastei na allakseis onomata sto java, alla sto xml prepei na einai ListView
        listGames = findViewById(R.id.listGames);
        loadGames();
    }

    private void loadGames() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Object response = MasterConnection.sendCommand(MASTER_HOST, MASTER_PORT, "getAllGames", null);
                //stelnei ston master gia na tou ferei ola ta paixnidia
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response instanceof List<?>) {
                            try {
                                @SuppressWarnings("unchecked")
                                List<Game> games = (List<Game>) response;
                                listGames.setAdapter(new com.example.GameBetApp.GameAdapter(games));
                            } catch (Exception e) {
                                Toast.makeText(GamesActivity.this, "Error parsing games", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(GamesActivity.this, "Unexpected response: " + response, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }
}
