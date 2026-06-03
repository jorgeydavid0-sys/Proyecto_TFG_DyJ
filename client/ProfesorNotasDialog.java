import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class ProfesorNotasDialog extends JDialog {

    private static final Color BG        = new Color(15, 15, 30);
    private static final Color HEADER_BG = new Color(10, 10, 25);
    private static final Color PANEL     = new Color(25, 25, 50);
    private static final Color ACCENT    = new Color(74, 144, 217);
    private static final Color SUCCESS   = new Color(46, 204, 113);
    private static final Color DANGER    = new Color(231, 76, 60);
    private static final Color FIELD     = new Color(35, 40, 70);
    private static final Color TEXT      = new Color(200, 210, 230);
    private static final Color MUTED     = new Color(120, 140, 180);
    private static final Color FG2       = new Color(150, 170, 200);

    private static final String[] TIPOS = {"Examen", "Practica", "Proyecto", "Trabajo", "Participacion"};

    private final Client client;
    private final java.util.List<String[]> alumnos = new ArrayList<>();

    private final JComboBox<String>   cursoCB    = new JComboBox<>();
    private final JComboBox<String>   claseCB    = new JComboBox<>();
    private final DefaultTableModel   tableModel = new DefaultTableModel(
            new String[]{"ID", "Nombre", "Curso", "Clase"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable              table      = new JTable(tableModel);
    private final JTextField          materiaField = new JTextField();
    private final JTextField          notaField    = new JTextField();
    private final JComboBox<String>   tipoCB       = new JComboBox<>(TIPOS);
    private final JTextArea           comentArea   = new JTextArea();
    private final JLabel              formAlumno   = new JLabel("— selecciona un alumno —");
    private final JLabel              statusLabel  = new JLabel(" ");
    private int selectedAlumnoId = -1;

    public ProfesorNotasDialog(JFrame parent, Client client, String data) {
        super(parent, false);
        this.client = client;
        setUndecorated(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        parseAlumnos(data);

        int w = parent != null ? (int)(parent.getWidth()  * 0.9) : 756;
        int h = parent != null ? (int)(parent.getHeight() * 0.9) : 594;

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.setBorder(BorderFactory.createLineBorder(ACCENT, 1));

        wrapper.add(buildHeader(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            buildTablePanel(), buildFormPanel());
        split.setDividerLocation((int)(w * 0.44));
        split.setBorder(null);
        split.setBackground(BG);
        wrapper.add(split, BorderLayout.CENTER);

        wrapper.add(buildStatusBar(), BorderLayout.SOUTH);

        setContentPane(wrapper);
        setSize(w, h);
        setLocationRelativeTo(parent);

        styleTable();
        populateFilters();
        applyFilter();

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

        JLabel title = new JLabel("Poner Nota a Alumno");
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
                    Point loc = ProfesorNotasDialog.this.getLocation();
                    ProfesorNotasDialog.this.setLocation(
                        loc.x + e.getX() - drag[0].x,
                        loc.y + e.getY() - drag[0].y);
                }
            }
        });
        return h;
    }

    private JPanel buildTablePanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(14, 14, 10, 7));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filters.setBackground(BG);
        filters.add(label("Curso:"));
        styleCombo(cursoCB); cursoCB.setPreferredSize(new Dimension(90, 28));
        filters.add(cursoCB);
        filters.add(Box.createHorizontalStrut(4));
        filters.add(label("Clase:"));
        styleCombo(claseCB); claseCB.setPreferredSize(new Dimension(65, 28));
        filters.add(claseCB);
        cursoCB.addActionListener(e -> applyFilter());
        claseCB.addActionListener(e -> applyFilter());
        p.add(filters, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(new Color(20, 20, 40));
        scroll.setBorder(BorderFactory.createLineBorder(new Color(50, 60, 90)));
        DarkScrollBarUI.apply(scroll);
        p.add(scroll, BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onSelect();
        });
        return p;
    }

    private JPanel buildFormPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG);
        outer.setBorder(BorderFactory.createEmptyBorder(14, 7, 10, 14));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(PANEL);
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 60, 90)),
            BorderFactory.createEmptyBorder(20, 22, 20, 22)
        ));

        GridBagConstraints c = new GridBagConstraints();
        c.fill    = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.gridx   = 0;

        JLabel formTitle = new JLabel("Añadir nota");
        formTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        formTitle.setForeground(ACCENT);
        c.gridy = 0; c.insets = new Insets(0, 0, 14, 0);
        form.add(formTitle, c);

        formAlumno.setFont(new Font("SansSerif", Font.ITALIC, 12));
        formAlumno.setForeground(MUTED);
        c.gridy = 1; c.insets = new Insets(0, 0, 16, 0);
        form.add(formAlumno, c);

        c.gridy = 2; c.insets = new Insets(0, 0, 5, 0);
        form.add(label("MATERIA"), c);
        styleField(materiaField);
        c.gridy = 3; c.insets = new Insets(0, 0, 14, 0);
        form.add(materiaField, c);

        JPanel notaTipoRow = new JPanel(new GridLayout(1, 2, 10, 0));
        notaTipoRow.setBackground(PANEL);
        JPanel notaP = new JPanel(new BorderLayout(0, 5)); notaP.setBackground(PANEL);
        notaP.add(label("NOTA (0–10)"), BorderLayout.NORTH);
        styleField(notaField); notaP.add(notaField, BorderLayout.CENTER);
        JPanel tipoP = new JPanel(new BorderLayout(0, 5)); tipoP.setBackground(PANEL);
        tipoP.add(label("TIPO"), BorderLayout.NORTH);
        styleCombo(tipoCB); tipoP.add(tipoCB, BorderLayout.CENTER);
        notaTipoRow.add(notaP); notaTipoRow.add(tipoP);
        c.gridy = 4; c.insets = new Insets(0, 0, 14, 0);
        form.add(notaTipoRow, c);

        c.gridy = 5; c.insets = new Insets(0, 0, 5, 0);
        form.add(label("COMENTARIO"), c);
        styleArea(comentArea);
        JScrollPane cs = new JScrollPane(comentArea);
        cs.setBorder(BorderFactory.createLineBorder(new Color(60, 80, 120)));
        DarkScrollBarUI.apply(cs);
        c.gridy = 6; c.weighty = 1.0; c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 18, 0);
        form.add(cs, c);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btns.setBackground(PANEL);
        JButton saveBtn = makeBtn("Guardar nota", SUCCESS);
        saveBtn.addActionListener(e -> saveNota());
        btns.add(saveBtn);
        c.gridy = 7; c.weighty = 0; c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        form.add(btns, c);

        outer.add(form, BorderLayout.CENTER);
        return outer;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        bar.setBackground(HEADER_BG);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(40, 40, 70)));
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(SUCCESS);
        bar.add(statusLabel);
        return bar;
    }

    // ── Lógica ───────────────────────────────────────────────────────────

    private void parseAlumnos(String data) {
        if (data == null || data.isEmpty()) return;
        for (String rec : data.split(java.util.regex.Pattern.quote("<<REC>>"))) {
            String[] f = rec.split(java.util.regex.Pattern.quote("<<FLD>>"), -1);
            if (f.length >= 4) alumnos.add(f);
        }
    }

    private void populateFilters() {
        Set<String> cursos = new LinkedHashSet<>();
        Set<String> clases = new LinkedHashSet<>();
        cursos.add("Todos"); clases.add("Todas");
        for (String[] a : alumnos) { cursos.add(a[2]); clases.add(a[3]); }
        for (String s : cursos) cursoCB.addItem(s);
        for (String s : clases) claseCB.addItem(s);
    }

    private void applyFilter() {
        String curso = (String) cursoCB.getSelectedItem();
        String clase = (String) claseCB.getSelectedItem();
        tableModel.setRowCount(0);
        for (String[] a : alumnos) {
            boolean okC = "Todos".equals(curso) || a[2].equals(curso);
            boolean okL = "Todas".equals(clase) || a[3].equals(clase);
            if (okC && okL) {
                try { tableModel.addRow(new Object[]{Integer.parseInt(a[0]), a[1], a[2], a[3]}); }
                catch (NumberFormatException ignored) {}
            }
        }
    }

    private void onSelect() {
        int row = table.getSelectedRow();
        if (row < 0) {
            selectedAlumnoId = -1;
            formAlumno.setText("— selecciona un alumno —");
            formAlumno.setForeground(MUTED);
            return;
        }
        selectedAlumnoId = (int) tableModel.getValueAt(row, 0);
        String nombre = (String) tableModel.getValueAt(row, 1);
        String curso  = (String) tableModel.getValueAt(row, 2);
        String clase  = (String) tableModel.getValueAt(row, 3);
        formAlumno.setText(nombre + "  ·  " + curso + "-" + clase);
        formAlumno.setForeground(ACCENT);
    }

    private void saveNota() {
        if (selectedAlumnoId < 0) { setStatus("Selecciona un alumno primero", true); return; }
        String materia = materiaField.getText().trim();
        String nota    = notaField.getText().trim();
        String tipo    = (String) tipoCB.getSelectedItem();
        String coment  = comentArea.getText().trim();
        if (materia.isEmpty()) { setStatus("La materia no puede estar vacía", true); return; }
        try {
            double n = Double.parseDouble(nota);
            if (n < 0 || n > 10) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            setStatus("La nota debe ser un número entre 0 y 10", true);
            return;
        }
        client.sendMessage("PROF_ADD_NOTA|" + selectedAlumnoId + "|" + materia + "|" + nota + "|" + tipo + "|" + coment);
        setStatus("Nota enviada para " + tableModel.getValueAt(table.getSelectedRow(), 1), false);
        materiaField.setText(""); notaField.setText(""); comentArea.setText("");
    }

    private void setStatus(String msg, boolean err) {
        statusLabel.setText(msg);
        statusLabel.setForeground(err ? DANGER : SUCCESS);
    }

    // ── Estilos ──────────────────────────────────────────────────────────

    private void styleTable() {
        table.setBackground(new Color(20, 20, 40));
        table.setForeground(TEXT);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setRowHeight(26);
        table.setGridColor(new Color(40, 50, 70));
        table.setSelectionBackground(new Color(50, 80, 130));
        table.setSelectionForeground(Color.WHITE);
        table.getTableHeader().setBackground(new Color(30, 35, 60));
        table.getTableHeader().setForeground(ACCENT);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getColumnModel().getColumn(0).setMaxWidth(45);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(55);
    }

    private JLabel label(String t) {
        JLabel l = new JLabel(t);
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
            BorderFactory.createEmptyBorder(6, 9, 6, 9)));
    }

    private void styleArea(JTextArea a) {
        a.setBackground(FIELD);
        a.setForeground(Color.WHITE);
        a.setCaretColor(Color.WHITE);
        a.setFont(new Font("SansSerif", Font.PLAIN, 13));
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setBorder(BorderFactory.createEmptyBorder(7, 9, 7, 9));
    }

    private void styleCombo(JComboBox<?> b) {
        b.setBackground(FIELD);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
    }

    private JButton makeBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
