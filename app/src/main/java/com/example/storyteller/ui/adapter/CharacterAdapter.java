package com.example.storyteller.ui.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.storyteller.R;
import com.example.storyteller.model.Character;
import java.util.ArrayList;
import java.util.List;

public class CharacterAdapter extends RecyclerView.Adapter<CharacterAdapter.CharacterViewHolder> {

    private final Context context;
    private List<Character> characters;
    private int expandedPosition = RecyclerView.NO_POSITION;

    public interface Listener {
        void onRegenerateCharacter(@NonNull Character character, int position);
        void onDeleteCharacter(@NonNull Character character, int position);
    }

    private Listener listener;

    public CharacterAdapter(Context context, List<Character> characters) {
        this.context = context;
        this.characters = characters;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
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

        String detail = character.getDetail();
        if (TextUtils.isEmpty(detail)) {
            detail = "暂无详细介绍";
        }
        holder.tvDetail.setText(detail);

        boolean expanded = position == expandedPosition;
        holder.tvDetail.setVisibility(expanded ? View.VISIBLE : View.GONE);
        holder.btnRegenerate.setVisibility(expanded ? View.VISIBLE : View.GONE);
        holder.btnDelete.setVisibility(expanded ? View.VISIBLE : View.GONE);

        holder.btnRegenerate.setOnClickListener(v -> {
            if (listener != null) {
                int adapterPosition = holder.getBindingAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    listener.onRegenerateCharacter(characters.get(adapterPosition), adapterPosition);
                }
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                int adapterPosition = holder.getBindingAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    listener.onDeleteCharacter(characters.get(adapterPosition), adapterPosition);
                }
            }
        });

        holder.itemView.setOnClickListener(v -> {
            int previous = expandedPosition;
            expandedPosition = expanded ? RecyclerView.NO_POSITION : holder.getBindingAdapterPosition();
            if (previous != RecyclerView.NO_POSITION) {
                notifyItemChanged(previous);
            }
            if (expandedPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(expandedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return characters == null ? 0 : characters.size();
    }

    public void setData(List<Character> list) {
        this.characters = list;
        this.expandedPosition = RecyclerView.NO_POSITION;
        notifyDataSetChanged();
    }

    public List<Character> getDataSnapshot() {
        return characters == null ? new ArrayList<>() : new ArrayList<>(characters);
    }

    public void updateItem(int position, Character character) {
        if (characters == null || position < 0 || position >= characters.size()) {
            return;
        }
        characters.set(position, character);
        notifyItemChanged(position);
    }

    public void removeItem(int position) {
        if (characters == null || position < 0 || position >= characters.size()) {
            return;
        }
        characters.remove(position);
        if (expandedPosition == position) {
            expandedPosition = RecyclerView.NO_POSITION;
        } else if (expandedPosition > position) {
            expandedPosition--;
        }
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, characters.size() - position);
    }

    public static class CharacterViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvRole;
        final TextView tvDetail;
        final Button btnRegenerate;
        final Button btnDelete;

        CharacterViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_character_name);
            tvRole = itemView.findViewById(R.id.tv_character_role);
            tvDetail = itemView.findViewById(R.id.tv_character_detail);
            btnRegenerate = itemView.findViewById(R.id.btn_character_regenerate);
            btnDelete = itemView.findViewById(R.id.btn_character_delete);
        }
    }
}

