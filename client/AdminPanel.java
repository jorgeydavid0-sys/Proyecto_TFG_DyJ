import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;

public class AdminPanel extends JPanel {

    private static final Color BG      = new Color(15, 15, 30);
    private static final Color PANEL   = new Color(25, 25, 50);
    private static final Color ACCENT  = new Color(74, 144, 217);
    private static final Color DANGER  = new Color(231, 76, 60);
    private static final Color SUCCESS = new Color(46, 204, 113);
    private static final Color FIELD   = new Color(35, 40, 70);
    private static final Color TEXT    = new Color(200, 210, 230);
    private static final Color MUTED   = new Color(120, 140, 180);

    private static final String[] PRESET_COLORS = {"#4A90D9","#E74C3C","#2ECC71","#F39C12","#9B59B6","#1ABC9C"};
    private static final String[] ROLES  = {"alumno","profesor","admin"};
    private static final String[] DIAS   = {"Lunes","Martes","Miercoles","Jueves","Viernes"};
    private static final String[] CURSOS = {"1ESO","2ESO","3ESO","4ESO","1BACH","2BACH","1DAM","2DAM","1ASIR","2ASIR","FP_BASICA"};

    private final Client client;
    private final int    myUserId;
    private final String myName;

    // ── Pestaña Usuarios ────────────────────────────────────────────────
    private final DefaultTableModel usersModel = new DefaultTableModel(
            new String[]{"ID","Nombre","Color","Rol","Curso","Clase"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable usersTable = new JTable(usersModel);
    private final JLabel         uFormTitle  = new JLabel("Nuevo usuario");
    private final JTextField     uName       = new JTextField(14);
    private final JPasswordField uPass       = new JPasswordField(14);
    private final JComboBox<String> uRol     = new JComboBox<>(ROLES);
    private final JTextField     uCurso      = new JTextField(8);
    private final JTextField     uClase      = new JTextField(4);
    private final JButton[]      uColorBtns  = new JButton[PRESET_COLORS.length];
    private final JButton        uSaveBtn    = makeBtn("Crear",    SUCCESS);
    private final JButton        uDeleteBtn  = makeBtn("Eliminar", DANGER);
    private final JButton        uCancelBtn  = makeBtn("Cancelar", new Color(70,70,100));
    private String uSelectedColor = PRESET_COLORS[0];
    private int    uEditingId     = -1;

    // ── Pestaña Anuncios ────────────────────────────────────────────────
    private final DefaultTableModel annModel = new DefaultTableModel(
            new String[]{"ID","Título","Autor","Fecha","Imp."}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable     annTable   = new JTable(annModel);
    private final JTextField annTitulo  = new JTextField(20);
    private final JTextArea  annContenido = new JTextArea(4, 20);
    private final JCheckBox  annImp     = new JCheckBox("Importante");
    private final JButton    annSaveBtn = makeBtn("Publicar",  ACCENT);
    private final JButton    annDelBtn  = makeBtn("Eliminar",  DANGER);
    private int annSelectedId = -1;

    // ── Pestaña Menú ────────────────────────────────────────────────────
    private final DefaultTableModel menuModel = new DefaultTableModel(
            new String[]{"ID","Fecha","1er Plato","2do Plato","Postre","Alérgenos"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable     menuTable  = new JTable(menuModel);
    private final JTextField menuFecha  = new JTextField(10);
    private final JTextField menuP1     = new JTextField(20);
    private final JTextField menuP2     = new JTextField(20);
    private final JTextField menuPostre = new JTextField(20);
    private final JTextField menuAlerg  = new JTextField(20);
    private final JButton    menuSaveBtn= makeBtn("Guardar",   ACCENT);
    private final JButton    menuDelBtn = makeBtn("Eliminar",  DANGER);
    private int menuSelectedId = -1;

    // ── Pestaña Horarios ────────────────────────────────────────────────
    private final DefaultTableModel horModel = new DefaultTableModel(
            new String[]{"ID","Día","Inicio","Fin","Asignatura","Aula"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable           horTable  = new JTable(horModel);
    private final JComboBox<String> horCurso = new JComboBox<>(CURSOS);
    private final JTextField        horClase = new JTextField(4);
    private final JComboBox<String> horDia   = new JComboBox<>(DIAS);
    private final JTextField        horHI    = new JTextField(5);
    private final JTextField        horHF    = new JTextField(5);
    private final JTextField        horAsig  = new JTextField(18);
    private final JTextField        horAula  = new JTextField(10);
    private final JButton           horDelBtn= makeBtn("Eliminar slot", DANGER);
    private int horSelectedId = -1;

    // ── Status global ────────────────────────────────────────────────────
    private final JLabel statusLabel = new JLabel("Listo");

    public AdminPanel(Client client, String nombre, int userId) {
        this.client   = client;
        this.myUserId = userId;
        this.myName   = nombre;
        setBackground(BG);
        setLayout(new BorderLayout());

        add(buildHeader(nombre), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(new Color(20, 20, 45));
        tabs.setForeground(TEXT);
        tabs.setFont(new Font("SansSerif", Font.BOLD, 12));
        tabs.addTab("👥 Usuarios",  buildUsersTab());
        tabs.addTab("📢 Anuncios",  buildAnunciosTab());
        tabs.addTab("🍽 Menú",      buildMenuTab());
        tabs.addTab("📅 Horarios",  buildHorariosTab());
        add(tabs, BorderLayout.CENTER);

        add(buildStatusBar(), BorderLayout.SOUTH);

        styleTable(usersTable);
        styleTable(annTable);
        styleTable(menuTable);
        styleTable(horTable);

        usersTable.getColumnModel().getColumn(0).setMaxWidth(45);
        annTable.getColumnModel().getColumn(0).setMaxWidth(45);
        menuTable.getColumnModel().getColumn(0).setMaxWidth(45);
        horTable.getColumnModel().getColumn(0).setMaxWidth(45);

        client.sendMessage("ADMIN_GET_USERS");
    }

    // ── Header ──────────────────────────────────────────────────────────

    private JPanel buildHeader(String nombre) {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(new Color(10, 10, 25));
        h.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,ACCENT),
            BorderFactory.createEmptyBorder(10,20,10,20)));
        JLabel title = new JLabel("Panel de Administración");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(ACCENT);
        JLabel user = new JLabel("Sesión: " + nombre + "  (admin)");
        user.setFont(new Font("SansSerif", Font.PLAIN, 12));
        user.setForeground(MUTED);
        h.add(title, BorderLayout.WEST);
        h.add(user,  BorderLayout.EAST);
        return h;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Pestaña USUARIOS
    // ══════════════════════════════════════════════════════════════════════

    private JPanel buildUsersTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            buildUsersTablePanel(), buildUsersFormPanel());
        split.setDividerLocation(430); split.setBorder(null); split.setBackground(BG);
        p.add(split, BorderLayout.CENTER);

        usersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onUserSelect();
        });
        return p;
    }

    private JPanel buildUsersTablePanel() {
        JPanel p = new JPanel(new BorderLayout(0,6));
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(12,12,8,6));
        JLabel lbl = new JLabel("Usuarios registrados");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12)); lbl.setForeground(TEXT);
        p.add(lbl, BorderLayout.NORTH);
        JScrollPane sc = new JScrollPane(usersTable);
        sc.getViewport().setBackground(new Color(20,20,40));
        sc.setBorder(BorderFactory.createLineBorder(new Color(50,60,90)));
        p.add(sc, BorderLayout.CENTER);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        btns.setBackground(BG);
        JButton refreshBtn = makeBtn("Actualizar", new Color(60,100,160));
        refreshBtn.addActionListener(e -> client.sendMessage("ADMIN_GET_USERS"));
        uDeleteBtn.setEnabled(false);
        uDeleteBtn.addActionListener(e -> confirmDeleteUser());
        btns.add(refreshBtn); btns.add(uDeleteBtn);
        p.add(btns, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildUsersFormPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(12,6,8,12));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(PANEL);
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50,60,90)),
            BorderFactory.createEmptyBorder(16,18,16,18)));

        GridBagConstraints c = gbc();
        uFormTitle.setFont(new Font("SansSerif", Font.BOLD, 13)); uFormTitle.setForeground(ACCENT);
        c.insets = new Insets(0,0,12,0); form.add(uFormTitle, c);

        c.insets = new Insets(4,0,2,0); c.gridy++;
        form.add(label("Nombre:"), c); styleField(uName);
        c.gridy++; form.add(uName, c);
        c.gridy++; form.add(label("Contraseña (vacío = sin cambios):"), c); styleField(uPass);
        c.gridy++; form.add(uPass, c);

        JPanel clRow = new JPanel(new GridLayout(1,2,8,0)); clRow.setBackground(PANEL);
        JPanel cp = new JPanel(new BorderLayout(0,2)); cp.setBackground(PANEL);
        cp.add(label("Curso:"), BorderLayout.NORTH); styleField(uCurso); cp.add(uCurso, BorderLayout.CENTER);
        JPanel lp = new JPanel(new BorderLayout(0,2)); lp.setBackground(PANEL);
        lp.add(label("Clase:"), BorderLayout.NORTH); styleField(uClase); lp.add(uClase, BorderLayout.CENTER);
        clRow.add(cp); clRow.add(lp);
        c.gridy++; form.add(clRow, c);

        c.gridy++; form.add(label("Rol:"), c); styleCombo(uRol);
        c.gridy++; form.add(uRol, c);
        c.gridy++; form.add(label("Color de avatar:"), c);
        c.gridy++; form.add(buildColorPicker(uColorBtns, col -> uSelectedColor = col), c);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnRow.setBackground(PANEL);
        uCancelBtn.setVisible(false);
        uCancelBtn.addActionListener(e -> resetUserForm());
        uSaveBtn.addActionListener(e -> saveUser());
        btnRow.add(uCancelBtn); btnRow.add(uSaveBtn);
        c.gridy++; c.insets = new Insets(14,0,0,0); form.add(btnRow, c);

        p.add(form, BorderLayout.NORTH);
        return p;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Pestaña ANUNCIOS
    // ══════════════════════════════════════════════════════════════════════

    private JPanel buildAnunciosTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            buildAnnTablePanel(), buildAnnFormPanel());
        split.setDividerLocation(420); split.setBorder(null); split.setBackground(BG);
        p.add(split, BorderLayout.CENTER);
        annTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onAnnSelect();
        });
        return p;
    }

    private JPanel buildAnnTablePanel() {
        JPanel p = new JPanel(new BorderLayout(0,6));
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(12,12,8,6));
        JLabel lbl = new JLabel("Anuncios publicados");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12)); lbl.setForeground(TEXT);
        p.add(lbl, BorderLayout.NORTH);
        JScrollPane sc = new JScrollPane(annTable);
        sc.getViewport().setBackground(new Color(20,20,40));
        sc.setBorder(BorderFactory.createLineBorder(new Color(50,60,90)));
        p.add(sc, BorderLayout.CENTER);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        btns.setBackground(BG);
        JButton refreshBtn = makeBtn("Actualizar", new Color(60,100,160));
        refreshBtn.addActionListener(e -> client.sendMessage("ADMIN_GET_ANUNCIOS"));
        annDelBtn.setEnabled(false);
        annDelBtn.addActionListener(e -> {
            if (annSelectedId >= 0) client.sendMessage("ADMIN_DEL_ANUNCIO|" + annSelectedId);
        });
        btns.add(refreshBtn); btns.add(annDelBtn);
        p.add(btns, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildAnnFormPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(12,6,8,12));
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(PANEL);
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50,60,90)),
            BorderFactory.createEmptyBorder(16,18,16,18)));
        GridBagConstraints c = gbc();
        JLabel ft = new JLabel("Nuevo anuncio");
        ft.setFont(new Font("SansSerif", Font.BOLD, 13)); ft.setForeground(ACCENT);
        c.insets = new Insets(0,0,12,0); form.add(ft, c);
        c.insets = new Insets(4,0,2,0);
        c.gridy++; form.add(label("Título:"), c); styleField(annTitulo);
        c.gridy++; form.add(annTitulo, c);
        c.gridy++; form.add(label("Contenido:"), c);
        styleArea(annContenido);
        JScrollPane cs = new JScrollPane(annContenido);
        cs.setBorder(BorderFactory.createLineBorder(new Color(60,80,120)));
        c.gridy++; form.add(cs, c);
        annImp.setBackground(PANEL); annImp.setForeground(new Color(255,200,80));
        annImp.setFont(new Font("SansSerif", Font.PLAIN, 12)); annImp.setFocusPainted(false);
        c.gridy++; form.add(annImp, c);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btns.setBackground(PANEL);
        annSaveBtn.addActionListener(e -> publishAnuncio());
        btns.add(annSaveBtn);
        c.gridy++; c.insets = new Insets(12,0,0,0); form.add(btns, c);
        p.add(form, BorderLayout.NORTH);
        return p;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Pestaña MENÚ
    // ══════════════════════════════════════════════════════════════════════

    private JPanel buildMenuTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            buildMenuTablePanel(), buildMenuFormPanel());
        split.setDividerLocation(450); split.setBorder(null); split.setBackground(BG);
        p.add(split, BorderLayout.CENTER);
        menuTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onMenuSelect();
        });
        return p;
    }

    private JPanel buildMenuTablePanel() {
        JPanel p = new JPanel(new BorderLayout(0,6));
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(12,12,8,6));
        JLabel lbl = new JLabel("Menú del comedor");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12)); lbl.setForeground(TEXT);
        p.add(lbl, BorderLayout.NORTH);
        JScrollPane sc = new JScrollPane(menuTable);
        sc.getViewport().setBackground(new Color(20,20,40));
        sc.setBorder(BorderFactory.createLineBorder(new Color(50,60,90)));
        p.add(sc, BorderLayout.CENTER);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        btns.setBackground(BG);
        JButton refreshBtn = makeBtn("Actualizar", new Color(60,100,160));
        refreshBtn.addActionListener(e -> client.sendMessage("ADMIN_GET_MENU"));
        menuDelBtn.setEnabled(false);
        menuDelBtn.addActionListener(e -> {
            if (menuSelectedId >= 0) client.sendMessage("ADMIN_DEL_MENU|" + menuSelectedId);
        });
        btns.add(refreshBtn); btns.add(menuDelBtn);
        p.add(btns, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildMenuFormPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(12,6,8,12));
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(PANEL);
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50,60,90)),
            BorderFactory.createEmptyBorder(16,18,16,18)));
        GridBagConstraints c = gbc();
        JLabel ft = new JLabel("Añadir / Editar día");
        ft.setFont(new Font("SansSerif", Font.BOLD, 13)); ft.setForeground(ACCENT);
        c.insets = new Insets(0,0,10,0); form.add(ft, c); c.insets = new Insets(4,0,2,0);
        JLabel hint = new JLabel("Fecha: AAAA-MM-DD");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 10)); hint.setForeground(MUTED);
        c.gridy++; form.add(hint, c);
        for (JTextField[] row : new JTextField[][]{{menuFecha},{menuP1},{menuP2},{menuPostre},{menuAlerg}}) {
            // handled below
        }
        String[] labels = {"Fecha:","Primer plato:","Segundo plato:","Postre:","Alérgenos:"};
        JTextField[] fields = {menuFecha, menuP1, menuP2, menuPostre, menuAlerg};
        for (int i = 0; i < labels.length; i++) {
            c.gridy++; form.add(label(labels[i]), c);
            styleField(fields[i]);
            c.gridy++; form.add(fields[i], c);
        }
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btns.setBackground(PANEL);
        JButton clearBtn = makeBtn("Limpiar", new Color(70,70,100));
        clearBtn.addActionListener(e -> clearMenuForm());
        menuSaveBtn.addActionListener(e -> saveMenu());
        btns.add(clearBtn); btns.add(menuSaveBtn);
        c.gridy++; c.insets = new Insets(12,0,0,0); form.add(btns, c);
        p.add(form, BorderLayout.NORTH);
        return p;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Pestaña HORARIOS
    // ══════════════════════════════════════════════════════════════════════

    private JPanel buildHorariosTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            buildHorTablePanel(), buildHorFormPanel());
        split.setDividerLocation(420); split.setBorder(null); split.setBackground(BG);
        p.add(split, BorderLayout.CENTER);
        horTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onHorSelect();
        });
        return p;
    }

    private JPanel buildHorTablePanel() {
        JPanel p = new JPanel(new BorderLayout(0,6));
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(12,12,8,6));

        JPanel selector = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        selector.setBackground(BG);
        selector.add(label("Curso:"));
        styleCombo(horCurso); horCurso.setPreferredSize(new Dimension(100, 26));
        selector.add(horCurso);
        selector.add(label("Clase:"));
        styleField(horClase); horClase.setPreferredSize(new Dimension(40, 26));
        selector.add(horClase);
        JButton loadBtn = makeBtn("Cargar", new Color(60,100,160));
        loadBtn.addActionListener(e -> {
            String cur = (String) horCurso.getSelectedItem();
            String cls = horClase.getText().trim();
            if (!cls.isEmpty()) client.sendMessage("ADMIN_GET_HORARIO|" + cur + "|" + cls);
        });
        selector.add(loadBtn);
        p.add(selector, BorderLayout.NORTH);

        JScrollPane sc = new JScrollPane(horTable);
        sc.getViewport().setBackground(new Color(20,20,40));
        sc.setBorder(BorderFactory.createLineBorder(new Color(50,60,90)));
        p.add(sc, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        btns.setBackground(BG);
        horDelBtn.setEnabled(false);
        horDelBtn.addActionListener(e -> {
            if (horSelectedId >= 0) client.sendMessage("ADMIN_DEL_HORARIO|" + horSelectedId);
        });
        btns.add(horDelBtn);
        p.add(btns, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildHorFormPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(12,6,8,12));
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(PANEL);
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50,60,90)),
            BorderFactory.createEmptyBorder(16,18,16,18)));
        GridBagConstraints c = gbc();
        JLabel ft = new JLabel("Añadir franja horaria");
        ft.setFont(new Font("SansSerif", Font.BOLD, 13)); ft.setForeground(ACCENT);
        c.insets = new Insets(0,0,10,0); form.add(ft, c); c.insets = new Insets(4,0,2,0);

        c.gridy++; form.add(label("Día:"), c);
        styleCombo(horDia); c.gridy++; form.add(horDia, c);

        JPanel hhRow = new JPanel(new GridLayout(1,2,8,0)); hhRow.setBackground(PANEL);
        JPanel hip = new JPanel(new BorderLayout(0,2)); hip.setBackground(PANEL);
        hip.add(label("Hora inicio:"), BorderLayout.NORTH); styleField(horHI); hip.add(horHI, BorderLayout.CENTER);
        JPanel hfp = new JPanel(new BorderLayout(0,2)); hfp.setBackground(PANEL);
        hfp.add(label("Hora fin:"), BorderLayout.NORTH); styleField(horHF); hfp.add(horHF, BorderLayout.CENTER);
        hhRow.add(hip); hhRow.add(hfp);
        c.gridy++; form.add(hhRow, c);

        c.gridy++; form.add(label("Asignatura:"), c); styleField(horAsig);
        c.gridy++; form.add(horAsig, c);
        c.gridy++; form.add(label("Aula:"), c); styleField(horAula);
        c.gridy++; form.add(horAula, c);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btns.setBackground(PANEL);
        JButton addBtn = makeBtn("Añadir franja", ACCENT);
        addBtn.addActionListener(e -> addHorario());
        btns.add(addBtn);
        c.gridy++; c.insets = new Insets(12,0,0,0); form.add(btns, c);
        p.add(form, BorderLayout.NORTH);
        return p;
    }

    // ══════════════════════════════════════════════════════════════════════
    // Mensajes del servidor
    // ══════════════════════════════════════════════════════════════════════

    public void handleServerMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            if (msg.startsWith("DATA_USERS|"))           loadUsers(msg.substring(11));
            else if (msg.startsWith("DATA_ANUNCIOS_ADMIN|")) loadAnuncios(msg.substring(20));
            else if (msg.startsWith("DATA_MENU_ADMIN|"))  loadMenu(msg.substring(16));
            else if (msg.startsWith("DATA_HORARIO_ADMIN|")) loadHorario(msg.substring(19));
            else if (msg.startsWith("ADMIN_OK|")) {
                String action = msg.split("\\|")[1];
                setStatus("OK: " + action, false);
                switch (action) {
                    case "create": case "edit": case "delete": resetUserForm(); client.sendMessage("ADMIN_GET_USERS"); break;
                    case "anuncio": case "del_anuncio":        client.sendMessage("ADMIN_GET_ANUNCIOS"); break;
                    case "menu": case "del_menu":              clearMenuForm(); client.sendMessage("ADMIN_GET_MENU"); break;
                    case "horario": case "del_horario":
                        String cur = (String) horCurso.getSelectedItem();
                        String cls = horClase.getText().trim();
                        if (!cls.isEmpty()) client.sendMessage("ADMIN_GET_HORARIO|" + cur + "|" + cls);
                        break;
                }
            } else if (msg.startsWith("ADMIN_ERR|")) {
                setStatus("Error: " + msg.substring(10), true);
            }
        });
    }

    private void loadUsers(String data) {
        usersModel.setRowCount(0);
        if (data.isEmpty()) return;
        for (String rec : data.split(java.util.regex.Pattern.quote("<<REC>>"))) {
            String[] f = rec.split(java.util.regex.Pattern.quote("<<FLD>>"), -1);
            if (f.length >= 6) try { usersModel.addRow(new Object[]{Integer.parseInt(f[0]),f[1],f[2],f[3],f[4],f[5]}); } catch (NumberFormatException ignored) {}
        }
        setStatus("Usuarios: " + usersModel.getRowCount(), false);
    }

    private void loadAnuncios(String data) {
        annModel.setRowCount(0);
        if (data.isEmpty()) return;
        for (String rec : data.split(java.util.regex.Pattern.quote("<<REC>>"))) {
            String[] f = rec.split(java.util.regex.Pattern.quote("<<FLD>>"), -1);
            if (f.length >= 6) try { annModel.addRow(new Object[]{Integer.parseInt(f[0]),f[1],f[3],f[4],"1".equals(f[5])?"★":""}); } catch (NumberFormatException ignored) {}
        }
        annSelectedId = -1; annDelBtn.setEnabled(false);
    }

    private void loadMenu(String data) {
        menuModel.setRowCount(0);
        if (data.isEmpty()) return;
        for (String rec : data.split(java.util.regex.Pattern.quote("<<REC>>"))) {
            String[] f = rec.split(java.util.regex.Pattern.quote("<<FLD>>"), -1);
            if (f.length >= 6) try { menuModel.addRow(new Object[]{Integer.parseInt(f[0]),f[1],f[2],f[3],f[4],f[5]}); } catch (NumberFormatException ignored) {}
        }
        menuSelectedId = -1; menuDelBtn.setEnabled(false);
    }

    private void loadHorario(String data) {
        horModel.setRowCount(0);
        if (data.isEmpty()) { setStatus("Sin franjas para esa clase", false); return; }
        for (String rec : data.split(java.util.regex.Pattern.quote("<<REC>>"))) {
            String[] f = rec.split(java.util.regex.Pattern.quote("<<FLD>>"), -1);
            if (f.length >= 6) try { horModel.addRow(new Object[]{Integer.parseInt(f[0]),f[1],f[2],f[3],f[4],f[5]}); } catch (NumberFormatException ignored) {}
        }
        horSelectedId = -1; horDelBtn.setEnabled(false);
        setStatus("Franjas cargadas: " + horModel.getRowCount(), false);
    }

    // ── Selecciones ──────────────────────────────────────────────────────

    private void onUserSelect() {
        int row = usersTable.getSelectedRow();
        if (row < 0) { resetUserForm(); return; }
        int uid = (int) usersModel.getValueAt(row, 0);
        fillUserForm(uid,
            (String)usersModel.getValueAt(row,1),
            (String)usersModel.getValueAt(row,2),
            (String)usersModel.getValueAt(row,3),
            (String)usersModel.getValueAt(row,4),
            (String)usersModel.getValueAt(row,5));
    }

    private void fillUserForm(int uid, String nombre, String color, String rol, String curso, String clase) {
        uEditingId = uid;
        uFormTitle.setText("Editando: " + nombre);
        uName.setText(nombre); uPass.setText("");
        uRol.setSelectedItem(rol);
        uCurso.setText(curso); uClase.setText(clase);
        applyColor(color, uColorBtns, c -> uSelectedColor = c);
        uSaveBtn.setText("Guardar cambios");
        uCancelBtn.setVisible(true);
        uDeleteBtn.setEnabled(uid != myUserId);
    }

    private void resetUserForm() {
        uEditingId = -1; uFormTitle.setText("Nuevo usuario");
        uName.setText(""); uPass.setText(""); uCurso.setText(""); uClase.setText("");
        uRol.setSelectedIndex(0);
        applyColor(PRESET_COLORS[0], uColorBtns, c -> uSelectedColor = c);
        uSaveBtn.setText("Crear"); uCancelBtn.setVisible(false); uDeleteBtn.setEnabled(false);
        usersTable.clearSelection();
    }

    private void saveUser() {
        String nombre = uName.getText().trim();
        String pass   = new String(uPass.getPassword()).trim();
        String rol    = (String) uRol.getSelectedItem();
        String curso  = uCurso.getText().trim();
        String clase  = uClase.getText().trim();
        if (nombre.isEmpty()) { setStatus("El nombre no puede estar vacío", true); return; }
        if (uEditingId == -1) {
            if (pass.isEmpty()) { setStatus("La contraseña es obligatoria", true); return; }
            client.sendMessage("ADMIN_CREATE_USER|" + nombre + "|" + pass + "|" + uSelectedColor + "|" + rol + "|" + curso + "|" + clase);
        } else {
            client.sendMessage("ADMIN_EDIT_USER|" + uEditingId + "|" + nombre + "|" + pass + "|" + uSelectedColor + "|" + rol + "|" + curso + "|" + clase);
        }
    }

    private void confirmDeleteUser() {
        int row = usersTable.getSelectedRow();
        if (row < 0) return;
        String nombre = (String) usersModel.getValueAt(row, 1);
        if (JOptionPane.showConfirmDialog(this,
                "¿Eliminar al usuario \"" + nombre + "\"?\nEsta acción no se puede deshacer.",
                "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION)
            client.sendMessage("ADMIN_DELETE_USER|" + uEditingId);
    }

    private void onAnnSelect() {
        int row = annTable.getSelectedRow();
        if (row < 0) { annSelectedId = -1; annDelBtn.setEnabled(false); return; }
        annSelectedId = (int) annModel.getValueAt(row, 0);
        annDelBtn.setEnabled(true);
    }

    private void publishAnuncio() {
        String titulo = annTitulo.getText().trim();
        String cont   = annContenido.getText().trim();
        if (titulo.isEmpty() || cont.isEmpty()) { setStatus("Título y contenido obligatorios", true); return; }
        client.sendMessage("ADMIN_ADD_ANUNCIO|" + titulo + "|" + cont + "|" + (annImp.isSelected() ? "1" : "0"));
        annTitulo.setText(""); annContenido.setText(""); annImp.setSelected(false);
    }

    private void onMenuSelect() {
        int row = menuTable.getSelectedRow();
        if (row < 0) { menuSelectedId = -1; menuDelBtn.setEnabled(false); return; }
        menuSelectedId = (int) menuModel.getValueAt(row, 0);
        menuDelBtn.setEnabled(true);
        menuFecha.setText((String)  menuModel.getValueAt(row, 1));
        menuP1.setText((String)     menuModel.getValueAt(row, 2));
        menuP2.setText((String)     menuModel.getValueAt(row, 3));
        menuPostre.setText((String) menuModel.getValueAt(row, 4));
        menuAlerg.setText((String)  menuModel.getValueAt(row, 5));
    }

    private void saveMenu() {
        String fecha = menuFecha.getText().trim();
        if (fecha.isEmpty() || menuP1.getText().trim().isEmpty()) { setStatus("Fecha y primer plato obligatorios", true); return; }
        client.sendMessage("ADMIN_UPSERT_MENU|" + fecha + "|" + menuP1.getText().trim() + "|"
            + menuP2.getText().trim() + "|" + menuPostre.getText().trim() + "|" + menuAlerg.getText().trim());
    }

    private void clearMenuForm() {
        menuFecha.setText(""); menuP1.setText(""); menuP2.setText("");
        menuPostre.setText(""); menuAlerg.setText("");
        menuSelectedId = -1; menuDelBtn.setEnabled(false);
        menuTable.clearSelection();
    }

    private void onHorSelect() {
        int row = horTable.getSelectedRow();
        if (row < 0) { horSelectedId = -1; horDelBtn.setEnabled(false); return; }
        horSelectedId = (int) horModel.getValueAt(row, 0);
        horDelBtn.setEnabled(true);
    }

    private void addHorario() {
        String cur  = (String) horCurso.getSelectedItem();
        String cls  = horClase.getText().trim();
        String dia  = (String) horDia.getSelectedItem();
        String hi   = horHI.getText().trim();
        String hf   = horHF.getText().trim();
        String asig = horAsig.getText().trim();
        String aula = horAula.getText().trim();
        if (cls.isEmpty() || hi.isEmpty() || hf.isEmpty() || asig.isEmpty()) { setStatus("Rellena clase, horas y asignatura", true); return; }
        client.sendMessage("ADMIN_ADD_HORARIO|" + cur + "|" + cls + "|" + dia + "|" + hi + "|" + hf + "|" + asig + "|" + aula);
    }

    // ── Status bar ───────────────────────────────────────────────────────

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 5));
        bar.setBackground(new Color(10, 10, 25));
        bar.setBorder(BorderFactory.createMatteBorder(1,0,0,0,new Color(40,40,70)));
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        statusLabel.setForeground(SUCCESS);
        bar.add(statusLabel);
        return bar;
    }

    private void setStatus(String msg, boolean err) {
        statusLabel.setText(msg);
        statusLabel.setForeground(err ? DANGER : SUCCESS);
    }

    // ── Helpers de estilo ────────────────────────────────────────────────

    private JPanel buildColorPicker(JButton[] btns, java.util.function.Consumer<String> onSelect) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        row.setBackground(PANEL);
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            final int idx = i; final String hex = PRESET_COLORS[i];
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(26, 26));
            b.setBackground(Color.decode(hex));
            b.setBorder(i == 0 ? BorderFactory.createLineBorder(Color.WHITE, 2) : BorderFactory.createLineBorder(new Color(60,60,80), 1));
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> {
                onSelect.accept(hex);
                for (int j = 0; j < btns.length; j++)
                    btns[j].setBorder(j == idx ? BorderFactory.createLineBorder(Color.WHITE, 2) : BorderFactory.createLineBorder(new Color(60,60,80), 1));
            });
            btns[i] = b; row.add(b);
        }
        return row;
    }

    private void applyColor(String hex, JButton[] btns, java.util.function.Consumer<String> onSelect) {
        onSelect.accept(hex);
        for (int i = 0; i < PRESET_COLORS.length; i++)
            btns[i].setBorder(PRESET_COLORS[i].equalsIgnoreCase(hex) ? BorderFactory.createLineBorder(Color.WHITE, 2) : BorderFactory.createLineBorder(new Color(60,60,80), 1));
    }

    private void styleTable(JTable t) {
        t.setBackground(new Color(20,20,40)); t.setForeground(TEXT);
        t.setFont(new Font("SansSerif", Font.PLAIN, 12)); t.setRowHeight(24);
        t.setGridColor(new Color(40,50,70));
        t.setSelectionBackground(new Color(50,80,130)); t.setSelectionForeground(Color.WHITE);
        t.getTableHeader().setBackground(new Color(30,35,60));
        t.getTableHeader().setForeground(ACCENT);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        t.setColumnSelectionAllowed(false);
    }

    private GridBagConstraints gbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1.0;
        c.insets = new Insets(4,0,4,0); c.gridx = 0; c.gridy = 0;
        return c;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text); l.setForeground(MUTED);
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

    private static JButton makeBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 12)); b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(7,14,7,14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            final Color orig = bg;
            public void mouseEntered(MouseEvent e) { b.setBackground(orig.brighter()); }
            public void mouseExited(MouseEvent e)  { b.setBackground(orig); }
        });
        return b;
    }
}
