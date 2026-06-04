import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class TemarioDialog extends JDialog {

    private static final Color BG      = new Color(15, 15, 30);
    private static final Color CARD    = new Color(25, 30, 55);
    private static final Color ACCENT  = new Color(130, 80, 220);
    private static final Color FG      = new Color(220, 230, 245);
    private static final Color FG2     = new Color(150, 170, 200);
    private static final Color IMP     = new Color(231, 76, 60);
    private static final long  MAX_PDF = 10L * 1024 * 1024;

    // ── Nodos del árbol ────────────────────────────────────────────────
    static class FolderNode {
        int id, padreId; String nombre, curso;
        FolderNode(int id, String nombre, int padreId, String curso) {
            this.id = id; this.nombre = nombre; this.padreId = padreId; this.curso = curso;
        }
        public String toString() { return "📁 " + nombre + (curso.isEmpty() ? "" : "  [" + curso + "]"); }
    }

    static class DocNode {
        int id, carpetaId, tamano; String nombre, curso;
        DocNode(int id, String nombre, int carpetaId, String curso, int tamano) {
            this.id = id; this.nombre = nombre; this.carpetaId = carpetaId;
            this.curso = curso; this.tamano = tamano;
        }
        public String toString() {
            return "📄 " + nombre + "  (" + (tamano / 1024) + " KB)";
        }
    }

    // ── Estado ────────────────────────────────────────────────────────
    private final Client client;
    private final String myRol, myCurso;
    private final String[] cursosDisp;
    private JTree tree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    private final Map<Integer, DefaultMutableTreeNode> folderNodes = new HashMap<>();
    private JLabel statusLabel;

    // ── Constructor ───────────────────────────────────────────────────
    public TemarioDialog(JFrame parent, Client client, String myRol, String myCurso,
                         String cursosDisp, String carpetasSerial, String docsSerial) {
        super(parent, false);
        this.client     = client;
        this.myRol      = myRol;
        this.myCurso    = myCurso;
        this.cursosDisp = cursosDisp.isEmpty() ? new String[0] : cursosDisp.split(",");
        setUndecorated(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        int w = parent != null ? (int)(parent.getWidth()  * 0.85) : 714;
        int h = parent != null ? (int)(parent.getHeight() * 0.85) : 561;

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.setBorder(BorderFactory.createLineBorder(ACCENT, 1));

        wrapper.add(buildHeader(), BorderLayout.NORTH);
        wrapper.add(buildBody(carpetasSerial, docsSerial), BorderLayout.CENTER);

        getRootPane().registerKeyboardAction(e -> dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        setContentPane(wrapper);
        setSize(w, h);
        setLocationRelativeTo(parent);
    }

    // ── Cabecera ──────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(new Color(10, 10, 25));
        h.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ACCENT),
            new EmptyBorder(10, 16, 10, 10)));

        JLabel title = new JLabel("Temario");
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
                if (drag[0] != null) {
                    Point loc = TemarioDialog.this.getLocation();
                    TemarioDialog.this.setLocation(loc.x + e.getX() - drag[0].x,
                                                   loc.y + e.getY() - drag[0].y);
                }
            }
        });
        return h;
    }

    // ── Cuerpo ────────────────────────────────────────────────────────
    private JPanel buildBody(String carpetasSerial, String docsSerial) {
        JPanel body = new JPanel(new BorderLayout(8, 0));
        body.setBackground(BG);
        body.setBorder(new EmptyBorder(12, 14, 12, 14));

        // ── Árbol ──────────────────────────────────────────────────────
        rootNode  = new DefaultMutableTreeNode("Temario");
        treeModel = new DefaultTreeModel(rootNode);
        tree = new JTree(treeModel);
        tree.setBackground(CARD); tree.setForeground(FG);
        tree.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tree.setBorder(new EmptyBorder(6, 6, 6, 6));
        tree.setRowHeight(22);
        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            { setBackgroundNonSelectionColor(CARD); setBackgroundSelectionColor(new Color(60, 40, 110));
              setTextNonSelectionColor(FG); setTextSelectionColor(Color.WHITE);
              setBorderSelectionColor(ACCENT); }
        });

        buildTree(carpetasSerial, docsSerial);
        expandAll();

        JScrollPane scroll = new JScrollPane(tree);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(50, 40, 80)));
        scroll.getViewport().setBackground(CARD);
        DarkScrollBarUI.apply(scroll);
        body.add(scroll, BorderLayout.CENTER);

        // ── Panel de botones ───────────────────────────────────────────
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.Y_AXIS));
        btnPanel.setBackground(BG);
        btnPanel.setBorder(new EmptyBorder(0, 8, 0, 0));
        btnPanel.setPreferredSize(new Dimension(160, 0));

        boolean esProfesor = "profesor".equals(myRol) || "admin".equals(myRol);

        if (esProfesor) {
            addBtn(btnPanel, "Nueva carpeta", new Color(90, 60, 160), e -> onNewFolder());
            btnPanel.add(Box.createVerticalStrut(8));
            addBtn(btnPanel, "Subir PDF", new Color(60, 130, 60), e -> onUpload());
            btnPanel.add(Box.createVerticalStrut(8));
        }

        addBtn(btnPanel, "Descargar PDF", new Color(50, 100, 180), e -> onDownload());
        btnPanel.add(Box.createVerticalStrut(16));

        // Status
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        statusLabel.setForeground(FG2);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPanel.add(statusLabel);
        btnPanel.add(Box.createVerticalGlue());

        body.add(btnPanel, BorderLayout.EAST);
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
    private void buildTree(String carpetasSerial, String docsSerial) {
        rootNode.removeAllChildren();
        folderNodes.clear();

        java.util.List<FolderNode> folders = new ArrayList<>();
        if (!carpetasSerial.isEmpty()) {
            for (String rec : carpetasSerial.split(java.util.regex.Pattern.quote("<<REC>>"))) {
                String[] f = rec.split(java.util.regex.Pattern.quote("<<FLD>>"), -1);
                if (f.length < 4) continue;
                int padreId = f[2].isEmpty() ? 0 : parseIntSafe(f[2]);
                folders.add(new FolderNode(parseIntSafe(f[0]), f[1], padreId, f[3]));
            }
        }

        // Crear nodos de carpeta
        for (FolderNode fn : folders) {
            folderNodes.put(fn.id, new DefaultMutableTreeNode(fn));
        }
        // Enlazar con padres
        for (FolderNode fn : folders) {
            DefaultMutableTreeNode node = folderNodes.get(fn.id);
            if (fn.padreId <= 0) rootNode.add(node);
            else {
                DefaultMutableTreeNode parent = folderNodes.get(fn.padreId);
                if (parent != null) parent.add(node); else rootNode.add(node);
            }
        }

        // Añadir documentos
        if (!docsSerial.isEmpty()) {
            for (String rec : docsSerial.split(java.util.regex.Pattern.quote("<<REC>>"))) {
                String[] d = rec.split(java.util.regex.Pattern.quote("<<FLD>>"), -1);
                if (d.length < 5) continue;
                DocNode dn = new DocNode(parseIntSafe(d[0]), d[1], parseIntSafe(d[2]), d[3], parseIntSafe(d[4]));
                DefaultMutableTreeNode folderNode = folderNodes.get(dn.carpetaId);
                if (folderNode != null) folderNode.add(new DefaultMutableTreeNode(dn));
                else rootNode.add(new DefaultMutableTreeNode(dn));
            }
        }
        treeModel.reload();
    }

    private void expandAll() {
        for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
    }

    // ── Acciones del profesor ─────────────────────────────────────────
    private void onNewFolder() {
        JTextField nombreField = new JTextField(20);
        JComboBox<String> cursoBox = new JComboBox<>(cursosDisp.length > 0 ? cursosDisp : new String[]{"General"});

        // Obtener carpeta padre seleccionada
        int padreId = getSelectedFolderId();
        String padreInfo = padreId > 0 ? "Dentro de: carpeta #" + padreId : "Carpeta raíz";

        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel("Nombre:")); panel.add(nombreField);
        panel.add(new JLabel("Curso:")); panel.add(cursoBox);
        panel.add(new JLabel("Ubicación:")); panel.add(new JLabel(padreInfo));

        int res = JOptionPane.showConfirmDialog(this, panel, "Nueva Carpeta",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        String nombre = nombreField.getText().trim();
        if (nombre.isEmpty()) return;
        String curso = (String) cursoBox.getSelectedItem();
        client.sendMessage("TEMARIO_CREATE_FOLDER|" + nombre + "|" + padreId + "|" + curso);
        setStatus("Creando carpeta...");
    }

    private void onUpload() {
        // Seleccionar carpeta destino
        int carpetaId = getSelectedFolderId();
        if (carpetaId <= 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una carpeta destino en el árbol.", "Aviso", JOptionPane.WARNING_MESSAGE);
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

        // Dialogo nombre y curso
        JTextField nombreField = new JTextField(file.getName(), 20);
        JComboBox<String> cursoBox = new JComboBox<>(cursosDisp.length > 0 ? cursosDisp : new String[]{"General"});
        // Prellenar curso de la carpeta seleccionada
        DefaultMutableTreeNode sel = getSelectedNode();
        if (sel != null && sel.getUserObject() instanceof FolderNode) {
            String c = ((FolderNode) sel.getUserObject()).curso;
            for (int i = 0; i < cursoBox.getItemCount(); i++) if (cursoBox.getItemAt(i).equals(c)) { cursoBox.setSelectedIndex(i); break; }
        }

        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel("Nombre del doc:")); panel.add(nombreField);
        panel.add(new JLabel("Visible para:")); panel.add(cursoBox);

        int res = JOptionPane.showConfirmDialog(this, panel, "Subir PDF",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        String nombre = nombreField.getText().trim();
        if (nombre.isEmpty()) nombre = file.getName();
        String curso = (String) cursoBox.getSelectedItem();

        try {
            byte[] data = java.nio.file.Files.readAllBytes(file.toPath());
            String b64 = java.util.Base64.getEncoder().encodeToString(data);
            client.sendMessage("TEMARIO_UPLOAD|" + carpetaId + "|" + nombre + "|" + curso + "|" + b64);
            setStatus("Subiendo archivo...");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error leyendo el archivo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Descarga ──────────────────────────────────────────────────────
    private void onDownload() {
        DefaultMutableTreeNode node = getSelectedNode();
        if (node == null || !(node.getUserObject() instanceof DocNode)) {
            JOptionPane.showMessageDialog(this, "Selecciona un documento en el árbol.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        DocNode dn = (DocNode) node.getUserObject();
        client.sendMessage("TEMARIO_DOWNLOAD|" + dn.id);
        setStatus("Descargando " + dn.nombre + "...");
    }

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

    // ── Respuestas del servidor ───────────────────────────────────────
    public void handleOk(String msg) {
        // TEMARIO_OK|folder|id|nombre|padre_id|curso
        // TEMARIO_OK|doc|id|nombre|carpeta_id|curso|tamano
        String[] p = msg.split("\\|", -1);
        if (p.length < 2) return;
        if ("TEMARIO_OK".equals(p[0]) && "folder".equals(p[1]) && p.length >= 6) {
            int id = parseIntSafe(p[2]); String nombre = p[3]; int padreId = parseIntSafe(p[4]); String curso = p[5];
            FolderNode fn = new FolderNode(id, nombre, padreId, curso);
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(fn);
            folderNodes.put(id, newNode);
            DefaultMutableTreeNode parent = padreId > 0 ? folderNodes.get(padreId) : rootNode;
            if (parent == null) parent = rootNode;
            treeModel.insertNodeInto(newNode, parent, parent.getChildCount());
            expandAll();
            setStatus("Carpeta '" + nombre + "' creada.");
        } else if ("TEMARIO_OK".equals(p[0]) && "doc".equals(p[1]) && p.length >= 7) {
            int id = parseIntSafe(p[2]); String nombre = p[3]; int carpetaId = parseIntSafe(p[4]); String curso = p[5]; int tamano = parseIntSafe(p[6]);
            DocNode dn = new DocNode(id, nombre, carpetaId, curso, tamano);
            DefaultMutableTreeNode folderNode = folderNodes.get(carpetaId);
            if (folderNode == null) folderNode = rootNode;
            DefaultMutableTreeNode docNode = new DefaultMutableTreeNode(dn);
            treeModel.insertNodeInto(docNode, folderNode, folderNode.getChildCount());
            expandAll();
            setStatus("'" + nombre + "' subido correctamente.");
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

    private int getSelectedFolderId() {
        DefaultMutableTreeNode node = getSelectedNode();
        if (node == null) return 0;
        Object obj = node.getUserObject();
        if (obj instanceof FolderNode) return ((FolderNode) obj).id;
        if (obj instanceof DocNode) {
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
            if (parent != null && parent.getUserObject() instanceof FolderNode)
                return ((FolderNode) parent.getUserObject()).id;
        }
        return 0;
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }
}
