package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseActivity;
import com.example.storyteller.model.ChatMessage;
import com.example.storyteller.ui.adapter.ChatMessageAdapter;
import java.util.ArrayList;
import java.util.List;

public class StoryGenerateActivity extends BaseActivity {

    private final List<ChatMessage> messages = new ArrayList<>();
    private ChatMessageAdapter adapter;
    private RecyclerView rvChat;
    private EditText etMessage;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_story_generate;
    }

    @Override
    protected void initView() {
        findViewById(R.id.btn_back_home).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_TARGET_TAB, MainActivity.TAB_HOME);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        rvChat = findViewById(R.id.rv_chat);
        etMessage = findViewById(R.id.et_message);
        Button btnSend = findViewById(R.id.btn_send);

        adapter = new ChatMessageAdapter(this, messages);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage());
    }

    @Override
    protected void initData() {
        // 占位
    }

    private void sendMessage() {
        String content = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            return;
        }
        appendMessage(new ChatMessage(content, true));
        etMessage.setText("");
        appendMessage(new ChatMessage("hello world", false));
    }

    private void appendMessage(ChatMessage message) {
        messages.add(message);
        adapter.notifyItemInserted(messages.size() - 1);
        rvChat.scrollToPosition(messages.size() - 1);
    }
}
