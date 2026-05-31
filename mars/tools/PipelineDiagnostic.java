package mars.tools;

import mars.tools.AbstractMarsToolAndApplication;
import mars.mips.hardware.AccessNotice;
import mars.mips.hardware.Memory;
import mars.mips.hardware.MemoryAccessNotice;
import mars.ProgramStatement;

import javax.swing.*;
import java.awt.*;
import java.util.Observable;

/**
 * DIAGNOSTIC TOOL — install alongside main tool.
 * Shows EVERY observer event so we can see what MARS fires.
 * Place in mars/tools/, compile & jar same way.
 */
public class PipelineDiagnostic extends AbstractMarsToolAndApplication {
    private JTextArea log;

    public PipelineDiagnostic() {
        super("Pipeline Diagnostic, 1.0", "Observer Event Log");
    }

    @Override public String getName() { return "Pipeline Diagnostic"; }

    @Override
    protected void addAsObserver() {
        addAsObserver(Memory.textBaseAddress, Memory.textLimitAddress);
    }

    @Override
    protected void processMIPSUpdate(Observable res, AccessNotice notice) {
        if (!notice.accessIsFromMIPS()) return;
        MemoryAccessNotice man = (MemoryAccessNotice) notice;
        int addr = man.getAddress();
        String type = notice.getAccessType()==AccessNotice.READ ? "READ" : "WRITE";

        String instrText = "";
        try {
            ProgramStatement s = Memory.getInstance().getStatementNoNotify(addr);
            if (s != null) instrText = s.getPrintableBasicAssemblyStatement();
        } catch(Exception ignored){}

        final String msg = type + " addr=0x"
            + Integer.toHexString(addr)
            + " align=" + (addr%4==0?"OK":"SKIP")
            + " → " + instrText;

        SwingUtilities.invokeLater(() -> {
            log.append(msg + "\n");
            log.setCaretPosition(log.getDocument().getLength());
        });
    }

    @Override
    protected void reset() { if(log!=null) log.setText(""); }

    @Override
    protected JComponent buildMainDisplayArea() {
        log = new JTextArea(20, 70);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        log.setBackground(new Color(0x0D1117));
        log.setForeground(new Color(0x58D68D));
        log.setEditable(false);
        return new JScrollPane(log);
    }
}