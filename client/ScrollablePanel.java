import javax.swing.*;
import java.awt.*;

public class ScrollablePanel extends JPanel implements Scrollable {

    public ScrollablePanel(LayoutManager layout) { super(layout); }

    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 20; }
    @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 80; }
    @Override public boolean getScrollableTracksViewportWidth()  { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }
}
