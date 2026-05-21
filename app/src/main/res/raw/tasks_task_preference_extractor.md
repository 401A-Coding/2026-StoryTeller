# 任务：从对话中提取用户写作偏好

分析以下对话历史，提取用户的写作偏好信息。

## 对话历史
{{conversation_history}}

## 提取规则
只提取明确表达的偏好，不要猜测。包括：
1. **写作风格**：简洁/华丽/幽默/悬疑/其他
2. **叙事视角**：第一人称/第三人称
3. **段落长度**：短/中/长
4. **禁忌内容**：避免血腥/暴力/敏感话题
5. **特殊要求**：其他个性化需求

## 输出格式
返回JSON格式：
```json
{
  "writing_style": "simple",
  "narrative_perspective": "first",
  "paragraph_length": "medium",
  "avoid_bloody": true,
  "avoid_violence": false,
  "avoid_sensitive": false,
  "special_requirements": "多用对话，少用描写"
}
```

如果没有检测到任何偏好，返回空对象：{}