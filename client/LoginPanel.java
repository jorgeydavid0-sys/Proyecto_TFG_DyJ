import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class LoginPanel extends JPanel {

    private static final Color BG     = new Color(15, 15, 30);
    private static final Color PANEL  = new Color(25, 25, 50);
    private static final Color ACCENT = new Color(74, 144, 217);

    private final Client client;

    private final JTextField   nameField   = new JTextField(18);
    private final JPasswordField passField = new JPasswordField(18);
    private final JLabel       statusLabel = new JLabel(" ");

    public LoginPanel(Client client) {
        this.client = client;
        setBackground(BG);
        setLayout(new GridBagLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel card = new JPanel();
        card.setBackground(PANEL);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT, 1),
            BorderFactory.createEmptyBorder(34, 44, 34, 44)
        ));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 0, 6, 0);
        c.fill   = GridBagConstraints.HORIZONTAL;
        c.gridx  = 0; c.gridy = 0;

        // Título
        JLabel title = new JLabel("Escuela Interactiva 2D", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(ACCENT);
        card.add(title, c);

        JLabel sub = new JLabel("Proyecto TFG — DAM 2º", SwingConstants.CENTER);
        sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        sub.setForeground(new Color(120, 140, 180));
        c.gridy++; card.add(sub, c);

        // Campos
        c.gridy++; c.insets = new Insets(18, 0, 6, 0);
        card.add(makeLabel("Usuario:"), c);

        styleField(nameField);
        c.gridy++; c.insets = new Insets(2, 0, 10, 0);
        card.add(nameField, c);

        c.gridy++; c.insets = new Insets(6, 0, 6, 0);
        card.add(makeLabel("Contraseña:"), c);

        styleField(passField);
        c.gridy++; c.insets = new Insets(2, 0, 20, 0);
        card.add(passField, c);

        // Botón entrar
        JButton loginBtn = makeButton("Entrar", ACCENT);
        loginBtn.addActionListener(e -> doLogin());
        c.gridy++; c.insets = new Insets(0, 0, 8, 0);
        card.add(loginBtn, c);

        // Status
        statusLabel.setForeground(new Color(231, 76, 60));
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        c.gridy++; c.insets = new Insets(4, 0, 0, 0);
        card.add(statusLabel, c);

        // Enter en password → login, Tab en usuario → contraseña
        passField.addActionListener(e -> doLogin());
        nameField.addActionListener(e -> passField.requestFocus());

        add(card);
    }

    private void doLogin() {
        String nombre = nameField.getText().trim();
        String pass   = new String(passField.getPassword()).trim();
        if (nombre.isEmpty() || pass.isEmpty()) { setStatus("Rellena todos los campos"); return; }
        setStatus("Conectando...");
        client.sendMessage("LOGIN|" + nombre + "|" + pass);
    }

    public void setStatus(String msg) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(msg));
    }

    private JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(180, 200, 230));
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        return l;
    }

    private void styleField(JTextField f) {
        f.setBackground(new Color(35, 40, 70));
        f.setForeground(Color.WHITE);
        f.setCaretColor(Color.WHITE);
        f.setFont(new Font("SansSerif", Font.PLAIN, 13));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 80, 120), 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
    }

    private JButton makeButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(9, 0, 9, 0));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            final Color orig = bg;
            public void mouseEntered(MouseEvent e) { b.setBackground(orig.brighter()); }
            public void mouseExited(MouseEvent e)  { b.setBackground(orig); }
        });
        return b;
    }
}
