package com.example.storyteller.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.db.StorySettingDao;
import com.example.storyteller.data.remote.ApiClient;
import com.example.storyteller.data.repository.StoryRepository;
import com.example.storyteller.data.repository.StoryRepositoryImpl;
import com.example.storyteller.model.Chapter;
import com.example.storyteller.model.ChatMessage;
import com.example.storyteller.model.Story;
import com.example.storyteller.model.StorySetting;
import com.example.storyteller.model.Volume;
import com.example.storyteller.ui.adapter.ChatMessageAdapter;
import com.example.storyteller.utils.AgentCommandExecutor;

import java.util.ArrayList;
import java.util.List;

/**
 * AI助手面板Fragment
 * 完全参考StoryGenerateActivity的实现
 */
public class AIPanelFragment extends BaseFragment {

    private static final String ARG_STORY_ID = "story_id";
    private static final String ARG_PREFILL_MESSAGE = "prefill_message";

    // UI Components
    private RecyclerView rvChat;
    private EditText etMessage;
    private ImageButton btnSend;
    private Button btnModeSelector;
    private Button btnModelSelector;
    private ProgressBar progressBar;
    private HorizontalScrollView scrollQuickActions;
    private com.google.android.material.chip.ChipGroup chipGroupQuickActions;
    private ImageView btnCloseAi;

    // Data
    private ChatMessageAdapter adapter;
    private List<ChatMessage> messages;
    private ApiClient apiClient;
    private StoryRepository storyRepository;
    private AgentCommandExecutor commandExecutor;
    private StorySettingDao settingDao;  // 设定DAO
    
    private int storyId;
    private String currentMode = "agent"; // agent 或 ask
    private String currentModel = "flash"; // flash 或 pro
    private String prefillMessage;
    private boolean hasStartedConversation = false;
    
    // Callback
    private OnCloseListener closeListener;
    private OnCommandExecutedListener commandExecutedListener;

    public interface OnCloseListener {
        void onClose();
    }
    
    public interface OnCommandExecutedListener {
        void onCommandExecuted();
    }

    public void setOnCloseListener(OnCloseListener listener) {
        this.closeListener = listener;
    }
    
    public void setOnCommandExecutedListener(OnCommandExecutedListener listener) {
        this.commandExecutedListener = listener;
    }

    public static AIPanelFragment newInstance(int storyId) {
        return newInstance(storyId, null);
    }

    public static AIPanelFragment newInstance(int storyId, String prefillMessage) {
        AIPanelFragment fragment = new AIPanelFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_STORY_ID, storyId);
        args.putString(ARG_PREFILL_MESSAGE, prefillMessage);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (getArguments() != null) {
            storyId = getArguments().getInt(ARG_STORY_ID, 0);
            prefillMessage = getArguments().getString(ARG_PREFILL_MESSAGE);
        }
        
        // 初始化
        apiClient = ApiClient.getInstance();
        storyRepository = new StoryRepositoryImpl(requireContext());
        commandExecutor = new AgentCommandExecutor(storyRepository);
        settingDao = new StorySettingDao(requireContext());  // 初始化设定DAO
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_ai_panel;
    }

    @Override
    protected void initView(View view) {
        rvChat = view.findViewById(R.id.rv_chat);
        etMessage = view.findViewById(R.id.et_message);
        btnSend = view.findViewById(R.id.btn_send);
        btnModeSelector = view.findViewById(R.id.btn_mode_selector);
        btnModelSelector = view.findViewById(R.id.btn_model_selector);
        progressBar = view.findViewById(R.id.progress_bar);
        scrollQuickActions = view.findViewById(R.id.scroll_quick_actions);
        chipGroupQuickActions = view.findViewById(R.id.chip_group_quick_actions);
        btnCloseAi = view.findViewById(R.id.btn_close_ai);
    }

    @Override
    protected void initData() {
        setupRecyclerView();
        setupListeners();
        
        // 如果有预填充消息，自动填入
        if (!TextUtils.isEmpty(prefillMessage)) {
            etMessage.setText(prefillMessage);
        }
    }

    private void setupRecyclerView() {
        messages = new ArrayList<>();
        adapter = new ChatMessageAdapter(requireContext(), messages);
        adapter.setShowWelcomeCard(true); // 显示欢迎卡片
        
        rvChat.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvChat.setAdapter(adapter);
    }

    private void setupListeners() {
        // 关闭按钮
        if (btnCloseAi != null) {
            btnCloseAi.setOnClickListener(v -> {
                if (closeListener != null) {
                    closeListener.onClose();
                }
            });
        }
        
        // 发送按钮
        btnSend.setOnClickListener(v -> sendMessage());
        
        // 模式选择
        btnModeSelector.setOnClickListener(v -> showModeSelectorPopup());
        
        // 模型选择
        btnModelSelector.setOnClickListener(v -> showModelSelectorPopup());
        
        // 回车发送
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void sendMessage() {
        String content = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            return;
        }
        
        // 用户开始对话，隐藏欢迎卡片
        if (!hasStartedConversation) {
            hasStartedConversation = true;
            adapter.setShowWelcomeCard(false);
        }
        
        appendMessage(new ChatMessage(content, true, false));
        etMessage.setText("");

        // Show loading
        progressBar.setVisibility(View.VISIBLE);

        if ("agent".equals(currentMode)) {
            // Agent mode: 使用步骤展示
            callAgentAPI(content);
        } else {
            // Ask mode: 有上下文但不执行编辑操作
            callAskAPI(content);
        }
    }

    /**
     * 添加消息到聊天列表
     */
    private void appendMessage(ChatMessage message) {
        messages.add(message);
        adapter.notifyItemInserted(messages.size() - 1);
        
        // 延迟滚动，确保布局完成
        rvChat.post(() -> {
            rvChat.smoothScrollToPosition(messages.size() - 1);
        });
        
        // 如果是 AI 回复且启用打字机效果，启动逐字显示
        if (!message.isFromUser() && message.isTyping()) {
            startTypewriterEffect(message, messages.size() - 1);
        }
    }

    /**
     * 调用 Agent API（智能体模式）
     */
    private void callAgentAPI(String userMessage) {
        // 构建小说上下文
        String context = buildStoryContext();
        
        // 创建处理中消息
        ChatMessage processingMsg = new ChatMessage(false, ChatMessage.MessageType.PROCESSING);
        final int[] messagePosition = {messages.size()};
        appendMessage(processingMsg);
        
        progressBar.setVisibility(View.GONE);
        
        // 步骤 1: 读取上下文
        processingMsg.addStep(new ChatMessage.ExecutionStep(
            "📖", "读取小说上下文", "", ChatMessage.StepStatus.RUNNING
        ));
        adapter.notifyItemChanged(messagePosition[0]);
        
        String storyContext = buildStoryContext();
        
        // 更新步骤 1 为完成
        try {
            Story story = storyRepository.getStoryById(storyId);
            List<Volume> volumes = parseVolumesFromStory(story);
            processingMsg.updateStep(0, ChatMessage.StepStatus.COMPLETED, 
                "共 " + volumes.size() + " 卷");
        } catch (Exception e) {
            processingMsg.updateStep(0, ChatMessage.StepStatus.COMPLETED, "读取完成");
        }
        adapter.notifyItemChanged(messagePosition[0]);
        
        // 步骤 2: 分析用户意图
        processingMsg.addStep(new ChatMessage.ExecutionStep(
            "🔍", "分析用户意图", "", ChatMessage.StepStatus.RUNNING
        ));
        adapter.notifyItemChanged(messagePosition[0]);
        
        apiClient.processAgentCommand(
            userMessage,
            storyContext,
            currentModel,
            requireContext(),
            new ApiClient.AgentCallback() {
                @Override
                public void onCommandReady(ApiClient.AgentCommand command) {
                    requireActivity().runOnUiThread(() -> {
                        // 更新步骤 2
                        processingMsg.updateStep(1, ChatMessage.StepStatus.COMPLETED,
                            "操作: " + command.action);
                        
                        // 步骤 3: 显示 reasoning（深度思考）
                        if (!TextUtils.isEmpty(command.reasoning)) {
                            processingMsg.addStep(new ChatMessage.ExecutionStep(
                                "💭", "深度思考", command.reasoning, 
                                ChatMessage.StepStatus.COMPLETED
                            ));
                        }
                        
                        // 如果是问答操作，直接显示答案，不显示执行步骤
                        if ("answer_question".equals(command.action)) {
                            // 尝试从 parameters 中获取答案
                            String answer = null;
                            if (command.parameters != null) {
                                // 尝试多个可能的字段名
                                answer = (String) command.parameters.get("answer");
                                if (answer == null || answer.isEmpty()) {
                                    answer = (String) command.parameters.get("response");
                                }
                                if (answer == null || answer.isEmpty()) {
                                    answer = (String) command.parameters.get("content");
                                }
                            }
                            
                            // 如果还是没有答案，使用 reasoning 作为答案
                            if (answer == null || answer.isEmpty()) {
                                answer = command.reasoning;
                            }
                            
                            // 如果仍然没有，显示默认消息
                            if (answer == null || answer.isEmpty()) {
                                answer = "已回答";
                            }
                            
                            // 标记为完成
                            processingMsg.setMessageType(ChatMessage.MessageType.COMPLETED);
                            processingMsg.setResultContent(answer);
                            
                            // 初始化 displayContent 为空，准备打字机效果
                            processingMsg.setDisplayContent("");
                            
                            // 启用打字机效果
                            processingMsg.setTyping(true);
                            adapter.notifyItemChanged(messagePosition[0]);
                            
                            // 滚动到底部
                            rvChat.post(() -> {
                                rvChat.smoothScrollToPosition(messages.size() - 1);
                            });
                            
                            // 启动打字机效果
                            startTypewriterEffect(processingMsg, messagePosition[0]);
                        } else {
                            // 非问答操作：显示执行步骤
                            processingMsg.addStep(new ChatMessage.ExecutionStep(
                                "⚙️", "执行操作", "", ChatMessage.StepStatus.RUNNING
                            ));
                            adapter.notifyItemChanged(messagePosition[0]);
                            
                            // 滚动到底部
                            rvChat.post(() -> {
                                rvChat.smoothScrollToPosition(messages.size() - 1);
                            });
                            
                            // 延迟一点让用户看到状态变化
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                AgentCommandExecutor.CommandResult result = 
                                    commandExecutor.executeCommand(command, storyId);
                                
                                // 更新步骤 4
                                processingMsg.updateStep(3, ChatMessage.StepStatus.COMPLETED,
                                    result.message);
                                
                                // 标记为完成
                                processingMsg.setMessageType(ChatMessage.MessageType.COMPLETED);
                                processingMsg.setResultContent(result.message);
                                
                                adapter.notifyItemChanged(messagePosition[0]);
                                
                                // 滚动到底部
                                rvChat.post(() -> {
                                    rvChat.smoothScrollToPosition(messages.size() - 1);
                                });
                                
                                // 如果执行成功，通知父Activity刷新UI
                                if (result.success && commandExecutedListener != null) {
                                    commandExecutedListener.onCommandExecuted();
                                }
                            }, 300); // 300ms 延迟
                        }
                    });
                }
                
                @Override
                public void onFailure(Exception e) {
                    requireActivity().runOnUiThread(() -> {
                        // 添加失败步骤
                        processingMsg.addStep(new ChatMessage.ExecutionStep(
                            "❌", "执行失败", e.getMessage(), 
                            ChatMessage.StepStatus.FAILED
                        ));
                        
                        // 标记为完成
                        processingMsg.setMessageType(ChatMessage.MessageType.COMPLETED);
                        processingMsg.setResultContent("抱歉，发生了错误：" + e.getMessage());
                        
                        // 设置重试信息
                        processingMsg.setCanRetry(true);
                        processingMsg.setOriginalUserMessage(userMessage);
                        
                        adapter.notifyItemChanged(messagePosition[0]);
                        
                        // 滚动到底部
                        rvChat.post(() -> {
                            rvChat.smoothScrollToPosition(messages.size() - 1);
                        });
                    });
                }
            }
        );
    }

    /**
     * 调用 Ask API（问答模式）
     */
    private void callAskAPI(String userMessage) {
        // 构建小说上下文
        String context = buildStoryContext();
        
        // 创建处理中消息
        ChatMessage processingMsg = new ChatMessage(false, ChatMessage.MessageType.PROCESSING);
        final int[] messagePosition = {messages.size()};
        appendMessage(processingMsg);
        
        progressBar.setVisibility(View.GONE);
        
        // 步骤 1: 读取上下文
        processingMsg.addStep(new ChatMessage.ExecutionStep(
            "📖", "读取小说上下文", "", ChatMessage.StepStatus.RUNNING
        ));
        adapter.notifyItemChanged(messagePosition[0]);
        
        // 滚动到底部
        rvChat.post(() -> {
            rvChat.smoothScrollToPosition(messages.size() - 1);
        });
        
        String storyContext = buildStoryContext();
        
        // 更新步骤 1 为完成
        try {
            Story story = storyRepository.getStoryById(storyId);
            List<Volume> volumes = parseVolumesFromStory(story);
            processingMsg.updateStep(0, ChatMessage.StepStatus.COMPLETED, 
                "共 " + volumes.size() + " 卷");
        } catch (Exception e) {
            processingMsg.updateStep(0, ChatMessage.StepStatus.COMPLETED, "读取完成");
        }
        adapter.notifyItemChanged(messagePosition[0]);
        
        // 滚动到底部
        rvChat.post(() -> {
            rvChat.smoothScrollToPosition(messages.size() - 1);
        });
        
        // 步骤 2: 分析问题
        processingMsg.addStep(new ChatMessage.ExecutionStep(
            "💭", "分析问题", "", ChatMessage.StepStatus.RUNNING
        ));
        adapter.notifyItemChanged(messagePosition[0]);
        
        // 滚动到底部
        rvChat.post(() -> {
            rvChat.smoothScrollToPosition(messages.size() - 1);
        });
        
        // 使用 processAgentCommand 获取回答
        apiClient.processAgentCommand(
            userMessage,
            storyContext,
            currentModel,
            requireContext(),
            new ApiClient.AgentCallback() {
                @Override
                public void onCommandReady(ApiClient.AgentCommand command) {
                    requireActivity().runOnUiThread(() -> {
                        // 更新步骤 2
                        processingMsg.updateStep(1, ChatMessage.StepStatus.COMPLETED,
                            "分析完成");
                        
                        // 显示 reasoning（如果有）
                        if (!TextUtils.isEmpty(command.reasoning)) {
                            processingMsg.addStep(new ChatMessage.ExecutionStep(
                                "🤔", "思考过程", command.reasoning, 
                                ChatMessage.StepStatus.COMPLETED
                            ));
                        }
                        
                        // 尝试从 parameters 中获取答案
                        String answer = null;
                        if (command.parameters != null) {
                            // 尝试多个可能的字段名
                            answer = (String) command.parameters.get("answer");
                            if (answer == null || answer.isEmpty()) {
                                answer = (String) command.parameters.get("response");
                            }
                            if (answer == null || answer.isEmpty()) {
                                answer = (String) command.parameters.get("content");
                            }
                        }
                        
                        // 如果还是没有答案，使用 reasoning 作为答案
                        if (answer == null || answer.isEmpty()) {
                            answer = command.reasoning;
                        }
                        
                        // 如果仍然没有，显示默认消息
                        if (answer == null || answer.isEmpty()) {
                            answer = "抱歉，我没有理解您的问题";
                        }
                        
                        // 标记为完成
                        processingMsg.setMessageType(ChatMessage.MessageType.COMPLETED);
                        processingMsg.setResultContent(answer);
                        
                        // 初始化 displayContent 为空，准备打字机效果
                        processingMsg.setDisplayContent("");
                        
                        // 启用打字机效果
                        processingMsg.setTyping(true);
                        adapter.notifyItemChanged(messagePosition[0]);
                        
                        // 滚动到底部
                        rvChat.post(() -> {
                            rvChat.smoothScrollToPosition(messages.size() - 1);
                        });
                        
                        // 启动打字机效果
                        startTypewriterEffect(processingMsg, messagePosition[0]);
                    });
                }
                
                @Override
                public void onFailure(Exception e) {
                    requireActivity().runOnUiThread(() -> {
                        // 添加失败步骤
                        processingMsg.addStep(new ChatMessage.ExecutionStep(
                            "❌", "回答失败", e.getMessage(), 
                            ChatMessage.StepStatus.FAILED
                        ));
                        
                        // 标记为完成
                        processingMsg.setMessageType(ChatMessage.MessageType.COMPLETED);
                        processingMsg.setResultContent("抱歉，发生了错误：" + e.getMessage());
                        
                        // 设置重试信息
                        processingMsg.setCanRetry(true);
                        processingMsg.setOriginalUserMessage(userMessage);
                        
                        adapter.notifyItemChanged(messagePosition[0]);
                        
                        // 滚动到底部
                        rvChat.post(() -> {
                            rvChat.smoothScrollToPosition(messages.size() - 1);
                        });
                    });
                }
            }
        );
    }
    
    /**
     * 启动打字机效果
     */
    private void startTypewriterEffect(ChatMessage message, int position) {
        final String fullText = !message.getResultContent().isEmpty() ? 
            message.getResultContent() : message.getContent();
        final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        final int[] currentIndex = {0};
        final int delay = 30; // 每个字符的延迟时间（毫秒）
        
        Runnable typewriterRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentIndex[0] <= fullText.length()) {
                    // 更新显示内容
                    String displayText = fullText.substring(0, currentIndex[0]);
                    message.setDisplayContent(displayText);
                    
                    // 通知适配器更新
                    adapter.notifyItemChanged(position);
                    
                    // 滚动到底部（使用 smoothScrollToPosition）
                    rvChat.smoothScrollToPosition(messages.size() - 1);
                    
                    currentIndex[0]++;
                    
                    // 继续下一个字符
                    if (currentIndex[0] <= fullText.length()) {
                        handler.postDelayed(this, delay);
                    } else {
                        // 完成打字
                        message.setTyping(false);
                    }
                }
            }
        };
        
        // 开始打字
        handler.post(typewriterRunnable);
    }

    /**
     * 构建小说上下文
     */
    private String buildStoryContext() {
        if (storyId <= 0) {
            return "未选择小说";
        }
        
        try {
            // 获取当前小说信息
            Story story = storyRepository.getStoryById(storyId);
            if (story == null) {
                return "未找到小说";
            }
            
            StringBuilder context = new StringBuilder();
            
            // 1. 小说基本信息
            context.append("【小说信息】\n");
            context.append("标题：").append(story.getTitle()).append("\n");
            if (story.getDescription() != null && !story.getDescription().isEmpty()) {
                context.append("简介：").append(story.getDescription()).append("\n");
            }
            context.append("\n");
            
            // 2. 卷章结构
            List<Volume> volumes = parseVolumesFromStory(story);
            context.append(AgentCommandExecutor.buildStoryContext(story, volumes));
            context.append("\n");
            
            // 3. 小说设定（重要！）
            List<StorySetting> settings = settingDao.getByStoryId(storyId);
            if (settings != null && !settings.isEmpty()) {
                context.append("【小说设定】\n");
                context.append("共 ").append(settings.size()).append(" 个设定\n\n");
                
                for (int i = 0; i < settings.size(); i++) {
                    StorySetting setting = settings.get(i);
                    context.append("设定 ").append(i + 1).append("：").append(setting.getTitle()).append("\n");
                    context.append("分类：").append(setting.getCategory());
                    if (setting.getSubCategory() != null && !setting.getSubCategory().isEmpty()) {
                        context.append(" · ").append(setting.getSubCategory());
                    }
                    context.append("\n");
                    
                    if (setting.getSummary() != null && !setting.getSummary().isEmpty()) {
                        context.append("摘要：").append(setting.getSummary()).append("\n");
                    }
                    
                    if (setting.getDetail() != null && !setting.getDetail().isEmpty()) {
                        context.append("详情：").append(setting.getDetail()).append("\n");
                    }
                    
                    // 标签
                    if (setting.getTags() != null && !setting.getTags().isEmpty()) {
                        try {
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            java.util.List<String> tagsList = gson.fromJson(
                                setting.getTags(),
                                new com.google.gson.reflect.TypeToken<java.util.List<String>>(){}.getType()
                            );
                            if (tagsList != null && !tagsList.isEmpty()) {
                                context.append("标签：").append(String.join(", ", tagsList)).append("\n");
                            }
                        } catch (Exception e) {
                            // 忽略解析错误
                        }
                    }
                    
                    // 别名
                    if (setting.getAliases() != null && !setting.getAliases().isEmpty()) {
                        try {
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            java.util.List<String> aliasesList = gson.fromJson(
                                setting.getAliases(),
                                new com.google.gson.reflect.TypeToken<java.util.List<String>>(){}.getType()
                            );
                            if (aliasesList != null && !aliasesList.isEmpty()) {
                                context.append("别名：").append(String.join(", ", aliasesList)).append("\n");
                            }
                        } catch (Exception e) {
                            // 忽略解析错误
                        }
                    }
                    
                    context.append("\n");
                }
            } else {
                context.append("【小说设定】\n暂无设定\n\n");
            }
            
            return context.toString();
            
        } catch (Exception e) {
            return "获取小说信息失败: " + e.getMessage();
        }
    }
    
    /**
     * 从 Story 的 structure JSON 中解析 Volume 列表
     * 包含完整的大纲字段
     */
    private List<Volume> parseVolumesFromStory(Story story) {
        List<Volume> volumes = new ArrayList<>();
        
        if (story.getStructure() == null || story.getStructure().isEmpty()) {
            return volumes;
        }
        
        try {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            com.google.gson.JsonArray volumesArray = gson.fromJson(
                story.getStructure(), 
                com.google.gson.JsonArray.class
            );
            
            for (int i = 0; i < volumesArray.size(); i++) {
                com.google.gson.JsonObject volumeObj = volumesArray.get(i).getAsJsonObject();
                
                Volume volume = new Volume();
                volume.setTitle(volumeObj.has("title") ? volumeObj.get("title").getAsString() : "未命名卷");
                
                // 解析卷大纲字段
                if (volumeObj.has("summary")) {
                    volume.setSummary(volumeObj.get("summary").getAsString());
                }
                if (volumeObj.has("targetWordCount")) {
                    volume.setTargetWordCount(volumeObj.get("targetWordCount").getAsInt());
                }
                if (volumeObj.has("targetChapterCount")) {
                    volume.setTargetChapterCount(volumeObj.get("targetChapterCount").getAsInt());
                }
                
                List<Chapter> chapters = new ArrayList<>();
                if (volumeObj.has("chapters")) {
                    com.google.gson.JsonArray chaptersArray = volumeObj.getAsJsonArray("chapters");
                    for (int j = 0; j < chaptersArray.size(); j++) {
                        com.google.gson.JsonObject chapterObj = chaptersArray.get(j).getAsJsonObject();
                        
                        Chapter chapter = new Chapter();
                        chapter.setTitle(chapterObj.has("title") ? chapterObj.get("title").getAsString() : "未命名章");
                        chapter.setContent(chapterObj.has("content") ? chapterObj.get("content").getAsString() : "");
                        
                        // 解析章节大纲字段
                        if (chapterObj.has("chapterRole")) {
                            chapter.setChapterRole(chapterObj.get("chapterRole").getAsString());
                        }
                        if (chapterObj.has("chapterSummary")) {
                            chapter.setChapterSummary(chapterObj.get("chapterSummary").getAsString());
                        }
                        if (chapterObj.has("chapterPurpose")) {
                            chapter.setChapterPurpose(chapterObj.get("chapterPurpose").getAsString());
                        }
                        if (chapterObj.has("suspenseLevel")) {
                            chapter.setSuspenseLevel(chapterObj.get("suspenseLevel").getAsFloat());
                        }
                        if (chapterObj.has("foreshadowing")) {
                            chapter.setForeshadowing(chapterObj.get("foreshadowing").getAsString());
                        }
                        if (chapterObj.has("twistLevel")) {
                            chapter.setTwistLevel(chapterObj.get("twistLevel").getAsFloat());
                        }
                        
                        // 解析拓展信息（角色、物品、位置）
                        if (chapterObj.has("involvedCharacters")) {
                            try {
                                chapter.setInvolvedCharacters(gson.fromJson(
                                    chapterObj.get("involvedCharacters"),
                                    new com.google.gson.reflect.TypeToken<List<String>>(){}.getType()
                                ));
                            } catch (Exception e) {
                                // 忽略解析错误
                            }
                        }
                        if (chapterObj.has("keyItems")) {
                            try {
                                chapter.setKeyItems(gson.fromJson(
                                    chapterObj.get("keyItems"),
                                    new com.google.gson.reflect.TypeToken<List<String>>(){}.getType()
                                ));
                            } catch (Exception e) {
                                // 忽略解析错误
                            }
                        }
                        if (chapterObj.has("sceneLocations")) {
                            try {
                                chapter.setSceneLocations(gson.fromJson(
                                    chapterObj.get("sceneLocations"),
                                    new com.google.gson.reflect.TypeToken<List<String>>(){}.getType()
                                ));
                            } catch (Exception e) {
                                // 忽略解析错误
                            }
                        }
                        if (chapterObj.has("timeConstraint")) {
                            chapter.setTimeConstraint(chapterObj.get("timeConstraint").getAsString());
                        }
                        
                        chapters.add(chapter);
                    }
                }
                
                volume.setChapters(chapters);
                volumes.add(volume);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return volumes;
    }

    /**
     * 显示模式选择弹窗
     */
    private void showModeSelectorPopup() {
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(requireContext(), btnModeSelector);
        popupMenu.getMenu().add(0, 1, 0, "Agent");
        popupMenu.getMenu().add(0, 2, 1, "Ask");
        
        // 标记当前选中的模式
        if ("agent".equals(currentMode)) {
            popupMenu.getMenu().getItem(0).setChecked(true);
        } else {
            popupMenu.getMenu().getItem(1).setChecked(true);
        }
        
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == 1) {
                currentMode = "agent";
                btnModeSelector.setText("Agent");
                Toast.makeText(requireContext(), "已启用 Agent 模式", Toast.LENGTH_SHORT).show();
                updatePlaceholder();
                return true;
            } else if (itemId == 2) {
                currentMode = "ask";
                btnModeSelector.setText("Ask");
                Toast.makeText(requireContext(), "已切换到 Ask 模式", Toast.LENGTH_SHORT).show();
                updatePlaceholder();
                return true;
            }
            return false;
        });
        
        popupMenu.show();
    }

    /**
     * 显示模型选择弹窗
     */
    private void showModelSelectorPopup() {
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(requireContext(), btnModelSelector);
        popupMenu.getMenu().add(0, 1, 0, "Flash");
        popupMenu.getMenu().add(0, 2, 1, "Pro");
        
        // 标记当前选中的模型
        if ("flash".equals(currentModel)) {
            popupMenu.getMenu().getItem(0).setChecked(true);
        } else {
            popupMenu.getMenu().getItem(1).setChecked(true);
        }
        
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == 1) {
                currentModel = "flash";
                btnModelSelector.setText("Flash");
                Toast.makeText(requireContext(), "已切换到 Flash 模型", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == 2) {
                currentModel = "pro";
                btnModelSelector.setText("Pro");
                Toast.makeText(requireContext(), "已切换到 Pro 模型", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        
        popupMenu.show();
    }

    /**
     * 根据当前模式更新placeholder
     */
    private void updatePlaceholder() {
        if ("agent".equals(currentMode)) {
            etMessage.setHint("输入指令，如：续写下一章、优化这段文字...");
        } else {
            etMessage.setHint("向我提问，如：如何写好悬疑小说？...");
        }
    }

    /**
     * 预填充消息（从外部调用）
     */
    public void prefillMessage(String message) {
        if (!TextUtils.isEmpty(message)) {
            etMessage.setText(message);
            etMessage.requestFocus();
        }
    }
}
