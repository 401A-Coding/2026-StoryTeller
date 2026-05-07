package com.example.storyteller.ui.activity;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseActivity;
import com.example.storyteller.data.remote.ApiClient;
import com.example.storyteller.model.ChatMessage;
import com.example.storyteller.ui.adapter.ChatMessageAdapter;
import java.util.ArrayList;
import java.util.List;

public class StoryGenerateActivity extends BaseActivity {

    private final List<ChatMessage> messages = new ArrayList<>();
    private ChatMessageAdapter adapter;
    private RecyclerView rvChat;
    private EditText etMessage;
    private ProgressBar progressBar;  // 添加加载指示器

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
        progressBar = findViewById(R.id.progress_bar);  // 假设布局中有此ID

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

        // 显示加载指示器
        progressBar.setVisibility(View.VISIBLE);

        // 获取API密钥（示例：从SharedPreferences或常量获取，实际中请安全存储）
        String apiKey = "sk-79f5871d5122405bab1078b900346e50";  // 替换为实际获取方式

        // 调用AI生成故事
        ApiClient.getInstance().generateStory(content, apiKey, new ApiClient.Callback() {
            @Override
            public void onSuccess(String story) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    appendMessage(new ChatMessage(story, false));
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    appendMessage(new ChatMessage("生成失败: " + e.getMessage(), false));
                });
            }
        });
    }

    private void appendMessage(ChatMessage message) {
        messages.add(message);
        adapter.notifyItemInserted(messages.size() - 1);
        rvChat.scrollToPosition(messages.size() - 1);
    }
}