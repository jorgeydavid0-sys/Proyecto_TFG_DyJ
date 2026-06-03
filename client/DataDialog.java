import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

public class DataDialog extends JDialog {

    private static final Color BG        = new Color(15, 15, 30);
    private static final Color HEADER_BG = new Color(10, 10, 25);
    private static final Color CARD      = new Color(25, 30, 55);
    private static final Color ACCENT    = new Color(74, 144, 217);
    private static final Color IMP       = new Color(231, 76, 60);
    private static final Color FG        = new Color(220, 230, 245);
    private static final Color FG2       = new Color(150, 170, 200);

    public DataDialog(JFrame parent, String type, String serialData) {
        super(parent, false);
        setUndecorated(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        int w = parent != null ? (int)(parent.getWidth()  * 0.9) : 756;
        int h = parent != null ? (int)(parent.getHeight() * 0.9) : 594;

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.setBorder(BorderFactory.createLineBorder(ACCENT, 1));

        wrapper.add(buildHeader(getTitle(type)), BorderLayout.NORTH);

        JPanel main = new JPanel();
        main.setBackground(BG);
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBorder(new EmptyBorder(18, 22, 18, 22));

        if (serialData.isEmpty()) {
            JLabel empty = new JLabel("No hay datos disponibles.");
            empty.setForeground(FG2);
            empty.setFont(new Font("SansSerif", Font.ITALIC, 14));
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            main.add(Box.createVerticalGlue());
            main.add(empty);
            main.add(Box.createVerticalGlue());
        } else {
            String[] records = serialData.split(java.util.regex.Pattern.quote("<<REC>>"));
            for (String rec : records) {
                String[] fields = rec.split(java.util.regex.Pattern.quote("<<FLD>>"), -1);
                main.add(buildCard(type, fields));
                main.add(Box.createVerticalStrut(10));
            }
        }

        JScrollPane scroll = new JScrollPane(main);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        DarkScrollBarUI.apply(scroll);
        wrapper.add(scroll, BorderLayout.CENTER);

        setContentPane(wrapper);
        setSize(w, h);
        setLocationRelativeTo(parent);

        getRootPane().registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private JPanel buildHeader(String title) {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(HEADER_BG);
        h.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ACCENT),
            BorderFactory.createEmptyBorder(12, 18, 12, 12)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(ACCENT);
        h.add(titleLabel, BorderLayout.WEST);

        JButton closeBtn = new JButton("✕");
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 15));
        closeBtn.setForeground(FG2);
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dispose());
        closeBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { closeBtn.setForeground(IMP); }
            public void mouseExited(MouseEvent e)  { closeBtn.setForeground(FG2); }
        });
        h.add(closeBtn, BorderLayout.EAST);

        final Point[] drag = {null};
        h.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e)  { drag[0] = e.getPoint(); }
            public void mouseReleased(MouseEvent e) { drag[0] = null; }
        });
        h.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (drag[0] != null) {
                    Point loc = DataDialog.this.getLocation();
                    DataDialog.this.setLocation(
                        loc.x + e.getX() - drag[0].x,
                        loc.y + e.getY() - drag[0].y);
                }
            }
        });
        return h;
    }

    private JPanel buildCard(String type, String[] f) {
        JPanel card = new JPanel();
        card.setBackground(CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 70, 110), 1),
            new EmptyBorder(14, 18, 14, 18)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        switch (type) {
            case "tablon":
                if (f.length >= 5) {
                    boolean imp = f[4].equals("1");
                    if (imp) card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(IMP, 2), new EmptyBorder(14, 18, 14, 18)));
                    addRow(card, f[0], imp ? IMP : new Color(255, 230, 100), 13, Font.BOLD);
                    addWrappingRow(card, f[1], FG, 12);
                    addRow(card, "— " + f[2] + "  ·  " + f[3], FG2, 10, Font.ITALIC);
                }
                break;
            case "horario":
                if (f.length >= 5) {
                    addRow(card, f[0] + "  " + f[1] + " – " + f[2], ACCENT, 14, Font.BOLD);
                    addRow(card, f[3], FG, 13, Font.PLAIN);
                    addRow(card, "Aula: " + f[4], FG2, 12, Font.ITALIC);
                }
                break;
            case "menu_comedor":
                if (f.length >= 5) {
                    addRow(card, f[0], ACCENT, 14, Font.BOLD);
                    addRow(card, "1°  " + f[1], FG, 13, Font.PLAIN);
                    addRow(card, "2°  " + f[2], FG, 13, Font.PLAIN);
                    addRow(card, "Postre:  " + f[3], FG, 13, Font.PLAIN);
                    addRow(card, "Alérgenos: " + f[4], new Color(255, 200, 80), 11, Font.ITALIC);
                }
                break;
            case "notas":
                if (f.length >= 5) {
                    double nota = parseDouble(f[1]);
                    Color notaColor = nota >= 5.0 ? new Color(46, 204, 113) : IMP;
                    addRow(card, f[0], FG, 14, Font.BOLD);
                    addRow(card, "Nota: " + f[1] + "  (" + f[2] + ")", notaColor, 14, Font.BOLD);
                    if (!f[3].isEmpty()) addRow(card, f[3], FG2, 12, Font.ITALIC);
                    addRow(card, f[4], FG2, 11, Font.PLAIN);
                }
                break;
        }
        return card;
    }

    private void addRow(JPanel p, String text, Color color, int size, int style) {
        JLabel l = new JLabel("<html>" + escapeHtml(text) + "</html>");
        l.setForeground(color);
        l.setFont(new Font("SansSerif", style, size));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(4));
    }

    private void addWrappingRow(JPanel p, String text, Color color, int size) {
        JTextArea ta = new JTextArea(text);
        ta.setForeground(color);
        ta.setFont(new Font("SansSerif", Font.PLAIN, size));
        ta.setBackground(CARD);
        ta.setEditable(false);
        ta.setFocusable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setAlignmentX(Component.LEFT_ALIGNMENT);
        ta.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        p.add(ta);
        p.add(Box.createVerticalStrut(4));
    }

    private static String getTitle(String type) {
        switch (type) {
            case "tablon":       return "Tablón de Anuncios";
            case "horario":      return "Horario / Calendario";
            case "menu_comedor": return "Menú del Comedor";
            case "notas":        return "Notas y Calificaciones";
            default:             return "Información";
        }
    }

    private static String escapeHtml(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    private static double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }
}
