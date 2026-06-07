package com.example.GameBetApp;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.List;

import GameBet.Game;
import GameBet.SearchRequest;

//Activity gia thn anazhthsh
public class SearchActivity extends Activity {

    private EditText etStars;
    private Spinner spinnerRisk, spinnerBetCategory;
    private Button btnSearch;
    private ListView listSearchResults;

    private static final String MASTER_HOST = "10.0.2.2";
    private static final int MASTER_PORT = 4445;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        //sundesh me xml
        etStars = findViewById(R.id.etStars);
        spinnerRisk = findViewById(R.id.spinnerRisk);
        spinnerBetCategory = findViewById(R.id.spinnerBetCategory);
        btnSearch = findViewById(R.id.btnSearch);
        listSearchResults = findViewById(R.id.listSearchResults);

        //oi epiloges twn risk kai betcategory
        String[] risks = {"all", "low", "medium", "high"};
        String[] betCategories = {"all", "$", "$$", "$$$"};

        ArrayAdapter<String> riskAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                risks
        );
        riskAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRisk.setAdapter(riskAdapter);

        ArrayAdapter<String> betAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                betCategories
        );
        betAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBetCategory.setAdapter(betAdapter);

        //otan pathsw to search ginetai h anazhthsh
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSearch();
            }
        });
    }

    private void doSearch() {

        //an den dwsei kanena asteri epilegetai to 0 kai ta emfanizei ola
        String starsText = etStars.getText().toString().trim();

        int minStars = 0;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                minStars = starsText.isEmpty() ? 0 : Integer.parseInt(starsText);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Stars must be a number", Toast.LENGTH_SHORT).show();
            return;
        }

        final String risk = spinnerRisk.getSelectedItem().toString();
        final String betCategory = spinnerBetCategory.getSelectedItem().toString();

        final SearchRequest request = new SearchRequest(minStars, risk, betCategory);

        new Thread(new Runnable() {
            @Override
            public void run() {

                //dhmiourgei SearchRequest me filtra anazhthshs kai stelnei ston master me searchGames
                //den stelnoume Object[] giati o backend perimenei GameBet.SearchRequest
                final Object response = com.example.GameBetApp.MasterConnection.sendCommand(
                        MASTER_HOST,
                        MASTER_PORT,
                        "searchGames",
                        request
                );

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response instanceof List<?>) {
                            try {
                                @SuppressWarnings("unchecked")
                                List<Game> games = (List<Game>) response;

                                //vazei ta search results sto ListView
                                listSearchResults.setAdapter(new com.example.GameBetApp.GameAdapter(games));

                            } catch (Exception e) {
                                Toast.makeText(SearchActivity.this, "Error parsing results", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(SearchActivity.this, "Unexpected response: " + response, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }
}