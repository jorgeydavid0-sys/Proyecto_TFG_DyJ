import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public class DarkScrollBarUI extends BasicScrollBarUI {

    private static final Color THUMB = new Color(74, 144, 217);
    private static final Color TRACK = new Color(18, 20, 40);

    public static void apply(JScrollPane pane) {
        JScrollBar vsb = pane.getVerticalScrollBar();
        vsb.setUI(new DarkScrollBarUI());
        vsb.setBackground(TRACK);
        vsb.setPreferredSize(new Dimension(8, 0));
        pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }

    @Override
    protected void configureScrollBarColors() {
        thumbColor           = THUMB;
        trackColor           = TRACK;
        thumbHighlightColor  = THUMB;
        thumbDarkShadowColor = THUMB.darker();
        thumbLightShadowColor = THUMB;
    }

    @Override
    protected JButton createDecreaseButton(int orientation) { return zeroButton(); }
    @Override
    protected JButton createIncreaseButton(int orientation) { return zeroButton(); }

    private JButton zeroButton() {
        JButton b = new JButton();
        Dimension d = new Dimension(0, 0);
        b.setPreferredSize(d);
        b.setMinimumSize(d);
        b.setMaximumSize(d);
        return b;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        g.setColor(TRACK);
        g.fillRect(r.x, r.y, r.width, r.height);
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
        if (r.isEmpty() || !scrollbar.isEnabled()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(THUMB);
        g2.fillRoundRect(r.x + 2, r.y + 3, r.width - 4, r.height - 6, 6, 6);
        g2.dispose();
    }
}
