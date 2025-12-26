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

    private static final Scanner SCANNER = new Scanner(System.in);
    private static final Stack<List<RenamePair>> HISTORY = new Stack<>();

    static class RenamePair {
        File currentFile;
        String oldName;
        String newName;
        RenamePair(File currentFile, String oldName, String newName) {
            this.currentFile = currentFile;
            this.oldName = oldName;
            this.newName = newName;
        }
    }

    public static void main(String[] args) {
        String folderPath = new File(".").getAbsolutePath();
        if (folderPath.endsWith(".")) folderPath = folderPath.substring(0, folderPath.length() - 2);
        File folder = new File(folderPath);

        while (true) {
            System.out.println("\n========================================");
            System.out.println("   批量替换工具 - 当前目录: " + folderPath);
            System.out.println("========================================");
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

            String input = SCANNER.nextLine().trim().toLowerCase();
            if (input.equals("q") || input.equals("quit")) break;
            if (input.equals("u") || input.equals("undo")) { undoLastAction(); continue; }

            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= 8) prepareRename(folder, choice);
                else System.out.println("请输入 1-8 之间的数字！");
            } catch (Exception e) { System.out.println("无效输入。"); }
        }
    }

    private static void prepareRename(File folder, int choice) {
        String paramX = "";
        int paramN = 0, paramPos = 0;
        String replaceTo = "";

        try {
            switch (choice) {
                case 2: case 3: System.out.print("位数 N: "); paramN = Integer.parseInt(SCANNER.nextLine()); break;
                case 4: System.out.print("起始位置 X: "); paramPos = Integer.parseInt(SCANNER.nextLine());
                    System.out.print("长度 N: "); paramN = Integer.parseInt(SCANNER.nextLine()); break;
                case 5: case 6: System.out.print("定位字符 X: "); paramX = SCANNER.nextLine(); break;
                case 7: case 8: System.out.print("定位字符 X: "); paramX = SCANNER.nextLine();
                    System.out.print("截取长度 N: "); paramN = Integer.parseInt(SCANNER.nextLine()); break;
            }
        } catch (Exception e) { System.out.println("参数输入错误"); return; }

        if (choice != 1) {
            System.out.print("将匹配到的内容替换为 (直接回车代表删除): ");
            replaceTo = SCANNER.nextLine();
        }

        File[] files = folder.listFiles();
        if (files == null) return;

        List<RenamePair> previewList = new ArrayList<>();
        for (File file : files) {
            if (!file.isFile()) continue;
            String originalName = file.getName();
            if (originalName.endsWith(".exe") || originalName.equals("FileTransNameTest.class") || originalName.endsWith(".java")) continue;

            String matchedPart = extractInfo(originalName, choice, paramX, paramN, paramPos);

            if (matchedPart != null && !matchedPart.isEmpty()) {
                String newName;
                if (choice == 1) {
                    if (originalName.startsWith("[" + matchedPart + "]")) continue;
                    newName = "[" + matchedPart + "]" + originalName;
                } else {
                    newName = originalName.replaceFirst(Pattern.quote(matchedPart), replaceTo);
                }
                if (!originalName.equals(newName)) {
                    previewList.add(new RenamePair(file, originalName, newName));
                }
            }
        }

        if (previewList.isEmpty()) {
            System.out.println("未找到符合规则的匹配内容。");
            return;
        }

        System.out.println("\n--- [处理预览] ---");
        for (RenamePair p : previewList) System.out.println(p.oldName + "  ->  " + p.newName);
        System.out.print("确认执行以上更名操作? (y/n): ");

        if (SCANNER.nextLine().trim().equalsIgnoreCase("y")) {
            List<RenamePair> successList = new ArrayList<>();
            for (RenamePair p : previewList) {
                File target = new File(p.currentFile.getParent() + File.separator + p.newName);
                if (p.currentFile.renameTo(target)) {
                    p.currentFile = target;
                    successList.add(p);
                }
            }
            if (!successList.isEmpty()) {
                HISTORY.push(successList);
                System.out.println(">>> 成功处理 " + successList.size() + " 个文件。输入 'u' 可回退预览。");
            }
        } else {
            System.out.println(">>> 操作已取消。");
        }
    }

    private static void undoLastAction() {
        if (HISTORY.isEmpty()) {
            System.out.println("当前没有可回退的操作记录。");
            return;
        }

        // 1. 获取最近的一条记录（先不移除）
        List<RenamePair> lastAction = HISTORY.peek();

        System.out.println("\n--- [回退预览] ---");
        for (RenamePair p : lastAction) {
            // 回退预览：显示当前名字 -> 原始名字
            System.out.println(p.currentFile.getName() + "  ->  " + p.oldName);
        }
        System.out.print("确认回退以上操作? (y/确认): ");

        if (SCANNER.nextLine().trim().equalsIgnoreCase("y")) {
            // 确认后，从栈中弹出
            HISTORY.pop();
            int count = 0;
            for (RenamePair p : lastAction) {
                File undoFile = new File(p.currentFile.getParent() + File.separator + p.oldName);
                if (p.currentFile.renameTo(undoFile)) {
                    p.currentFile = undoFile;
                    count++;
                }
            }
            System.out.println(">>> 已成功回退 " + count + " 个文件。");
        } else {
            System.out.println(">>> 回退操作已取消。");
        }
    }

    private static String extractInfo(String name, int choice, String x, int n, int pos) {
        try {
            switch (choice) {
                case 1: Matcher m = Pattern.compile("19\\d{2}|20\\d{2}").matcher(name);
                    return m.find() ? m.group() : null;
                case 2: return name.substring(0, Math.min(name.length(), n));
                case 3: String noExt = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
                    return noExt.substring(Math.max(0, noExt.length() - n));
                case 4: return name.substring(pos, Math.min(name.length(), pos + n));
                case 5: return name.contains(x) ? name.substring(name.indexOf(x) + x.length()) : null;
                case 6: return name.contains(x) ? name.substring(0, name.indexOf(x)) : null;
                case 7: if (!name.contains(x)) return null;
                    int s = name.indexOf(x) + x.length();
                    return name.substring(s, Math.min(name.length(), s + n));
                case 8: if (!name.contains(x)) return null;
                    int e = name.indexOf(x);
                    return name.substring(Math.max(0, e - n), e);
                default: return null;
            }
        } catch (Exception e) { return null; }
    }

}
