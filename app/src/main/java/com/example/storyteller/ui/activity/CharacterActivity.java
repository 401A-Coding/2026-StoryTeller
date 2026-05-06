package com.example.storyteller.ui.activity;

import android.content.Intent;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseActivity;
import com.example.storyteller.model.Character;
import com.example.storyteller.ui.adapter.CharacterAdapter;
import java.util.ArrayList;
import java.util.List;

public class CharacterActivity extends BaseActivity {

    @Override
    protected int getLayoutId() {
        return R.layout.activity_character;
    }

    @Override
    protected void initView() {
        findViewById(R.id.btn_back_home).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_TARGET_TAB, MainActivity.TAB_HOME);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
    }

    @Override
    protected void initData() {
        RecyclerView recyclerView = findViewById(R.id.rv_character_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new CharacterAdapter(this, getPlaceholderCharacters()));
    }

    private List<Character> getPlaceholderCharacters() {
        List<Character> list = new ArrayList<>();
        list.add(new Character(1, "林若曦", "身份：故事主角 / 冒险者", 0));
        list.add(new Character(1, "顾言", "身份：搭档 / 战术分析师", 0));
        list.add(new Character(1, "苏浅", "身份：神秘向导 / 情报提供者", 0));
        list.add(new Character(1, "阿烈", "身份：守护者 / 骑士", 0));
        list.add(new Character(1, "纪衡", "身份：反派 / 阴影掌控者", 0));
        return list;
    }
}
