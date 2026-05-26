import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ProfesorAnuncioDialog extends JDialog {

    private static final Color BG     = new Color(15, 15, 30);
    private static final Color PANEL  = new Color(25, 25, 50);
    private static final Color ACCENT = new Color(74, 144, 217);
    private static final Color DANGER = new Color(231, 76, 60);
    private static final Color FIELD  = new Color(35, 40, 70);
    private static final Color TEXT   = new Color(200, 210, 230);
    private static final Color MUTED  = new Color(120, 140, 180);

    private final Client      client;
    private final JTextField  tituloField   = new JTextField(25);
    private final JTextArea   contenidoArea = new JTextArea(5, 25);
    private final JCheckBox   importanteBox = new JCheckBox("Marcar como importante");
    private final JLabel      statusLabel   = new JLabel(" ");

    public ProfesorAnuncioDialog(JFrame parent, Client client) {
        super(parent, "Publicar Anuncio", true);
        this.client = client;
        setBackground(BG);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private void buildUI() {
        JPanel main = new JPanel(new GridBagLayout());
        main.setBackground(PANEL);
        main.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT, 1),
            BorderFactory.createEmptyBorder(24, 28, 20, 28)));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(4, 0, 4, 0);
        c.gridx = 0; c.gridy = 0;

        JLabel title = new JLabel("Nuevo anuncio", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(ACCENT);
        c.insets = new Insets(0, 0, 16, 0);
        main.add(title, c);

        c.insets = new Insets(4, 0, 2, 0);
        c.gridy++; main.add(label("Título:"), c);
        styleField(tituloField);
        c.gridy++; main.add(tituloField, c);

        c.gridy++; main.add(label("Contenido:"), c);
        styleArea(contenidoArea);
        JScrollPane scroll = new JScrollPane(contenidoArea);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(60, 80, 120)));
        c.gridy++; main.add(scroll, c);

        importanteBox.setBackground(PANEL);
        importanteBox.setForeground(new Color(255, 200, 80));
        importanteBox.setFont(new Font("SansSerif", Font.PLAIN, 12));
        importanteBox.setFocusPainted(false);
        c.gridy++; c.insets = new Insets(8, 0, 4, 0);
        main.add(importanteBox, c);

        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        statusLabel.setForeground(DANGER);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridy++; c.insets = new Insets(2, 0, 4, 0);
        main.add(statusLabel, c);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setBackground(PANEL);
        JButton cancelBtn = makeBtn("Cancelar", new Color(70, 70, 100));
        JButton publishBtn = makeBtn("Publicar", ACCENT);
        cancelBtn.addActionListener(e -> dispose());
        publishBtn.addActionListener(e -> publish());
        btns.add(cancelBtn);
        btns.add(publishBtn);
        c.gridy++; c.insets = new Insets(12, 0, 0, 0);
        main.add(btns, c);

        setContentPane(main);
    }

    private void publish() {
        String titulo = tituloField.getText().trim();
        String contenido = contenidoArea.getText().trim();
        if (titulo.isEmpty())    { statusLabel.setText("El título no puede estar vacío"); return; }
        if (contenido.isEmpty()) { statusLabel.setText("El contenido no puede estar vacío"); return; }
        String imp = importanteBox.isSelected() ? "1" : "0";
        client.sendMessage("PROF_ADD_ANUNCIO|" + titulo + "|" + contenido + "|" + imp);
        dispose();
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MUTED);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        return l;
    }

    private void styleField(JTextField f) {
        f.setBackground(FIELD);
        f.setForeground(Color.WHITE);
        f.setCaretColor(Color.WHITE);
        f.setFont(new Font("SansSerif", Font.PLAIN, 13));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 80, 120), 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
    }

    private void styleArea(JTextArea a) {
        a.setBackground(FIELD);
        a.setForeground(Color.WHITE);
        a.setCaretColor(Color.WHITE);
        a.setFont(new Font("SansSerif", Font.PLAIN, 13));
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
    }

    private JButton makeBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(7, 16, 7, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
