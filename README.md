# AI 故事生成助手 - Android 项目
适用于课程大作业、GitHub 仓库展示、小组协作开发说明

---

## 一、项目介绍
**项目名称**：AI 故事生成助手（Android 本地版）
**开发环境**：Android Studio + Java
**数据方案**：全本地存储（SQLite + SharedPreferences），仅调用大模型 API 生成内容，**无云端部署**
**项目目标**：完成一个可正常运行、界面美观、功能完整的 Android 应用原型，熟悉移动端 UI、数据存储、网络请求、交互逻辑开发。

### 核心功能
- 启动页、首页、故事生成、人物画像、平行剧情、我的书架、个人中心、设置
- AI 故事生成（类型/篇幅/构思 → 大纲+正文）
- 本地保存/修改/删除/书架管理
- 人物卡片滑动、树形多结局剧情展示
- 素材库（分类、搜索、筛选、插入）
- 本地数据存储、用户行为记录、导出预览
- 交互优化：加载提示、错误重试、页面跳转、滑动展开
- 创意扩展：语音输入/朗读、个性化推荐、社交基础、主题设置

---

## 二、当前项目结构
包路径：`com.example.storyteller`

```
com.example.storyteller
├── base                  # 基础父类（统一规范）
│   ├── BaseActivity      // 所有 Activity 继承
│   └── BaseFragment      // 所有 Fragment 继承
├── ui
│   ├── activity          // 页面 Activity
│   ├── fragment          // 碎片（首页/书架/个人中心等）
│   └── adapter           // 列表适配器（故事/人物/素材）
├── model                 // 数据实体类
│   ├── Story.java        // 故事模型
│   ├── Character.java    // 人物模型
│   └── 后续可新增 Plot、Material 等
├── data                  # 数据层
│   ├── local             // 本地存储
│   │   ├── db            // SQLite 数据库（建表、DAO）
│   │   └── prefs         // SharedPreferences 配置存储
│   └── remote            // 网络请求（仅大模型 API）
├── utils                 // 通用工具类
│   ├── JsonUtils.java    // JSON 解析
│   ├── AudioUtils.java   // 语音输入/朗读
│   └── 后续可新增 ToastUtils、LogUtils 等
```

---

## 三、当前完成情况
- ✅ 项目初始化、GitHub 仓库搭建
- ✅ 基础包结构建立
- ✅ BaseActivity / BaseFragment 完成
- ✅ 核心模型（Story、Character）完成
- ✅ 数据库 DBHelper、配置 PrefsUtils 完成
- ✅ API 客户端 ApiClient 完成
- ✅ 首页 Fragment、列表适配器 StoryAdapter 占位完成
- ✅ 通用工具类占位完成

---

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

---

## 五、每周开发建议（对应 6 周计划）
1. **第1周**：完善框架、建库、建表、工具类验证
2. **第2周**：完成全部 UI 布局、页面跳转、底部导航
3. **第3周**：AI 生成 + 本地增删改查
4. **第4周**：人物画像、树形剧情、素材库
5. **第5周**：语音、个性化、主题、交互优化
6. **第6周**：测试、BUG 修复、文档、打包 APK

---

## 六、依赖说明（已内置）
- AndroidX + Material Design
- OkHttp（网络请求）
- Gson（JSON 解析）
- SQLite（本地数据库）
- 系统 TTS / SpeechRecognizer（语音）

---

## 七、提交规范（GitHub）
- 每周功能合并到 `main`
- 功能分支命名：`feature/模块名`
- 提交信息：`feat: 完成XXX` / `fix: 修复XXX` / `ui: 优化XXX`

---

