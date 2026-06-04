import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ProfesorAnuncioDialog extends JDialog {

    private static final Color BG        = new Color(15, 15, 30);
    private static final Color HEADER_BG = new Color(10, 10, 25);
    private static final Color PANEL     = new Color(25, 25, 50);
    private static final Color ACCENT    = new Color(74, 144, 217);
    private static final Color DANGER    = new Color(231, 76, 60);
    private static final Color FIELD     = new Color(35, 40, 70);
    private static final Color FG2       = new Color(150, 170, 200);
    private static final Color MUTED     = new Color(120, 140, 180);

    private final Client     client;
    private final JTextField tituloField   = new JTextField();
    private final JTextArea  contenidoArea = new JTextArea();
    private final JCheckBox  importanteBox = new JCheckBox("Marcar como importante");
    private final JLabel     statusLabel   = new JLabel(" ");

    public ProfesorAnuncioDialog(JFrame parent, Client client) {
        super(parent, true);
        this.client = client;
        setUndecorated(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        int w = parent != null ? (int)(parent.getWidth()  * 0.9) : 756;
        int h = parent != null ? (int)(parent.getHeight() * 0.9) : 594;

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.setBorder(BorderFactory.createLineBorder(ACCENT, 1));

        wrapper.add(buildHeader(), BorderLayout.NORTH);
        wrapper.add(buildForm(),   BorderLayout.CENTER);
        wrapper.add(buildFooter(), BorderLayout.SOUTH);

        setContentPane(wrapper);
        setSize(w, h);
        setResizable(false);
        setLocationRelativeTo(parent);

        getRootPane().registerKeyboardAction(
            e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(HEADER_BG);
        h.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ACCENT),
            BorderFactory.createEmptyBorder(12, 18, 12, 12)
        ));

        JLabel title = new JLabel("Publicar Anuncio");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(ACCENT);
        h.add(title, BorderLayout.WEST);

        JButton closeBtn = new JButton("✕");
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 15));
        closeBtn.setForeground(FG2);
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dispose());
        closeBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { closeBtn.setForeground(DANGER); }
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
                    Point loc = ProfesorAnuncioDialog.this.getLocation();
                    ProfesorAnuncioDialog.this.setLocation(
                        loc.x + e.getX() - drag[0].x,
                        loc.y + e.getY() - drag[0].y);
                }
            }
        });
        return h;
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(PANEL);
        form.setBorder(BorderFactory.createEmptyBorder(28, 40, 20, 40));

        GridBagConstraints c = new GridBagConstraints();
        c.fill    = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.gridx   = 0;

        JLabel subtitle = new JLabel("Nuevo anuncio para todos los alumnos");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(MUTED);
        c.gridy = 0; c.insets = new Insets(0, 0, 22, 0);
        form.add(subtitle, c);

        c.gridy = 1; c.insets = new Insets(0, 0, 5, 0);
        form.add(mkLabel("TÍTULO"), c);
        styleField(tituloField);
        c.gridy = 2; c.insets = new Insets(0, 0, 20, 0);
        form.add(tituloField, c);

        c.gridy = 3; c.insets = new Insets(0, 0, 5, 0);
        form.add(mkLabel("CONTENIDO"), c);
        styleArea(contenidoArea);
        JScrollPane areaScroll = new JScrollPane(contenidoArea);
        areaScroll.setBorder(BorderFactory.createLineBorder(new Color(60, 80, 120)));
        DarkScrollBarUI.apply(areaScroll);
        c.gridy = 4; c.weighty = 1.0; c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 20, 0);
        form.add(areaScroll, c);

        importanteBox.setBackground(PANEL);
        importanteBox.setForeground(new Color(255, 200, 80));
        importanteBox.setFont(new Font("SansSerif", Font.PLAIN, 13));
        importanteBox.setFocusPainted(false);
        c.gridy = 5; c.weighty = 0; c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 10, 0);
        form.add(importanteBox, c);

        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(DANGER);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridy = 6; c.insets = new Insets(0, 0, 0, 0);
        form.add(statusLabel, c);

        return form;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(HEADER_BG);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(40, 50, 80)));

        JButton cancelBtn  = makeBtn("Cancelar",        new Color(60, 65, 100));
        JButton publishBtn = makeBtn("Publicar anuncio", ACCENT);
        cancelBtn.addActionListener(e -> dispose());
        publishBtn.addActionListener(e -> publish());
        footer.add(cancelBtn);
        footer.add(publishBtn);
        return footer;
    }

    private void publish() {
        String titulo    = tituloField.getText().trim();
        String contenido = contenidoArea.getText().trim();
        if (titulo.isEmpty())    { statusLabel.setText("El título no puede estar vacío");    return; }
        if (contenido.isEmpty()) { statusLabel.setText("El contenido no puede estar vacío"); return; }
        String imp = importanteBox.isSelected() ? "1" : "0";
        client.sendMessage("PROF_ADD_ANUNCIO|" + titulo + "|" + contenido + "|" + imp);
        dispose();
    }

    private JLabel mkLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        return l;
    }

    private void styleField(JTextField f) {
        f.setBackground(FIELD);
        f.setForeground(Color.WHITE);
        f.setCaretColor(Color.WHITE);
        f.setFont(new Font("SansSerif", Font.PLAIN, 14));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 80, 120), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)));
    }

    private void styleArea(JTextArea a) {
        a.setBackground(FIELD);
        a.setForeground(Color.WHITE);
        a.setCaretColor(Color.WHITE);
        a.setFont(new Font("SansSerif", Font.PLAIN, 14));
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
    }

    private JButton makeBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(9, 22, 9, 22));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
