package mars.tools;

import mars.mips.hardware.*;
import mars.mips.instructions.*;
import mars.ProgramStatement;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

/**
 * Pipeline Hazard Visualizer v7
 * Features:
 *  - 5-stage pipeline: IF, ID, EX, MEM, WB
 *  - RAW hazard detection with optional forwarding
 *  - Instruction Counter Panel (STALLs, FWDs, Clean)
 *  - Export to CSV
 *  - Live CPI Calculator
 *  - Color Theme Switcher (Light / Dark / High Contrast)
 */
public class PipelineHazardVisualizer extends AbstractMarsToolAndApplication {

    private static final String NAME    = "Pipeline Hazard Visualizer";
    private static final String VERSION = "7.0";

    // ── Stage constants ───────────────────────────────────────────────────────
    private static final String IF    = "IF";
    private static final String ID    = "ID";
    private static final String EX    = "EX";
    private static final String MEM   = "MEM";
    private static final String WB    = "WB";
    private static final String STALL = "S";
    private static final String FWD   = "F";

    // ══════════════════════════════════════════════════════════════════════════
    //  THEMES
    // ══════════════════════════════════════════════════════════════════════════
    private static class Theme {
        String name;
        Color bg, fg, tableBg, tableAlt, headerBg, headerFg;
        Color cIF, cID, cEX, cMEM, cWB, cSTALL, cFWD, cEmpty;
        Color panelBg, statsBg, statsFg;

        Theme(String name,
              Color bg, Color fg,
              Color tableBg, Color tableAlt,
              Color headerBg, Color headerFg,
              Color cIF, Color cID, Color cEX, Color cMEM, Color cWB,
              Color cSTALL, Color cFWD, Color cEmpty,
              Color panelBg, Color statsBg, Color statsFg) {
            this.name=name; this.bg=bg; this.fg=fg;
            this.tableBg=tableBg; this.tableAlt=tableAlt;
            this.headerBg=headerBg; this.headerFg=headerFg;
            this.cIF=cIF; this.cID=cID; this.cEX=cEX;
            this.cMEM=cMEM; this.cWB=cWB;
            this.cSTALL=cSTALL; this.cFWD=cFWD; this.cEmpty=cEmpty;
            this.panelBg=panelBg; this.statsBg=statsBg; this.statsFg=statsFg;
        }
    }

    private static final Theme LIGHT = new Theme("Light",
        new Color(0xF5F5F5), Color.BLACK,
        Color.WHITE, new Color(0xF0F0F0),
        new Color(0x2D3436), Color.WHITE,
        new Color(0xADD8E6), new Color(0x90EE90),
        new Color(0xFFB6C1), new Color(0xFFFFE0), new Color(0xDDA0DD),
        new Color(0xFF4500), new Color(0x00CC66), Color.WHITE,
        new Color(0xE8E8E8), new Color(0xFFFFFF), Color.BLACK);

    private static final Theme DARK = new Theme("Dark",
        new Color(0x1E1E2E), new Color(0xCDD6F4),
        new Color(0x181825), new Color(0x1E1E2E),
        new Color(0x11111B), new Color(0xCDD6F4),
        new Color(0x89B4FA), new Color(0xA6E3A1),
        new Color(0xF38BA8), new Color(0xF9E2AF), new Color(0xCBA6F7),
        new Color(0xF38BA8), new Color(0xA6E3A1), new Color(0x313244),
        new Color(0x181825), new Color(0x313244), new Color(0xCDD6F4));

    private static final Theme HIGH_CONTRAST = new Theme("High Contrast",
        Color.BLACK, Color.WHITE,
        Color.BLACK, new Color(0x111111),
        Color.WHITE, Color.BLACK,
        Color.CYAN, Color.GREEN,
        Color.YELLOW, new Color(0xFF8800), Color.MAGENTA,
        Color.RED, new Color(0x00FF00), Color.BLACK,
        Color.BLACK, new Color(0x222222), Color.WHITE);

    private static final Theme[] THEMES = {LIGHT, DARK, HIGH_CONTRAST};
    private Theme currentTheme = LIGHT;

    // ══════════════════════════════════════════════════════════════════════════
    //  State
    // ══════════════════════════════════════════════════════════════════════════
    private java.util.List<PipelineInstruction> instructions = new ArrayList<>();
    private int  currentCycle  = 0;
    private boolean useForwarding = true;

    // Counters
    private int totalStalls = 0;
    private int totalFwds   = 0;
    private int totalClean  = 0;

    // ══════════════════════════════════════════════════════════════════════════
    //  Swing components
    // ══════════════════════════════════════════════════════════════════════════
    private JTable            timingTable;
    private DefaultTableModel tableModel;
    private JCheckBox         forwardingCheckBox;
    private JComboBox<String> themeCombo;

    // Stats labels
    private JLabel lblStalls, lblFwds, lblClean, lblTotal;
    private JLabel lblCPI, lblIdealCPI, lblCycles, lblInstrs;

    // Panels (kept for theme repainting)
    private JPanel mainPanel, controlPanel, statsPanel, cpiPanel;

    // ══════════════════════════════════════════════════════════════════════════
    //  Constructor
    // ══════════════════════════════════════════════════════════════════════════
    public PipelineHazardVisualizer() {
        super(NAME + ", " + VERSION, NAME);
    }

    @Override public String getName() { return NAME; }

    // ══════════════════════════════════════════════════════════════════════════
    //  Build UI
    // ══════════════════════════════════════════════════════════════════════════
    @Override
    protected JComponent buildMainDisplayArea() {
        mainPanel = new JPanel(new BorderLayout(4, 4));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        mainPanel.add(buildControlPanel(), BorderLayout.NORTH);
        mainPanel.add(buildTablePanel(),   BorderLayout.CENTER);
        mainPanel.add(buildBottomPanel(),  BorderLayout.SOUTH);

        applyTheme();
        return mainPanel;
    }

    // ── Control panel ─────────────────────────────────────────────────────────
    private JPanel buildControlPanel() {
        controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Controls"));

        // Forwarding toggle
        forwardingCheckBox = new JCheckBox("Enable Forwarding", true);
        forwardingCheckBox.addActionListener(e -> useForwarding = forwardingCheckBox.isSelected());

        // Theme switcher
        String[] themeNames = {"Light", "Dark", "High Contrast"};
        themeCombo = new JComboBox<>(themeNames);
        themeCombo.addActionListener(e -> {
            currentTheme = THEMES[themeCombo.getSelectedIndex()];
            applyTheme();
        });

        // Reset button
        JButton btnReset = new JButton("⟳ Reset");
        btnReset.addActionListener(e -> reset());

        // Export CSV button
        JButton btnExport = new JButton("💾 Export CSV");
        btnExport.addActionListener(e -> exportCSV());

        controlPanel.add(new JLabel("Forwarding:"));
        controlPanel.add(forwardingCheckBox);
        controlPanel.add(Box.createHorizontalStrut(10));
        controlPanel.add(new JLabel("Theme:"));
        controlPanel.add(themeCombo);
        controlPanel.add(Box.createHorizontalStrut(10));
        controlPanel.add(btnReset);
        controlPanel.add(btnExport);

        return controlPanel;
    }

    // ── Table panel ───────────────────────────────────────────────────────────
    private JScrollPane buildTablePanel() {
        tableModel = new DefaultTableModel() {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tableModel.addColumn("Instruction");

        timingTable = new JTable(tableModel);
        timingTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        timingTable.getTableHeader().setReorderingAllowed(false);
        timingTable.setDefaultRenderer(Object.class, new PipelineCellRenderer());
        timingTable.setRowHeight(26);
        timingTable.getColumnModel().getColumn(0).setPreferredWidth(200);

        JScrollPane sp = new JScrollPane(timingTable);
        sp.setPreferredSize(new Dimension(900, 260));
        sp.setBorder(BorderFactory.createTitledBorder("Pipeline Timing Diagram"));
        return sp;
    }

    // ── Bottom: stats + CPI ───────────────────────────────────────────────────
    private JPanel buildBottomPanel() {
        JPanel bottom = new JPanel(new GridLayout(1, 2, 6, 0));
        bottom.add(buildStatsPanel());
        bottom.add(buildCPIPanel());
        return bottom;
    }

    private JPanel buildStatsPanel() {
        statsPanel = new JPanel(new GridBagLayout());
        statsPanel.setBorder(BorderFactory.createTitledBorder("Instruction Counter"));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(3, 8, 3, 8);
        gc.anchor = GridBagConstraints.WEST;

        lblStalls = makeStatLabel("STALLs: 0");
        lblFwds   = makeStatLabel("Forwards: 0");
        lblClean  = makeStatLabel("Clean: 0");
        lblTotal  = makeStatLabel("Total Instructions: 0");

        // Color code the counter labels
        lblStalls.setForeground(new Color(0xFF4500));
        lblFwds.setForeground(new Color(0x008800));

        gc.gridx=0; gc.gridy=0; statsPanel.add(new JLabel("🔴"), gc);
        gc.gridx=1;             statsPanel.add(lblStalls, gc);
        gc.gridx=0; gc.gridy=1; statsPanel.add(new JLabel("🟢"), gc);
        gc.gridx=1;             statsPanel.add(lblFwds, gc);
        gc.gridx=0; gc.gridy=2; statsPanel.add(new JLabel("🔵"), gc);
        gc.gridx=1;             statsPanel.add(lblClean, gc);
        gc.gridx=0; gc.gridy=3; gc.gridwidth=2;
        statsPanel.add(new JSeparator(), gc);
        gc.gridy=4;
        statsPanel.add(lblTotal, gc);

        return statsPanel;
    }

    private JPanel buildCPIPanel() {
        cpiPanel = new JPanel(new GridBagLayout());
        cpiPanel.setBorder(BorderFactory.createTitledBorder("CPI Calculator"));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(3, 8, 3, 8);
        gc.anchor = GridBagConstraints.WEST;

        lblInstrs   = makeStatLabel("Instructions: 0");
        lblCycles   = makeStatLabel("Total Cycles: 0");
        lblIdealCPI = makeStatLabel("Ideal CPI: 1.00");
        lblCPI      = makeStatLabel("Actual CPI: —");
        lblCPI.setFont(lblCPI.getFont().deriveFont(Font.BOLD, 14f));
        lblCPI.setForeground(new Color(0x0055AA));

        gc.gridx=0; gc.gridy=0; gc.gridwidth=2; cpiPanel.add(lblInstrs, gc);
        gc.gridy=1;  cpiPanel.add(lblCycles, gc);
        gc.gridy=2;  cpiPanel.add(new JSeparator(), gc);
        gc.gridy=3;  cpiPanel.add(lblIdealCPI, gc);
        gc.gridy=4;  cpiPanel.add(lblCPI, gc);

        return cpiPanel;
    }

    private JLabel makeStatLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 12f));
        return l;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  MARS hooks
    // ══════════════════════════════════════════════════════════════════════════
    @Override
    protected void addAsObserver() {
        addAsObserver(Memory.textBaseAddress, Memory.textLimitAddress);
    }

    @Override
    protected void processMIPSUpdate(Observable resource, AccessNotice notice) {
        if (!notice.accessIsFromMIPS()) return;
        if (notice.getAccessType() != AccessNotice.READ) return;

        int address = ((MemoryAccessNotice) notice).getAddress();
        if (address % 4 != 0) return;

        try {
            ProgramStatement stmt =
                Memory.getInstance().getStatementNoNotify(address);
            if (stmt == null) return;

            final ProgramStatement fStmt = stmt;
            final int fAddr = address;
            SwingUtilities.invokeLater(() -> addNewInstruction(fStmt, fAddr));
        } catch (Exception ignored) {}
    }

    private synchronized void addNewInstruction(ProgramStatement stmt, int address) {
        // Skip if last instruction has the same address and is still in IF
        if (!instructions.isEmpty()) {
            PipelineInstruction last = instructions.get(instructions.size()-1);
            if (last.address == address && last.getCurrentStage().equals(IF)) return;
        }

        PipelineInstruction pi = new PipelineInstruction(stmt, address);
        instructions.add(pi);

        Vector<Object> row = new Vector<>();
        row.add(stmt.getPrintableBasicAssemblyStatement());
        tableModel.addRow(row);

        simulateCycle();
        updateStats();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Pipeline simulation
    // ══════════════════════════════════════════════════════════════════════════
    private void simulateCycle() {
        currentCycle++;

        if (tableModel.getColumnCount() <= currentCycle) {
            tableModel.addColumn("C" + currentCycle);
            timingTable.getColumnModel()
                .getColumn(currentCycle).setPreferredWidth(38);
        }

        boolean stallPropagating = false;

        for (int i = 0; i < instructions.size(); i++) {
            PipelineInstruction pi = instructions.get(i);
            if (pi.isFinished()) continue;

            String cur  = pi.getCurrentStage();
            String next = pi.getNextStage(stallPropagating);

            // Hazard detection when entering ID
            if (next.equals(ID) || (cur.equals(STALL) && !stallPropagating)) {
                if (detectRAW(i)) {
                    if (!useForwarding || !canForward(i)) {
                        next = STALL;
                        stallPropagating = true;
                    } else {
                        next = FWD; // forwarded — no stall
                    }
                }
            } else if (stallPropagating && (cur.equals(IF) || cur.equals(STALL))) {
                next = STALL;
            }

            pi.addStage(next, currentCycle);
            tableModel.setValueAt(next, i, currentCycle);

            if (next.equals(STALL)) stallPropagating = true;
        }

        timingTable.scrollRectToVisible(
            timingTable.getCellRect(tableModel.getRowCount()-1, currentCycle, true));
    }

    // ── RAW detection ─────────────────────────────────────────────────────────
    private boolean detectRAW(int idx) {
        PipelineInstruction cur = instructions.get(idx);
        int[] srcs = cur.getSourceRegisters();
        for (int j = 0; j < idx; j++) {
            PipelineInstruction prev = instructions.get(j);
            if (prev.isFinished()) continue;
            String stage = prev.getCurrentStage();
            if (stage.equals(EX) || stage.equals(MEM) || stage.equals(WB)) {
                int dest = prev.getDestRegister();
                if (dest > 0) {
                    for (int s : srcs) if (s == dest) return true;
                }
            }
        }
        return false;
    }

    private boolean canForward(int idx) {
        PipelineInstruction cur = instructions.get(idx);
        int[] srcs = cur.getSourceRegisters();
        for (int j = 0; j < idx; j++) {
            PipelineInstruction prev = instructions.get(j);
            if (prev.isFinished()) continue;
            int dest = prev.getDestRegister();
            if (dest > 0) {
                for (int s : srcs) {
                    if (s == dest && prev.isLoad()
                            && prev.getCurrentStage().equals(EX)) {
                        return false; // load-use: cannot forward
                    }
                }
            }
        }
        return true;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Stats + CPI update
    // ══════════════════════════════════════════════════════════════════════════
    private void updateStats() {
        totalStalls = 0;
        totalFwds   = 0;
        totalClean  = 0;

        for (int r = 0; r < tableModel.getRowCount(); r++) {
            boolean hadStall = false, hadFwd = false;
            for (int c = 1; c < tableModel.getColumnCount(); c++) {
                Object v = tableModel.getValueAt(r, c);
                if (v == null) continue;
                if (v.equals(STALL)) hadStall = true;
                if (v.equals(FWD))   hadFwd   = true;
            }
            if (hadStall)      totalStalls++;
            else if (hadFwd)   totalFwds++;
            else               totalClean++;
        }

        int instrCount = instructions.size();
        lblStalls.setText("STALLs (load-use hazards): " + totalStalls);
        lblFwds  .setText("Forwarded (RAW resolved):  " + totalFwds);
        lblClean .setText("Clean (no hazard):         " + totalClean);
        lblTotal .setText("Total Instructions: " + instrCount);

        lblInstrs.setText("Instructions: " + instrCount);
        lblCycles.setText("Total Cycles:  " + currentCycle);

        if (instrCount > 0) {
            double cpi = (double) currentCycle / instrCount;
            lblCPI.setText(String.format("Actual CPI:  %.2f", cpi));
            lblIdealCPI.setText("Ideal CPI:   1.00");
        } else {
            lblCPI.setText("Actual CPI: —");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Export CSV
    // ══════════════════════════════════════════════════════════════════════════
    private void exportCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Pipeline as CSV");
        fc.setSelectedFile(new File("pipeline_diagram.csv"));
        if (fc.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        if (!file.getName().endsWith(".csv"))
            file = new File(file.getAbsolutePath() + ".csv");

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            // Header row
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < tableModel.getColumnCount(); c++) {
                if (c > 0) sb.append(",");
                sb.append('"').append(tableModel.getColumnName(c)).append('"');
            }
            pw.println(sb);

            // Data rows
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                sb = new StringBuilder();
                for (int c = 0; c < tableModel.getColumnCount(); c++) {
                    if (c > 0) sb.append(",");
                    Object v = tableModel.getValueAt(r, c);
                    sb.append('"').append(v == null ? "" : v.toString()).append('"');
                }
                pw.println(sb);
            }

            // Append summary
            pw.println();
            pw.println("\"=== SUMMARY ===\"");
            pw.println("\"Instructions\",\"" + instructions.size() + "\"");
            pw.println("\"Total Cycles\",\"" + currentCycle + "\"");
            pw.println("\"STALLs\",\""  + totalStalls + "\"");
            pw.println("\"Forwarded\",\"" + totalFwds + "\"");
            pw.println("\"Clean\",\""   + totalClean + "\"");
            if (instructions.size() > 0) {
                double cpi = (double) currentCycle / instructions.size();
                pw.printf("\"Actual CPI\",\"%.2f\"%n", cpi);
            }
            pw.println("\"Ideal CPI\",\"1.00\"");

            JOptionPane.showMessageDialog(null,
                "✅ CSV exported to:\n" + file.getAbsolutePath(),
                "Export Successful", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null,
                "❌ Export failed: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Theme application
    // ══════════════════════════════════════════════════════════════════════════
    private void applyTheme() {
        Theme t = currentTheme;

        // Main panels
        applyBg(mainPanel,    t.bg);
        applyBg(controlPanel, t.panelBg);
        applyBg(statsPanel,   t.statsBg);
        applyBg(cpiPanel,     t.statsBg);

        // Control labels/checkboxes
        for (Component c : controlPanel.getComponents()) {
            if (c instanceof JLabel)    { c.setForeground(t.fg); }
            if (c instanceof JCheckBox) {
                c.setBackground(t.panelBg);
                c.setForeground(t.fg);
            }
        }

        // Stats labels
        lblStalls.setForeground(t.cSTALL);
        lblFwds  .setForeground(t.cFWD);
        lblClean .setForeground(t.fg);
        lblTotal .setForeground(t.fg);
        lblInstrs.setForeground(t.fg);
        lblCycles.setForeground(t.fg);
        lblIdealCPI.setForeground(t.fg);
        lblCPI.setForeground(t.cIF);

        // Table
        timingTable.setBackground(t.tableBg);
        timingTable.setForeground(t.fg);
        timingTable.setGridColor(t.headerBg);
        timingTable.getTableHeader().setBackground(t.headerBg);
        timingTable.getTableHeader().setForeground(t.headerFg);

        // Repaint everything
        if (mainPanel != null) {
            SwingUtilities.updateComponentTreeUI(mainPanel.getTopLevelAncestor() != null
                ? mainPanel.getTopLevelAncestor() : mainPanel);
            mainPanel.repaint();
            timingTable.repaint();
        }
    }

    private void applyBg(JPanel p, Color c) {
        if (p != null) p.setBackground(c);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Reset
    // ══════════════════════════════════════════════════════════════════════════
    @Override
    protected void reset() {
        instructions.clear();
        currentCycle  = 0;
        totalStalls   = 0;
        totalFwds     = 0;
        totalClean    = 0;
        tableModel.setRowCount(0);
        tableModel.setColumnCount(1);
        updateStats();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PipelineInstruction inner class
    // ══════════════════════════════════════════════════════════════════════════
    private class PipelineInstruction {
        ProgramStatement stmt;
        int address;
        java.util.List<String> stages = new ArrayList<>();

        PipelineInstruction(ProgramStatement stmt, int address) {
            this.stmt    = stmt;
            this.address = address;
        }

        void addStage(String stage, int cycle) {
            while (stages.size() < cycle) stages.add("");
            stages.add(stage);
        }

        String getCurrentStage() {
            return stages.isEmpty() ? "" : stages.get(stages.size()-1);
        }

        String getNextStage(boolean stallAbove) {
            String cur = getCurrentStage();
            if (stallAbove && (cur.equals(IF) || cur.equals(STALL))) return STALL;
            switch (cur) {
                case "":    return IF;
                case "IF":  return ID;
                case "S":   return ID;
                case "F":   return EX;
                case "ID":  return EX;
                case "EX":  return MEM;
                case "MEM": return WB;
                case "WB":  return "DONE";
                default:    return "DONE";
            }
        }

        boolean isFinished() { return getCurrentStage().equals("DONE"); }

        int[] getSourceRegisters() {
            ArrayList<Integer> src = new ArrayList<>();
            try {
                int[] ops = stmt.getOperands();
                String mn = stmt.getInstruction().getName().toLowerCase();
                if (isLoad()) {
                    if (ops.length >= 3) src.add(ops[2]);
                } else if (isStore()) {
                    if (ops.length >= 1) src.add(ops[0]);
                    if (ops.length >= 3) src.add(ops[2]);
                } else if (isBranch()) {
                    if (ops.length >= 1) src.add(ops[0]);
                    if (ops.length >= 2) src.add(ops[1]);
                } else if (ops.length >= 3) {
                    src.add(ops[1]); src.add(ops[2]);
                } else if (ops.length >= 2) {
                    src.add(ops[1]);
                }
            } catch (Exception ignored) {}
            int[] r = new int[src.size()];
            for (int i = 0; i < src.size(); i++) r[i] = src.get(i);
            return r;
        }

        int getDestRegister() {
            try {
                int[] ops = stmt.getOperands();
                if (isStore() || isBranch()) return -1;
                return (ops.length >= 1) ? ops[0] : -1;
            } catch (Exception ignored) { return -1; }
        }

        boolean isLoad() {
            String n = stmt.getInstruction().getName().toLowerCase();
            return n.startsWith("lw")||n.startsWith("lb")||n.startsWith("lh");
        }
        boolean isStore() {
            String n = stmt.getInstruction().getName().toLowerCase();
            return n.startsWith("sw")||n.startsWith("sb")||n.startsWith("sh");
        }
        boolean isBranch() {
            String n = stmt.getInstruction().getName().toLowerCase();
            return n.startsWith("b") && !n.equals("break");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Cell renderer — theme aware
    // ══════════════════════════════════════════════════════════════════════════
    private class PipelineCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

            Theme t = currentTheme;
            setHorizontalAlignment(column == 0 ? LEFT : CENTER);
            setForeground(Color.BLACK);

            if (column == 0) {
                setBackground(row % 2 == 0 ? t.tableBg : t.tableAlt);
                setForeground(t.fg);
                setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
                return this;
            }

            String val = (value == null) ? "" : value.toString();
            setFont(getFont().deriveFont(Font.BOLD, 11f));

            switch (val) {
                case "IF":  setBackground(t.cIF);    setForeground(Color.BLACK); break;
                case "ID":  setBackground(t.cID);    setForeground(Color.BLACK); break;
                case "EX":  setBackground(t.cEX);    setForeground(Color.BLACK); break;
                case "MEM": setBackground(t.cMEM);   setForeground(Color.WHITE); break;
                case "WB":  setBackground(t.cWB);    setForeground(Color.BLACK); break;
                case "S":   // STALL
                    setBackground(t.cSTALL);
                    setForeground(Color.WHITE);
                    setText("STALL");
                    break;
                case "F":   // FWD
                    setBackground(t.cFWD);
                    setForeground(Color.WHITE);
                    setText("FWD");
                    break;
                default:
                    setBackground(t.cEmpty);
                    setForeground(t.fg);
                    break;
            }
            return this;
        }
    }
}
