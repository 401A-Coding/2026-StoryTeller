package com.example.storyteller.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.storyteller.R;
import com.example.storyteller.model.Character;
import java.util.List;

public class CharacterAdapter extends RecyclerView.Adapter<CharacterAdapter.CharacterViewHolder> {

    private final Context context;
    private final List<Character> characters;

    public CharacterAdapter(Context context, List<Character> characters) {
        this.context = context;
        this.characters = characters;
    }

    @NonNull
    @Override
    public CharacterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_character, parent, false);
        return new CharacterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CharacterViewHolder holder, int position) {
        Character character = characters.get(position);
        holder.tvName.setText(character.getName());
        holder.tvRole.setText(character.getProfile());
    }

    @Override
    public int getItemCount() {
        return characters == null ? 0 : characters.size();
    }

    public static class CharacterViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvRole;

        CharacterViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_character_name);
            tvRole = itemView.findViewById(R.id.tv_character_role);
        }
    }
}

