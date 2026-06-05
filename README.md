# 启文 / StoryTeller · Android 智能小说创作助手

> 全本地、零云端的小说 AI 协作写作 App。
> 多模型接入 · 卷章结构 · 设定体系 · 剧情树 · 内置模板中心 · 关系图谱 · 深色模式。

---

## 一、项目介绍

| 项          | 内容                                                                |
|------------|-------------------------------------------------------------------|
| **项目名称**   | 启文（中文应用名） / StoryTeller（仓库名）                                      |
| **项目仓库**   | <https://github.com/401A-Coding/2026-StoryTeller>                 |
| **项目主页**   | <https://401a-coding.github.io/2026-StoryTeller>                  |
| **开发环境**   | Android Studio · Java 11 · Gradle 9.4.1 · AGP 9.2.1               |
| **最低 SDK** | Android 7.0 (API 24)                                              |
| **目标 SDK** | Android 16 (API 36)                                               |
| **数据方案**   | 全本地（SQLite + EncryptedSharedPreferences），调用大模型 API 生成内容，**无云端部署** |
| **当前版本**   | 1.1.0（versionCode 2606066）                                        |
| **许可证**    | MIT License                                                       |

### 1.1 核心特性

- **AI 故事生成**：多模型支持（MiniMax、DeepSeek 等），覆盖大纲生成 / 正文续写 / 素材提取 / 人物画像
- **卷章结构管理**：多部作品隔离 · 卷/章自由编排 · 阅读模式与编辑模式双视图
- **六大类目设定体系**：世界 / 角色 / 地点 / 剧情 / 规则体系 / 创作控制，38 个子分类 + 专属属性字段
- **Agent 智能助手**：自然语言指令直接驱动写大纲、扩写、改稿；条件分支审核让模型在动手前先确认
- **剧情树 / 平行剧情**：多结局分支 + 树形时间线 + 关系图谱联动（vis.js WebView 可视化）
- **素材库**：手动新建 · 番茄小说抓取 · AI 识别入库 · 候选审核 · **内置模板中心**（5 套开箱即用）
- **人物画像与关系网**：角色卡片 + 关系候选 + 关系网络图（vis.js）
- **写作偏好**：语言风格、叙事视角、情感基调、节奏控制等多维配置
- **封面生成**：支持文生图（text-to-image）与图生图（image-to-image）API
- **文档编辑**：Markdown 编辑与渲染（Markwon）
- **深色模式**：完整浅/深/跟随系统三态

### 1.2 三大典型使用场景

```
┌─ 场景一：冷启动 ─────────────────────────────────────────┐
│ 模板中心 → 选择题材模板 → 一键安装 6 条核心素材            │
│      → 进入工作区 → AI 助手续写                           │
└──────────────────────────────────────────────────────────┘

┌─ 场景二：网络素材二次创作 ────────────────────────────────┐
│ 番茄小说 URL → Jsoup 抓取 → MaterialCandidateExtractor  │
│ AI 识别 → 候选审核 → 入库 → 剧情树组织                    │
└──────────────────────────────────────────────────────────┘

┌─ 场景三：长篇持续迭代 ──────────────────────────────────┐
│ 章节规划 → 剧情树梳理 → 分支预览 → 应用到正文              │
│      → AI 改稿扩写 → 文档沉淀                             │
└──────────────────────────────────────────────────────────┘
```

---

## 二、项目结构

源码位于 [`app/src/main/java/com/example/storyteller/`](app/src/main/java/com/example/storyteller/)
，整体按 **基础 / 界面 / 数据 / 模型 / 工具** 五大层组织。

### 2.1 `base/` — 基础类

| 类              | 作用                        |
|----------------|---------------------------|
| `BaseActivity` | 统一 Activity 基类（状态栏、边距、权限） |
| `BaseFragment` | 统一 Fragment 基类            |

### 2.2 `ui/` — 界面层

#### Activity（11 个主页面）

| 类                        | 功能                   |
|--------------------------|----------------------|
| `SplashActivity`         | 启动页（带 logo 动画 + 版本号） |
| `MainActivity`           | 主页面 · 底部 4 Tab 导航    |
| `StoryWorkspaceActivity` | 小说工作区主容器（左右双抽屉）      |
| `CharacterActivity`      | 角色管理                 |
| `PlotTreeActivity`       | 剧情树管理                |
| `SettingDetailActivity`  | 设定详情编辑               |
| `AiMemoryActivity`       | AI 记忆管理              |
| `MemoryDetailActivity`   | 记忆详情                 |
| `NovelDetailActivity`    | 小说详情页                |
| `DocumentEditorActivity` | 文档编辑器（Markdown）      |
| `PlotGraphActivity`      | 关系图查看器（WebView）      |

#### Fragment（22 个功能模块）

- **导航容器**：`HomeFragment`、`StoryInfoPanelFragment`、`StoryManagementFragment`、`MoreFragment`、
  `SettingsFragment`
- **目录列表**：`BookshelfFragment`、`ReferenceLibraryFragment`、`MaterialLibraryFragment`、
  `StorySettingsListFragment`
- **工作区**：`WritingFragment`、`ArchitectureFragment`、`OutlineFragment`、`CharactersFragment`、
  `StoryPlotTreeFragment`
- **AI 交互**：`AIPanelFragment`（Ask / Agent 双模式）
- **文档**：`DocumentsFragment`
- **关系图**：`PlotGraphFragment` + 自绘 `PlotTreeCanvasView`
- **其他**：`AboutFragment`、`HelpFragment`、`FeedbackFragment`、`MyCreationsFragment`

#### Adapter（22 个列表/卡片适配器）

覆盖章节、设定、人物、消息、剧情树、模板卡片、素材候选、模型选项、导入小说、最近作品等所有列表与卡片场景。

#### Dialog / BottomSheet（14 个弹窗组件）

- **素材/设定**：`CreateSettingDialog`、`MaterialCandidateReviewDialogFragment`、
  `ExtractionResultDialogFragment`、`MemoryExtractionDialog`
- **剧情/角色**：`ChapterOutlineEditDialog`、`GlobalOutlineEditDialog`、`VolumeOutlineEditDialog`、
  `CharacterRegenerateBottomSheetDialogFragment`
- **模型配置**：`ModelProviderSettingsDialogHelper`
- **图片选择**：`SettingImageSelectionDialog`
- **写作偏好**：`WritingPreferenceDialog`
- **预设模板**：`PresetTemplateDialogFragment`（模板中心列表）、`PresetTemplatePreviewDialogFragment`
  （模板预览）
- **小说创建**：`CreateStoryDialog`

### 2.3 `model/` — 数据模型（23 个实体）

| 模型                          | 类别     | 说明                                               |
|-----------------------------|--------|--------------------------------------------------|
| `Story`                     | 业务     | 小说主体，含 `seriesName` / `genre` / `coverColor`     |
| `Volume` / `Chapter`        | 业务     | 卷 / 章节                                           |
| `Character`                 | 业务     | 人物（关联到小说）                                        |
| `StorySetting`              | 业务     | 设定条目（六大类目 + `presetTemplateId` 字段）               |
| `SettingRelationship`       | 业务     | 设定之间的关系                                          |
| `StoryDocument`             | 业务     | 文档条目                                             |
| `ChatMessage`               | AI     | 与 AI 的聊天消息                                       |
| `AiMemory`                  | AI     | 长期记忆条目                                           |
| `UserWritingPreference`     | AI     | 用户写作偏好（语言 / 视角 / 基调 / 节奏）                        |
| `ImportedNovel`             | 素材     | 抓取导入的小说源信息                                       |
| `NovelSummary`              | 摘要     | 小说摘要                                             |
| `PlotChapterSummary`        | 摘要     | 单章剧情摘要                                           |
| `PlotOverviewSummary`       | 摘要     | 全书剧情概览                                           |
| `PlotSummarySnapshot`       | 摘要     | 剧情摘要快照                                           |
| **`PresetTemplate`**        | **模板** | **预设模板完整定义（templateId/version/source/settings）** |
| **`PresetTemplateIndex`**   | **模板** | **预设模板清单项（_index.json 结构）**                      |
| **`PresetSettingItem`**     | **模板** | **预设模板中的单条素材条目**                                 |
| `PlotTreeBranch`            | 剧情树    | 剧情分支                                             |
| `PlotTreeEvent`             | 剧情树    | 剧情事件                                             |
| `PlotTreeWorkspaceSnapshot` | 剧情树    | 剧情树工作区快照                                         |
| `RelationExtractionResult`  | 关系     | 关系提取结果                                           |
| `BehaviorLog`               | 日志     | 行为日志（用于偏好提取）                                     |

### 2.4 `data/` — 数据访问与存储

```
data/
├── local/
│   ├── db/      # SQLite DAO 层（9 个 DAO）
│   │   ├── DBHelper.java
│   │   ├── StoryDao.java
│   │   ├── StorySettingDao.java       # 含 deleteByPresetTemplateId() 模板专用
│   │   ├── CharacterDao.java
│   │   ├── AiMemoryDao.java
│   │   ├── BehaviorLogDao.java
│   │   ├── ImportedNovelDao.java
│   │   ├── SettingRelationshipDao.java
│   │   └── StoryDocumentDao.java
│   └── prefs/   # 加密 SharedPreferences（API Key 等）
├── remote/
│   ├── ApiClient.java                 # OkHttp 通用客户端
│   ├── ApiKeyManager.java             # 加密存储各 Provider Key
│   ├── ModelConfig.java               # 多模型 Provider 配置
│   ├── MaterialCandidateExtractor.java# AI 素材识别
│   ├── GenericContentExtractor.java   # 通用网页内容提取
│   ├── NovelCrawler.java              # 番茄小说爬虫
│   └── FanqieSelectors.java           # 番茄小说 CSS 选择器
└── repository/
    ├── StoryRepository.java
    └── StoryRepositoryImpl.java
```

### 2.5 `utils/` — 工具类（19 个）

| 工具类                        | 作用                               |
|----------------------------|----------------------------------|
| `PresetTemplateManager`    | **预设模板管理器（发现/加载/安装/卸载/状态查询/缓存）** |
| `AgentCommandExecutor`     | Agent 命令执行器（解析 + 落库）             |
| `AiMemoryManager`          | AI 记忆管理（增删改查 + 触发提取）             |
| `ConversationMemory`       | 对话窗口内短期记忆                        |
| `PromptManager`            | 提示词模板管理（按任务类型）                   |
| `PreferenceExtractor`      | 偏好提取（从行为日志反推用户偏好）                |
| `PreferenceManager`        | 写作偏好读写                           |
| `RelationExtractor`        | 关系提取（从文本中识别实体间关系）                |
| `SpecificAttributesParser` | 设定专属属性字段解析                       |
| `SettingCategoryConfig`    | **六大类目 + 38 子分类的集中配置**           |
| `MaterialContentParser`    | 素材内容解析                           |
| `ImportedNovelFileManager` | 导入小说文件管理                         |
| `DatabaseMigrationUtils`   | 数据库迁移工具                          |
| `ReadingController`        | 阅读模式控制器（翻页 / 字号 / 进度）            |
| `AudioUtils`               | 音频工具（朗读）                         |
| `JsonUtils`                | JSON 工具                          |
| `ThemeManager`             | 主题管理器                            |
| `ThemeColorUtils`          | 主题颜色工具                           |
| `TaskType`                 | 任务类型枚举（生成 / 提取 / 审核 等）           |

### 2.6 `assets/`

```
assets/
├── LICENSE
├── plot_graph.html           # vis.js 关系图渲染页
└── presets/                  # 内置模板（详见第四章）
    ├── _index.json
    ├── cosmic_horror_v1.json   # 克苏鲁末日
    ├── xianxia_v1.json         # 仙侠修真
    ├── western_fantasy_v1.json # 剑与魔法
    ├── cyberpunk_v1.json       # 赛博朋克
    └── urban_abilities_v1.json # 都市异能
```

---

## 三、内置模板中心（Preset Template Center）

> 「零基础开书」入口：选择题材模板 → 预览内容 → 一键安装到当前小说的素材库。

### 3.1 架构概览

```
┌────────────────────────────────────────────────────────────────┐
│                       PresetTemplateManager                    │
│   · listTemplates()                                             │
│   · loadTemplate(id)              ◀── 内存缓存（按 templateId）   │
│   · install(template, storyId, mode)                            │
│   · uninstall(templateId, storyId)                              │
│   · getInstalledState(templateId, storyId)                      │
│   · listInstalledStates(storyId)                                │
└──────────────┬─────────────────────────────────────┬────────────┘
               │                                     │
   ┌───────────▼──────────┐            ┌──────────────▼─────────────┐
   │  assets/presets/     │            │  StorySettingDao            │
   │  ├ _index.json       │            │  · insert / getByStoryId    │
   │  ├ {id}_v1.json ×N   │            │  · deleteByPresetTemplateId│
   │  └ (按 version 演进)   │            │    OVERWRITE 模式专用        │
   └──────────────────────┘            └────────────────────────────┘
```

### 3.2 三层数据模型

```jsonc
// 1) 索引文件：assets/presets/_index.json
{
  "templates": [
    { "id": "cosmic_horror_v1", "name": "克苏鲁末日",
      "description": "...", "featured": true }
  ]
}

// 2) 模板文件：assets/presets/{id}_v{version}.json
{
  "templateId": "cosmic_horror_v1",
  "templateName": "克苏鲁末日",
  "version": 1,
  "description": "...",
  "source": { "type": "preset_template", "title": "黄昏分界", "author": "黑山老鬼" },
  "settings": [
    { "category": "世界",   "subCategory": "时代背景",
      "title": "...", "summary": "...", "detail": "...", "tags": [...] },
    ...（6 条素材，覆盖六大类目）
  ]
}

// 3) 安装结果：写入 StorySetting 表，sourceType = "preset_template"
//    附 presetTemplateId + presetVersion 字段，便于版本对比与卸载
```

### 3.3 当前内置的 5 套模板

| 模板 ID                | 名称    | 参考作品    | 作者   |
|----------------------|-------|---------|------|
| `cosmic_horror_v1`   | 克苏鲁末日 | 《黄昏分界》  | 黑山老鬼 |
| `xianxia_v1`         | 仙侠修真  | 《凡人修仙传》 | 忘语   |
| `western_fantasy_v1` | 剑与魔法  | 西幻经典    | —    |
| `cyberpunk_v1`       | 赛博朋克  | 赛博朋克经典  | —    |
| `urban_abilities_v1` | 都市异能  | 都市异能类   | —    |

每套模板包含 **6 条素材**（世界 / 角色 / 地点 / 剧情 / 规则体系 / 创作控制各 1 条），覆盖完整写作维度。

### 3.4 安装模式（InstallMode）

| 模式              | 行为                                                                |
|-----------------|-------------------------------------------------------------------|
| `SKIP_EXISTING` | **默认** · 目标小说中已存在同名素材时跳过，不创建副本、不覆盖                                |
| `RENAME`        | 重名时自动追加后缀（如 `xxx_1`）                                              |
| `OVERWRITE`     | 先调用 `deleteByPresetTemplateId()` 清空该 templateId 在当前小说中的旧素材，再插入新内容 |

每种模式都返回 `InstallResult { total / installed / replaced / skipped / renamed / failed }`，UI 层用
`formatInstallSummary()` 拼接用户可读的统计文本。

### 3.5 关键设计决策

- **数据与代码分离**：模板内容在 `assets/presets/*.json`，业务逻辑在 `PresetTemplateManager`
  ，新增/修改模板无需重新编译
- **版本号管理**：`{id}_v{version}.json` + 数据库中保存 `presetVersion` 字段，可识别"可更新"状态
- **类型标识**：`StorySetting.sourceType = "preset_template"`，与爬虫 / AI 提取素材严格区分
- **URL 防御**：模板 source 字段不设假 URL；UI 层在 URL 为空时自动隐藏链接卡片
- **进程内缓存**：`PresetTemplateManager.templateCache` 避免重复读 assets + Gson 解析
- **后台 IO**：安装 / 卸载走 `Executors.newSingleThreadExecutor()`，UI 不阻塞
- **来源溯源**：依赖 `source.title` + `source.author` 体现模板参考作品，URL 字段仅在确实有源链接时填写

### 3.6 扩展指南：新增一套模板

1. 在 `assets/presets/` 下新建 `{your_id}_v1.json`（6 条素材）
2. 在 `_index.json` 的 `templates` 数组中追加：
   ```json
   { "id": "{your_id}_v1", "name": "...", "description": "...", "featured": false }
   ```
3. 重新打包 APK 即可。模板中心会自动列出，按 `featured` 排序展示。
4. 升级时：新建 `{your_id}_v2.json`，索引中追加新条目，旧用户可见"可更新"提示。

---

## 四、设定分类体系

采用六大顶层分类、**38** 个子分类的统一体系（[
`SettingCategoryConfig`](app/src/main/java/com/example/storyteller/utils/SettingCategoryConfig.java)）：

| 顶层分类     | 子分类                                                          |
|----------|--------------------------------------------------------------|
| **世界**   | 地理环境 · 时代背景 · 历史背景 · 文明种族 · 文化习俗 · 社会制度 · 政治势力 · 科技发展 · 物品资源 |
| **角色**   | 主要角色 · 次要角色 · 反派角色 · 组织阵营                                    |
| **地点**   | 国家地区 · 城市 · 村庄 · 自然景观 · 关键场景 · 建筑设施 · 特殊空间                   |
| **剧情**   | 主线剧情 · 支线剧情 · 关键事件 · 悬念伏笔 · 章节规划 · 矛盾冲突 · 时间线                |
| **规则体系** | 力量体系 · 魔法或超能力 · 战斗系统 · 经济体系 · 时间规则 · 限制条件                    |
| **创作控制** | 主题内核 · 语言风格 · 情感基调 · 叙事视角 · 节奏控制                             |

> 部分子分类支持 **AI 配图**（`SettingCategoryConfig.supportsAiImageGeneration()`），目前覆盖：角色、地点、世界。

详细字段设计参见 [SpecificAttributes_Design_v3.md](./SpecificAttributes_Design_v3.md)。

---

## 五、技术栈与依赖

### 5.1 构建系统

| 项         | 版本               |
|-----------|------------------|
| AGP       | 9.2.1            |
| Gradle    | 9.4.1            |
| Java      | 11               |
| MinSDK    | 24 (Android 7.0) |
| TargetSDK | 36 (Android 16)  |

### 5.2 核心依赖

| 库                     | 版本             | 用途                                                |
|-----------------------|----------------|---------------------------------------------------|
| Material Components   | 1.14.0         | Material Design 组件（BottomSheet / Chip / Snackbar） |
| AppCompat             | 1.7.1          | 向后兼容                                              |
| ConstraintLayout      | 2.2.1          | 约束布局                                              |
| RecyclerView          | 1.4.0          | 列表容器                                              |
| ViewPager2            | 1.1.0          | 页面切换                                              |
| OkHttp                | 5.3.2          | HTTP 网络请求                                         |
| Gson                  | 2.14.0         | JSON 解析（API + 模板）                                 |
| Jsoup                 | 1.18.3         | HTML 解析 / 网页爬取                                    |
| Glide                 | 4.16.0         | 图片加载                                              |
| Markwon               | 4.6.2          | Markdown 渲染                                       |
| security-crypto       | 1.1.0-alpha06  | EncryptedSharedPreferences                        |
| localbroadcastmanager | 1.1.0          | 跨 Fragment 通信                                     |
| ZXing                 | 3.5.3          | 二维码生成                                             |
| vis.js                | (WebView)      | 关系图 / 剧情树可视化                                      |
| JUnit / Espresso      | 4.13.2 / 3.7.0 | 单元 / 仪器测试                                         |

### 5.3 运行时依赖说明

- **OkHttp** 通过自定义拦截器注入 Provider API Key（按 `ModelConfig.Provider` 分发）
- **vis.js** 通过本地 `assets/plot_graph.html` 加载；JS 与原生通过 WebView 双向桥接
- **Gson** 负责 API 响应反序列化 + 模板 JSON 反序列化（`PresetTemplate` / `PresetSettingItem`）

---

## 六、版本历史

| 版本    | 日期         | 更新内容                                        |
|-------|------------|---------------------------------------------|
| 1.0.0 | 2026.05.28 | 首发版本：完成小说创作主工作流（卷章 / 设定 / AI 助手 / 关系图）      |
| 1.0.1 | 2026.05.29 | 首页布局改版                                      |
| 1.0.2 | 2026.05.29 | 配置 CI / CD 与 GitHub Release 工作流             |
| 1.0.3 | 2026.05.30 | 朗读功能（`AudioUtils`）                          |
| 1.0.4 | 2026.05.30 | 阅读模式（`ReadingController`） · 应用图标 · 多模型校验与提示 |
| 1.1.0 | 2026.06.06 | 剧情树功能 · 设定模板功能                              |

---

## 七、发布流程（CI/CD）

使用 GitHub Actions 实现自动化构建和发布。

### 7.1 发布步骤

```bash
# 1. PR 合并到 main 后，确保本地 main 最新
git checkout main && git pull

# 2. 修改 app/build.gradle.kts 中的 versionCode / versionName
#    versionCode 建议格式：YYMMDDNN（如 26060501）
#    versionName 遵循语义化版本 v{major}.{minor}.{patch}

# 3. 创建 tag
git tag v1.1.0

# 4. 推送 tag 触发构建
git push origin v1.1.0
```

### 7.2 GitHub Actions 工作流程

推送 tag 后自动执行：

1. 解密 keystore（从 Secrets 获取）
2. 构建 Release APK（签名）
3. 创建 GitHub Release（草稿模式，可手动编辑后发布）

### 7.3 首次配置

1. **生成签名密钥**：在 Android Studio 中生成 JKS 文件
2. **Base64 编码 keystore**：
   ```bash
   # Linux / macOS
   base64 keystore.jks | tr -d '\n' > keystore_b64.txt

   # Windows PowerShell
   [Convert]::ToBase64String((Get-Content -Path "keystore.jks" -Encoding Byte)) -replace '\r?\n' | Set-Content keystore_b64.txt
   ```
3. **添加 GitHub Secrets**：
    - `KEY_STORE_FILE`：Base64 编码的 keystore 内容
    - `KEY_STORE_PASSWORD`：Keystore 密码
    - `KEY_PASSWORD`：密钥密码

### 7.4 Workflow 文件

- [`.github/workflows/android.yml`](.github/workflows/android.yml)：CI 检查（PR / 推送到 main 时构建
  debug + lint）
- [`.github/workflows/release.yml`](.github/workflows/release.yml)：Release 发布（推送 `v*.*.*` tag
  时构建 release）

---

## 八、提交与协作规范

### 8.1 分支策略

- `main`：稳定分支，每周合并功能分支
- `feature/<module>`：功能分支，命名示例 `feature/preset-template-center`
- `release/v<version>`：发布分支（如 `release/v1.0.5`）
- `hotfix/<desc>`：紧急修复分支

### 8.2 提交信息规范

```
<type>(<scope>): <subject>

<body>

<footer>
```

| type       | 用途         |
|------------|------------|
| `feat`     | 新功能        |
| `fix`      | 修复 Bug     |
| `ui`       | 纯界面优化      |
| `refactor` | 重构（不改变行为）  |
| `docs`     | 文档变更       |
| `chore`    | 杂项（依赖、构建等） |
| `test`     | 测试相关       |

**示例**：

```
feat(preset): 增加 4 套内置模板（仙侠 / 西幻 / 赛博 / 都市）
fix(preset): 修复 cosmic_horror 索引 id 与文件名不一致
ui(preset): 模板卡片加预览按钮 + strings 提取
```

### 8.3 静态检查

CI 会执行 `./gradlew compileDebugSources lint`，提交前请在本地通过编译与 lint。

---

## 九、常见问题（FAQ）

**Q1: 模板安装后素材的归属怎么算？**
A: 每条素材都带 `sourceType="preset_template"` + `presetTemplateId` + `presetVersion`，与用户自建 /
抓取 / AI 提取的素材严格区分；卸载时按 `presetTemplateId` 精准删除。

**Q2: 模型 API Key 安全吗？**
A: 通过 `EncryptedSharedPreferences`（`security-crypto` 1.1.0-alpha06）加密本地存储，**不会**上传到任何服务器。

**Q3: 模板可以二次修改吗？**
A: 可以。安装后即与用户自建素材一致，可在「设定详情」页自由编辑；下次执行 OVERWRITE 模式安装会被覆盖。

**Q4: 为什么 `build.gradle.kts` 里有两个 Material 依赖？**
A: `libs.material` (1.13.0) 是核心 Material Components；`libs.google.material` (1.14.0) 提供较新的
BottomSheet 行为与新主题属性，按需引入。

---

## 十、致谢

- [vis.js](https://visjs.org/) — 关系图与剧情树可视化
- [Markwon](https://github.com/noties/Markwon) — Markdown 渲染
- [OkHttp](https://square.github.io/okhttp/) / [Gson](https://github.com/google/gson) / [Glide](https://github.com/bumptech/glide)
  等优秀开源库

---

<div align="center">

**启文 · 让写作更专注，让创作更自由**

</div>
