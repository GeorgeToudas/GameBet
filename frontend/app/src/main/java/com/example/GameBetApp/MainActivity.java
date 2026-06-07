package com.example.GameBetApp;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

//main menu ths efarmoghs
public class MainActivity extends Activity {

    private Button btnViewGames, btnSearchGames, btnAddBalance;
    private EditText etBalance;

    private static final String MASTER_HOST = "10.0.2.2";
    private static final int MASTER_PORT = 4445;
    private static final String PLAYER_ID = "1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //sundesei metablhtwn java me xml
        btnViewGames = findViewById(R.id.btnViewGames);
        btnSearchGames = findViewById(R.id.btnSearchGames);
        btnAddBalance = findViewById(R.id.btnAddBalance);
        etBalance = findViewById(R.id.etBalance);
        //button pou anoigei to view game
        btnViewGames.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GamesActivity.class);
                startActivity(intent);
            }
        });
        //button pou anoigei to search games
        btnSearchGames.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                startActivity(intent);
            }
        });
        //button pou kanei to add balance
        btnAddBalance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amountText = etBalance.getText().toString().trim();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    if (amountText.isEmpty()) {
                        Toast.makeText(MainActivity.this, "Give amount first", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                final double amount;
                try {
                    amount = Double.parseDouble(amountText);
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "Amount must be a number", Toast.LENGTH_SHORT).show();
                    return;
                }

                //to connection me ton master ginetai se allo thread
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final Object response = MasterConnection.sendCommand(
                                MASTER_HOST,
                                MASTER_PORT,
                                "addBalance",
                                new Object[]{PLAYER_ID, amount}
                        );

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this,
                                        String.valueOf(response),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }).start();
            }
        });
    }
}
