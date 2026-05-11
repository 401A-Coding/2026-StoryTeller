# 智能体命令执行器使用指南

## 📖 概述

重构后的 `AgentCommandExecutor` 采用命令模式，提供了清晰、可复用的智能体命令执行功能。

## 🚀 快速开始

### 1. 初始化

```java
// 在 Activity 或 Fragment 中
private AgentCommandExecutor commandExecutor;
private StoryRepository storyRepository;

@Override
protected void initData() {
    // 创建 Repository
    storyRepository = new StoryRepositoryImpl(this);
    
    // 创建命令执行器
    commandExecutor = new AgentCommandExecutor(storyRepository);
}
```

### 2. 执行命令

```java
// 从 AI 获取命令
ApiClient.AgentCommand command = ...; // 由 AI 返回

// 执行命令
int storyId = currentStory.getId();
CommandResult result = commandExecutor.executeCommand(command, storyId);

// 处理结果
if (result.success) {
    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show();
    refreshUI(); // 刷新界面
} else {
    Toast.makeText(this, "错误：" + result.message, Toast.LENGTH_LONG).show();
}
```

## 📋 支持的命令类型

### 1. 添加卷 (add_volume)

**AI 返回格式：**
```json
{
  "action": "add_volume",
  "parameters": {
    "volume_title": "第二卷：新的冒险"
  }
}
```

**执行效果：**
- 创建新卷对象
- 添加到故事结构
- 自动保存到数据库
- 返回：`"✅ 已成功添加卷：《第二卷：新的冒险》"`

---

### 2. 添加章节 (add_chapter)

**AI 返回格式：**
```json
{
  "action": "add_chapter",
  "parameters": {
    "volume_id": 1,
    "chapter_title": "神秘的宝藏",
    "chapter_content": "在一个风雨交加的夜晚..."
  }
}
```

**参数说明：**
- `volume_id`: 卷ID（从1开始），可选，默认最后一个卷
- `chapter_title`: 章节标题
- `chapter_content`: 章节内容

**执行效果：**
- 在指定卷中添加新章节
- 自动更新结构
- 返回：`"✅ 已成功添加章节：《神秘的宝藏》"`

---

### 3. 编辑章节 (edit_chapter)

支持三种编辑类型：

#### 3.1 重写 (rewrite)

**AI 返回格式：**
```json
{
  "action": "edit_chapter",
  "parameters": {
    "volume_id": 1,
    "chapter_id": 1,
    "edit_type": "rewrite",
    "new_content": "全新的章节内容...",
    "new_title": "新标题"  // 可选
  }
}
```

**执行效果：**
- 完全替换章节内容
- 如果提供 `new_title`，同时更新标题
- 返回：`"✅ 已成功重写第1卷第1章"`

---

#### 3.2 续写 (append)

**AI 返回格式：**
```json
{
  "action": "edit_chapter",
  "parameters": {
    "volume_id": 1,
    "chapter_id": 1,
    "edit_type": "append",
    "new_content": "追加的内容..."
  }
}
```

**执行效果：**
- 将新内容追加到章节末尾
- 用两个换行符分隔（`\n\n`）
- 返回：`"✅ 已成功续写第1卷第1章"`

---

#### 3.3 修改 (modify)

**AI 返回格式：**
```json
{
  "action": "edit_chapter",
  "parameters": {
    "volume_id": 1,
    "chapter_id": 1,
    "edit_type": "modify",
    "new_content": "修改后的内容...",
    "new_title": "新标题"
  }
}
```

**执行效果：**
- 目前等同于重写（未来可实现局部修改）
- 返回：`"✅ 已成功修改第1卷第1章"`

---

### 4. 生成情节建议 (generate_plot)

**AI 返回格式：**
```json
{
  "action": "generate_plot",
  "parameters": {
    "response": "基于当前情节，我建议：\n1. 引入反派角色\n2. 设置时间限制"
  }
}
```

**执行效果：**
- 不修改任何数据
- 返回建议内容
- 返回：`"情节建议：\n基于当前情节，我建议：..."`

---

### 5. 回答问题 (answer_question)

**AI 返回格式：**
```json
{
  "action": "answer_question",
  "parameters": {
    "response": "这是一个很好的问题..."
  }
}
```

**执行效果：**
- 纯问答，不执行任何操作
- 返回回答内容
- **不会触发 UI 刷新**

---

### 6. 创建角色 (create_character) ⏳

**状态：** 待实现

**预期格式：**
```json
{
  "action": "create_character",
  "parameters": {
    "name": "张三",
    "description": "主角，勇敢善良...",
    "role": "protagonist"
  }
}
```

---

## 🔍 CommandResult 结构

```java
public static class CommandResult {
    public final boolean success;   // 是否成功
    public final String message;    // 结果消息
    public final String action;     // 执行的操作类型
    public final Object data;       // 附加数据（如新增的章节对象）
}
```

### 使用示例

```java
CommandResult result = executor.executeCommand(command, storyId);

// 检查是否成功
if (result.success) {
    // 显示成功消息
    Log.i(TAG, result.message);
    
    // 获取操作类型
    String action = result.action; // "add_chapter", "edit_chapter", etc.
    
    // 获取附加数据
    if ("add_chapter".equals(action)) {
        Chapter newChapter = (Chapter) result.data;
        // 使用新章节对象
    }
} else {
    // 处理错误
    Log.e(TAG, "执行失败: " + result.message);
}
```

---

## 💡 最佳实践

### 1. 错误处理

```java
CommandResult result = executor.executeCommand(command, storyId);

if (!result.success) {
    // 显示错误提示
    showErrorDialog(result.message);
    
    // 记录日志
    Log.e(TAG, "命令执行失败: " + result.message);
    
    return;
}

// 成功后再执行后续操作
refreshUI();
saveToHistory(result);
```

### 2. UI 刷新

```java
// 只在需要时刷新 UI
if (result.success && !"answer_question".equals(command.action)) {
    // 执行了实际操作，需要刷新
    refreshStoryView();
} else {
    // 只是问答，不需要刷新
    // 只需显示消息
}
```

### 3. 用户确认

对于危险操作（如删除），添加确认对话框：

```java
if (isDangerousOperation(command.action)) {
    new AlertDialog.Builder(this)
        .setTitle("确认操作")
        .setMessage(result.message + "\n\n确定要执行吗？")
        .setPositiveButton("确定", (dialog, which) -> {
            // 用户确认后执行
            performAction(command, storyId);
        })
        .setNegativeButton("取消", null)
        .show();
} else {
    // 安全操作，直接执行
    CommandResult result = executor.executeCommand(command, storyId);
    handleResult(result);
}
```

### 4. 操作历史

```java
// 记录操作历史，用于撤销
private Stack<CommandHistory> historyStack = new Stack<>();

private void executeAndRecord(AgentCommand command, int storyId) {
    CommandResult result = executor.executeCommand(command, storyId);
    
    if (result.success) {
        // 记录到历史
        historyStack.push(new CommandHistory(command, storyId, result));
        
        // 限制历史记录数量
        if (historyStack.size() > MAX_HISTORY) {
            historyStack.remove(0);
        }
    }
}

// 撤销最后一步操作
public void undoLastAction() {
    if (!historyStack.isEmpty()) {
        CommandHistory lastAction = historyStack.pop();
        undoAction(lastAction);
    }
}
```

---

## 🧪 测试示例

### 单元测试

```java
@Test
public void testAddChapter() {
    // 准备
    StoryRepository mockRepo = mock(StoryRepository.class);
    Story testStory = createTestStory();
    when(mockRepo.getStoryById(1)).thenReturn(testStory);
    
    AgentCommandExecutor executor = new AgentCommandExecutor(mockRepo);
    
    AgentCommand command = new AgentCommand();
    command.action = "add_chapter";
    command.parameters = new HashMap<>();
    command.parameters.put("volume_id", 1);
    command.parameters.put("chapter_title", "测试章节");
    command.parameters.put("chapter_content", "测试内容");
    
    // 执行
    CommandResult result = executor.executeCommand(command, 1);
    
    // 验证
    assertTrue(result.success);
    assertTrue(result.message.contains("测试章节"));
    verify(mockRepo).updateStory(any(Story.class));
}

@Test
public void testEditChapterRewrite() {
    // 准备
    StoryRepository mockRepo = mock(StoryRepository.class);
    Story testStory = createTestStoryWithChapters();
    when(mockRepo.getStoryById(1)).thenReturn(testStory);
    
    AgentCommandExecutor executor = new AgentCommandExecutor(mockRepo);
    
    AgentCommand command = new AgentCommand();
    command.action = "edit_chapter";
    command.parameters = new HashMap<>();
    command.parameters.put("volume_id", 1);
    command.parameters.put("chapter_id", 1);
    command.parameters.put("edit_type", "rewrite");
    command.parameters.put("new_content", "新内容");
    
    // 执行
    CommandResult result = executor.executeCommand(command, 1);
    
    // 验证
    assertTrue(result.success);
    assertTrue(result.message.contains("重写"));
    
    // 验证内容已更新
    Chapter chapter = testStory.getVolumes().get(0).getChapters().get(0);
    assertEquals("新内容", chapter.getContent());
}
```

---

## ❓ 常见问题

### Q1: 如何添加新的命令类型？

**A:** 只需在 `AgentCommandExecutor` 中添加新的处理方法：

```java
// 1. 在 executeCommand 的 switch 中添加 case
case "new_action":
    return handleNewAction(command.parameters, currentStoryId);

// 2. 实现处理方法
private CommandResult handleNewAction(Map<String, Object> params, int storyId) {
    // 实现业务逻辑
    Story story = repository.getStoryById(storyId);
    // ... 执行操作 ...
    repository.updateStory(story);
    
    return CommandResult.success("操作完成");
}
```

**无需修改 Activity！**

---

### Q2: 如何在其他地方复用命令执行器？

**A:** 在任何需要智能体功能的地方创建实例：

```java
public class AnotherActivity extends BaseActivity {
    private AgentCommandExecutor executor;
    
    @Override
    protected void initData() {
        StoryRepository repo = new StoryRepositoryImpl(this);
        executor = new AgentCommandExecutor(repo);
    }
    
    private void processUserInput(String input) {
        // 直接使用
        CommandResult result = executor.executeCommand(command, storyId);
        // ...
    }
}
```

---

### Q3: 如何处理并发执行？

**A:** `AgentCommandExecutor` 是无状态的，可以安全地在多个线程中使用。但需要注意：

```java
// ✅ 安全：每次调用都是独立的
new Thread(() -> {
    CommandResult result = executor.executeCommand(cmd1, storyId);
}).start();

new Thread(() -> {
    CommandResult result = executor.executeCommand(cmd2, storyId);
}).start();

// ⚠️ 注意：如果多个命令修改同一个故事，需要确保数据库操作的原子性
// StoryDao 已经使用了 SQLiteDatabase，它本身是线程安全的
```

---

### Q4: 如何调试命令执行？

**A:** 添加日志：

```java
// 在 executeCommand 开始时
Log.d(TAG, "执行命令: " + command.action);
Log.d(TAG, "参数: " + new Gson().toJson(command.parameters));

// 在执行完成后
Log.d(TAG, "结果: " + result.success + ", 消息: " + result.message);

// 在 Activity 中
Log.d(TAG, "收到 AI 命令: " + command.action);
CommandResult result = executor.executeCommand(command, storyId);
Log.d(TAG, "执行结果: " + result.message);
```

---

## 📚 相关文档

- [重构总结](REFACTORING_SUMMARY.md) - 详细的重构过程和成果
- [验证清单](REFACTORING_VERIFICATION.md) - 完整的验证测试项
- [AgentCommandExecutor.java](app/src/main/java/com/example/storyteller/utils/AgentCommandExecutor.java) - 源代码
- [StoryRepository.java](app/src/main/java/com/example/storyteller/data/repository/StoryRepository.java) - Repository 接口

---

## 🎉 结语

重构后的命令执行器提供了清晰、简洁、可扩展的 API，让智能体功能的集成变得简单高效。

**核心优势：**
- ✅ 一行代码执行命令
- ✅ 结构化的结果返回
- ✅ 完善的错误处理
- ✅ 易于测试和调试
- ✅ 高度可复用

祝您使用愉快！🚀
