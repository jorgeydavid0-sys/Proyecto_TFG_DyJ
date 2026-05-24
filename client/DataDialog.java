import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class DataDialog extends JDialog {

    private static final Color BG     = new Color(15, 15, 30);
    private static final Color CARD   = new Color(25, 30, 55);
    private static final Color ACCENT = new Color(74, 144, 217);
    private static final Color IMP    = new Color(231, 76, 60);
    private static final Color FG     = new Color(220, 230, 245);
    private static final Color FG2    = new Color(150, 170, 200);

    public DataDialog(JFrame parent, String type, String serialData) {
        super(parent, getTitle(type), false);
        setBackground(BG);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel main = new JPanel();
        main.setBackground(BG);
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Título
        JLabel title = new JLabel(getTitle(type));
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(ACCENT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        main.add(title);
        main.add(Box.createVerticalStrut(12));

        if (serialData.isEmpty()) {
            JLabel empty = new JLabel("No hay datos disponibles.", SwingConstants.CENTER);
            empty.setForeground(FG2);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            main.add(empty);
        } else {
            String[] records = serialData.split(java.util.regex.Pattern.quote("<<REC>>"));
            for (String rec : records) {
                String[] fields = rec.split(java.util.regex.Pattern.quote("<<FLD>>"), -1);
                main.add(buildCard(type, fields));
                main.add(Box.createVerticalStrut(8));
            }
        }

        JScrollPane scroll = new JScrollPane(main);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JButton closeBtn = new JButton("Cerrar");
        closeBtn.setBackground(ACCENT);
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFocusPainted(false);
        closeBtn.setBorder(new EmptyBorder(6, 20, 6, 20));
        closeBtn.addActionListener(e -> dispose());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setBackground(BG);
        btnPanel.add(closeBtn);

        setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        setSize(560, 480);
        setLocationRelativeTo(parent);
    }

    private JPanel buildCard(String type, String[] f) {
        JPanel card = new JPanel();
        card.setBackground(CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 70, 110), 1),
            new EmptyBorder(10, 14, 10, 14)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        switch (type) {
            case "tablon":
                // f: titulo, contenido, autor, fecha, importante
                if (f.length >= 5) {
                    boolean imp = f[4].equals("1");
                    if (imp) card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(IMP, 2), new EmptyBorder(10, 14, 10, 14)));
                    addRow(card, f[0], imp ? IMP : new Color(255, 230, 100), 14, Font.BOLD);
                    addRow(card, f[1], FG,  12, Font.PLAIN);
                    addRow(card, "— " + f[2] + "  ·  " + f[3], FG2, 10, Font.ITALIC);
                }
                break;
            case "horario":
                // f: dia, hora_inicio, hora_fin, asignatura, aula
                if (f.length >= 5) {
                    addRow(card, f[0] + "  " + f[1] + " – " + f[2], ACCENT, 13, Font.BOLD);
                    addRow(card, f[3], FG, 12, Font.PLAIN);
                    addRow(card, "Aula: " + f[4], FG2, 11, Font.ITALIC);
                }
                break;
            case "menu_comedor":
                // f: fecha, primer_plato, segundo_plato, postre, alergenos
                if (f.length >= 5) {
                    addRow(card, f[0], ACCENT, 13, Font.BOLD);
                    addRow(card, "1°  " + f[1], FG,  12, Font.PLAIN);
                    addRow(card, "2°  " + f[2], FG,  12, Font.PLAIN);
                    addRow(card, "Postre:  " + f[3], FG, 12, Font.PLAIN);
                    addRow(card, "Alérgenos: " + f[4], new Color(255, 200, 80), 10, Font.ITALIC);
                }
                break;
            case "notas":
                // f: materia, calificacion, tipo, comentario, fecha
                if (f.length >= 5) {
                    double nota = parseDouble(f[1]);
                    Color notaColor = nota >= 5.0 ? new Color(46, 204, 113) : IMP;
                    addRow(card, f[0], FG, 13, Font.BOLD);
                    addRow(card, "Nota: " + f[1] + "  (" + f[2] + ")", notaColor, 13, Font.BOLD);
                    if (!f[3].isEmpty()) addRow(card, f[3], FG2, 11, Font.ITALIC);
                    addRow(card, f[4], FG2, 10, Font.PLAIN);
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
        p.add(Box.createVerticalStrut(3));
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
