import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件批量重命名工具
 * 模式1：年份前置 [YYYY]文件名
 * 模式2-8：精准匹配并替换
 *
 * @author August Lee
 * @since 2025/12/26 16:40
 */
public class FileRenameTool {

    // 标准输入扫描器，用于读取用户输入
    private static final Scanner SCANNER = new Scanner(System.in);
    // 操作历史栈，用于存储每次重命名操作，支持撤销功能
    private static final Stack<List<RenamePair>> HISTORY = new Stack<>();

    // 常量定义
    private static final int MIN_CHOICE = 0;  // 最小模式编号（0为切换目录）
    private static final int MAX_CHOICE = 8;  // 最大模式编号
    private static final String YEAR_PATTERN = "19\\d{2}|20\\d{2}";  // 年份匹配正则表达式（1900-2099）
    private static final String[] EXCLUDED_EXTENSIONS = {".exe", ".java"};  // 需要排除的文件扩展名
    private static final String EXCLUDED_CLASS = "FileRenameTool.class";  // 需要排除的特定类文件

    /**
     * 重命名对，用于记录文件重命名操作
     */
    static class RenamePair {
        File currentFile;    // 当前文件对象（重命名后会更新）
        String oldName;      // 原始文件名
        String newName;      // 新文件名

        RenamePair(File currentFile, String oldName, String newName) {
            this.currentFile = currentFile;
            this.oldName = oldName;
            this.newName = newName;
        }
    }

    /**
     * 主方法
     * 支持从命令行参数读取路径（拖入文件/文件夹时会自动传入）
     *
     * @param args 命令行参数，第一个参数为路径（可选）
     */
    public static void main(String[] args) {
        // 初始化工作目录：优先使用命令行参数，否则使用当前目录
        File folder = initializeWorkingDirectory(args);
        String folderPath = folder.getAbsolutePath();

        // 主循环：持续显示菜单并处理用户输入
        while (true) {
            printMenu(folderPath);

            // 读取并规范化用户输入（转小写、去空格）
            String input = SCANNER.nextLine().trim().toLowerCase();
            
            // 处理退出命令
            if (isQuitCommand(input)) {
                break;
            }
            
            // 处理撤销命令
            if (isUndoCommand(input)) {
                undoLastAction();
                continue;
            }

            // 解析数字输入并执行对应的操作
            try {
                int choice = Integer.parseInt(input);
                if (choice == 0) {
                    // 选项0：切换工作目录
                    File newFolder = changeDirectory();
                    if (newFolder != null) {
                        folder = newFolder;
                        folderPath = folder.getAbsolutePath();
                        System.out.println(">>> 已切换到: " + folderPath);
                    }
                } else if (choice >= 1 && choice <= MAX_CHOICE) {
                    // 选项1-8：执行重命名操作
                    prepareRename(folder, choice);
                } else {
                    System.out.println("请输入 " + MIN_CHOICE + "-" + MAX_CHOICE + " 之间的数字！");
                }
            } catch (NumberFormatException e) {
                System.out.println("无效输入。");
            }
        }
    }

    /**
     * 打印主菜单
     *
     * @param folderPath 当前文件夹路径
     */
    private static void printMenu(String folderPath) {
        System.out.println("\n========================================");
        System.out.println("   批量替换工具 - 当前目录/文件: " + folderPath);
        System.out.println("========================================");
        System.out.println("0. 切换工作目录/文件");
        System.out.println("1. 匹配年份 (19xx/20xx) -> 前置 [年份]");
        System.out.println("2. 匹配前 N 位字符");
        System.out.println("3. 匹配后 N 位字符");
        System.out.println("4. 匹配从位置 X 开始的 N 位字符");
        System.out.println("5. 匹配指定字符 X 之后的全部内容");
        System.out.println("6. 匹配指定字符 X 之前的全部内容");
        System.out.println("7. 匹配指定字符 X 之后 N 位字符");
        System.out.println("8. 匹配指定字符 X 之前 N 位字符");
        System.out.println("----------------------------------------");
        System.out.println("u. 回退上一步操作 (undo)");
        System.out.println("q. 退出程序 (quit)");
        System.out.print("请选择模式: ");
    }

    /**
     * 初始化工作目录
     * 优先使用命令行参数，如果没有参数则使用当前目录
     *
     * @param args 命令行参数
     * @return 工作目录的 File 对象
     */
    private static File initializeWorkingDirectory(String[] args) {
        // 如果有命令行参数，使用第一个参数作为路径
        if (args != null && args.length > 0 && args[0] != null && !args[0].trim().isEmpty()) {
            File dir = validateAndGetDirectory(args[0]);
            if (dir != null) {
                return dir;
            }
            // 如果路径无效，提示并继续使用当前目录
            System.out.println("警告: 命令行参数路径无效，使用当前目录。");
        }
        
        // 默认使用当前工作目录
        String folderPath = new File(".").getAbsolutePath();
        // 移除路径末尾的 "." 字符
        if (folderPath.endsWith(".")) {
            folderPath = folderPath.substring(0, folderPath.length() - 2);
        }
        return new File(folderPath);
    }

    /**
     * 切换工作目录
     * 允许用户输入或拖入路径（支持文件或文件夹）
     *
     * @return 新的工作目录 File 对象，如果取消或失败则返回 null
     */
    private static File changeDirectory() {
        System.out.print("请输入目录/文件路径（可直接拖入文件或文件夹）: ");
        String input = SCANNER.nextLine().trim();
        
        if (input.isEmpty()) {
            System.out.println(">>> 操作已取消。");
            return null;
        }
        
        // 规范化路径（去除引号等）
        String normalizedPath = normalizePath(input);
        File dir = validateAndGetDirectory(normalizedPath);
        
        if (dir != null) {
            return dir;
        } else {
            System.out.println(">>> 路径无效或不存在，请检查后重试。");
            return null;
        }
    }

    /**
     * 规范化路径
     * 去除路径两端的引号和空格（Windows拖入文件时会自动加引号）
     *
     * @param path 原始路径
     * @return 规范化后的路径
     */
    private static String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        
        // 去除首尾空格
        path = path.trim();
        
        // 去除首尾的引号（支持单引号和双引号）
        if ((path.startsWith("\"") && path.endsWith("\"")) ||
            (path.startsWith("'") && path.endsWith("'"))) {
            path = path.substring(1, path.length() - 1);
        }
        
        return path.trim();
    }

    /**
     * 验证并获取目录
     * 如果输入的是文件，返回文件所在目录；如果是文件夹，直接返回
     *
     * @param path 路径字符串
     * @return 目录的 File 对象，如果路径无效则返回 null
     */
    private static File validateAndGetDirectory(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        
        File file = new File(path);
        
        // 检查文件或目录是否存在
        if (!file.exists()) {
            return null;
        }
        
        // 如果是文件，返回其所在目录
        if (file.isFile()) {
            return file.getParentFile();
        }
        
        // 如果是目录，直接返回
        if (file.isDirectory()) {
            return file;
        }
        
        return null;
    }

    /**
     * 判断是否为退出命令
     *
     * @param input 用户输入
     * @return true 如果是退出命令
     */
    private static boolean isQuitCommand(String input) {
        return "q".equals(input) || "quit".equals(input);
    }

    /**
     * 判断是否为回退命令
     *
     * @param input 用户输入
     * @return true 如果是回退命令
     */
    private static boolean isUndoCommand(String input) {
        return "u".equals(input) || "undo".equals(input);
    }

    /**
     * 准备重命名操作
     * 根据用户选择的模式，收集参数并执行重命名流程
     *
     * @param folder 目标文件夹
     * @param choice 选择的模式（1-8）
     */
    private static void prepareRename(File folder, int choice) {
        // 初始化参数变量
        String paramX = "";      // 定位字符（用于模式5-8）
        int paramN = 0;           // 位数/长度（用于模式2-4, 7-8）
        int paramPos = 0;         // 起始位置（用于模式4）
        String replaceTo = "";    // 替换内容（用于模式2-8）

        // 根据模式获取用户输入的参数
        try {
            paramX = getParameterX(choice);
            paramN = getParameterN(choice);
            paramPos = getParameterPos(choice);
        } catch (NumberFormatException e) {
            System.out.println("参数输入错误");
            return;
        }

        // 模式1是年份前置，不需要替换内容；其他模式需要用户输入替换内容
        if (choice != 1) {
            System.out.print("将匹配到的内容替换为 (直接回车代表删除): ");
            replaceTo = SCANNER.nextLine();
        }

        // 获取文件夹中的所有文件
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }

        // 遍历文件，生成重命名预览列表
        List<RenamePair> previewList = buildPreviewList(files, choice, paramX, paramN, paramPos, replaceTo);

        // 如果没有匹配的文件，提示用户并返回
        if (previewList.isEmpty()) {
            System.out.println("未找到符合规则的匹配内容。");
            return;
        }

        // 显示预览并等待用户确认
        showPreview(previewList);
        System.out.print("确认执行以上更名操作? (y/n): ");

        // 用户确认后执行重命名，否则取消操作
        if (SCANNER.nextLine().trim().equalsIgnoreCase("y")) {
            executeRename(previewList);
        } else {
            System.out.println(">>> 操作已取消。");
        }
    }

    /**
     * 获取参数 X（定位字符）
     *
     * @param choice 选择的模式
     * @return 参数 X 的值
     */
    private static String getParameterX(int choice) {
        if (choice == 5 || choice == 6 || choice == 7 || choice == 8) {
            System.out.print("定位字符 X: ");
            return SCANNER.nextLine();
        }
        return "";
    }

    /**
     * 获取参数 N（位数/长度）
     *
     * @param choice 选择的模式
     * @return 参数 N 的值
     */
    private static int getParameterN(int choice) {
        if (choice == 2 || choice == 3) {
            System.out.print("位数 N: ");
            return Integer.parseInt(SCANNER.nextLine());
        } else if (choice == 4) {
            System.out.print("长度 N: ");
            return Integer.parseInt(SCANNER.nextLine());
        } else if (choice == 7 || choice == 8) {
            System.out.print("截取长度 N: ");
            return Integer.parseInt(SCANNER.nextLine());
        }
        return 0;
    }

    /**
     * 获取参数 Pos（起始位置）
     *
     * @param choice 选择的模式
     * @return 参数 Pos 的值
     */
    private static int getParameterPos(int choice) {
        if (choice == 4) {
            System.out.print("起始位置 X: ");
            return Integer.parseInt(SCANNER.nextLine());
        }
        return 0;
    }

    /**
     * 构建预览列表
     * 遍历所有文件，根据选择的模式提取匹配内容并生成新文件名
     *
     * @param files    文件数组
     * @param choice   选择的模式
     * @param paramX   参数 X（定位字符）
     * @param paramN   参数 N（位数/长度）
     * @param paramPos 参数 Pos（起始位置）
     * @param replaceTo 替换内容
     * @return 预览列表，包含所有需要重命名的文件对
     */
    private static List<RenamePair> buildPreviewList(File[] files, int choice, String paramX,
                                                      int paramN, int paramPos, String replaceTo) {
        List<RenamePair> previewList = new ArrayList<>();

        // 遍历文件夹中的所有文件
        for (File file : files) {
            // 跳过目录，只处理文件
            if (!file.isFile()) {
                continue;
            }

            String originalName = file.getName();
            // 跳过需要排除的文件（如 .exe, .java 等）
            if (shouldSkipFile(originalName)) {
                continue;
            }

            // 根据选择的模式从文件名中提取匹配内容
            String matchedPart = extractInfo(originalName, choice, paramX, paramN, paramPos);

            // 如果成功提取到匹配内容，生成新文件名
            if (matchedPart != null && !matchedPart.isEmpty()) {
                String newName = generateNewName(originalName, matchedPart, choice, replaceTo);
                // 只有当新文件名与旧文件名不同时，才添加到预览列表
                if (newName != null && !originalName.equals(newName)) {
                    previewList.add(new RenamePair(file, originalName, newName));
                }
            }
        }

        return previewList;
    }

    /**
     * 判断是否应该跳过该文件
     *
     * @param fileName 文件名
     * @return true 如果应该跳过
     */
    private static boolean shouldSkipFile(String fileName) {
        if (fileName.equals(EXCLUDED_CLASS)) {
            return true;
        }
        for (String ext : EXCLUDED_EXTENSIONS) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 生成新文件名
     *
     * @param originalName 原始文件名
     * @param matchedPart 匹配到的部分
     * @param choice      选择的模式
     * @param replaceTo   替换内容
     * @return 新文件名，如果不需要重命名则返回 null
     */
    private static String generateNewName(String originalName, String matchedPart, int choice, String replaceTo) {
        if (choice == 1) {
            // 模式1：年份前置
            if (originalName.startsWith("[" + matchedPart + "]")) {
                return null; // 已经包含年份前缀，跳过
            }
            return "[" + matchedPart + "]" + originalName;
        } else {
            // 模式2-8：替换匹配内容
            return originalName.replaceFirst(Pattern.quote(matchedPart), replaceTo);
        }
    }

    /**
     * 显示预览
     *
     * @param previewList 预览列表
     */
    private static void showPreview(List<RenamePair> previewList) {
        System.out.println("\n--- [处理预览] ---");
        for (RenamePair pair : previewList) {
            System.out.println(pair.oldName + "  ->  " + pair.newName);
        }
    }

    /**
     * 执行重命名操作
     * 将预览列表中的文件逐一重命名，并记录到历史栈中以便撤销
     *
     * @param previewList 预览列表
     */
    private static void executeRename(List<RenamePair> previewList) {
        List<RenamePair> successList = new ArrayList<>();

        // 遍历预览列表，执行实际的重命名操作
        for (RenamePair pair : previewList) {
            // 构建目标文件路径
            File target = new File(pair.currentFile.getParent() + File.separator + pair.newName);
            // 执行重命名，如果成功则更新文件对象并记录到成功列表
            if (pair.currentFile.renameTo(target)) {
                pair.currentFile = target;  // 更新为新的文件对象
                successList.add(pair);
            }
        }

        // 如果有成功重命名的文件，将其推入历史栈以便后续撤销
        if (!successList.isEmpty()) {
            HISTORY.push(successList);
            System.out.println(">>> 成功处理 " + successList.size() + " 个文件。输入 'u' 可回退预览。");
        }
    }

    /**
     * 回退上一步操作
     * 从历史栈中取出最近一次操作，将文件恢复为原始名称
     */
    private static void undoLastAction() {
        // 检查是否有可回退的历史记录
        if (HISTORY.isEmpty()) {
            System.out.println("当前没有可回退的操作记录。");
            return;
        }

        // 获取最近的一条记录（使用 peek 先不移除，等用户确认后再移除）
        List<RenamePair> lastAction = HISTORY.peek();

        // 显示回退预览，让用户确认
        System.out.println("\n--- [回退预览] ---");
        for (RenamePair pair : lastAction) {
            // 显示：当前文件名 -> 原始文件名
            System.out.println(pair.currentFile.getName() + "  ->  " + pair.oldName);
        }
        System.out.print("确认回退以上操作? (y/确认): ");

        // 用户确认后执行回退操作
        if (SCANNER.nextLine().trim().equalsIgnoreCase("y")) {
            // 确认后，从栈中弹出该记录
            HISTORY.pop();
            int count = 0;

            // 遍历所有文件，将文件名恢复为原始名称
            for (RenamePair pair : lastAction) {
                File undoFile = new File(pair.currentFile.getParent() + File.separator + pair.oldName);
                if (pair.currentFile.renameTo(undoFile)) {
                    pair.currentFile = undoFile;  // 更新文件对象
                    count++;
                }
            }

            System.out.println(">>> 已成功回退 " + count + " 个文件。");
        } else {
            System.out.println(">>> 回退操作已取消。");
        }
    }

    /**
     * 从文件名中提取信息
     * 根据不同的模式，从文件名中提取匹配的部分
     *
     * @param name   文件名
     * @param choice 选择的模式（1-8）
     * @param x      参数 X（定位字符，用于模式5-8）
     * @param n      参数 N（位数/长度，用于模式2-4, 7-8）
     * @param pos    参数 Pos（起始位置，用于模式4）
     * @return 提取到的信息，如果提取失败则返回 null
     */
    private static String extractInfo(String name, int choice, String x, int n, int pos) {
        try {
            switch (choice) {
                case 1:
                    // 模式1：匹配年份 (19xx/20xx)
                    // 使用正则表达式查找文件名中的年份，返回第一个匹配的年份
                    Matcher matcher = Pattern.compile(YEAR_PATTERN).matcher(name);
                    return matcher.find() ? matcher.group() : null;

                case 2:
                    // 模式2：匹配前 N 位字符
                    // 从文件名开头截取 N 个字符（如果文件名长度不足 N，则截取全部）
                    return name.substring(0, Math.min(name.length(), n));

                case 3:
                    // 模式3：匹配后 N 位字符（不含扩展名）
                    // 先去除扩展名，然后从末尾截取 N 个字符
                    String nameWithoutExt = name.contains(".") 
                            ? name.substring(0, name.lastIndexOf('.')) 
                            : name;
                    return nameWithoutExt.substring(Math.max(0, nameWithoutExt.length() - n));

                case 4:
                    // 模式4：匹配从位置 X 开始的 N 位字符
                    // 从指定位置 pos 开始，截取 N 个字符
                    return name.substring(pos, Math.min(name.length(), pos + n));

                case 5:
                    // 模式5：匹配指定字符 X 之后的全部内容
                    // 找到字符 X 的位置，返回 X 之后的所有字符
                    return name.contains(x) ? name.substring(name.indexOf(x) + x.length()) : null;

                case 6:
                    // 模式6：匹配指定字符 X 之前的全部内容
                    // 找到字符 X 的位置，返回 X 之前的所有字符
                    return name.contains(x) ? name.substring(0, name.indexOf(x)) : null;

                case 7:
                    // 模式7：匹配指定字符 X 之后 N 位字符
                    // 找到字符 X 的位置，返回 X 之后 N 个字符
                    if (!name.contains(x)) {
                        return null;
                    }
                    int startIndex = name.indexOf(x) + x.length();
                    return name.substring(startIndex, Math.min(name.length(), startIndex + n));

                case 8:
                    // 模式8：匹配指定字符 X 之前 N 位字符
                    // 找到字符 X 的位置，返回 X 之前 N 个字符
                    if (!name.contains(x)) {
                        return null;
                    }
                    int endIndex = name.indexOf(x);
                    return name.substring(Math.max(0, endIndex - n), endIndex);

                default:
                    return null;
            }
        } catch (Exception e) {
            // 捕获所有异常（如索引越界等），返回 null 表示提取失败
            return null;
        }
    }
}
