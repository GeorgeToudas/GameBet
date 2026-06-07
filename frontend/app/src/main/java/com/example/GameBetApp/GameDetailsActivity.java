package com.example.GameBetApp;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import GameBet.Game;

public class GameDetailsActivity extends Activity {
    //layout pou efanizei tis plhrofories gia kathe paixnidi
    private TextView tvDetailGameName, tvDetailProvider, tvDetailStars, tvDetailRisk,
            tvDetailBetCategory, tvDetailMinBet, tvDetailMaxBet, tvDetailJackpot;

    private EditText etBetAmount, etRating;
    private Button btnPlay, btnRate;
    //auto einai apo proigoumenh othoni to game
    private Game game;

    private static final String MASTER_HOST = "10.0.2.2";
    private static final int MASTER_PORT = 4445;

    private static final String PLAYER_ID = "1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_details);

        tvDetailGameName = findViewById(R.id.tvDetailGameName);
        tvDetailProvider = findViewById(R.id.tvDetailProvider);
        tvDetailStars = findViewById(R.id.tvDetailStars);
        tvDetailRisk = findViewById(R.id.tvDetailRisk);
        tvDetailBetCategory = findViewById(R.id.tvDetailBetCategory);
        tvDetailMinBet = findViewById(R.id.tvDetailMinBet);
        tvDetailMaxBet = findViewById(R.id.tvDetailMaxBet);
        tvDetailJackpot = findViewById(R.id.tvDetailJackpot);

        etBetAmount = findViewById(R.id.etBetAmount);
        etRating = findViewById(R.id.etRating);

        btnPlay = findViewById(R.id.btnPlay);
        btnRate = findViewById(R.id.btnRate);
        //stalthike apo GameAdapter
        game = (Game) getIntent().getSerializableExtra("game");

        if (game != null) {
            tvDetailGameName.setText(game.getGameName());
            tvDetailProvider.setText("Provider: " + game.getProviderName());
            tvDetailStars.setText("Stars: " + game.getStars());
            tvDetailRisk.setText("Risk: " + game.getRiskLevel());
            tvDetailBetCategory.setText("Bet Category: " + game.getBetCategory());
            tvDetailMinBet.setText("Min Bet: " + game.getMinBet());
            tvDetailMaxBet.setText("Max Bet: " + game.getMaxBet());
            tvDetailJackpot.setText("Jackpot: " + game.getJackpot());
        } else {
            Toast.makeText(this, "Game not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //stelnw ston master play kai rate aithmata
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playGame();
            }
        });

        btnRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rateGame();
            }
        });
    }

    private void playGame() {
        String betText = etBetAmount.getText().toString().trim();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            if (betText.isEmpty()) {
                Toast.makeText(this, "Give bet amount", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        final double betAmount;
        try {
            betAmount = Double.parseDouble(betText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Bet must be a number", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                final Object response = MasterConnection.sendCommand(
                        MASTER_HOST,
                        MASTER_PORT,
                        "play",
                        new Object[]{game.getGameName(), PLAYER_ID, betAmount}
                );

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(GameDetailsActivity.this, String.valueOf(response), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private void rateGame() {
        String ratingText = etRating.getText().toString().trim();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            if (ratingText.isEmpty()) {
                Toast.makeText(this, "Give rating", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        final int rating;
        try {
            rating = Integer.parseInt(ratingText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Rating must be a number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (rating < 1 || rating > 5) {
            Toast.makeText(this, "Rating must be 1-5", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                final Object response = MasterConnection.sendCommand(
                        MASTER_HOST,
                        MASTER_PORT,
                        "rateGame",
                        new Object[]{game.getGameName(), rating}
                );

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(GameDetailsActivity.this, String.valueOf(response), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }
}
