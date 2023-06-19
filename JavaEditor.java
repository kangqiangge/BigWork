package BigWork;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaEditor {
    private JFrame frame;
    private JTextPane textPane;
    private JFileChooser fileChooser;
    private Timer autoSaveTimer;

    private File currentFile; // 当前打开的文件

    public JavaEditor() {
        frame = new JFrame("Java Editor");

        // 创建菜单栏
        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);

        // 创建文件菜单
        JMenu fileMenu = new JMenu("文件");
        menuBar.add(fileMenu);

        // 创建文件菜单项：打开
        JMenuItem openMenuItem = new JMenuItem("打开");
        openMenuItem.addActionListener(new OpenFileListener());
        fileMenu.add(openMenuItem);

        // 创建文件菜单项：保存
        JMenuItem saveMenuItem = new JMenuItem("保存");
        saveMenuItem.addActionListener(new SaveFileListener());
        fileMenu.add(saveMenuItem);

        // 创建文件菜单项：退出
        JMenuItem exitMenuItem = new JMenuItem("退出");
        exitMenuItem.addActionListener(new ExitListener());
        fileMenu.add(exitMenuItem);

        // 创建编辑菜单
        JMenu editMenu = new JMenu("编辑");
        menuBar.add(editMenu);
        System.out.println();
        // 创建编辑菜单项：查找
        JMenuItem findMenuItem = new JMenuItem("查找");
        findMenuItem.addActionListener(new FindListener());
        editMenu.add(findMenuItem);

        // 创建编辑菜单项：替换
        JMenuItem replaceMenuItem = new JMenuItem("替换");
        replaceMenuItem.addActionListener(new ReplaceListener());
        editMenu.add(replaceMenuItem);

        // 创建文本编辑区域
        textPane = new JTextPane();
        frame.add(new JScrollPane(textPane), BorderLayout.CENTER);
        textPane.getDocument().addDocumentListener(new SyntaxHighlighter(textPane));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setVisible(true);

        fileChooser = new JFileChooser();

        // 创建定时器，每隔一定时间触发自动保存操作
        autoSaveTimer = new Timer(3000, new AutoSaveListener()); // 3000毫秒 = 3秒
        autoSaveTimer.setInitialDelay(3000); // 延迟3000毫秒 = 3秒开始执行
        autoSaveTimer.setRepeats(true); // 设置定时器重复执行

        autoSaveTimer.start(); // 启动定时器
    }

    private class OpenFileListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int returnVal = fileChooser.showOpenDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                currentFile = file;
                try {
                    // 读取文件内容到文本编辑区域
                    FileReader reader = new FileReader(file);
                    textPane.read(reader, null);
                    reader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private class SaveFileListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            saveFile();
        }
    }

    private class ExitListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            System.exit(0);
        }
    }

    private class FindListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String searchText = JOptionPane.showInputDialog(frame, "请输入要查找的字符串:");
            if (searchText != null && !searchText.isEmpty()) {
                String text = textPane.getText();
                Pattern pattern = Pattern.compile(Pattern.quote(searchText));
                Matcher matcher = pattern.matcher(text);
                Highlighter highlighter = textPane.getHighlighter();
                highlighter.removeAllHighlights();
                while (matcher.find()) {
                    try {
                        int start = matcher.start();
                        int end = matcher.end();
                        highlighter.addHighlight(start, end, DefaultHighlighter.DefaultPainter);
                    } catch (BadLocationException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    private class ReplaceListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String searchText = JOptionPane.showInputDialog(frame, "请输入要查找的字符串:");
            if (searchText != null && !searchText.isEmpty()) {
                String replaceText = JOptionPane.showInputDialog(frame, "请输入要替换的字符串:");
                if (replaceText != null) {
                    String text = textPane.getText();
                    Pattern pattern = Pattern.compile(Pattern.quote(searchText));
                    Matcher matcher = pattern.matcher(text);
                    String replacedText = matcher.replaceAll(replaceText);
                    textPane.setText(replacedText);
                }
            }
        }
    }

    private class AutoSaveListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            saveFileSilently(); // 无弹窗保存
            createOrUpdateBackupFile();
        }
    }

    private void saveFile() {
        File file = getSaveFile();
        if (file != null) {
            currentFile = file;
            try {
                // 将文本编辑区域的内容保存到文件
                FileWriter writer = new FileWriter(file);
                textPane.write(writer);
                writer.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void saveFileSilently() {
        if (currentFile != null) {
            try {
                // 将文本编辑区域的内容保存到文件
                FileWriter writer = new FileWriter(currentFile);
                textPane.write(writer);
                writer.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void createOrUpdateBackupFile() {
        if (currentFile != null) {
            Path sourcePath = currentFile.toPath();
            Path backupPath = getBackupFilePath(sourcePath);
            try {
                Files.copy(sourcePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private Path getBackupFilePath(Path sourceFilePath) {
        String sourceFileName = sourceFilePath.getFileName().toString();
        String backupFileName = sourceFileName + ".bak";
        return sourceFilePath.resolveSibling(backupFileName);
    }

    private File getSaveFile() {
        int returnVal = fileChooser.showSaveDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }
        return null;
    }

    class SyntaxHighlighter implements DocumentListener {
        private Set keywords;
        private Style keywordStyle;
        private Style normalStyle;
        public SyntaxHighlighter(JTextPane editor) {
// 准备着色使用的样式
            keywordStyle = ((StyledDocument) editor.getDocument()).addStyle("Keyword_Style", null);
            normalStyle = ((StyledDocument) editor.getDocument()).addStyle("Keyword_Style", null);
            StyleConstants.setForeground(keywordStyle, Color.RED);
            StyleConstants.setForeground(normalStyle, Color.BLACK);
// 准备关键字
            keywords = new HashSet();
            keywords.add("abstract");
            keywords.add("assert");
            keywords.add("boolean");
            keywords.add("break");
            keywords.add("byte");
            keywords.add("case");
            keywords.add("catch");
            keywords.add("char");
            keywords.add("class");
            keywords.add("const");
            keywords.add("continue");
            keywords.add("default");
            keywords.add("do");
            keywords.add("double");
            keywords.add("else");
            keywords.add("enum");
            keywords.add("extends");
            keywords.add("final");
            keywords.add("finally");
            keywords.add("float");
            keywords.add("for");
            keywords.add("if");
            keywords.add("implements");
            keywords.add("import");
            keywords.add("instanceof");
            keywords.add("int");
            keywords.add("interface");
            keywords.add("long");
            keywords.add("native");
            keywords.add("new");
            keywords.add("package");
            keywords.add("private");
            keywords.add("protected");
            keywords.add("public");
            keywords.add("return");
            keywords.add("short");
            keywords.add("static");
            keywords.add("strictfp");
            keywords.add("super");
            keywords.add("switch");
            keywords.add("synchronized");
            keywords.add("this");
            keywords.add("throw");
            keywords.add("throws");
            keywords.add("transient");
            keywords.add("try");
            keywords.add("void");
            keywords.add("volatile");
            keywords.add("while");
        }
        public void colouring(StyledDocument doc, int pos, int len) throws BadLocationException {
            int start = indexOfWordStart(doc, pos);
            int end = indexOfWordEnd(doc, pos + len);
            char ch;
            while (start < end) {
                ch = getCharAt(doc, start);
                if (Character.isLetter(ch) || ch == '_') {

// 如果是以字母或者下划线开头, 说明是单词
// pos为处理后的最后一个下标
                    start = colouringWord(doc, start);
                } else {
                    SwingUtilities.invokeLater(new ColouringTask(doc, start, 1, normalStyle));
                    ++start;
                }
            }
        }

        /**

         * 对单词进行着色, 并返回单词结束的下标.
         */

        public int colouringWord(StyledDocument doc, int pos) throws BadLocationException {

            int wordEnd = indexOfWordEnd(doc, pos);

            String word = doc.getText(pos, wordEnd - pos);

            if (keywords.contains(word)) {

// 如果是关键字, 就进行关键字的着色, 否则使用普通的着色.

// 这里有一点要注意, 在insertUpdate和removeUpdate的方法调用的过程中, 不能修改doc的属性.

// 但我们又要达到能够修改doc的属性, 所以把此任务放到这个方法的外面去执行.

// 实现这一目的, 可以使用新线程, 但放到swing的事件队列里去处理更轻便一点.

                SwingUtilities.invokeLater(new ColouringTask(doc, pos, wordEnd - pos, keywordStyle));

            } else {

                SwingUtilities.invokeLater(new ColouringTask(doc, pos, wordEnd - pos, normalStyle));

            }

            return wordEnd;

        }

        /**

         * 取得在文档中下标在pos处的字符.

         *

         * 如果pos为doc.getLength(), 返回的是一个文档的结束符, 不会抛出异常. 如果pos<0, 则会抛出异常.

         * 所以pos的有效值是[0, doc.getLength()]

         *

         * @param doc

         * @param pos

         * @return

         * @throws BadLocationException

         */

        public char getCharAt(Document doc, int pos) throws BadLocationException {

            return doc.getText(pos, 1).charAt(0);

        }

        /**

         * 取得下标为pos时, 它所在的单词开始的下标. Â±wor^dÂ± (^表示pos, Â±表示开始或结束的下标)

         *

         * @param doc

         * @param pos

         * @return

         * @throws BadLocationException

         */

        public int indexOfWordStart(Document doc, int pos) throws BadLocationException {

// 从pos开始向前找到第一个非单词字符.

            for (; pos > 0 && isWordCharacter(doc, pos - 1); --pos);

            return pos;

        }

        /**

         * 取得下标为pos时, 它所在的单词结束的下标. Â±wor^dÂ± (^表示pos, Â±表示开始或结束的下标)

         *

         * @param doc

         * @param pos

         * @return

         * @throws BadLocationException

         */

        public int indexOfWordEnd(Document doc, int pos) throws BadLocationException {

// 从pos开始向前找到第一个非单词字符.

            for (; isWordCharacter(doc, pos); ++pos);

            return pos;

        }

        /**

         * 如果一个字符是字母, 数字, 下划线, 则返回true.

         *

         * @param doc

         * @param pos

         * @throws BadLocationException

         */

        public boolean isWordCharacter(Document doc, int pos) throws BadLocationException {

            char ch = getCharAt(doc, pos);

            if (Character.isLetter(ch) || Character.isDigit(ch) || ch == '_') { return true; }

            return false;

        }

        @Override

        public void changedUpdate(DocumentEvent e) {

        }

        @Override

        public void insertUpdate(DocumentEvent e) {

            try {

                colouring((StyledDocument) e.getDocument(), e.getOffset(), e.getLength());

            } catch (BadLocationException e1) {

                e1.printStackTrace();

            }

        }

        @Override

        public void removeUpdate(DocumentEvent e) {

            try {

// 因为删除后光标紧接着影响的单词两边, 所以长度就不需要了

                colouring((StyledDocument) e.getDocument(), e.getOffset(), 0);

            } catch (BadLocationException e1) {

                e1.printStackTrace();

            }

        }
        private class ColouringTask implements Runnable {
            private StyledDocument doc;
            private Style style;
            private int pos;
            private int len;
            public ColouringTask(StyledDocument doc, int pos, int len, Style style) {

                this.doc = doc;

                this.pos = pos;

                this.len = len;

                this.style = style;

            }

            public void run() {
                try {

// 这里就是对字符进行着色
                    doc.setCharacterAttributes(pos, len, style, true);

                } catch (Exception e) {}

            }

        }

    }



    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new JavaEditor();
            }
        });
    }
}
