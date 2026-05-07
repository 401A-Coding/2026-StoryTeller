# AI 故事生成助手 - Android 项目

## 一、项目介绍
**项目名称**：AI 故事生成助手（Android 本地版）
**开发环境**：Android Studio + Java
**数据方案**：全本地存储（SQLite + SharedPreferences），仅调用大模型 API 生成内容，**无云端部署**
**项目目标**：完成一个可正常运行、界面美观、功能完整的 Android 应用原型，熟悉移动端 UI、数据存储、网络请求、交互逻辑开发。

### 核心功能
- AI 故事生成（类型/篇幅/构思 → 大纲+正文）
- 本地保存/修改/删除/书架管理
- 人物画像界面展示人物卡片，可滑动与编辑、
- 平行剧情页面可撰写多版剧情方案，树形多结局剧情展示
- 素材库（分类、搜索、筛选、插入）
- 本地数据存储、用户行为记录、导出预览
- 交互优化：加载提示、错误重试、页面跳转、滑动展开
- 创意扩展：语音输入/朗读、个性化推荐、社交基础、主题设置


## 二、项目结构
包路径：`com.example.storyteller`

```
com.example.storyteller
├── base
│   ├── BaseActivity
│   └── BaseFragment
├── ui
│   ├── activity
│   │   ├── MainActivity
│   │   ├── StoryGenerateActivity
│   │   ├── CharacterActivity
│   │   ├── PlotTreeActivity
│   │   ├── MaterialActivity
│   │   ├── StoryPreviewActivity
│   │   └── SettingsActivity
│   ├── fragment
│   │   ├── HomeFragment
│   │   ├── BookshelfFragment
│   │   └── MineFragment
│   └── adapter
│       ├── StoryAdapter
│       ├── CharacterAdapter
│       └── ChatMessageAdapter
├── model
│   ├── Story.java
│   ├── Character.java
│   └── ChatMessage.java
├── data
│   ├── local
│   │   ├── db/DBHelper.java
│   │   └── prefs/PrefsUtils.java
│   └── remote/ApiClient.java
└── utils
    ├── JsonUtils.java
    └── AudioUtils.java
```

### UI 导航与布局
- `activity_main.xml` + `bottom_nav_menu.xml`：底部导航承载 3 个 Fragment（首页/书架/我的）
- `fragment_home.xml`：搜索占位 + 当前小说标题 + 4 个卡片入口
- `fragment_bookshelf.xml`：书架标题 + Story 列表
- `fragment_mine.xml`：个人信息 + 进入设置按钮
- `activity_story_generate.xml`：聊天列表 + 输入发送
- `activity_character.xml`：人物列表
- `activity_plot_tree.xml` / `activity_material.xml` / `activity_story_preview.xml` / `activity_settings.xml`：占位页面
- 列表 item：`item_story.xml`、`item_character.xml`、`item_chat_message.xml`

## 三、当前完成情况
- ✅ 基础包结构建立（BaseActivity / BaseFragment）
- ✅ 主页底部导航与 3 个 Fragment
- ✅ 首页 4 个入口卡片与“当前小说”占位
- ✅ 书架列表（占位 Story 数据）
- ✅ 个人中心信息区与设置入口
- ✅ 故事生成聊天界面（本地占位回复 hello world）
- ✅ 人物画像列表（占位 5 人物）
- ✅ 其余 Activity 页面占位与返回首页入口
- ✅ 本地数据库与偏好配置工具类
- ✅ API Client / JSON 工具类占位

## 四、后续开发指导
### 1. 新增页面（Activity / Fragment）
1. **Activity**
   - 在 `ui/activity` 新建类，继承 `BaseActivity`
   - 实现 `getLayoutId()`、`initView()`、`initData()`
   - 在 `AndroidManifest.xml` 注册

2. **Fragment**
   - 在 `ui/fragment` 新建类，继承 `BaseFragment`
   - 实现 `getLayoutId()`、`initView()`、`initData()`
   - 用于底部导航切换（首页/生成/书架/我的）

### 2. 新增列表界面
1. 在 `ui/adapter` 新建对应 Adapter（继承 RecyclerView.Adapter）
2. 编写 `item_xxx.xml` 布局
3. 在 Activity/Fragment 中设置 LayoutManager + Adapter

### 3. 本地存储开发流程
1. 需要建表 → 在 `DBHelper` 中写 CREATE TABLE
2. 数据读写 → 在 `data/local/db` 新建 `XXXDao` 类
3. 简单配置 → 使用 `PrefsUtils`

### 4. AI 生成功能开发
1. 在 `data/remote/ApiClient` 实现 `generateStory(...)`
2. 入参：类型、篇幅、构思、用户自定义设定
3. 请求：OkHttp POST + JSON
4. 回调：返回故事内容 → 展示 → 本地保存

### 5. 交互与体验优化
- 加载状态：ProgressBar / 弹窗
- 错误提示：网络异常、生成失败、存储失败
- 页面跳转：Intent / Fragment 切换
- 流畅度：避免主线程耗时操作

### 6. 命名规范（保持统一）
- 布局：`activity_xxx` / `fragment_xxx` / `item_xxx`
- 控件：`tv_xxx` `rv_xxx` `btn_xxx` `et_xxx`
- 类：大驼峰；方法/变量：小驼峰
- 字符串统一放到 `strings.xml`


## 五、每周开发建议（对应 6 周计划）
1. **第1周**：完善框架、建库、建表、工具类验证
2. **第2周**：完成全部 UI 布局、页面跳转、底部导航
3. **第3周**：AI 生成 + 本地增删改查
4. **第4周**：人物画像、树形剧情、素材库
5. **第5周**：语音、个性化、主题、交互优化
6. **第6周**：测试、BUG 修复、文档、打包 APK


## 六、依赖说明（已内置）
- AndroidX + Material Design
- RecyclerView
- OkHttp（网络请求）
- Gson（JSON 解析）
- SQLite（本地数据库）
- 系统 TTS / SpeechRecognizer（语音，工具类占位）

## 七、提交规范（GitHub）
- 每周功能合并到 `main`
- 功能分支命名：`feature/模块名`
- 提交信息：`feat: 完成XXX` / `fix: 修复XXX` / `ui: 优化XXX`
