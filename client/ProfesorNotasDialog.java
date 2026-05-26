import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class ProfesorNotasDialog extends JDialog {

    private static final Color BG      = new Color(15, 15, 30);
    private static final Color PANEL   = new Color(25, 25, 50);
    private static final Color ACCENT  = new Color(74, 144, 217);
    private static final Color SUCCESS = new Color(46, 204, 113);
    private static final Color DANGER  = new Color(231, 76, 60);
    private static final Color FIELD   = new Color(35, 40, 70);
    private static final Color TEXT    = new Color(200, 210, 230);
    private static final Color MUTED   = new Color(120, 140, 180);

    private static final String[] TIPOS = {"Examen", "Practica", "Proyecto", "Trabajo", "Participacion"};
    private static final String[] DIAS  = {"Lunes","Martes","Miercoles","Jueves","Viernes"};

    private final Client client;

    // Datos de alumnos: [id, nombre, curso, clase]
    private final java.util.List<String[]> alumnos = new ArrayList<>();

    // Filtros
    private final JComboBox<String> cursoCB = new JComboBox<>();
    private final JComboBox<String> claseCB = new JComboBox<>();

    // Tabla de alumnos
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID","Nombre","Curso","Clase"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(tableModel);

    // Formulario de nota
    private final JTextField  materiaField  = new JTextField(18);
    private final JTextField  notaField     = new JTextField(6);
    private final JComboBox<String> tipoCB  = new JComboBox<>(TIPOS);
    private final JTextArea   comentArea    = new JTextArea(3, 18);
    private final JLabel      formAlumno    = new JLabel("— selecciona un alumno —");
    private final JLabel      statusLabel   = new JLabel(" ");
    private int selectedAlumnoId = -1;

    public ProfesorNotasDialog(JFrame parent, Client client, String data) {
        super(parent, "Poner Nota a Alumno", false);
        this.client = client;
        setBackground(BG);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        parseAlumnos(data);
        buildUI();
        setSize(720, 520);
        setLocationRelativeTo(parent);
    }

    private void parseAlumnos(String data) {
        if (data == null || data.isEmpty()) return;
        for (String rec : data.split(java.util.regex.Pattern.quote("<<REC>>"))) {
            String[] f = rec.split(java.util.regex.Pattern.quote("<<FLD>>"), -1);
            if (f.length >= 4) alumnos.add(f);
        }
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG);

        // ── Header ──────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(10, 10, 25));
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,ACCENT),
            BorderFactory.createEmptyBorder(10,18,10,18)));
        JLabel title = new JLabel("Poner Nota a Alumno");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(ACCENT);
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // ── Centro: tabla izq + formulario dcha ─────────────────────────
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildTablePanel(), buildFormPanel());
        split.setDividerLocation(320);
        split.setBorder(null);
        split.setBackground(BG);
        add(split, BorderLayout.CENTER);

        // ── Status bar ──────────────────────────────────────────────────
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        bar.setBackground(new Color(10, 10, 25));
        bar.setBorder(BorderFactory.createMatteBorder(1,0,0,0,new Color(40,40,70)));
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        statusLabel.setForeground(SUCCESS);
        bar.add(statusLabel);
        add(bar, BorderLayout.SOUTH);

        populateFilters();
        applyFilter();
        styleTable();
    }

    private JPanel buildTablePanel() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 6));

        // Filtros
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filters.setBackground(BG);
        filters.add(label("Curso:"));
        styleCombo(cursoCB); cursoCB.setPreferredSize(new Dimension(90, 26));
        filters.add(cursoCB);
        filters.add(label("Clase:"));
        styleCombo(claseCB); claseCB.setPreferredSize(new Dimension(60, 26));
        filters.add(claseCB);
        cursoCB.addActionListener(e -> applyFilter());
        claseCB.addActionListener(e -> applyFilter());
        p.add(filters, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(new Color(20,20,40));
        scroll.setBorder(BorderFactory.createLineBorder(new Color(50,60,90)));
        p.add(scroll, BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onSelect();
        });
        return p;
    }

    private JPanel buildFormPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(12, 6, 8, 12));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(PANEL);
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50,60,90)),
            BorderFactory.createEmptyBorder(16,18,16,18)));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1.0;
        c.insets = new Insets(4,0,4,0); c.gridx = 0; c.gridy = 0;

        JLabel formTitle = new JLabel("Añadir nota");
        formTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        formTitle.setForeground(ACCENT);
        c.insets = new Insets(0,0,10,0);
        form.add(formTitle, c); c.insets = new Insets(4,0,4,0);

        formAlumno.setFont(new Font("SansSerif", Font.ITALIC, 12));
        formAlumno.setForeground(MUTED);
        c.gridy++; form.add(formAlumno, c);

        c.gridy++; form.add(label("Materia:"), c);
        styleField(materiaField);
        c.gridy++; form.add(materiaField, c);

        JPanel notaTipoRow = new JPanel(new GridLayout(1, 2, 8, 0));
        notaTipoRow.setBackground(PANEL);
        JPanel notaP = new JPanel(new BorderLayout(0,2)); notaP.setBackground(PANEL);
        notaP.add(label("Nota (0-10):"), BorderLayout.NORTH);
        styleField(notaField); notaP.add(notaField, BorderLayout.CENTER);
        JPanel tipoP = new JPanel(new BorderLayout(0,2)); tipoP.setBackground(PANEL);
        tipoP.add(label("Tipo:"), BorderLayout.NORTH);
        styleCombo(tipoCB); tipoP.add(tipoCB, BorderLayout.CENTER);
        notaTipoRow.add(notaP); notaTipoRow.add(tipoP);
        c.gridy++; form.add(notaTipoRow, c);

        c.gridy++; form.add(label("Comentario:"), c);
        styleArea(comentArea);
        JScrollPane cs = new JScrollPane(comentArea);
        cs.setBorder(BorderFactory.createLineBorder(new Color(60,80,120)));
        c.gridy++; form.add(cs, c);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btns.setBackground(PANEL);
        JButton cancelBtn = makeBtn("Cerrar", new Color(70,70,100));
        JButton saveBtn   = makeBtn("Guardar nota", SUCCESS);
        cancelBtn.addActionListener(e -> dispose());
        saveBtn.addActionListener(e -> saveNota());
        btns.add(cancelBtn); btns.add(saveBtn);
        c.gridy++; c.insets = new Insets(14,0,0,0);
        form.add(btns, c);

        p.add(form, BorderLayout.NORTH);
        return p;
    }

    // ── Lógica ───────────────────────────────────────────────────────────

    private void populateFilters() {
        Set<String> cursos = new LinkedHashSet<>();
        Set<String> clases = new LinkedHashSet<>();
        cursos.add("Todos");
        clases.add("Todas");
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
        if (row < 0) { selectedAlumnoId = -1; formAlumno.setText("— selecciona un alumno —"); return; }
        selectedAlumnoId = (int) tableModel.getValueAt(row, 0);
        String nombre = (String) tableModel.getValueAt(row, 1);
        String curso  = (String) tableModel.getValueAt(row, 2);
        String clase  = (String) tableModel.getValueAt(row, 3);
        formAlumno.setText(nombre + "  ·  " + curso + "-" + clase);
        formAlumno.setForeground(ACCENT);
    }

    private void saveNota() {
        if (selectedAlumnoId < 0) { setStatus("Selecciona un alumno", true); return; }
        String materia = materiaField.getText().trim();
        String nota    = notaField.getText().trim();
        String tipo    = (String) tipoCB.getSelectedItem();
        String coment  = comentArea.getText().trim();
        if (materia.isEmpty()) { setStatus("La materia no puede estar vacía", true); return; }
        try {
            double n = Double.parseDouble(nota);
            if (n < 0 || n > 10) throw new NumberFormatException();
        } catch (NumberFormatException e) { setStatus("La nota debe ser un número entre 0 y 10", true); return; }
        client.sendMessage("PROF_ADD_NOTA|" + selectedAlumnoId + "|" + materia + "|" + nota + "|" + tipo + "|" + coment);
        setStatus("Nota enviada para " + tableModel.getValueAt(table.getSelectedRow(), 1), false);
        materiaField.setText(""); notaField.setText(""); comentArea.setText("");
    }

    private void setStatus(String msg, boolean err) {
        statusLabel.setText(msg);
        statusLabel.setForeground(err ? DANGER : SUCCESS);
    }

    private void styleTable() {
        table.setBackground(new Color(20,20,40)); table.setForeground(TEXT);
        table.setFont(new Font("SansSerif", Font.PLAIN, 12)); table.setRowHeight(24);
        table.setGridColor(new Color(40,50,70));
        table.setSelectionBackground(new Color(50,80,130)); table.setSelectionForeground(Color.WHITE);
        table.getTableHeader().setBackground(new Color(30,35,60));
        table.getTableHeader().setForeground(ACCENT);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        table.getColumnModel().getColumn(0).setMaxWidth(45);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(50);
    }

    private JLabel label(String t) {
        JLabel l = new JLabel(t); l.setForeground(MUTED);
        l.setFont(new Font("SansSerif", Font.BOLD, 11)); return l;
    }

    private void styleField(JTextField f) {
        f.setBackground(FIELD); f.setForeground(Color.WHITE); f.setCaretColor(Color.WHITE);
        f.setFont(new Font("SansSerif", Font.PLAIN, 13));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60,80,120),1),
            BorderFactory.createEmptyBorder(4,8,4,8)));
    }

    private void styleArea(JTextArea a) {
        a.setBackground(FIELD); a.setForeground(Color.WHITE); a.setCaretColor(Color.WHITE);
        a.setFont(new Font("SansSerif", Font.PLAIN, 12));
        a.setLineWrap(true); a.setWrapStyleWord(true);
        a.setBorder(BorderFactory.createEmptyBorder(6,8,6,8));
    }

    private void styleCombo(JComboBox<?> b) {
        b.setBackground(FIELD); b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
    }

    private JButton makeBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 12)); b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(7,14,7,14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
