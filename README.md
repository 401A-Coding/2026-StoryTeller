# AI 故事生成助手 - Android 项目

## 一、项目介绍
**项目名称**：AI 故事生成助手（Android 本地版）
**项目仓库**：https://github.com/401A-Coding/2026-StoryTeller
**项目主页**：https://401a-coding.github.io/2026-StoryTeller
**开发环境**：Android Studio + Java  
**数据方案**：全本地存储（SQLite + SharedPreferences），仅调用大模型 API 生成内容，**无云端部署**  
**项目目标**：完成一个可正常运行、界面美观、功能完整的 Android 应用原型，熟悉移动端 UI、数据存储、网络请求、交互逻辑开发。

### 核心功能
- AI 故事生成（类型/篇幅/构思 → 大纲+正文）
- 本地保存/修改/删除/书架管理
- 人物画像界面展示人物卡片，可滑动与编辑
- 平行剧情页面可撰写多版剧情方案，树形多结局剧情展示
- 素材库（分类、搜索、筛选、插入）
- 本地数据存储、用户行为记录、导出预览
- 交互优化：加载提示、错误重试、页面跳转、滑动展开
- 创意扩展：语音输入/朗读、个性化推荐、社交基础、主题设置

## 二、项目结构

### 1. base（基础类模块）
- BaseActivity、BaseFragment：统一 Activity/Fragment 基类，封装通用逻辑

### 2. ui（界面层模块）
- activity：各主页面 Activity（主页、故事生成、人物、素材、剧情树、预览、设置等）
- fragment：底部导航 Fragment（首页/书架/我的）
- adapter：RecyclerView 适配器（故事、人物、聊天、素材等）
- dialog：弹窗与底部弹窗组件

### 3. model（数据模型模块）
- Story、Character、ChatMessage、Material、NovelSummary、Volume、Chapter、BehaviorLog 等实体类

### 4. data（数据访问与存储模块）
- local：本地 SQLite 数据库（db/ DBHelper 与各实体 Dao）、偏好设置（prefs/）负责简单配置项存储
- remote：API 客户端、API Key 管理、素材提取、网络爬虫
- repository：数据仓库接口与实现，统一数据访问

### 5. utils（工具类模块）
- JsonUtils、AudioUtils、AgentCommandExecutor 等

## 三、当前完成情况
- ✅ 基础包结构建立（BaseActivity / BaseFragment）
- ✅ 主页底部导航与 3 个 Fragment
- ✅ 首页 4 个入口卡片与“当前小说”占位
- ✅ 书架列表（Story 数据占位）
- ✅ 个人中心信息区与设置入口
- ✅ 故事生成聊天界面（本地占位回复 hello world）
- ✅ 人物画像列表（占位 5 人物）
- ✅ 其余 Activity 页面占位与返回首页入口
- ✅ 本地数据库与偏好配置工具类
- ✅ API Client / JSON 工具类占位
- ✅ Story/Character/Material/BehaviorLog 等数据模型初步实现
- ✅ 数据访问层（Dao/Repository）初步实现
- ✅ 素材相关适配器与弹窗初步实现

## 四、开发指导
### 1. 新增页面（Activity / Fragment）
- Activity：在 `ui/activity` 新建类，继承 `BaseActivity`，实现 `getLayoutId()`、`initView()`、`initData()`，并在 `AndroidManifest.xml` 注册
- Fragment：在 `ui/fragment` 新建类，继承 `BaseFragment`，实现 `getLayoutId()`、`initView()`、`initData()`，用于底部导航切换

### 2. 新增列表/弹窗界面
- Adapter：在 `ui/adapter` 新建对应 Adapter（继承 RecyclerView.Adapter）
- Dialog：在 `ui/dialog` 新建弹窗类（如 BottomSheetDialogFragment）
- 编写 `item_xxx.xml` 布局
- 在 Activity/Fragment 中设置 LayoutManager + Adapter

### 3. 本地存储开发流程
- 建表：在 `DBHelper` 中写 CREATE TABLE
- Dao：在 `data/local/db` 新建/完善 `XXXDao` 类，负责实体数据操作
- 配置：使用 `PrefsUtils` 进行简单配置项存储

### 4. AI 生成功能开发
- 在 `data/remote/ApiClient` 实现 `generateStory(...)` 等方法
- 入参：类型、篇幅、构思、用户自定义设定
- 请求：OkHttp POST + JSON
- 回调：返回故事内容 → 展示 → 本地保存

### 5. 数据仓库与网络
- Repository：在 `data/repository` 实现统一数据访问接口，便于后续扩展
- 网络相关：ApiKey 管理、素材提取、网络爬虫等

### 6. 交互与体验优化
- 加载状态：ProgressBar / 弹窗
- 错误提示：网络异常、生成失败、存储失败
- 页面跳转：Intent / Fragment 切换
- 流畅度：避免主线程耗时操作

### 7. 命名规范（保持统一）
- 布局：`activity_xxx` / `fragment_xxx` / `item_xxx`
- 控件：`tv_xxx` `rv_xxx` `btn_xxx` `et_xxx`
- 类：大驼峰；方法/变量：小驼峰
- 字符串统一放到 `strings.xml`

## 五、依赖说明（已内置）
- AndroidX + Material Design
- RecyclerView
- OkHttp（网络请求）
- Gson（JSON 解析）
- SQLite（本地数据库）
- 系统 TTS / SpeechRecognizer（语音，工具类占位）

## 六、提交规范（GitHub）
- 每周功能合并到 `main`
- 功能分支命名：`feature/模块名`
- 提交信息：`feat: 完成XXX` / `fix: 修复XXX` / `ui: 优化XXX`
