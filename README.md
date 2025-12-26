# 文件批量重命名工具

一个基于 Java 开发的命令行文件批量重命名工具，支持通过 GraalVM 编译为原生 Windows 可执行文件（.exe），无需 JVM 环境即可运行。

## 功能特性

### 核心功能

1. **8 种重命名模式**
   - **模式 0**：切换工作目录/文件（支持拖入文件或文件夹）
   - **模式 1**：匹配年份 (19xx/20xx) → 前置 [年份]
   - **模式 2**：匹配前 N 位字符
   - **模式 3**：匹配后 N 位字符（不含扩展名）
   - **模式 4**：匹配从位置 X 开始的 N 位字符
   - **模式 5**：匹配指定字符 X 之后的全部内容
   - **模式 6**：匹配指定字符 X 之前的全部内容
   - **模式 7**：匹配指定字符 X 之后 N 位字符
   - **模式 8**：匹配指定字符 X 之前 N 位字符

2. **便捷操作**
   - 支持拖入文件/文件夹到 exe 文件，自动识别路径
   - 支持运行时切换工作目录
   - 操作前预览，确认后执行
   - 支持撤销操作（undo），可回退上一步重命名

3. **安全特性**
   - 自动排除系统文件（.exe, .java 等）
   - 操作历史记录，支持撤销
   - 预览机制，避免误操作

## 使用方法

### 启动方式

#### 方式一：拖入文件/文件夹（推荐）

1. 将需要重命名的**文件夹**直接拖到 `FileRenameTool.exe` 上
   - 程序会自动使用该文件夹作为工作目录

2. 将需要重命名的**文件**直接拖到 `FileRenameTool.exe` 上
   - 程序会自动使用该文件所在的目录作为工作目录

#### 方式二：双击运行

- 双击 `FileRenameTool.exe`，程序会使用当前工作目录

#### 方式三：命令行启动

```bash
FileRenameTool.exe "C:\Users\Desktop\MyFolder"
```

### 操作流程

1. **启动程序**后，会显示主菜单：

```
========================================
   批量替换工具 - 当前目录/文件: E:\workspace\project\file-rename-tool
========================================
0. 切换工作目录/文件
1. 匹配年份 (19xx/20xx) -> 前置 [年份]
2. 匹配前 N 位字符
3. 匹配后 N 位字符
4. 匹配从位置 X 开始的 N 位字符
5. 匹配指定字符 X 之后的全部内容
6. 匹配指定字符 X 之前的全部内容
7. 匹配指定字符 X 之后 N 位字符
8. 匹配指定字符 X 之前 N 位字符
----------------------------------------
u. 回退上一步操作 (undo)
q. 退出程序 (quit)
请选择模式:
```

2. **选择模式**：输入对应的数字（0-8）

3. **输入参数**：根据选择的模式，输入相应的参数
   - 模式 1：无需额外参数
   - 模式 2-3：输入位数 N
   - 模式 4：输入起始位置 X 和长度 N
   - 模式 5-6：输入定位字符 X
   - 模式 7-8：输入定位字符 X 和截取长度 N

4. **输入替换内容**（模式 2-8）：
   - 输入要替换为的内容
   - 直接回车表示删除匹配到的内容

5. **预览确认**：程序会显示重命名预览，输入 `y` 确认执行

6. **撤销操作**：输入 `u` 可以撤销上一步操作

### 使用示例

#### 示例 1：年份前置

**场景**：将文件名 `2023年度报告.pdf` 重命名为 `[2023]2023年度报告.pdf`

1. 选择模式 `1`
2. 程序自动识别年份并添加前缀

#### 示例 2：删除前 3 位字符

**场景**：将文件名 `ABC测试文件.txt` 重命名为 `测试文件.txt`

1. 选择模式 `2`
2. 输入位数：`3`
3. 输入替换内容：直接回车（删除）
4. 确认执行

#### 示例 3：替换指定字符后的内容

**场景**：将文件名 `文档-旧版本.docx` 重命名为 `文档-新版本.docx`

1. 选择模式 `5`
2. 输入定位字符：`-`
3. 输入替换内容：`-新版本`
4. 确认执行

## 编译指南

### 环境准备 (Windows 平台)

由于编译过程涉及底层 C 代码的链接，你必须安装 C 编译器环境。

#### 1. 安装 Visual Studio

1. 下载并安装 [Visual Studio 2022](https://visualstudio.microsoft.com/)（或 2019）
2. 在安装程序中勾选：**"使用 C++ 的桌面开发"**
3. 确保安装了 Windows SDK 和 C++ 构建工具

#### 2. 安装 GraalVM

1. 从 [GraalVM 官网](https://www.graalvm.org/) 下载对应版本的 JDK（建议 JDK 17 或 21）
2. 解压到指定目录，例如：`C:\Program Files\GraalVM\graalvm-jdk-21`
3. 配置环境变量：
   - 将 `JAVA_HOME` 指向 GraalVM 目录
   - 将 `%JAVA_HOME%\bin` 添加到 `PATH`

#### 3. 安装 native-image 工具

在命令行执行：

```bash
gu install native-image
```

> **注意**：新版本 GraalVM 已默认集成 native-image，可跳过此步骤。

### 编译打包流程

#### 步骤 1：打开编译环境

搜索并运行：**x64 Native Tools Command Prompt for VS 2022**

> 这是 Visual Studio 提供的命令行工具，已配置好编译环境。

#### 步骤 2：定位到源码目录

```bash
cd E:\workspace\project\file-rename-tool\file-rename-tool\src
```

#### 步骤 3：编译 Java 源码

```bash
javac -encoding UTF-8 FileRenameTool.java
```

#### 步骤 4：生成原生可执行文件

```bash
native-image --no-fallback -Dfile.encoding=GBK -Dsun.stdout.encoding=GBK -Dsun.stderr.encoding=GBK FileRenameTool
```

**参数说明**：
- `--no-fallback`：禁用回退模式，确保生成真正的原生可执行文件
- `-Dfile.encoding=GBK`：设置文件编码为 GBK（支持中文路径和文件名）
- `-Dsun.stdout.encoding=GBK`：设置标准输出编码为 GBK
- `-Dsun.stderr.encoding=GBK`：设置标准错误输出编码为 GBK

#### 步骤 5：验证生成结果

编译完成后，会在当前目录生成 `FileRenameTool.exe` 文件（约 8-9MB）。

### 完整编译命令（单行）

```bash
cd E:\workspace\project\file-rename-tool\file-rename-tool\src && javac -encoding UTF-8 FileRenameTool.java && native-image --no-fallback -Dfile.encoding=GBK -Dsun.stdout.encoding=GBK -Dsun.stderr.encoding=GBK FileRenameTool
```

### 编译注意事项

1. **编码设置**：必须设置编码为 GBK，否则中文路径和文件名会出现乱码
2. **编译时间**：首次编译可能需要 1-2 分钟，请耐心等待
3. **文件大小**：生成的 exe 文件约 8-9MB，已包含所有运行时依赖
4. **兼容性**：生成的 exe 文件可在任何 Windows 10/11 系统上运行，无需安装 JVM

## 项目结构

```
file-rename-tool/
├── src/
│   └── FileRenameTool.java    # 主程序源码
├── out/                        # 编译输出目录
└── README.md                   # 项目说明文档
```

## 技术栈

- **开发语言**：Java
- **编译工具**：GraalVM Native Image
- **目标平台**：Windows (x64)
- **运行时**：无需 JVM，原生可执行文件

## 注意事项

1. **文件排除**：程序会自动排除 `.exe`、`.java` 等系统文件，避免误操作
2. **操作安全**：所有重命名操作都会先预览，确认后执行
3. **撤销功能**：支持撤销上一步操作，但仅支持撤销最近一次操作
4. **路径支持**：支持相对路径和绝对路径，支持中文路径
5. **拖入功能**：Windows 系统会自动为拖入的路径添加引号，程序会自动处理

## 常见问题

### Q: 编译时提示找不到 native-image 命令？

A: 确保已正确安装 GraalVM 并配置环境变量，或手动执行 `gu install native-image`。

### Q: 生成的 exe 文件无法运行？

A: 检查是否在正确的 Windows 平台上运行，确保是 x64 架构。

### Q: 中文路径或文件名显示乱码？

A: 确保编译时使用了 `-Dfile.encoding=GBK` 参数。

### Q: 拖入文件后程序没有反应？

A: 检查文件路径是否正确，确保文件或文件夹存在。

## 作者

August Lee

## 许可证

本项目采用 MIT 许可证。

## 更新日志

### v1.0.0 (2025/12/26)
- 初始版本发布
- 支持 8 种重命名模式
- 支持拖入文件/文件夹功能
- 支持操作撤销
- 支持 GraalVM 原生编译

