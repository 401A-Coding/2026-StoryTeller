# AI 故事生成助手 - Android 项目

## 一、项目介绍

**项目名称**：AI 故事生成助手（StoryTeller）  
**项目仓库**：[https://github.com/401A-Coding/2026-StoryTeller](https://github.com/401A-Coding/2026-StoryTeller)  
**项目主页**：[https://401a-coding.github.io/2026-StoryTeller](https://401a-coding.github.io/2026-StoryTeller)  
**开发环境**：Android Studio + Java + Gradle 9.2.1  
**最低 SDK**：Android 7.0 (API 24)  
**目标 SDK**：Android 16 (API 36)  
**数据方案**：全本地存储（SQLite + 加密 SharedPreferences），调用大模型 API 生成内容，**无云端部署**  
**当前版本**：1.0.1

### 核心功能

- **AI 故事生成**：多模型支持（MiniMax、DeepSeek等），支持大纲生成、正文续写、素材提取
- **小说管理工作区**：卷/章结构管理，支持多部小说切换
- **设定管理系统**：六大分类体系（世界观/角色/地点/剧情/规则/创作控制），30个子分类专属属性
- **AI 助手交互**：Agent 模式支持自然语言指令执行，提供条件分支审核功能
- **人物画像**：角色卡片展示，支持关系网络管理
- **平行剧情/剧情树**：多结局剧情方案，支持树形可视化与分支预览
- **关系图可视化**：基于 vis.js 的交互式关系网络展示
- **素材库管理**：分类、搜索、筛选，支持从网页导入小说内容
- **文档编辑**：Markdown 编辑与渲染
- **AI 记忆管理**：对话记忆存储与提取
- **写作偏好设置**：语言风格、叙事视角、情感基调等个性化配置
- **封面生成**：支持图生图/文生图 API 生成书籍封面
- **导入小说**：支持从番茄小说等平台导入小说内容
- **深色模式**：完整的深色主题适配

---

## 二、项目结构

### 1. base（基础类模块）
- `BaseActivity`、`BaseFragment`：统一 Activity/Fragment 基类，封装通用逻辑

### 2. ui（界面层模块）

#### Activity（10个主页面）
| 类名 | 功能说明 |
|------|----------|
| `MainActivity` | 主页面，底部导航 |
| `StoryWorkspaceActivity` | 小说工作区主容器 |
| `CharacterActivity` | 角色管理 |
| `PlotTreeActivity` | 剧情树管理 |
| `SettingDetailActivity` | 设定详情编辑 |
| `AiMemoryActivity` | AI记忆管理 |
| `MemoryDetailActivity` | 记忆详情 |
| `NovelDetailActivity` | 小说详情页 |
| `DocumentEditorActivity` | 文档编辑器 |
| `PlotGraphActivity` | 关系图查看器 |

#### Fragment（20个功能模块）
- 导航容器类：`HomeFragment`、`StoryInfoPanelFragment`、`StoryManagementFragment`、`MoreFragment`、`SettingsFragment`
- 目录列表类：`StorySettingsListFragment`、`BookshelfFragment`、`ReferenceLibraryFragment`、`MaterialLibraryFragment`
- 工作区类：`WritingFragment`、`ArchitectureFragment`、`OutlineFragment`、`CharactersFragment`
- AI交互类：`AIPanelFragment`
- 文档类：`DocumentsFragment`
- 关系图：`PlotGraphFragment`
- 其他：`AboutFragment`、`HelpFragment`、`FeedbackFragment`、`MyCreationsFragment`

#### Adapter（20+列表适配器）
#### Dialog（10+弹窗组件）

### 3. model（数据模型模块 - 17个实体类）
| 模型 | 说明 |
|------|------|
| `Story` | 小说主体，含seriesName字段 |
| `Volume` | 卷 |
| `Chapter` | 章节 |
| `StorySetting` | 设定，支持30个子分类专属属性 |
| `SettingRelationship` | 设定关系 |
| `Character` | 角色 |
| `StoryDocument` | 文档 |
| `ChatMessage` | 聊天消息 |
| `AiMemory` | AI记忆 |
| `UserWritingPreference` | 用户写作偏好 |
| `ImportedNovel` | 导入的小说 |
| `NovelSummary` | 小说摘要 |
| `PlotChapterSummary` | 剧情章节摘要 |
| `PlotOverviewSummary` | 剧情概览摘要 |
| `PlotSummarySnapshot` | 剧情摘要快照 |
| `RelationExtractionResult` | 关系提取结果 |
| `BehaviorLog` | 行为日志 |

### 4. data（数据访问与存储模块）
- **local**：SQLite 数据库（DBHelper）
- **remote**：API 客户端（多模型支持）、素材提取、网络爬虫（Jsoup）
- **repository**：数据仓库接口与实现

### 5. utils（工具类模块 - 17个）
| 工具类 | 说明 |
|--------|------|
| `AgentCommandExecutor` | Agent 命令执行器（94KB） |
| `AiMemoryManager` | AI记忆管理器 |
| `PromptManager` | 提示词管理 |
| `RelationExtractor` | 关系提取工具 |
| `SpecificAttributesParser` | 专属属性解析器 |
| `PreferenceExtractor` | 偏好提取器 |
| `PreferenceManager` | 偏好管理器 |
| `ThemeManager` | 主题管理器 |
| `ThemeColorUtils` | 主题颜色工具 |
| `ImportedNovelFileManager` | 导入小说文件管理 |
| `MaterialContentParser` | 素材内容解析 |
| `DatabaseMigrationUtils` | 数据库迁移工具 |
| `ConversationMemory` | 对话记忆 |
| `AudioUtils` | 音频工具 |
| `JsonUtils` | JSON工具 |
| `TaskType` | 任务类型枚举 |

---

## 三、设定分类体系

项目采用六大顶层分类、30个子分类的设定体系：

| 大类 | 子分类 |
|------|--------|
| **世界观设定** | 地理环境、时代背景、历史背景、文明种族、文化习俗、社会制度、政治势力、科技发展、物品资源 |
| **角色设定** | 主要角色、次要角色、反派角色、组织阵营 |
| **地点设定** | 国家地区、城市、村庄、自然景观、关键场景、建筑设施、特殊空间 |
| **剧情设定** | 主线剧情、支线剧情、关键事件、悬念伏笔、章节规划、矛盾冲突、时间线 |
| **规则体系** | 力量体系、魔法/超能力、战斗系统、经济体系、时间规则、限制条件 |
| **创作控制** | 主题内核、语言风格、情感基调、叙事视角 |

详细设计参见 [SpecificAttributes_Design_v3.md](./SpecificAttributes_Design_v3.md)

---

## 四、技术栈与依赖

### 构建系统
- **AGP**：9.2.1
- **Gradle**：8.x
- **Java**：11

### 核心依赖
| 库 | 版本 | 用途 |
|---|------|------|
| Material | 1.14.0 | Material Design 组件 |
| OkHttp | 5.3.2 | HTTP 网络请求 |
| Gson | 2.14.0 | JSON 解析 |
| Glide | 4.16.0 | 图片加载 |
| Markwon | 4.6.2 | Markdown 渲染 |
| Jsoup | 1.18.3 | HTML 解析/网页爬取 |
| ZXing | 3.5.3 | 二维码生成 |
| security-crypto | 1.1.0-alpha06 | SharedPreferences 加密 |
| vis.js | (WebView) | 关系图可视化 |

---

## 五、版本历史

| 版本 | 日期 | 更新内容               |
|------|------|--------------------|
|1.0.0|2026.05.28| 首发版本，初步实现完整小说创作工作流 |
| 1.0.1 | 2026.05.29 | 当前版本，更新首页布局        |

---

## 六、提交规范（GitHub）

- 每周功能合并到 `main`
- 功能分支命名：`feature/模块名`
- 提交信息：`feat: 完成XXX` / `fix: 修复XXX` / `ui: 优化XXX`
