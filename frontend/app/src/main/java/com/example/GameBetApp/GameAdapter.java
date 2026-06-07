
package com.example.GameBetApp;

import android.content.Intent;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import GameBet.Game;

//adapter gia ListView pairnei list game object kai emfanizei sto layout item_game.xml
//allagi apo RecyclerView se ListView/BaseAdapter gia na mhn xreiazetai AndroidX kai na paizei me minSdk 8
public class GameAdapter extends BaseAdapter {

    private final List<Game> games;

    public GameAdapter(List<Game> games) {
        this.games = games;
    }

    @Override
    public int getCount() {
        return games == null ? 0 : games.size();
    }

    @Override
    public Object getItem(int position) {
        return games.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    //layout gia kathe grammh ths list
    public View getView(int position, View convertView, ViewGroup parent) {
        GameViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_game, parent, false);

            holder = new GameViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (GameViewHolder) convertView.getTag();
        }

        //pernei to game apo thn thesh
        final Game game = games.get(position);

        //vasika info games
        holder.tvGameName.setText(game.getGameName());
        holder.tvProvider.setText("Provider: " + game.getProviderName());
        holder.tvRisk.setText("Risk: " + game.getRiskLevel());
        holder.tvJackpot.setText("Jackpot: " + game.getJackpot());

        String logoName = game.getGameLogo();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            if (logoName != null && !logoName.trim().isEmpty()) {
                //painei onoma eikonas apo json
                logoName = logoName.trim();

                //an sto json einai px logos/lucky7.png, kratame mono to lucky7 gia drawable/lucky7.png
                int slashIndex = Math.max(logoName.lastIndexOf('/'), logoName.lastIndexOf('\\'));
                if (slashIndex >= 0) {
                    logoName = logoName.substring(slashIndex + 1);
                }
                int dotIndex = logoName.lastIndexOf('.');
                if (dotIndex > 0) {
                    logoName = logoName.substring(0, dotIndex);
                }

                //an upaxei eikona psaxnei sto drawable gia na vrei resource me auto to onoma
                int imageResId = parent.getContext().getResources().getIdentifier(
                        logoName,
                        "drawable",
                        parent.getContext().getPackageName()
                );

                //an thn vrei thn emfanizei alliws default
                if (imageResId != 0) {
                    holder.imgGameLogo.setImageResource(imageResId);
                } else {
                    holder.imgGameLogo.setImageResource(R.mipmap.ic_launcher);
                }
            } else {
                holder.imgGameLogo.setImageResource(R.mipmap.ic_launcher);
            }
        }

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), GameDetailsActivity.class);
                intent.putExtra("game", game);
                v.getContext().startActivity(intent);
            }
        });

        return convertView;
    }

    //krataei anafores gia ta views tou item_game.xml gia na mhn ginontai sunexeia findViewById
    static class GameViewHolder {
        TextView tvGameName, tvProvider, tvRisk, tvJackpot;
        ImageView imgGameLogo;

        public GameViewHolder(View itemView) {
            imgGameLogo = itemView.findViewById(R.id.imgGameLogo);
            tvGameName = itemView.findViewById(R.id.tvGameName);
            tvProvider = itemView.findViewById(R.id.tvProvider);
            tvRisk = itemView.findViewById(R.id.tvRisk);
            tvJackpot = itemView.findViewById(R.id.tvJackpot);
        }
    }
}
