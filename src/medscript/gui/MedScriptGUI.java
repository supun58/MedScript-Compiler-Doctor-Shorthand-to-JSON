package medscript.gui;

import medscript.compiler.*;
import medscript.compiler.AST.*;
import medscript.compiler.Parser.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.*;
import java.util.*;

public class MedScriptGUI extends JFrame {

    private final JTextArea inputArea = new JTextArea(20, 60);
    private final JTextArea jsonArea = new JTextArea(20, 60);
    private final JTextArea diagArea = new JTextArea(12, 60);

    private final DefaultTableModel tokenModel = new DefaultTableModel(
            new Object[]{"Type", "Lexeme", "Line", "Col"}, 0);

    private static final Color PRIMARY_COLOR = new Color(41, 128, 185);
    private static final Color SUCCESS_COLOR = new Color(39, 174, 96);
    private static final Color DANGER_COLOR = new Color(231, 76, 60);
    private static final Color BG_COLOR = new Color(245, 246, 250);
    private static final Color PANEL_COLOR = Color.WHITE;
    private static final Color TEXT_COLOR = new Color(44, 62, 80);

    public MedScriptGUI() {
        super("MedScript Compiler - Doctor Shorthand → JSON");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(15, 15));
        getContentPane().setBackground(BG_COLOR);

        getContentPane().setBackground(BG_COLOR);

        inputArea.setFont(new Font("Consolas", Font.PLAIN, 15));
        inputArea.setLineWrap(false);
        inputArea.setBackground(PANEL_COLOR);
        inputArea.setForeground(TEXT_COLOR);
        inputArea.setCaretColor(PRIMARY_COLOR);
        inputArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane inScroll = new JScrollPane(inputArea);
        inScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(PRIMARY_COLOR, 2), 
            " Input (MedScript) ",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14),
            PRIMARY_COLOR));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabs.setBackground(BG_COLOR);

        JTable tokenTable = new JTable(tokenModel);
        tokenTable.setFont(new Font("Consolas", Font.PLAIN, 13));
        tokenTable.setRowHeight(25);
        tokenTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tokenTable.getTableHeader().setBackground(PRIMARY_COLOR);
        tokenTable.getTableHeader().setForeground(Color.WHITE);
        JScrollPane tokenScroll = new JScrollPane(tokenTable);
        tabs.addTab("Tokens", tokenScroll);

        diagArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        diagArea.setEditable(false);
        diagArea.setBackground(PANEL_COLOR);
        diagArea.setForeground(TEXT_COLOR);
        diagArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane diagScroll = new JScrollPane(diagArea);
        tabs.addTab("Diagnostics", diagScroll);

        jsonArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        jsonArea.setEditable(false);
        jsonArea.setBackground(PANEL_COLOR);
        jsonArea.setForeground(TEXT_COLOR);
        jsonArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane jsonScroll = new JScrollPane(jsonArea);
        tabs.addTab("JSON Output", jsonScroll);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inScroll, tabs);
        split.setResizeWeight(0.5);
        split.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        btns.setBackground(BG_COLOR);
        
        JButton compileBtn = createStyledButton("Compile", SUCCESS_COLOR);
        JButton clearBtn = createStyledButton("Clear", DANGER_COLOR);
        JButton examplesBtn = createStyledButton("Load Examples", PRIMARY_COLOR);
        
        btns.add(compileBtn);
        btns.add(clearBtn);
        btns.add(examplesBtn);

        compileBtn.addActionListener(e -> compile());
        clearBtn.addActionListener(e -> clearAll());
        
        JPopupMenu examplesMenu = new JPopupMenu();
        examplesMenu.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        JMenuItem sample1 = new JMenuItem("[OK] Sample OK");
        JMenuItem sample2 = new JMenuItem("[ERROR] Sample Syntax Error");
        JMenuItem sample3 = new JMenuItem("[WARNING] Sample Semantic Error");
        
        sample1.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sample2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sample3.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        sample1.addActionListener(e -> loadSample("samples/sample_ok.med"));
        sample2.addActionListener(e -> loadSample("samples/sample_syntax_error.med"));
        sample3.addActionListener(e -> loadSample("samples/sample_semantic_error.med"));
        
        examplesMenu.add(sample1);
        examplesMenu.add(sample2);
        examplesMenu.add(sample3);
        
        examplesBtn.addActionListener(e -> {
            examplesMenu.show(examplesBtn, 0, examplesBtn.getHeight());
        });

        add(btns, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);

    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(140, 35));
        
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(bgColor.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(bgColor);
            }
        });
        
        return btn;
    }

    private void compile() {
        String input = inputArea.getText();
        tokenModel.setRowCount(0);
        diagArea.setText("");
        jsonArea.setText("");

        try {
            // Tokens
            MedLexer lx = new MedLexer(new StringReader(input));
            Token t;
            do {
                t = lx.nextToken();
                tokenModel.addRow(new Object[]{t.type, t.lexeme, t.line, t.column});
            } while (t.type != TokenType.EOF);

            // Parse
            Parser parser = new Parser(new StringReader(input));
            ParseResult pr = parser.parse();

            // Semantic
            SemanticAnalyzer sem = new SemanticAnalyzer();
            java.util.List<Diagnostic> semDiags = sem.analyze(pr.program);

            java.util.List<Diagnostic> all = new ArrayList<>();
            all.addAll(pr.diagnostics);
            all.addAll(semDiags);

            if (all.isEmpty()) diagArea.append("✅ No errors/warnings.\n");
            else for (Diagnostic d: all) diagArea.append(d.toString() + "\n");

            jsonArea.setText(JsonEmitter.toJson(pr.program));

        } catch (Exception ex) {
            diagArea.setText("ERROR: " + ex.getMessage());
        }
    }

    private void clearAll() {
        inputArea.setText("");
        jsonArea.setText("");
        diagArea.setText("");
        tokenModel.setRowCount(0);
    }

    private void loadSample(String filePath) {
        try {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            inputArea.setText(content.toString());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, 
                "Error loading file: " + ex.getMessage(), 
                "File Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private String defaultSample() {
        return ""
        + "patient Nimal age 22 weight 58kg\n"
        + "allergy penicillin\n\n"
        + "rx:\n"
        + "  Tab PCM 500mg po tds 5d after_food\n"
        + "  Cap Amox 250mg po bd 7d\n"
        + "  Syr Cetirizine 5mg/5ml 10ml hs 3d\n"
        + "notes:\n"
        + "  avoid alcohol\n"
        + "  return if fever persists\n";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MedScriptGUI().setVisible(true));
    }
}
