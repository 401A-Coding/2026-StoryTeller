package com.example.storyteller.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.db.StoryDocumentDao;
import com.example.storyteller.data.local.db.StorySettingDao;
import com.example.storyteller.data.remote.ApiClient;
import com.example.storyteller.data.remote.ModelConfig;
import com.example.storyteller.data.repository.StoryRepository;
import com.example.storyteller.data.repository.StoryRepositoryImpl;
import com.example.storyteller.model.Chapter;
import com.example.storyteller.model.ChatMessage;
import com.example.storyteller.model.Story;
import com.example.storyteller.model.StoryDocument;
import com.example.storyteller.model.StorySetting;
import com.example.storyteller.model.Volume;
import com.example.storyteller.ui.adapter.ChatMessageAdapter;
import com.example.storyteller.ui.dialog.WritingPreferenceDialog;
import com.example.storyteller.utils.AgentCommandExecutor;
import com.example.storyteller.utils.ConversationMemory;
import com.example.storyteller.utils.PromptManager;

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
    private ImageButton btnAgentSettings;  // Agent模式设置按钮
    private ProgressBar progressBar;
    private HorizontalScrollView scrollQuickActions;
    private com.google.android.material.chip.ChipGroup chipGroupQuickActions;
    private ImageView btnCloseAi;
    private ImageView btnAiSettings;  // AI设置按钮

    // Data
    private ChatMessageAdapter adapter;
    private List<ChatMessage> messages;
    private ApiClient apiClient;
    private StoryRepository storyRepository;
    private AgentCommandExecutor commandExecutor;
    private StorySettingDao settingDao;  // 设定DAO
    private PromptManager promptManager;  // Prompt管理器
    private ConversationMemory conversationMemory;  // 短期记忆
    
    private int storyId;
    private String currentMode = "editor"; // editor/setting/outline/document/ask/review
    private String currentModel = ModelConfig.DEFAULT_MODEL;
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
        commandExecutor = new AgentCommandExecutor(storyRepository, requireContext());  // 传入Context
        settingDao = new StorySettingDao(requireContext());  // 初始化设定DAO
        promptManager = new PromptManager(requireContext());  // 初始化Prompt管理器
        conversationMemory = new ConversationMemory();  // 初始化短期记忆
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
        btnAgentSettings = view.findViewById(R.id.btn_agent_settings);  // Agent模式设置按钮
        progressBar = view.findViewById(R.id.progress_bar);
        scrollQuickActions = view.findViewById(R.id.scroll_quick_actions);
        chipGroupQuickActions = view.findViewById(R.id.chip_group_quick_actions);
        btnCloseAi = view.findViewById(R.id.btn_close_ai);
        btnAiSettings = view.findViewById(R.id.btn_ai_settings);  // AI设置按钮
    }

    @Override
    protected void initData() {
        setupRecyclerView();
        setupListeners();
        
        // 初始化Agent设置按钮可见性
        updateAgentSettingsButtonVisibility();
        
        // 显示AI设置按钮（始终显示）
        if (btnAiSettings != null) {
            btnAiSettings.setVisibility(View.VISIBLE);
        }
        
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
        
        // 设置按钮
        if (btnAiSettings != null) {
            btnAiSettings.setOnClickListener(v -> showAiSettingsPopup());
        }
        
        // 发送按钮
        btnSend.setOnClickListener(v -> sendMessage());
        
        // 模式选择（Ask/Agent）
        btnModeSelector.setOnClickListener(v -> showTopModeSelectorPopup());
        
        // 模型选择
        btnModelSelector.setOnClickListener(v -> showModelSelectorPopup());
        
        // Agent模式设置按钮
        if (btnAgentSettings != null) {
            btnAgentSettings.setOnClickListener(v -> showAgentSubModePopup());
        }
        
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

        if ("editor".equals(currentMode) || "setting".equals(currentMode) || 
            "outline".equals(currentMode) || "document".equals(currentMode) || "review".equals(currentMode)) {
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
        
        // 添加到短期记忆
        if (conversationMemory != null) {
            conversationMemory.addMessage(message);
        }
        
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
        
        // 根据当前模式选择对应的 System Prompt
        String systemPrompt = getSystemPromptForMode(currentMode);
        
        // 使用自定义 System Prompt 调用 API
        apiClient.processAgentCommandWithSystemPrompt(
            userMessage,
            storyContext,
            currentModel,
            systemPrompt,
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
                        } else if ("review_report".equals(command.action)) {
                            // 全篇审核：走执行命令流程获取格式化结果，添加打字机效果
                            processingMsg.addStep(new ChatMessage.ExecutionStep(
                                "⚙️", "生成报告", "", ChatMessage.StepStatus.RUNNING
                            ));
                            adapter.notifyItemChanged(messagePosition[0]);
                            
                            rvChat.post(() -> {
                                rvChat.smoothScrollToPosition(messages.size() - 1);
                            });
                            
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                AgentCommandExecutor.CommandResult result = 
                                    commandExecutor.executeCommand(command, storyId);
                                
                                // 更新最后添加的步骤（生成报告）
                                processingMsg.updateStep(processingMsg.getSteps().size() - 1, 
                                    ChatMessage.StepStatus.COMPLETED, "报告生成完成");
                                
                                // 标记为完成
                                processingMsg.setMessageType(ChatMessage.MessageType.COMPLETED);
                                processingMsg.setResultContent(result.message);
                                processingMsg.setDisplayContent("");
                                processingMsg.setTyping(true);
                                
                                adapter.notifyItemChanged(messagePosition[0]);
                                
                                rvChat.post(() -> {
                                    rvChat.smoothScrollToPosition(messages.size() - 1);
                                });
                                
                                startTypewriterEffect(processingMsg, messagePosition[0]);
                                
                                // 如果执行成功，通知父Activity刷新UI
                                if (result.success && commandExecutedListener != null) {
                                    commandExecutedListener.onCommandExecuted();
                                }
                            }, 300);
                        } else if ("review_aspect".equals(command.action)) {
                            // 定向审核：走执行命令流程获取格式化结果，添加打字机效果
                            processingMsg.addStep(new ChatMessage.ExecutionStep(
                                "⚙️", "生成报告", "", ChatMessage.StepStatus.RUNNING
                            ));
                            adapter.notifyItemChanged(messagePosition[0]);
                            
                            rvChat.post(() -> {
                                rvChat.smoothScrollToPosition(messages.size() - 1);
                            });
                            
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                AgentCommandExecutor.CommandResult result = 
                                    commandExecutor.executeCommand(command, storyId);
                                
                                // 更新最后添加的步骤（生成报告）
                                processingMsg.updateStep(processingMsg.getSteps().size() - 1, 
                                    ChatMessage.StepStatus.COMPLETED, "报告生成完成");
                                
                                // 标记为完成
                                processingMsg.setMessageType(ChatMessage.MessageType.COMPLETED);
                                processingMsg.setResultContent(result.message);
                                processingMsg.setDisplayContent("");
                                processingMsg.setTyping(true);
                                
                                adapter.notifyItemChanged(messagePosition[0]);
                                
                                rvChat.post(() -> {
                                    rvChat.smoothScrollToPosition(messages.size() - 1);
                                });
                                
                                startTypewriterEffect(processingMsg, messagePosition[0]);
                            }, 300);
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
        
        // 使用自定义 System Prompt（consultant）获取回答
        String systemPrompt = getSystemPromptForMode("ask");
        apiClient.processAgentCommandWithSystemPrompt(
            userMessage,
            storyContext,
            currentModel,
            systemPrompt,
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
                    
                    // 禁用自动滚动，避免抖动
                    // rvChat.smoothScrollToPosition(messages.size() - 1);
                    
                    currentIndex[0]++;
                    
                    // 继续下一个字符
                    if (currentIndex[0] <= fullText.length()) {
                        handler.postDelayed(this, delay);
                    } else {
                        // 完成打字后滚动到底部
                        message.setTyping(false);
                        rvChat.post(() -> {
                            rvChat.smoothScrollToPosition(messages.size() - 1);
                        });
                    }
                }
            }
        };
        
        // 开始打字
        handler.post(typewriterRunnable);
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
     * 显示顶层模式选择弹窗（Ask/Agent）
     */
    private void showTopModeSelectorPopup() {
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(requireContext(), btnModeSelector);
        popupMenu.getMenu().add(0, 1, 0, "问答");
        popupMenu.getMenu().add(0, 2, 1, "智能体");
        
        // 标记当前选中的模式
        if ("ask".equals(currentMode)) {
            popupMenu.getMenu().getItem(0).setChecked(true);
        } else {
            popupMenu.getMenu().getItem(1).setChecked(true);
        }
        
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == 1) {
                // 切换到Ask模式
                setMode("ask");
                btnModeSelector.setText("问答");
                updateAgentSettingsButtonVisibility();
                Toast.makeText(requireContext(), "已切换到问答模式", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == 2) {
                // 切换到Agent模式（默认为editor）
                setMode("editor");
                btnModeSelector.setText("智能体");
                updateAgentSettingsButtonVisibility();
                Toast.makeText(requireContext(), "已切换到智能体模式", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        
        popupMenu.show();
    }
    
    /**
     * 显示Agent子模式选择弹窗（写作/设定/大纲/文档）
     */
    private void showAgentSubModePopup() {
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(requireContext(), btnAgentSettings);
        popupMenu.getMenu().add(0, 1, 0, "写作");
        popupMenu.getMenu().add(0, 2, 1, "设定");
        popupMenu.getMenu().add(0, 3, 2, "大纲");
        popupMenu.getMenu().add(0, 4, 3, "文档");
        popupMenu.getMenu().add(0, 5, 4, "审核");
        
        // 标记当前选中的模式
        int selectedIndex = 0;
        switch (currentMode) {
            case "editor": selectedIndex = 0; break;
            case "setting": selectedIndex = 1; break;
            case "outline": selectedIndex = 2; break;
            case "document": selectedIndex = 3; break;
            case "review": selectedIndex = 4; break;
        }
        popupMenu.getMenu().getItem(selectedIndex).setChecked(true);
        
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            switch (itemId) {
                case 1:
                    setMode("editor");
                    Toast.makeText(requireContext(), "已切换到写作模式", Toast.LENGTH_SHORT).show();
                    return true;
                case 2:
                    setMode("setting");
                    Toast.makeText(requireContext(), "已切换到设定模式", Toast.LENGTH_SHORT).show();
                    return true;
                case 3:
                    setMode("outline");
                    Toast.makeText(requireContext(), "已切换到大纲模式", Toast.LENGTH_SHORT).show();
                    return true;
                case 4:
                    setMode("document");
                    Toast.makeText(requireContext(), "已切换到文档模式", Toast.LENGTH_SHORT).show();
                    return true;
                case 5:
                    setMode("review");
                    Toast.makeText(requireContext(), "已切换到审核模式", Toast.LENGTH_SHORT).show();
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
        
        // 获取所有可用模型（仅已启用的提供商）
        java.util.List<ModelConfig.ModelInfo> allModels = ModelConfig.getAllModels();
        java.util.List<ModelConfig.ModelInfo> enabledModels = new java.util.ArrayList<>();
        for (ModelConfig.ModelInfo model : allModels) {
            if (ModelConfig.isProviderEnabled(requireContext(), model.provider)) {
                enabledModels.add(model);
            }
        }
        
        final java.util.Map<Integer, String> itemIdToModelId = new java.util.HashMap<>();
        
        for (int i = 0; i < enabledModels.size(); i++) {
            ModelConfig.ModelInfo model = enabledModels.get(i);
            popupMenu.getMenu().add(0, i + 1, i, model.fullName); // 显示全称
            itemIdToModelId.put(i + 1, model.modelId);
        }
        
        // 标记当前选中的模型
        for (int i = 0; i < enabledModels.size(); i++) {
            if (enabledModels.get(i).modelId.equals(currentModel)) {
                popupMenu.getMenu().getItem(i).setChecked(true);
                break;
            }
        }
        
        popupMenu.setOnMenuItemClickListener(item -> {
            String modelId = itemIdToModelId.get(item.getItemId());
            if (modelId != null) {
                currentModel = modelId;
                ModelConfig.ModelInfo model = ModelConfig.getModelInfo(modelId);
                String displayName = model != null ? model.displayName : modelId; // 按钮显示简称
                btnModelSelector.setText(displayName);
                Toast.makeText(requireContext(), "已切换到 " + displayName, Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        
        popupMenu.show();
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
    
    /**
     * 根据当前模式获取对应的 System Prompt
     * @param mode 模式名称：editor, setting, outline, document, ask
     * @return System Prompt 文本
     */
    private String getSystemPromptForMode(String mode) {
        switch (mode) {
            case "setting":
                return promptManager.getAgentSystemPrompt("setting", null);
            case "outline":
                return promptManager.getAgentSystemPrompt("outline", null);
            case "document":
                return promptManager.getAgentSystemPrompt("document", null);
            case "review":
                return promptManager.getAgentSystemPrompt("review", null);
            case "ask":
                return promptManager.getAgentSystemPrompt("consultant", null);
            case "editor":
            default:
                // 默认使用编辑助手 prompt
                return promptManager.getAgentSystemPrompt("editor", null);
        }
    }
    
    /**
     * 公开方法：设置AI模式
     * @param mode 模式名称：editor/setting/outline/document/ask/review
     */
    public void setMode(String mode) {
        this.currentMode = mode;
        updatePlaceholder();
        updateAgentSettingsButtonVisibility();
        android.util.Log.d("AIPanelFragment", "AI模式已切换为: " + mode);
    }
    
    /**
     * 更新Agent设置按钮的可见性
     * - Ask模式：隐藏
     * - Agent模式（editor/setting/outline/document/review）：显示
     */
    private void updateAgentSettingsButtonVisibility() {
        if (btnAgentSettings == null) return;
        
        if ("ask".equals(currentMode)) {
            btnAgentSettings.setVisibility(View.GONE);
        } else {
            btnAgentSettings.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 根据当前模式更新placeholder
     */
    private void updatePlaceholder() {
        if (etMessage == null) return;
        
        switch (currentMode) {
            case "setting":
                etMessage.setHint("输入指令，如：创建一个新角色、添加世界观设定...");
                break;
            case "outline":
                etMessage.setHint("输入指令，如：生成第一卷大纲、优化章节作用...");
                break;
            case "document":
                etMessage.setHint("输入指令，如：添加参考文档、从文档提取角色...");
                break;
            case "review":
                etMessage.setHint("输入指令，如：审核当前剧情、检查一致性...");
                break;
            case "ask":
                etMessage.setHint("向我提问，如：如何写好悬疑小说？...");
                break;
            case "editor":
            default:
                etMessage.setHint("输入指令，如：续写下一章、优化这段文字...");
                break;
        }
    }
    
    /**
     * 构建小说上下文（根据不同模式提供不同信息）
     */
    private String buildStoryContext() {
        if (storyId <= 0) {
            return "未选择小说";
        }
        
        try {
            Story story = storyRepository.getStoryById(storyId);
            if (story == null) {
                return "未找到小说";
            }
            
            StringBuilder context = new StringBuilder();
            
            // 基础信息（所有模式都需要）
            context.append("【小说信息】\n");
            context.append("标题：").append(story.getTitle()).append("\n");
            if (!TextUtils.isEmpty(story.getDescription())) {
                context.append("简介：").append(story.getDescription()).append("\n");
            }
            context.append("\n");
            
            // 根据模式添加特定上下文
            switch (currentMode) {
                case "setting":
                    appendSettingContext(context, story);
                    break;
                case "outline":
                    appendOutlineContext(context, story);
                    break;
                case "document":
                    appendDocumentContext(context, story);
                    break;
                case "review":
                    appendReviewContext(context, story);
                    break;
                case "editor":
                default:
                    // 写作模式：使用原有的buildStoryContext逻辑
                    List<Volume> volumes = parseVolumesFromStory(story);
                    context.append(AgentCommandExecutor.buildStoryContext(story, volumes));
                    break;
            }
            
            // 添加短期记忆（对话历史）
            if (conversationMemory != null && conversationMemory.hasHistory()) {
                context.append(conversationMemory.buildContextSummary());
            }
            
            // 添加用户写作偏好
            appendUserPreferenceContext(context);
            
            // 添加AI记忆
            appendAiMemoryContext(context);
            
            return context.toString();
            
        } catch (Exception e) {
            return "获取小说信息失败: " + e.getMessage();
        }
    }
    
    /**
     * 添加用户写作偏好到上下文中
     */
    private void appendUserPreferenceContext(StringBuilder context) {
        try {
            com.example.storyteller.utils.PreferenceManager prefManager = 
                com.example.storyteller.utils.PreferenceManager.getInstance(requireContext());
            com.example.storyteller.model.UserWritingPreference preference;
            
            if (storyId > 0) {
                // 获取合并后的偏好（小说专属优先，否则使用全局）
                preference = prefManager.getMergedPreference(storyId);
            } else {
                preference = prefManager.getGlobalPreference();
            }
            
            if (preference.hasAnyPreference()) {
                context.append(preference.buildPreferenceDescription());
            }
        } catch (Exception e) {
            // 忽略偏好加载错误
            android.util.Log.e("AIPanelFragment", "Failed to load user preferences", e);
        }
    }
    
    /**
     * 添加AI记忆到上下文中
     */
    private void appendAiMemoryContext(StringBuilder context) {
        try {
            com.example.storyteller.utils.AiMemoryManager memoryManager = 
                com.example.storyteller.utils.AiMemoryManager.getInstance(requireContext());
            
            if (storyId > 0) {
                String memoryContext = memoryManager.buildMemoryContext(storyId);
                if (!memoryContext.isEmpty()) {
                    context.append(memoryContext);
                }
            }
        } catch (Exception e) {
            // 忽略记忆加载错误
            android.util.Log.e("AIPanelFragment", "Failed to load AI memory", e);
        }
    }
    
    /**
     * 添加设定模式的上下文
     */
    private void appendSettingContext(StringBuilder context, Story story) {
        List<StorySetting> settings = settingDao.getByStoryId(storyId);
        if (settings != null && !settings.isEmpty()) {
            context.append("【现有设定】共").append(settings.size()).append("个\n\n");
            
            // 按category分组
            java.util.Map<String, List<StorySetting>> grouped = new java.util.HashMap<>();
            for (StorySetting s : settings) {
                String key = s.getCategory() + " > " + s.getSubCategory();
                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
            }
            
            for (java.util.Map.Entry<String, List<StorySetting>> entry : grouped.entrySet()) {
                context.append("📂 ").append(entry.getKey()).append("\n");
                for (StorySetting s : entry.getValue()) {
                    context.append("  - ").append(s.getTitle());
                    if (!TextUtils.isEmpty(s.getSummary())) {
                        context.append(": ").append(s.getSummary());
                    }
                    context.append("\n");
                }
                context.append("\n");
            }
        } else {
            context.append("【现有设定】暂无设定\n\n");
        }
    }
    
    /**
     * 添加大纲模式的上下文
     */
    private void appendOutlineContext(StringBuilder context, Story story) {
        // 全局大纲
        context.append("【全局大纲】\n");
        String globalOutline = story.getGlobalOutline();
        if (!TextUtils.isEmpty(globalOutline)) {
            context.append(globalOutline).append("\n\n");
        } else {
            context.append("暂无全局大纲\n\n");
        }
        
        // 卷章大纲结构
        List<Volume> volumes = parseVolumesFromStory(story);
        context.append("【卷章大纲结构】\n");
        context.append("共 ").append(volumes.size()).append(" 卷\n\n");
        
        for (int i = 0; i < volumes.size(); i++) {
            Volume v = volumes.get(i);
            context.append("📖 卷").append(i+1).append(": ").append(v.getTitle()).append("\n");
            
            // 卷纲信息
            if (!TextUtils.isEmpty(v.getSummary())) {
                context.append("  摘要: ").append(v.getSummary()).append("\n");
            }
            if (v.getTargetWordCount() > 0) {
                context.append("  目标字数: ").append(v.getTargetWordCount()).append("\n");
            }
            if (v.getTargetChapterCount() > 0) {
                context.append("  目标章节: ").append(v.getTargetChapterCount()).append("\n");
            }
            
            // 章节大纲
            List<Chapter> chapters = v.getChapters();
            if (chapters != null && !chapters.isEmpty()) {
                context.append("  共 ").append(chapters.size()).append(" 章\n\n");
                
                for (int j = 0; j < chapters.size(); j++) {
                    Chapter c = chapters.get(j);
                    context.append("  📄 章").append(j+1).append(": ").append(c.getTitle()).append("\n");
                    
                    if (!TextUtils.isEmpty(c.getChapterRole())) {
                        context.append("    作用: ").append(c.getChapterRole()).append("\n");
                    }
                    if (!TextUtils.isEmpty(c.getChapterSummary())) {
                        context.append("    摘要: ").append(c.getChapterSummary()).append("\n");
                    }
                    if (!TextUtils.isEmpty(c.getForeshadowing())) {
                        context.append("    伏笔: ").append(c.getForeshadowing()).append("\n");
                    }
                    if (c.getSuspenseLevel() > 0) {
                        context.append("    悬念: ").append(c.getSuspenseLevel()).append("/10\n");
                    }
                    if (c.getTwistLevel() > 0) {
                        context.append("    转折: ").append(c.getTwistLevel()).append("/5\n");
                    }
                    
                    // 拓展信息
                    if (c.getInvolvedCharacters() != null && !c.getInvolvedCharacters().isEmpty()) {
                        context.append("    角色: ").append(String.join(", ", c.getInvolvedCharacters())).append("\n");
                    }
                    if (c.getKeyItems() != null && !c.getKeyItems().isEmpty()) {
                        context.append("    物品: ").append(String.join(", ", c.getKeyItems())).append("\n");
                    }
                    if (c.getSceneLocations() != null && !c.getSceneLocations().isEmpty()) {
                        context.append("    场景: ").append(String.join(", ", c.getSceneLocations())).append("\n");
                    }
                    
                    context.append("\n");
                }
            } else {
                context.append("  暂无章节\n\n");
            }
        }
        
        // 伏笔汇总
        context.append("【伏笔追踪】\n");
        List<String> foreshadowings = new ArrayList<>();
        for (Volume v : volumes) {
            if (v.getChapters() != null) {
                for (Chapter c : v.getChapters()) {
                    if (!TextUtils.isEmpty(c.getForeshadowing())) {
                        foreshadowings.add("卷" + (volumes.indexOf(v)+1) + 
                                         "章" + (v.getChapters().indexOf(c)+1) + ": " + c.getForeshadowing());
                    }
                }
            }
        }
        if (!foreshadowings.isEmpty()) {
            for (String f : foreshadowings) {
                context.append("- ").append(f).append("\n");
            }
        } else {
            context.append("暂无伏笔记录\n");
        }
    }
    
    /**
     * 添加文档模式的上下文
     */
    private void appendDocumentContext(StringBuilder context, Story story) {
        StoryDocumentDao documentDao = new StoryDocumentDao(requireContext());
        List<StoryDocument> documents = documentDao.getDocumentsByStory(storyId);
        
        context.append("【现有文档】共").append(documents.size()).append("个\n\n");
        
        if (!documents.isEmpty()) {
            // 按分类分组
            java.util.Map<String, List<StoryDocument>> grouped = new java.util.HashMap<>();
            for (StoryDocument doc : documents) {
                String category = doc.getCategoryDisplayName();
                grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(doc);
            }
            
            for (java.util.Map.Entry<String, List<StoryDocument>> entry : grouped.entrySet()) {
                context.append("📁 ").append(entry.getKey()).append("\n");
                for (StoryDocument doc : entry.getValue()) {
                    context.append("  - ").append(doc.getTitle()).append("\n");
                    // 显示前100字符预览
                    String preview = doc.getContent();
                    if (preview.length() > 100) {
                        preview = preview.substring(0, 100) + "...";
                    }
                    context.append("    预览: ").append(preview).append("\n");
                }
                context.append("\n");
            }
        } else {
            context.append("暂无文档\n\n");
        }
    }
    
    /**
     * 添加审核模式的上下文
     * 需要提供完整的大纲、设定和正文内容，以便AI进行全面审核
     */
    private void appendReviewContext(StringBuilder context, Story story) {
        // 全局大纲
        context.append("【全局大纲】\n");
        String globalOutline = story.getGlobalOutline();
        if (!TextUtils.isEmpty(globalOutline)) {
            context.append(globalOutline).append("\n\n");
        } else {
            context.append("暂无全局大纲\n\n");
        }
        
        // 卷章大纲结构
        List<Volume> volumes = parseVolumesFromStory(story);
        context.append("【卷章大纲结构】\n");
        context.append("共 ").append(volumes.size()).append(" 卷\n\n");
        
        for (int i = 0; i < volumes.size(); i++) {
            Volume v = volumes.get(i);
            context.append("📖 卷").append(i+1).append(": ").append(v.getTitle()).append("\n");
            
            // 卷纲信息
            if (!TextUtils.isEmpty(v.getSummary())) {
                context.append("  摘要: ").append(v.getSummary()).append("\n");
            }
            
            // 章节大纲
            List<Chapter> chapters = v.getChapters();
            if (chapters != null && !chapters.isEmpty()) {
                context.append("  共 ").append(chapters.size()).append(" 章\n\n");
                
                for (int j = 0; j < chapters.size(); j++) {
                    Chapter c = chapters.get(j);
                    context.append("  📄 章").append(j+1).append(": ").append(c.getTitle()).append("\n");
                    
                    if (!TextUtils.isEmpty(c.getChapterRole())) {
                        context.append("    作用: ").append(c.getChapterRole()).append("\n");
                    }
                    if (!TextUtils.isEmpty(c.getChapterSummary())) {
                        context.append("    摘要: ").append(c.getChapterSummary()).append("\n");
                    }
                    if (!TextUtils.isEmpty(c.getForeshadowing())) {
                        context.append("    伏笔: ").append(c.getForeshadowing()).append("\n");
                    }
                    
                    // 拓展信息
                    if (c.getInvolvedCharacters() != null && !c.getInvolvedCharacters().isEmpty()) {
                        context.append("    角色: ").append(String.join(", ", c.getInvolvedCharacters())).append("\n");
                    }
                    if (c.getKeyItems() != null && !c.getKeyItems().isEmpty()) {
                        context.append("    物品: ").append(String.join(", ", c.getKeyItems())).append("\n");
                    }
                    if (c.getSceneLocations() != null && !c.getSceneLocations().isEmpty()) {
                        context.append("    场景: ").append(String.join(", ", c.getSceneLocations())).append("\n");
                    }
                    
                    context.append("\n");
                }
            } else {
                context.append("  暂无章节\n\n");
            }
        }
        
        // 现有设定
        List<StorySetting> settings = settingDao.getByStoryId(storyId);
        if (settings != null && !settings.isEmpty()) {
            context.append("【现有设定】共").append(settings.size()).append("个\n\n");
            
            // 按category分组
            java.util.Map<String, List<StorySetting>> grouped = new java.util.HashMap<>();
            for (StorySetting s : settings) {
                String key = s.getCategory() + " > " + s.getSubCategory();
                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
            }
            
            for (java.util.Map.Entry<String, List<StorySetting>> entry : grouped.entrySet()) {
                context.append("📂 ").append(entry.getKey()).append("\n");
                for (StorySetting s : entry.getValue()) {
                    context.append("  - ").append(s.getTitle());
                    if (!TextUtils.isEmpty(s.getSummary())) {
                        context.append(": ").append(s.getSummary());
                    }
                    context.append("\n");
                }
                context.append("\n");
            }
        } else {
            context.append("【现有设定】暂无设定\n\n");
        }
        
        // 正文内容（用于审核实际写作）
        if (!TextUtils.isEmpty(story.getContent())) {
            context.append("【正文内容】\n");
            // 限制长度，避免超出token限制
            String content = story.getContent();
            if (content.length() > 3000) {
                content = content.substring(0, 3000) + "...（内容过长，已截断）";
            }
            context.append(content).append("\n\n");
        } else {
            context.append("【正文内容】暂无正文\n\n");
        }
    }

    /**
     * 显示AI设置菜单
     */
    private void showAiSettingsPopup() {
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(requireContext(), btnAiSettings);
        popupMenu.getMenu().add(0, 1, 0, "写作偏好");
        popupMenu.getMenu().add(0, 2, 1, "AI记忆管理");
        
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            switch (itemId) {
                case 1:
                    // 打开写作偏好设置
                    showWritingPreferenceDialog();
                    return true;
                case 2:
                    // 打开AI记忆管理
                    showAiMemoryDialog();
                    return true;
            }
            return false;
        });
        
        popupMenu.show();
    }
    

    /**
     * 显示写作偏好设置对话框
     */
    private void showWritingPreferenceDialog() {
        WritingPreferenceDialog dialog;
        
        // 获取对话历史
        String conversationHistory = "";
        if (conversationMemory != null && conversationMemory.hasHistory()) {
            conversationHistory = conversationMemory.buildContextSummary();
        }
        
        if (storyId > 0) {
            // 获取小说标题
            String storyTitle = "";
            try {
                com.example.storyteller.data.repository.StoryRepository repo = 
                    new com.example.storyteller.data.repository.StoryRepositoryImpl(requireContext());
                com.example.storyteller.model.Story story = repo.getStoryById(storyId);
                if (story != null) {
                    storyTitle = story.getTitle();
                }
            } catch (Exception e) {
                // 忽略
            }
            dialog = WritingPreferenceDialog.newInstance(storyId, storyTitle, conversationHistory);
        } else {
            dialog = WritingPreferenceDialog.newInstance();
            dialog.setConversationHistory(conversationHistory);
        }
        
        dialog.setOnPreferenceSavedListener(preference -> {
            Toast.makeText(requireContext(), "偏好已保存，将在下一次对话中生效", Toast.LENGTH_SHORT).show();
        });
        
        dialog.show(getParentFragmentManager(), "writing_preference");
    }
    
    /**
     * 显示AI记忆管理对话框
     */
    private void showAiMemoryDialog() {
        if (storyId <= 0) {
            Toast.makeText(requireContext(), "请先选择小说", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 获取小说标题
        String storyTitle = "";
        if (getContext() != null) {
            try {
                com.example.storyteller.data.repository.StoryRepository repo = 
                    new com.example.storyteller.data.repository.StoryRepositoryImpl(requireContext());
                com.example.storyteller.model.Story story = repo.getStoryById(storyId);
                if (story != null) {
                    storyTitle = story.getTitle();
                }
            } catch (Exception e) {
                // 忽略
            }
        }
        
        // 启动独立的AI记忆管理Activity
        Intent intent = new Intent(requireContext(), com.example.storyteller.ui.activity.AiMemoryActivity.class);
        intent.putExtra(com.example.storyteller.ui.activity.AiMemoryActivity.EXTRA_STORY_ID, storyId);
        intent.putExtra(com.example.storyteller.ui.activity.AiMemoryActivity.EXTRA_STORY_TITLE, storyTitle);
        startActivity(intent);
    }
    
    /**
     * 从对话中提取记忆
     */
    public void extractMemoriesFromConversation() {
        if (storyId <= 0) {
            Toast.makeText(requireContext(), "请先选择小说", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 显示加载对话框
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(requireContext());
        progressDialog.setMessage("正在分析对话和小说信息...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // 构建完整上下文
        String conversationHistory = "";
        if (conversationMemory != null && conversationMemory.hasHistory()) {
            conversationHistory = conversationMemory.buildContextSummary();
        }
        
        com.example.storyteller.data.local.db.CharacterDao characterDao = 
            new com.example.storyteller.data.local.db.CharacterDao(requireContext());
        StorySettingDao settingDao = new StorySettingDao(requireContext());
        StoryRepository storyRepository = new StoryRepositoryImpl(requireContext());
        
        com.example.storyteller.utils.AiMemoryManager memoryManager = 
            com.example.storyteller.utils.AiMemoryManager.getInstance(requireContext());
        
        String fullContext = memoryManager.buildFullContextForExtraction(
            conversationHistory,
            storyId,
            characterDao,
            settingDao,
            storyRepository
        );
        
        // 调用AI提取记忆
        String prompt = buildMemoryExtractionPrompt(fullContext);
        
        // 设置RequestOptions，增加max_tokens避免JSON被截断
        ApiClient.RequestOptions options = new ApiClient.RequestOptions()
            .setMaxTokens(3000)  // 足够容纳15-20条记忆的JSON
            .setTemperature(0.3);  // 降低温度，提高结构化输出的准确性
        
        apiClient.generateStory(prompt, currentModel, requireContext(), options, new ApiClient.Callback() {
            @Override
            public void onSuccess(String responseText) {
                // 必须在主线程更新UI
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    
                    // 解析AI返回的记忆
                    List<com.example.storyteller.model.AiMemory> extractedMemories = parseExtractedMemories(responseText);
                    
                    if (extractedMemories.isEmpty()) {
                        Toast.makeText(requireContext(), "未提取到重要记忆", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // 显示提取结果对话框
                    com.example.storyteller.ui.dialog.MemoryExtractionDialog dialog = 
                        com.example.storyteller.ui.dialog.MemoryExtractionDialog.newInstance(storyId, extractedMemories);
                    dialog.setOnMemoriesSavedListener(count -> {
                        Toast.makeText(requireContext(), "已保存 " + count + " 条记忆", Toast.LENGTH_SHORT).show();
                    });
                    dialog.show(getParentFragmentManager(), "memory_extraction");
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                // 必须在主线程更新UI
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(requireContext(), "提取失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    /**
     * 构建记忆提取Prompt
     */
    private String buildMemoryExtractionPrompt(String fullContext) {
        // 使用外置的Prompt模板
        java.util.Map<String, Object> variables = new java.util.HashMap<>();
        variables.put("full_context", fullContext);
        
        String prompt = promptManager.getTaskPrompt("memory_extractor", variables);
        
        if (prompt == null || prompt.isEmpty()) {
            // 降级：使用硬编码的Prompt（备用方案）
            android.util.Log.w("AIPanelFragment", "Failed to load memory extraction prompt, using fallback");
            return buildFallbackMemoryExtractionPrompt(fullContext);
        }
        
        return prompt;
    }
    
    /**
     * 备用的硬编码Prompt（仅在外置Prompt加载失败时使用）
     */
    private String buildFallbackMemoryExtractionPrompt(String fullContext) {
        return "请分析以下小说的全部信息，提取需要长期记住的重要内容。\n\n" +
               "【输入数据】\n" + fullContext + "\n\n" +
               "【提取要求】\n" +
               "1. 识别对话中提到的新信息（未在角色/设定/大纲中出现的）\n" +
               "2. 识别重要的剧情转折点或关键事件\n" +
               "3. 识别用户的特殊偏好或写作要求\n" +
               "4. 识别需要保持一致性的设定\n\n" +
               "【返回格式】\n" +
               "请返回JSON格式的记忆列表：\n" +
               "{\n" +
               "  \"memories\": [\n" +
               "    {\n" +
               "      \"type\": \"personality\",  // 可选: plot/personality/world/other\n" +
               "      \"title\": \"简短标题\",\n" +
               "      \"content\": \"详细内容（可为空）\",\n" +
               "      \"importance\": 3  // 1-5，3为中等重要性\n" +
               "    }\n" +
               "  ]\n" +
               "}\n\n" +
               "【注意事项】\n" +
               "- 只提取真正重要的信息，不要提取琐碎细节\n" +
               "- 如果某条信息已存在于角色/设定/大纲中，不要重复提取\n" +
               "- importance评分标准：\n" +
               "  - 5: 核心设定，绝对不能忘记\n" +
               "  - 4: 重要信息，应该记住\n" +
               "  - 3: 一般信息，可以记住\n" +
               "  - 2: 次要信息，可选择性记住\n" +
               "  - 1: 不重要，不应记住\n" +
               "- 只返回importance >= 3的记忆\n" +
               "- 直接返回JSON，不要包含其他文字";
    }
    
    /**
     * 解析AI返回的记忆
     */
    private List<com.example.storyteller.model.AiMemory> parseExtractedMemories(String jsonResponse) {
        List<com.example.storyteller.model.AiMemory> memories = new ArrayList<>();
        
        try {
            // 尝试解析JSON
            org.json.JSONObject jsonObject = new org.json.JSONObject(jsonResponse);
            org.json.JSONArray memoriesArray = jsonObject.getJSONArray("memories");
            
            for (int i = 0; i < memoriesArray.length(); i++) {
                org.json.JSONObject memoryObj = memoriesArray.getJSONObject(i);
                
                com.example.storyteller.model.AiMemory memory = new com.example.storyteller.model.AiMemory();
                memory.setMemoryType(memoryObj.optString("type", com.example.storyteller.model.AiMemory.TYPE_OTHER));
                memory.setTitle(memoryObj.optString("title", "未命名记忆"));
                memory.setContent(memoryObj.optString("content", ""));
                memory.setImportance(memoryObj.optInt("importance", 3));
                
                // 只保存重要性 >= 3 的记忆
                if (memory.getImportance() >= 3) {
                    memories.add(memory);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("AIPanelFragment", "Failed to parse memories", e);
            // 如果JSON解析失败，尝试简单的文本解析
            memories = parseMemoriesFromText(jsonResponse);
        }
        
        return memories;
    }
    
    /**
     * 从文本中简单解析记忆（备用方案）
     */
    private List<com.example.storyteller.model.AiMemory> parseMemoriesFromText(String text) {
        List<com.example.storyteller.model.AiMemory> memories = new ArrayList<>();
        
        // 简单的行解析，每行可能是一条记忆
        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("{") && !line.startsWith("}")) {
                com.example.storyteller.model.AiMemory memory = new com.example.storyteller.model.AiMemory();
                memory.setMemoryType(com.example.storyteller.model.AiMemory.TYPE_OTHER);
                memory.setTitle(line.length() > 50 ? line.substring(0, 50) : line);
                memory.setContent(line);
                memory.setImportance(3);
                memories.add(memory);
            }
        }
        
        return memories;
    }
}
