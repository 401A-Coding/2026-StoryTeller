# AI 智能体集成指南

## 📖 概述

本文档说明如何将 AI 智能体功能集成到故事生成页面，实现自然语言编辑小说。

---

## 🎯 核心功能

### 1. **智能意图识别**
用户可以用自然语言表达需求，AI 自动识别并执行相应操作：

- "帮我添加一个悬疑章节" → 自动创建新章节
- "给主角起个名字" → 生成角色建议
- "接下来情节怎么发展？" → 提供情节建议
- "第一章写得怎么样？" → 分析并回答

### 2. **上下文感知**
AI 了解当前小说的：
- 标题、类型
- 卷章结构
- 最近的内容
- 角色信息（未来扩展）

### 3. **安全执行**
- AI 只返回命令，不直接修改数据
- 前端解析命令并执行实际操作
- 用户可以预览和确认（可选）

---

## 🔧 在 StoryGenerateActivity 中集成

### 步骤1：添加成员变量

```java
public class StoryGenerateActivity extends AppCompatActivity {
    // ... 现有代码 ...
    
    private AgentCommandExecutor commandExecutor;
    private boolean isAgentMode = false;  // 是否启用智能体模式
}
```

### 步骤2：初始化执行器

在 `onCreate()` 中添加：

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_story_generate);
    
    // 初始化智能体命令执行器
    commandExecutor = new AgentCommandExecutor(this);
    
    // ... 其他初始化代码 ...
}
```

### 步骤3：修改聊天发送逻辑

找到发送消息的方法（可能是 `sendMessage()` 或类似方法），修改为：

```java
private void sendMessage(String message) {
    if (message.trim().isEmpty()) return;
    
    // 显示用户消息
    addMessageToChat(message, true);
    
    // 显示"思考中"状态
    showTypingIndicator();
    
    if (isAgentMode) {
        // 智能体模式：构建上下文并发送
        String context = AgentCommandExecutor.buildStoryContext(currentStory, volumes);
        
        ApiClient.getInstance().processAgentCommand(
            message,
            context,
            this,
            new ApiClient.AgentCallback() {
                @Override
                public void onCommandReady(ApiClient.AgentCommand command) {
                    runOnUiThread(() -> {
                        hideTypingIndicator();
                        
                        // 执行命令
                        String result = commandExecutor.executeCommand(command, currentStory.getId());
                        
                        // 显示 AI 回复
                        if (!result.isEmpty()) {
                            addMessageToChat(result, false);
                        }
                        
                        // 如果执行了操作，刷新 UI
                        if (!command.action.equals("answer_question")) {
                            refreshStoryView();
                        }
                    });
                }
                
                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() -> {
                        hideTypingIndicator();
                        addMessageToChat("抱歉，发生了错误：" + e.getMessage(), false);
                    });
                }
            }
        );
    } else {
        // 普通聊天模式：使用原有的 generateStory 方法
        ApiClient.getInstance().generateStory(message, this, new ApiClient.Callback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    hideTypingIndicator();
                    addMessageToChat(response, false);
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    hideTypingIndicator();
                    addMessageToChat("抱歉，发生了错误：" + e.getMessage(), false);
                });
            }
        });
    }
}
```

### 步骤4：添加智能体模式切换按钮

在布局文件中添加开关（例如在工具栏或设置中）：

```xml
<!-- 在 activity_story_generate.xml 中添加 -->
<Switch
    android:id="@+id/switch_agent_mode"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="智能体模式" />
```

在 Activity 中绑定：

```java
Switch switchAgentMode = findViewById(R.id.switch_agent_mode);
switchAgentMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
    isAgentMode = isChecked;
    Toast.makeText(this, 
        isChecked ? "已启用智能体模式" : "已切换到普通聊天模式", 
        Toast.LENGTH_SHORT).show();
});
```

### 步骤5：刷新视图方法

```java
private void refreshStoryView() {
    // 重新加载小说数据
    currentStory = storyDao.getStoryById(currentStory.getId());
    
    // 重新渲染卷章结构
    layoutContent.removeAllViews();
    
    // 先添加"添加卷"按钮
    Button btnAddVolume = new Button(this);
    btnAddVolume.setId(R.id.btn_add_volume);
    btnAddVolume.setText("+ 添加卷");
    layoutContent.addView(btnAddVolume);
    
    // 解析并渲染结构
    String structureJson = currentStory.getStructure();
    if (!TextUtils.isEmpty(structureJson)) {
        parseStoryStructure(structureJson);
    } else {
        parseStoryContent(currentStory.getContent());
    }
}
```

---

## 💡 使用示例

### 场景1：添加章节

**用户输入：**
```
帮我添加一个新章节，讲述主角发现了一个神秘宝藏
```

**AI 返回命令：**
```json
{
  "action": "add_chapter",
  "parameters": {
    "volume_id": 1,
    "chapter_title": "神秘宝藏",
    "chapter_content": "在一个风雨交加的夜晚，主角偶然发现了..."
  },
  "reasoning": "用户想要添加一个关于发现宝藏的新章节"
}
```

**系统执行：**
1. 解析命令
2. 在数据库中创建新章节
3. 刷新 UI 显示新章节
4. 回复用户："已成功添加章节：神秘宝藏"

---

### 场景2：情节建议

**用户输入：**
```
接下来情节应该怎么发展？给我一些建议
```

**AI 返回命令：**
```json
{
  "action": "generate_plot",
  "parameters": {
    "response": "基于当前情节，我建议：\n1. 引入一个反派角色\n2. 设置一个时间限制增加紧张感\n3. 让主角面临道德抉择"
  }
}
```

**系统执行：**
1. 识别为问答类型
2. 直接在聊天框显示建议
3. 不修改小说内容

---

### 场景3：普通聊天

**用户输入：**
```
你觉得这个故事有趣吗？
```

**AI 返回命令：**
```json
{
  "action": "answer_question",
  "parameters": {
    "response": "这个故事很有潜力！主角的性格塑造很生动，建议在后续章节中..."
  }
}
```

**系统执行：**
1. 显示 AI 的回答
2. 不执行任何操作

---

## 🚀 扩展功能建议

### 1. **确认机制**
在执行重要操作前，让用户确认：

```java
// 显示确认对话框
new AlertDialog.Builder(this)
    .setTitle("确认操作")
    .setMessage("AI 建议添加章节\"" + chapterTitle + "\"，是否执行？")
    .setPositiveButton("执行", (dialog, which) -> {
        // 执行命令
    })
    .setNegativeButton("取消", null)
    .show();
```

### 2. **撤销功能**
记录 AI 执行的操作，允许撤销：

```java
private Stack<UndoAction> undoStack = new Stack<>();

public void undoLastAction() {
    if (!undoStack.isEmpty()) {
        UndoAction action = undoStack.pop();
        action.undo();
    }
}
```

### 3. **更多命令类型**

扩展支持的操作：
- `delete_chapter`: 删除章节
- `merge_chapters`: 合并章节
- `rewrite_content`: 重写内容
- `suggest_title`: 建议标题
- `analyze_structure`: 分析结构
- `create_character_sheet`: 创建角色档案

### 4. **批量操作**
允许 AI 一次执行多个操作：

```json
{
  "actions": [
    {
      "action": "add_chapter",
      "parameters": {...}
    },
    {
      "action": "edit_chapter",
      "parameters": {...}
    }
  ]
}
```

---

## ⚠️ 注意事项

1. **API 成本**
   - 智能体模式每次调用都会消耗 API 额度
   - 考虑添加调用频率限制

2. **错误处理**
   - AI 可能返回格式错误的 JSON
   - 始终要有 fallback 机制

3. **用户体验**
   - 显示清晰的加载状态
   - 操作完成后给予反馈
   - 提供明确的模式指示

4. **数据安全**
   - 验证所有 AI 返回的参数
   - 限制可执行的操作范围
   - 记录操作日志

---

## 📊 架构总结

```
用户输入
  ↓
ApiClient.processAgentCommand()
  ↓
AI 分析意图 → 返回结构化命令
  ↓
AgentCommandExecutor.executeCommand()
  ↓
执行具体操作（增删改查）
  ↓
更新数据库 + 刷新 UI
  ↓
反馈给用户
```

---

## 🎉 完成！

现在你的应用拥有了强大的 AI 智能体功能，用户可以通过自然语言轻松编辑和管理小说！
