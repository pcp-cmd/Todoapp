# CoolCat - TodoApp

[![Android Checks](https://github.com/pcp-cmd/Todoapp/actions/workflows/android-checks.yml/badge.svg)](https://github.com/pcp-cmd/Todoapp/actions/workflows/android-checks.yml)

一款基于 Jetpack Compose 构建的 Android 待办事项应用，采用 Claude 风格的温暖配色设计，支持任务分类、子任务、周期重复、日历同步和任务关联等完整功能。

## ✨ 功能特性

### 任务管理
- 创建、编辑、删除任务，支持标题、描述、截止日期、提醒时间
- 四级优先级（无 / 低 / 中 / 高）
- 一键勾选完成，记录完成时间戳
- 支持**子任务**（一级嵌套），可独立勾选和删除

### 分类系统
- 两级层级分类（父分类 → 子标签）
- 8 种预设配色方案
- 级联删除：删除父分类自动移除子分类

### 周期重复
- 支持 iCal RRULE 格式的重复规则
- 频率：每日 / 每周 / 每月 / 每年
- 支持间隔（INTERVAL）、指定星期（BYDAY）、指定日期（BYMONTHDAY）
- 完成任务后自动创建下一个周期实例

### 任务关联
- 双向关联任意两个任务
- 专属「关联」视图，一览所有任务关系
- 点击可直接跳转到任务详情

### 日历同步
- 将任务同步到 Android 系统日历
- 日历事件包含 10 分钟提前提醒
- 支持从日历通知深度链接回应用内任务详情
- 自动检测外部删除的日历事件并重建

### 其他
- 三种排序方式：按截止日期 / 优先级 / 创建时间
- 三种主题模式：浅色 / 深色 / 跟随系统
- 数据备份与恢复（JSON 格式导出/导入）
- 两种视图模式：时间线视图 / 分类视图

## 🛠 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **语言** | Kotlin | 1.9.20 |
| **UI** | Jetpack Compose (Material 3) | BOM 2024.01.00 |
| **数据库** | Room | 2.6.1 (KSP) |
| **偏好设置** | DataStore Preferences | 1.0.0 |
| **导航** | Navigation Compose | 2.7.6 |
| **架构** | MVVM + Repository | ViewModel 2.7.0 |
| **序列化** | Gson | 2.10.1 |
| **构建** | Gradle Kotlin DSL | AGP 8.2.0 |

## 📐 架构

```
┌─────────────────────────────────────────┐
│                   UI                     │
│  Jetpack Compose Screens + Components    │
├─────────────────────────────────────────┤
│               ViewModel                  │
│   TaskViewModel / CategoryViewModel      │
│            / SettingsViewModel           │
├─────────────────────────────────────────┤
│              Repository                  │
│  TaskRepository / CategoryRepository     │
│           / CalendarRepository           │
├─────────────────────────────────────────┤
│           Local Database                 │
│     Room (AppDatabase + DAOs)            │
│     DataStore (UserPreferences)          │
└─────────────────────────────────────────┘
```

- **MVVM 架构**：Compose UI → ViewModel → Repository → Room/DataStore
- **响应式数据流**：Room `Flow` → Repository → ViewModel `StateFlow` → Compose `collectAsState()`
- **单 Activity 架构**：通过 Compose Navigation 管理页面路由

## 📁 项目结构

```
app/src/main/java/com/todoapp/
├── data/
│   ├── entity/          # Room 实体：Task, Category, TaskLink
│   ├── local/           # 数据库 & DAO：AppDatabase, TaskDao, CategoryDao, TaskLinkDao
│   ├── preferences/     # DataStore 用户偏好设置
│   └── repository/      # 数据仓库：Task, Category, Calendar
├── domain/
│   └── RepeatRuleEngine.kt    # iCal 重复规则解析引擎
├── ui/
│   ├── MainActivity.kt        # 入口 + 底部导航
│   ├── navigation/NavGraph.kt # 路由 & 深度链接
│   ├── components/            # 可复用组件：TaskCard, TaskCheckbox, CategoryTag
│   ├── screens/
│   │   ├── task/              # 任务主页 & 任务详情
│   │   ├── category/          # 分类管理
│   │   ├── link/              # 关联视图
│   │   └── settings/          # 设置
│   └── theme/                 # 配色、字体、主题
└── util/
    ├── BackupManager.kt       # JSON 备份与恢复
    └── DateUtils.kt           # 日期格式化工具
```

## 🎨 设计风格

采用 Claude 风格的温暖配色：

- **浅色模式**：暖象牙色背景 `#FAF9F5`，赤陶色强调 `#C66B4D`，深棕色文字
- **深色模式**：暖黑色背景 `#1A1917`，浅赤陶强调 `#E89A6C`，暖白色文字
- **字体**：衬线体为主（Serif），搭配无衬线标签字体

## 🚀 构建运行

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17+
- Android SDK 34
- 最低支持 Android 8.0 (API 26)

### 构建步骤

```bash
# 克隆项目
git clone https://github.com/pcp-cmd/Todoapp.git
cd Todoapp

# 使用 Gradle 构建
./gradlew assembleDebug
```

或直接在 Android Studio 中打开项目，点击 Run 即可。

## 📄 License

本项目仅供学习参考。
