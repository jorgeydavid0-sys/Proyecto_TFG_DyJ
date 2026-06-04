import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class TrabajoDialog extends JDialog {

    private static final Color BG     = new Color(15, 15, 30);
    private static final Color CARD   = new Color(25, 30, 55);
    private static final Color ACCENT = new Color(60, 160, 80);
    private static final Color FG     = new Color(220, 230, 245);
    private static final Color FG2    = new Color(150, 170, 200);
    private static final Color IMP    = new Color(231, 76, 60);
    private static final long  MAX_PDF = 10L * 1024 * 1024;

    // ── Nodos ────────────────────────────────────────────────────────
    static class TrabajoNode {
        int id; String nombre, descripcion, fechaEntrega, curso;
        TrabajoNode(int id, String nombre, String desc, String fecha, String curso) {
            this.id = id; this.nombre = nombre; this.descripcion = desc;
            this.fechaEntrega = fecha; this.curso = curso;
        }
        public String toString() {
            String f = fechaEntrega.isEmpty() ? "" : "  ·  hasta " + fechaEntrega;
            return "📋 " + nombre + "  [" + curso + "]" + f;
        }
    }

    static class EntregaNode {
        int entregaId, alumnoId, trabajoId, tamano;
        String alumnoNombre, nombreArchivo, fecha, nota, comentario;
        boolean haEntregado;
        EntregaNode(int trabajoId, int alumnoId, String alumnoNombre, boolean haEntregado,
                    int entregaId, String nombreArchivo, int tamano, String fecha, String nota, String comentario) {
            this.trabajoId = trabajoId; this.alumnoId = alumnoId; this.alumnoNombre = alumnoNombre;
            this.haEntregado = haEntregado; this.entregaId = entregaId;
            this.nombreArchivo = nombreArchivo; this.tamano = tamano;
            this.fecha = fecha; this.nota = nota; this.comentario = comentario;
        }
        public String toString() {
            if (!haEntregado) return "❌  " + alumnoNombre + "  —  Sin entregar";
            String n = nota.isEmpty() ? "sin nota" : "nota: " + nota;
            return "✅  " + alumnoNombre + "  —  " + nombreArchivo + "  (" + n + ")";
        }
    }

    // ── Estado ───────────────────────────────────────────────────────
    private final Client client;
    private final String myRol, myCurso;
    private final int    myUserId;
    private final String[] cursosDisp;
    private final boolean esProfesor;
    private JTree tree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    private final Map<Integer, DefaultMutableTreeNode> trabajoNodes = new HashMap<>();
    private JLabel statusLabel;
    private JTextArea descArea;

    // ── Constructor ──────────────────────────────────────────────────
    public TrabajoDialog(JFrame parent, Client client, String myRol, String myCurso, int myUserId,
                         String cursosDisp, String trabajosSerial, String studentsSerial, String entregasSerial) {
        super(parent, false);
        this.client     = client;
        this.myRol      = myRol;
        this.myCurso    = myCurso;
        this.myUserId   = myUserId;
        this.cursosDisp = cursosDisp.isEmpty() ? new String[0] : cursosDisp.split(",");
        this.esProfesor = "profesor".equals(myRol) || "admin".equals(myRol);
        setUndecorated(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        int w = parent != null ? (int)(parent.getWidth()  * 0.88) : 740;
        int h = parent != null ? (int)(parent.getHeight() * 0.88) : 580;

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.setBorder(BorderFactory.createLineBorder(ACCENT, 1));
        wrapper.add(buildHeader(), BorderLayout.NORTH);
        wrapper.add(buildBody(trabajosSerial, studentsSerial, entregasSerial), BorderLayout.CENTER);

        getRootPane().registerKeyboardAction(e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setContentPane(wrapper);
        setSize(w, h);
        setLocationRelativeTo(parent);
    }

    // ── Cabecera ─────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(new Color(10, 10, 25));
        h.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ACCENT),
            new EmptyBorder(10, 16, 10, 10)));
        JLabel title = new JLabel("Trabajos y Entregas");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        title.setForeground(ACCENT);
        h.add(title, BorderLayout.WEST);
        JButton close = new JButton("✕");
        close.setFont(new Font("SansSerif", Font.BOLD, 14));
        close.setForeground(FG2); close.setFocusPainted(false);
        close.setBorderPainted(false); close.setContentAreaFilled(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addActionListener(e -> dispose());
        close.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { close.setForeground(IMP); }
            public void mouseExited(MouseEvent e)  { close.setForeground(FG2); }
        });
        h.add(close, BorderLayout.EAST);
        final Point[] drag = {null};
        h.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e)  { drag[0] = e.getPoint(); }
            public void mouseReleased(MouseEvent e) { drag[0] = null; }
        });
        h.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (drag[0] == null) return;
                Point loc = TrabajoDialog.this.getLocation();
                TrabajoDialog.this.setLocation(loc.x + e.getX() - drag[0].x, loc.y + e.getY() - drag[0].y);
            }
        });
        return h;
    }

    // ── Cuerpo ───────────────────────────────────────────────────────
    private JPanel buildBody(String trabajosSerial, String studentsSerial, String entregasSerial) {
        JPanel body = new JPanel(new BorderLayout(8, 0));
        body.setBackground(BG);
        body.setBorder(new EmptyBorder(12, 14, 12, 14));

        // Árbol
        rootNode  = new DefaultMutableTreeNode("Trabajos");
        treeModel = new DefaultTreeModel(rootNode);
        tree = new JTree(treeModel);
        tree.setBackground(CARD); tree.setForeground(FG);
        tree.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tree.setBorder(new EmptyBorder(6, 6, 6, 6));
        tree.setRowHeight(22);
        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            { setBackgroundNonSelectionColor(CARD); setBackgroundSelectionColor(new Color(30, 80, 40));
              setTextNonSelectionColor(FG); setTextSelectionColor(Color.WHITE);
              setBorderSelectionColor(ACCENT); }
        });
        tree.addTreeSelectionListener(e -> onTreeSelection());

        buildTree(trabajosSerial, studentsSerial, entregasSerial);
        expandAll();

        JScrollPane treeScroll = new JScrollPane(tree);
        treeScroll.setBorder(BorderFactory.createLineBorder(new Color(40, 70, 45)));
        treeScroll.getViewport().setBackground(CARD);
        DarkScrollBarUI.apply(treeScroll);

        // Panel derecho
        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.setBackground(BG);
        right.setPreferredSize(new Dimension(190, 0));

        // Descripción
        descArea = new JTextArea(4, 18);
        descArea.setFont(new Font("SansSerif", Font.PLAIN, 11));
        descArea.setForeground(FG2); descArea.setBackground(CARD);
        descArea.setEditable(false); descArea.setLineWrap(true); descArea.setWrapStyleWord(true);
        descArea.setBorder(new EmptyBorder(6, 6, 6, 6));
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setBorder(BorderFactory.createLineBorder(new Color(40, 60, 45)));
        descScroll.setPreferredSize(new Dimension(190, 80));
        right.add(descScroll, BorderLayout.NORTH);

        // Botones
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.Y_AXIS));
        btnPanel.setBackground(BG);

        if (esProfesor) {
            addBtn(btnPanel, "Nuevo trabajo",      new Color(40, 120, 55),  e -> onNuevoTrabajo());
            btnPanel.add(Box.createVerticalStrut(6));
            addBtn(btnPanel, "Descargar entrega",  new Color(50, 100, 180), e -> onDescargar());
            btnPanel.add(Box.createVerticalStrut(6));
            addBtn(btnPanel, "Poner nota",         new Color(160, 100, 20), e -> onPonerNota());
        } else {
            addBtn(btnPanel, "Entregar PDF",       new Color(50, 100, 180), e -> onEntregar());
        }

        btnPanel.add(Box.createVerticalStrut(14));
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        statusLabel.setForeground(FG2);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPanel.add(statusLabel);
        btnPanel.add(Box.createVerticalGlue());
        right.add(btnPanel, BorderLayout.CENTER);

        body.add(treeScroll, BorderLayout.CENTER);
        body.add(right, BorderLayout.EAST);
        return body;
    }

    private void addBtn(JPanel p, String text, Color bg, ActionListener al) {
        JButton btn = new JButton(text);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        btn.setBackground(bg); btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false); btn.setBorderPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(al);
        p.add(btn);
    }

    // ── Construcción del árbol ────────────────────────────────────────
    private void buildTree(String trabajosSerial, String studentsSerial, String entregasSerial) {
        rootNode.removeAllChildren();
        trabajoNodes.clear();

        // Parsear trabajos
        java.util.List<TrabajoNode> trabajosList = new ArrayList<>();
        if (!trabajosSerial.isEmpty()) {
            for (String rec : trabajosSerial.split(java.util.regex.Pattern.quote("<<REC>>"))) {
                String[] f = rec.split(java.util.regex.Pattern.quote("<<FLD>>"), -1);
                if (f.length < 5) continue;
                trabajosList.add(new TrabajoNode(parseInt(f[0]), f[1], f[2], f[3], f[4]));
            }
        }

        // Parsear alumnos (para profesor)
        Map<Integer, String[]> studentMap = new HashMap<>(); // id → {nombre, curso}
        if (!studentsSerial.isEmpty()) {
            for (String rec : studentsSerial.split(java.util.regex.Pattern.quote("<<REC>>"))) {
                String[] f = rec.split(java.util.regex.Pattern.quote("<<FLD>>"), -1);
                if (f.length < 3) continue;
                studentMap.put(parseInt(f[0]), new String[]{f[1], f[2]});
            }
        }

        // Parsear entregas
        // id<<FLD>>trabajo_id<<FLD>>alumno_id<<FLD>>nombre_archivo<<FLD>>tamano<<FLD>>fecha<<FLD>>nota<<FLD>>comentario
        Map<String, int[]> entregaKeyToId = new HashMap<>(); // "trabajoId_alumnoId" → entregaId
        Map<Integer, String[]> entregaMap = new HashMap<>();  // entregaId → {nombre_archivo, tamano, fecha, nota, comentario}
        if (!entregasSerial.isEmpty()) {
            for (String rec : entregasSerial.split(java.util.regex.Pattern.quote("<<REC>>"))) {
                String[] f = rec.split(java.util.regex.Pattern.quote("<<FLD>>"), -1);
                if (f.length < 8) continue;
                int eId = parseInt(f[0]), tId = parseInt(f[1]), aId = parseInt(f[2]);
                entregaKeyToId.put(tId + "_" + aId, new int[]{eId});
                entregaMap.put(eId, new String[]{f[3], f[4], f[5], f[6], f[7]});
            }
        }

        // Construir árbol
        for (TrabajoNode tn : trabajosList) {
            DefaultMutableTreeNode tNode = new DefaultMutableTreeNode(tn);
            trabajoNodes.put(tn.id, tNode);
            rootNode.add(tNode);

            if (esProfesor) {
                // Añadir un EntregaNode por cada alumno del curso
                for (Map.Entry<Integer, String[]> se : studentMap.entrySet()) {
                    if (!se.getValue()[1].equals(tn.curso)) continue;
                    int aId = se.getKey();
                    String aNombre = se.getValue()[0];
                    String key = tn.id + "_" + aId;
                    int[] eIdArr = entregaKeyToId.get(key);
                    if (eIdArr != null) {
                        String[] ed = entregaMap.get(eIdArr[0]);
                        EntregaNode en = new EntregaNode(tn.id, aId, aNombre, true,
                            eIdArr[0], ed[0], parseInt(ed[1]), ed[2], ed[3], ed[4]);
                        tNode.add(new DefaultMutableTreeNode(en));
                    } else {
                        EntregaNode en = new EntregaNode(tn.id, aId, aNombre, false, 0, "", 0, "", "", "");
                        tNode.add(new DefaultMutableTreeNode(en));
                    }
                }
            } else {
                // Solo la entrega propia
                String key = tn.id + "_" + myUserId;
                int[] eIdArr = entregaKeyToId.get(key);
                if (eIdArr != null) {
                    String[] ed = entregaMap.get(eIdArr[0]);
                    EntregaNode en = new EntregaNode(tn.id, myUserId, "Yo", true,
                        eIdArr[0], ed[0], parseInt(ed[1]), ed[2], ed[3], ed[4]);
                    tNode.add(new DefaultMutableTreeNode(en));
                } else {
                    EntregaNode en = new EntregaNode(tn.id, myUserId, "Yo", false, 0, "", 0, "", "", "");
                    tNode.add(new DefaultMutableTreeNode(en));
                }
            }
        }
        treeModel.reload();
    }

    private void expandAll() {
        for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
    }

    private void onTreeSelection() {
        DefaultMutableTreeNode node = getSelectedNode();
        if (node == null) return;
        Object obj = node.getUserObject();
        if (obj instanceof TrabajoNode) {
            TrabajoNode tn = (TrabajoNode) obj;
            descArea.setText("Descripción:\n" + (tn.descripcion.isEmpty() ? "(sin descripción)" : tn.descripcion));
        } else if (obj instanceof EntregaNode) {
            EntregaNode en = (EntregaNode) obj;
            if (en.haEntregado && !en.nota.isEmpty())
                descArea.setText("Nota: " + en.nota + "\n" + (en.comentario.isEmpty() ? "" : "Comentario:\n" + en.comentario));
            else if (en.haEntregado)
                descArea.setText("Entregado: " + en.nombreArchivo + "\n" + en.fecha);
            else
                descArea.setText("Sin entregar.");
        }
    }

    // ── Acciones profesor ─────────────────────────────────────────────
    private void onNuevoTrabajo() {
        JTextField nombreField = new JTextField(20);
        JTextArea  descField   = new JTextArea(3, 20);
        descField.setLineWrap(true); descField.setWrapStyleWord(true);
        JTextField fechaField  = new JTextField("dd/mm/aaaa", 10);
        JComboBox<String> cursoBox = new JComboBox<>(cursosDisp.length > 0 ? cursosDisp : new String[]{"General"});

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4,4,4,4); gc.anchor = GridBagConstraints.WEST;
        gc.gridx=0; gc.gridy=0; panel.add(new JLabel("Nombre:"), gc);
        gc.gridx=1; panel.add(nombreField, gc);
        gc.gridx=0; gc.gridy=1; panel.add(new JLabel("Descripción:"), gc);
        gc.gridx=1; panel.add(new JScrollPane(descField), gc);
        gc.gridx=0; gc.gridy=2; panel.add(new JLabel("Fecha entrega:"), gc);
        gc.gridx=1; panel.add(fechaField, gc);
        gc.gridx=0; gc.gridy=3; panel.add(new JLabel("Curso:"), gc);
        gc.gridx=1; panel.add(cursoBox, gc);

        int res = JOptionPane.showConfirmDialog(this, panel, "Nuevo Trabajo",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        String nombre = nombreField.getText().trim();
        if (nombre.isEmpty()) return;
        String desc  = descField.getText().trim().replace("|", " ");
        String fecha = fechaField.getText().trim();
        String curso = (String) cursoBox.getSelectedItem();
        client.sendMessage("TRABAJO_CREATE|" + nombre + "|" + desc + "|" + fecha + "|" + curso);
        setStatus("Creando trabajo...");
    }

    private void onDescargar() {
        EntregaNode en = getSelectedEntrega();
        if (en == null || !en.haEntregado) {
            JOptionPane.showMessageDialog(this, "Selecciona una entrega entregada.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        client.sendMessage("TRABAJO_DOWNLOAD|" + en.entregaId);
        setStatus("Descargando " + en.nombreArchivo + "...");
    }

    private void onPonerNota() {
        EntregaNode en = getSelectedEntrega();
        if (en == null || !en.haEntregado) {
            JOptionPane.showMessageDialog(this, "Selecciona una entrega para calificar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JTextField notaField = new JTextField(en.nota.isEmpty() ? "" : en.nota, 6);
        JTextField comentField = new JTextField(en.comentario, 20);
        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel("Nota (0-10):")); panel.add(notaField);
        panel.add(new JLabel("Comentario:"));  panel.add(comentField);
        int res = JOptionPane.showConfirmDialog(this, panel, "Poner Nota — " + en.alumnoNombre,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        try {
            double nota = Double.parseDouble(notaField.getText().trim().replace(",", "."));
            if (nota < 0 || nota > 10) throw new NumberFormatException();
            String comentario = comentField.getText().trim().replace("|", " ");
            client.sendMessage("TRABAJO_SET_NOTA|" + en.entregaId + "|" + nota + "|" + comentario);
            setStatus("Guardando nota...");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "La nota debe ser un número entre 0 y 10.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Acción alumno ─────────────────────────────────────────────────
    private void onEntregar() {
        DefaultMutableTreeNode selNode = getSelectedNode();
        TrabajoNode tn = null;
        if (selNode != null) {
            Object obj = selNode.getUserObject();
            if (obj instanceof TrabajoNode) tn = (TrabajoNode) obj;
            else if (obj instanceof EntregaNode) {
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selNode.getParent();
                if (parent != null && parent.getUserObject() instanceof TrabajoNode)
                    tn = (TrabajoNode) parent.getUserObject();
            }
        }
        if (tn == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un trabajo en el árbol.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos PDF", "pdf"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = fc.getSelectedFile();
        if (file.length() > MAX_PDF) {
            JOptionPane.showMessageDialog(this, "El archivo supera el límite de 10 MB.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            byte[] data = java.nio.file.Files.readAllBytes(file.toPath());
            String b64  = java.util.Base64.getEncoder().encodeToString(data);
            client.sendMessage("TRABAJO_SUBMIT|" + tn.id + "|" + file.getName() + "|" + b64);
            setStatus("Enviando entrega...");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error leyendo archivo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Respuestas del servidor ───────────────────────────────────────
    public void receiveFile(String nombre, String base64) {
        try {
            byte[] data = java.util.Base64.getDecoder().decode(base64);
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File(nombre));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try (FileOutputStream fos = new FileOutputStream(fc.getSelectedFile())) {
                    fos.write(data);
                    setStatus("Guardado: " + fc.getSelectedFile().getName());
                    JOptionPane.showMessageDialog(this, "Archivo guardado correctamente.");
                }
            } else { setStatus(" "); }
        } catch (Exception ex) {
            handleErr("Error al guardar: " + ex.getMessage());
        }
    }

    public void handleOk(String msg) {
        String[] p = msg.split("\\|", -1);
        if (p.length < 2) return;

        if ("created".equals(p[1]) && p.length >= 7) {
            // TRABAJO_OK|created|id|nombre|descripcion|fecha|curso
            TrabajoNode tn = new TrabajoNode(parseInt(p[2]), p[3], p[4], p[5], p[6]);
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(tn);
            trabajoNodes.put(tn.id, newNode);
            // Añadir placeholder de entrega propia (alumno) o vacío (profesor)
            if (!esProfesor) {
                EntregaNode en = new EntregaNode(tn.id, myUserId, "Yo", false, 0, "", 0, "", "", "");
                newNode.add(new DefaultMutableTreeNode(en));
            }
            treeModel.insertNodeInto(newNode, rootNode, rootNode.getChildCount());
            expandAll();
            setStatus("Trabajo '" + tn.nombre + "' creado.");

        } else if ("submitted".equals(p[1]) && p.length >= 7) {
            // TRABAJO_OK|submitted|entrega_id|trabajo_id|alumno_id|nombre_archivo|tamano
            int entregaId = parseInt(p[2]), trabajoId = parseInt(p[3]);
            String nombreArch = p[5]; int tamano = parseInt(p[6]);
            DefaultMutableTreeNode tNode = trabajoNodes.get(trabajoId);
            if (tNode != null) {
                // Actualizar o añadir nodo de entrega propia
                for (int i = 0; i < tNode.getChildCount(); i++) {
                    DefaultMutableTreeNode child = (DefaultMutableTreeNode) tNode.getChildAt(i);
                    if (child.getUserObject() instanceof EntregaNode) {
                        EntregaNode en = (EntregaNode) child.getUserObject();
                        if (en.alumnoId == myUserId) {
                            EntregaNode updated = new EntregaNode(trabajoId, myUserId, "Yo", true,
                                entregaId, nombreArch, tamano, "ahora", "", "");
                            child.setUserObject(updated);
                            treeModel.nodeChanged(child);
                            setStatus("Entrega enviada: " + nombreArch);
                            return;
                        }
                    }
                }
            }
            setStatus("Entrega enviada: " + nombreArch);

        } else if ("nota".equals(p[1]) && p.length >= 4) {
            // TRABAJO_OK|nota|entrega_id|nota|comentario
            int entregaId = parseInt(p[2]); String nota = p[3];
            String comentario = p.length >= 5 ? p[4] : "";
            // Actualizar nodo correspondiente
            for (Map.Entry<Integer, DefaultMutableTreeNode> e : trabajoNodes.entrySet()) {
                DefaultMutableTreeNode tNode = e.getValue();
                for (int i = 0; i < tNode.getChildCount(); i++) {
                    DefaultMutableTreeNode child = (DefaultMutableTreeNode) tNode.getChildAt(i);
                    if (child.getUserObject() instanceof EntregaNode) {
                        EntregaNode en = (EntregaNode) child.getUserObject();
                        if (en.entregaId == entregaId) {
                            EntregaNode updated = new EntregaNode(en.trabajoId, en.alumnoId, en.alumnoNombre,
                                true, entregaId, en.nombreArchivo, en.tamano, en.fecha, nota, comentario);
                            child.setUserObject(updated);
                            treeModel.nodeChanged(child);
                            setStatus("Nota " + nota + " guardada para " + en.alumnoNombre);
                            return;
                        }
                    }
                }
            }
        }
    }

    public void handleErr(String msg) {
        setStatus("Error: " + msg);
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private DefaultMutableTreeNode getSelectedNode() {
        TreePath path = tree.getSelectionPath();
        if (path == null) return null;
        return (DefaultMutableTreeNode) path.getLastPathComponent();
    }

    private EntregaNode getSelectedEntrega() {
        DefaultMutableTreeNode node = getSelectedNode();
        if (node == null) return null;
        Object obj = node.getUserObject();
        return obj instanceof EntregaNode ? (EntregaNode) obj : null;
    }

    private void setStatus(String msg) { if (statusLabel != null) statusLabel.setText(msg); }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }
}
