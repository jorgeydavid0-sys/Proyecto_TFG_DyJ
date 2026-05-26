import java.security.MessageDigest;
import java.sql.*;
import java.util.*;

public class DBManager {

    // ── Configuración de BD ─────────────────────────────────────────────
    private static final String DB_URL  = "jdbc:postgresql://localhost:5432/escuela_interactiva";
    private static final String DB_USER = "postgres";
    private static final String DB_PASS = "1234";

    private boolean connected = false;

    // ── Separadores para serialización sin dependencias ─────────────────
    public static final String REC = "<<REC>>";
    public static final String FLD = "<<FLD>>";

    // ── Datos en memoria (fallback) ─────────────────────────────────────
    // Formato: [id, nombre, passHash, color, rol, curso, clase]
    private final List<String[]> mockUsuarios = new ArrayList<>();
    private final List<String[]> mockAnuncios = new ArrayList<>();
    private final List<String[]> mockHorario  = new ArrayList<>();
    private final List<String[]> mockMenu     = new ArrayList<>();
    private final List<String[]> mockNotas    = new ArrayList<>();
    private int nextUserId    = 10;
    private int nextAnuncioId = 10;
    private int nextNotaId    = 20;
    private int nextHorarioId = 100;
    private int nextMenuId    = 10;

    public DBManager() {
        mockUsuarios.add(new String[]{"1","David",  hash("12345","david"),  "#4A90D9","alumno", "2DAM","A"});
        mockUsuarios.add(new String[]{"2","Laura",  hash("1234","laura"),   "#E74C3C","alumno", "2DAM","A"});
        mockUsuarios.add(new String[]{"3","Marcos", hash("1234","marcos"),  "#9B59B6","profesor","",   ""});
        mockUsuarios.add(new String[]{"4","Elena",  hash("1234","elena"),   "#2ECC71","admin",  "",    ""});
        mockUsuarios.add(new String[]{"5","Ana",    hash("1234","ana"),     "#9B59B6","alumno", "1DAM","A"});
        mockUsuarios.add(new String[]{"6","Carlos", hash("1234","carlos"),  "#F39C12","alumno", "1DAM","A"});

        mockAnuncios.add(new String[]{"1","Bienvenidos al curso 2025-2026",
            "Bienvenidos a un nuevo año escolar.","Direccion","2026-05-20","1","0"});
        mockAnuncios.add(new String[]{"2","Entrega TFG — PLAZO FINAL",
            "Fecha limite 30 de junio a las 23:59.","Coordinacion DAM","2026-05-18","1","0"});
        mockAnuncios.add(new String[]{"3","Concurso de Programacion",
            "Inscripciones hasta el 10 de junio.","Dpto. Informatica","2026-05-16","0","0"});

        // Horario mock: [id, curso, clase, dia, hora_inicio, hora_fin, asignatura, aula]
        mockHorario.add(new String[]{"1","2DAM","A","Lunes",   "08:30","10:15","Programacion Mult.","Lab 201"});
        mockHorario.add(new String[]{"2","2DAM","A","Lunes",   "10:30","12:15","Base de Datos",     "Aula 203"});
        mockHorario.add(new String[]{"3","2DAM","A","Martes",  "08:30","10:15","Desarrollo Web",    "Lab 2"});
        mockHorario.add(new String[]{"4","2DAM","A","Miercoles","08:30","10:15","Programacion Mult.","Lab 201"});
        mockHorario.add(new String[]{"5","2DAM","A","Jueves",  "08:30","10:15","Desarrollo Web",    "Lab 2"});
        mockHorario.add(new String[]{"6","2DAM","A","Viernes", "08:30","10:15","Sistemas Inf.",     "Aula 105"});
        mockHorario.add(new String[]{"7","1DAM","A","Lunes",   "08:30","10:15","Programacion",      "Lab 201"});
        mockHorario.add(new String[]{"8","1DAM","A","Martes",  "08:30","10:15","Bases de Datos",    "Aula 203"});

        mockMenu.add(new String[]{"1","2026-05-26","Crema de calabaza","Pollo al horno","Fruta","Lacteos"});
        mockMenu.add(new String[]{"2","2026-05-27","Ensalada de pasta","Merluza plancha","Yogur","Pescado,Lacteos"});
        mockMenu.add(new String[]{"3","2026-05-28","Lentejas estofadas","Tortilla espanola","Helado","Huevo,Lacteos"});

        mockNotas.add(new String[]{"1","1","Programacion Mult.","8.5","Examen","Buen trabajo","2026-05-15"});
        mockNotas.add(new String[]{"2","1","Base de Datos",      "9.0","Examen","Excelente",  "2026-05-10"});
        mockNotas.add(new String[]{"3","2","Programacion Mult.","7.0","Examen","Revisar",     "2026-05-15"});
    }

    public void connect() {
        try {
            Class.forName("org.postgresql.Driver");
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            conn.close();
            connected = true;
            System.out.println("  Conectado a PostgreSQL");
        } catch (Exception e) {
            connected = false;
            System.out.println("  Sin PostgreSQL, usando datos en memoria.");
        }
    }

    public boolean isConnected() { return connected; }

    // ── Autenticación ───────────────────────────────────────────────────

    public String[] verifyUser(String nombre, String password) {
        if (connected) {
            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT id, nombre, color, rol FROM usuarios WHERE nombre=? AND password=?")) {
                ps.setString(1, nombre);
                ps.setString(2, hash(password, nombre.toLowerCase()));
                ResultSet rs = ps.executeQuery();
                if (rs.next())
                    return new String[]{String.valueOf(rs.getInt(1)), rs.getString(2), rs.getString(3), rs.getString(4)};
            } catch (SQLException e) { System.out.println("DB error: " + e.getMessage()); }
            return null;
        }
        for (String[] u : mockUsuarios) {
            if (u[1].equalsIgnoreCase(nombre) && u[2].equals(hash(password, nombre.toLowerCase())))
                return new String[]{u[0], u[1], u[3], u[4]};
        }
        return null;
    }

    public String[] createUser(String nombre, String password, String color) {
        if (connected) {
            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO usuarios(nombre,password,color,rol) VALUES(?,?,?,?) RETURNING id,nombre,color,rol")) {
                ps.setString(1, nombre);
                ps.setString(2, hash(password, nombre.toLowerCase()));
                ps.setString(3, color);
                ps.setString(4, "alumno");
                ResultSet rs = ps.executeQuery();
                if (rs.next())
                    return new String[]{String.valueOf(rs.getInt(1)), rs.getString(2), rs.getString(3), rs.getString(4)};
            } catch (SQLException e) { System.out.println("DB error: " + e.getMessage()); }
            return null;
        }
        for (String[] u : mockUsuarios) { if (u[1].equalsIgnoreCase(nombre)) return null; }
        String id = String.valueOf(nextUserId++);
        mockUsuarios.add(new String[]{id, nombre, hash(password, nombre.toLowerCase()), color, "alumno", "", ""});
        return new String[]{id, nombre, color, "alumno"};
    }

    // ── Consultas del juego (alumno) ────────────────────────────────────

    public String getAnunciosSerial() {
        if (connected) {
            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(
                     "SELECT titulo,contenido,autor,fecha::text,importante FROM anuncios ORDER BY importante DESC, fecha DESC LIMIT 20")) {
                StringBuilder sb = new StringBuilder();
                while (rs.next()) {
                    if (sb.length() > 0) sb.append(REC);
                    sb.append(rs.getString(1)).append(FLD).append(rs.getString(2)).append(FLD)
                      .append(rs.getString(3)).append(FLD).append(rs.getString(4)).append(FLD)
                      .append(rs.getBoolean(5) ? "1" : "0");
                }
                return sb.toString();
            } catch (SQLException e) { System.out.println("DB error: " + e.getMessage()); }
        }
        StringBuilder sb = new StringBuilder();
        List<String[]> sorted = new ArrayList<>(mockAnuncios);
        sorted.sort((a, b) -> { int d = b[5].compareTo(a[5]); return d != 0 ? d : b[4].compareTo(a[4]); });
        for (String[] a : sorted) {
            if (sb.length() > 0) sb.append(REC);
            sb.append(a[1]).append(FLD).append(a[2]).append(FLD).append(a[3]).append(FLD).append(a[4]).append(FLD).append(a[5]);
        }
        return sb.toString();
    }

    public String[] getUserInfo(int userId) {
        if (connected) {
            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = c.prepareStatement("SELECT curso, clase FROM usuarios WHERE id=?")) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return new String[]{
                    rs.getString(1) != null ? rs.getString(1) : "",
                    rs.getString(2) != null ? rs.getString(2) : ""
                };
            } catch (SQLException e) { System.out.println("DB error: " + e.getMessage()); }
            return new String[]{"",""};
        }
        for (String[] u : mockUsuarios) {
            if (u[0].equals(String.valueOf(userId))) return new String[]{u[5], u[6]};
        }
        return new String[]{"",""};
    }

    public String getHorarioSerial(String curso, String clase) {
        if (curso == null || curso.isEmpty()) return "";
        if (connected) {
            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT dia,hora_inicio,hora_fin,asignatura,aula FROM horarios WHERE curso=? AND clase=? " +
                     "ORDER BY CASE dia WHEN 'Lunes' THEN 1 WHEN 'Martes' THEN 2 WHEN 'Miercoles' THEN 3 " +
                     "WHEN 'Jueves' THEN 4 WHEN 'Viernes' THEN 5 END, hora_inicio")) {
                ps.setString(1, curso);
                ps.setString(2, clase);
                ResultSet rs = ps.executeQuery();
                StringBuilder sb = new StringBuilder();
                while (rs.next()) {
                    if (sb.length() > 0) sb.append(REC);
                    sb.append(rs.getString(1)).append(FLD).append(rs.getString(2)).append(FLD)
                      .append(rs.getString(3)).append(FLD).append(rs.getString(4)).append(FLD)
                      .append(rs.getString(5));
                }
                return sb.toString();
            } catch (SQLException e) { System.out.println("DB error: " + e.getMessage()); }
        }
        Map<String,Integer> ord = new HashMap<>();
        ord.put("Lunes",1); ord.put("Martes",2); ord.put("Miercoles",3); ord.put("Jueves",4); ord.put("Viernes",5);
        List<String[]> list = new ArrayList<>();
        for (String[] h : mockHorario) if (h[1].equalsIgnoreCase(curso) && h[2].equalsIgnoreCase(clase)) list.add(h);
        list.sort((a,b) -> { int d = ord.getOrDefault(a[3],9) - ord.getOrDefault(b[3],9); return d != 0 ? d : a[4].compareTo(b[4]); });
        StringBuilder sb = new StringBuilder();
        for (String[] h : list) {
            if (sb.length() > 0) sb.append(REC);
            sb.append(h[3]).append(FLD).append(h[4]).append(FLD).append(h[5]).append(FLD).append(h[6]).append(FLD).append(h[7]);
        }
        return sb.toString();
    }

    public String getMenuSerial() {
        if (connected) {
            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(
                     "SELECT fecha::text,primer_plato,segundo_plato,postre,alergenos FROM menu_comedor ORDER BY fecha LIMIT 7")) {
                StringBuilder sb = new StringBuilder();
                while (rs.next()) {
                    if (sb.length() > 0) sb.append(REC);
                    sb.append(rs.getString(1)).append(FLD).append(rs.getString(2)).append(FLD)
                      .append(rs.getString(3)).append(FLD).append(rs.getString(4)).append(FLD)
                      .append(rs.getString(5) != null ? rs.getString(5) : "Ninguno");
                }
                return sb.toString();
            } catch (SQLException e) { System.out.println("DB error: " + e.getMessage()); }
        }
        StringBuilder sb = new StringBuilder();
        for (String[] m : mockMenu) {
            if (sb.length() > 0) sb.append(REC);
            sb.append(m[1]).append(FLD).append(m[2]).append(FLD).append(m[3]).append(FLD).append(m[4]).append(FLD).append(m[5]);
        }
        return sb.toString();
    }

    public String getNotasSerial(int userId) {
        if (connected) {
            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT materia,calificacion,tipo,comentario,fecha::text FROM notas WHERE id_alumno=? ORDER BY fecha DESC")) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                StringBuilder sb = new StringBuilder();
                while (rs.next()) {
                    if (sb.length() > 0) sb.append(REC);
                    sb.append(rs.getString(1)).append(FLD).append(rs.getString(2)).append(FLD)
                      .append(rs.getString(3)).append(FLD).append(rs.getString(4)).append(FLD).append(rs.getString(5));
                }
                return sb.toString();
            } catch (SQLException e) { System.out.println("DB error: " + e.getMessage()); }
        }
        String uid = String.valueOf(userId);
        StringBuilder sb = new StringBuilder();
        for (String[] n : mockNotas) {
            if (!n[1].equals(uid)) continue;
            if (sb.length() > 0) sb.append(REC);
            sb.append(n[2]).append(FLD).append(n[3]).append(FLD).append(n[4]).append(FLD).append(n[5]).append(FLD).append(n[6]);
        }
        return sb.toString();
    }

    // ── Profesor: alumnos y poner notas/anuncios ────────────────────────

    public String getAlumnosSerial() {
        if (connected) {
            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(
                     "SELECT id, nombre, curso, clase FROM usuarios WHERE rol='alumno' ORDER BY curso, clase, nombre")) {
                StringBuilder sb = new StringBuilder();
                while (rs.next()) {
                    if (sb.length() > 0) sb.append(REC);
                    sb.append(rs.getInt(1)).append(FLD).append(rs.getString(2)).append(FLD)
                      .append(rs.getString(3) != null ? rs.getString(3) : "").append(FLD)
                      .append(rs.getString(4) != null ? rs.getString(4) : "");
                }
                return sb.toString();
            } catch (SQLException e) { System.out.println("DB error: " + e.getMessage()); }
        }
        StringBuilder sb = new StringBuilder();
        for (String[] u : mockUsuarios) {
            if (!"alumno".equals(u[4])) continue;
            if (sb.length() > 0) sb.append(REC);
            sb.append(u[0]).append(FLD).append(u[1]).append(FLD).append(u[5]).append(FLD).append(u[6]);
        }
        return sb.toString();
    }

    public boolean addNota(int alumnoId, String materia, String calificacion, String tipo, String comentario) {
        if (connected) {
            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO notas(id_alumno,materia,calificacion,tipo,comentario) VALUES(?,?,?,?,?)")) {
                ps.setInt(1, alumnoId);
                ps.setString(2, materia);
                ps.setBigDecimal(3, new java.math.BigDecimal(calificacion));
                ps.setString(4, tipo);
                ps.setString(5, comentario);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) { System.out.println("DB error: " + e.getMessage()); return false; }
        }
        String id = String.valueOf(nextNotaId++);
        mockNotas.add(new String[]{id, String.valueOf(alumnoId), materia, calificacion, tipo, comentario, new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date())});
        return true;
    }

    public boolean addAnuncio(String titulo, String contenido, String autor, int autorId, boolean importante) {
        if (connected) {
            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO anuncios(titulo,contenido,autor,autor_id,importante) VALUES(?,?,?,?,?)")) {
                ps.setString(1, titulo);
                ps.setString(2, contenido);
                ps.setString(3, autor);
                ps.setInt(4, autorId);
                ps.setBoolean(5, importante);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) { System.out.println("DB error: " + e.getMessage()); return false; }
        }
        String id = String.valueOf(nextAnuncioId++);
        mockAnuncios.add(new String[]{id, titulo, contenido, autor, new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()), importante ? "1" : "0", String.valueOf(autorId)});
        return true;
    }

    // ── Gestión de usuarios (admin) ─────────────────────────────────────

    public String getUsersSerial() {
        if (connected) {
            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(
                     "SELECT id, nombre, color, rol, COALESCE(curso,''), COALESCE(clase,'') FROM usuarios ORDER BY id")) {
                StringBuilder sb = new StringBuilder();
                while (rs.next()) {
                    if (sb.length() > 0) sb.append(REC);
                    sb.append(rs.getInt(1)).append(FLD).append(rs.getString(2)).append(FLD)
                      .append(rs.getString(3)).append(FLD).append(rs.getString(4)).append(FLD)
                      .append(rs.getString(5)).append(FLD).append(rs.getString(6));
                }
                return sb.toString();
            } catch (SQLException e) { System.out.println("DB error: " + e.getMessage()); }
        }
        StringBuilder sb = new StringBuilder();
        for (String[] u : mockUsuarios) {
            if (sb.length() > 0) sb.append(REC);
            sb.append(u[0]).append(FLD).append(u[1]).append(FLD).append(u[3]).append(FLD)
              .append(u[4]).append(FLD).append(u[5]).append(FLD).append(u[6]);
        }
        return sb.toString();
    }

    public boolean deleteUser(int userId) {
        if (connected) {
            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = c.prepareStatement("DELETE FROM usuarios WHERE id=?")) {
                ps.setInt(1, userId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) { System.out.println("DB error: " + e.getMessage()); return false; }
        }
        return mockUsuarios.removeIf(u -> u[0].equals(String.valueOf(userId)));
    }

    public boolean editUser(int userId, String nombre, String newPassword, String color, String rol, String curso, String clase) {
        if (connected) {
            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
                PreparedStatement ps;
                if (newPassword != null && !newPassword.isEmpty()) {
                    ps = c.prepareStatement("UPDATE usuarios SET nombre=?,password=?,color=?,rol=?,curso=?,clase=? WHERE id=?");
                    ps.setString(1, nombre); ps.setString(2, hash(newPassword, nombre.toLowerCase()));
                    ps.setString(3, color);  ps.setString(4, rol);
                    ps.setString(5, curso);  ps.setString(6, clase); ps.setInt(7, userId);
                } else {
                    ps = c.prepareStatement("UPDATE usuarios SET nombre=?,color=?,rol=?,curso=?,clase=? WHERE id=?");
                    ps.setString(1, nombre); ps.setString(2, color);
                    ps.setString(3, rol);    ps.setString(4, curso);
                    ps.setString(5, clase);  ps.setInt(6, userId);
                }
                return ps.executeUpdate() > 0;
            } catch (SQLException e) { System.out.println("DB error: " + e.getMessage()); return false; }
        }
        for (String[] u : mockUsuarios) {
            if (u[0].equals(String.valueOf(userId))) {
                u[1] = nombre;
                if (newPassword != null && !newPassword.isEmpty()) u[2] = hash(newPassword, nombre.toLowerCase());
                u[3] = color; u[4] = rol; u[5] = curso; u[6] = clase;
                return true;
            }
        }
        return false;
    }

    public String[] createUserAdmin(String nombre, String password, String color, String rol, String curso, String clase) {
        if (!rol.equals("alumno") && !rol.equals("profesor") && !rol.equals("admin")) rol = "alumno";
        if (connected) {
            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO usuarios(nombre,password,color,rol,curso,clase) VALUES(?,?,?,?,?,?) RETURNING id,nombre,color,rol")) {
                ps.setString(1, nombre); ps.setString(2, hash(password, nombre.toLowerCase()));
                ps.setString(3, color);  ps.setString(4, rol);
                ps.setString(5, curso);  ps.setString(6, clase);
                ResultSet rs = ps.executeQuery();
                if (rs.next())
                    return new String[]{String.valueOf(rs.getInt(1)), rs.getString(2), rs.getString(3), rs.getString(4)};
            } catch (SQLException e) { System.out.println("DB error: " + e.getMessage()); }
            return null;
        }
        for (String[] u : mockUsuarios) { if (u[1].equalsIgnoreCase(nombre)) return null; }
        String id = String.valueOf(nextUserId++);
        mockUsuarios.add(new String[]{id, nombre, hash(password, nombre.toLowerCase()), color, rol, curso, clase});
        return new String[]{id, nombre, color, rol};
    }

    // ── Admin: anuncios ─────────────────────────────────────────────────

    public String getAnunciosAdminSerial() {
        if (connected) {
            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(
                     "SELECT id,titulo,contenido,autor,fecha::text,importante FROM anuncios ORDER BY fecha DESC LIMIT 30")) {
                StringBuilder sb = new StringBuilder();
                while (rs.next()) {
                    if (sb.length() > 0) sb.append(REC);
                    sb.append(rs.getInt(1)).append(FLD).append(rs.getString(2)).append(FLD)
                      .append(rs.getString(3)).append(FLD).append(rs.getString(4)).append(FLD)
                      .append(rs.getString(5)).append(FLD).append(rs.getBoolean(6) ? "1" : "0");
                }
                return sb.toString();
            } catch (SQLException e) { System.out.println("DB error: " + e.getMessage()); }
        }
        StringBuilder sb = new StringBuilder();
        for (String[] a : mockAnuncios) {
            if (sb.length() > 0) sb.append(REC);
            sb.append(a[0]).append(FLD).append(a[1]).append(FLD).append(a[2]).append(FLD)
              .append(a[3]).append(FLD).append(a[4]).append(FLD).append(a[5]);
        }
        return sb.toString();
    }

    public boolean deleteAnuncio(int id) {
        if (connected) {
            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = c.prepareStatement("DELETE FROM anuncios WHERE id=?")) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) { System.out.println("DB error: " + e.getMessage()); return false; }
        }
        return mockAnuncios.removeIf(a -> a[0].equals(String.valueOf(id)));
    }

    // ── Admin: menú del comedor ─────────────────────────────────────────

    public String getMenuAdminSerial() {
        if (connected) {
            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(
                     "SELECT id,fecha::text,primer_plato,segundo_plato,postre,alergenos FROM menu_comedor ORDER BY fecha LIMIT 14")) {
                StringBuilder sb = new StringBuilder();
                while (rs.next()) {
                    if (sb.length() > 0) sb.append(REC);
                    sb.append(rs.getInt(1)).append(FLD).append(rs.getString(2)).append(FLD)
                      .append(rs.getString(3)).append(FLD).append(rs.getString(4)).append(FLD)
                      .append(rs.getString(5)).append(FLD).append(rs.getString(6) != null ? rs.getString(6) : "Ninguno");
                }
                return sb.toString();
            } catch (SQLException e) { System.out.println("DB error: " + e.getMessage()); }
        }
        StringBuilder sb = new StringBuilder();
        for (String[] m : mockMenu) {
            if (sb.length() > 0) sb.append(REC);
            sb.append(m[0]).append(FLD).append(m[1]).append(FLD).append(m[2]).append(FLD)
              .append(m[3]).append(FLD).append(m[4]).append(FLD).append(m[5]);
        }
        return sb.toString();
    }

    public boolean upsertMenuDia(String fecha, String p1, String p2, String postre, String alerg) {
        if (connected) {
            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO menu_comedor(fecha,primer_plato,segundo_plato,postre,alergenos) VALUES(?::date,?,?,?,?) " +
                     "ON CONFLICT(fecha) DO UPDATE SET primer_plato=EXCLUDED.primer_plato, segundo_plato=EXCLUDED.segundo_plato, " +
                     "postre=EXCLUDED.postre, alergenos=EXCLUDED.alergenos")) {
                ps.setString(1, fecha); ps.setString(2, p1); ps.setString(3, p2);
                ps.setString(4, postre); ps.setString(5, alerg);
                ps.executeUpdate();
                return true;
            } catch (SQLException e) { System.out.println("DB error: " + e.getMessage()); return false; }
        }
        for (String[] m : mockMenu) {
            if (m[1].equals(fecha)) { m[2] = p1; m[3] = p2; m[4] = postre; m[5] = alerg; return true; }
        }
        mockMenu.add(new String[]{String.valueOf(nextMenuId++), fecha, p1, p2, postre, alerg});
        return true;
    }

    public boolean deleteMenuDia(int id) {
        if (connected) {
            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = c.prepareStatement("DELETE FROM menu_comedor WHERE id=?")) {
                ps.setInt(1, id); return ps.executeUpdate() > 0;
            } catch (SQLException e) { System.out.println("DB error: " + e.getMessage()); return false; }
        }
        return mockMenu.removeIf(m -> m[0].equals(String.valueOf(id)));
    }

    // ── Admin: horarios ─────────────────────────────────────────────────

    public String getHorarioAdminSerial(String curso, String clase) {
        if (connected) {
            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT id,dia,hora_inicio,hora_fin,asignatura,aula FROM horarios WHERE curso=? AND clase=? " +
                     "ORDER BY CASE dia WHEN 'Lunes' THEN 1 WHEN 'Martes' THEN 2 WHEN 'Miercoles' THEN 3 " +
                     "WHEN 'Jueves' THEN 4 WHEN 'Viernes' THEN 5 END, hora_inicio")) {
                ps.setString(1, curso); ps.setString(2, clase);
                ResultSet rs = ps.executeQuery();
                StringBuilder sb = new StringBuilder();
                while (rs.next()) {
                    if (sb.length() > 0) sb.append(REC);
                    sb.append(rs.getInt(1)).append(FLD).append(rs.getString(2)).append(FLD)
                      .append(rs.getString(3)).append(FLD).append(rs.getString(4)).append(FLD)
                      .append(rs.getString(5)).append(FLD).append(rs.getString(6));
                }
                return sb.toString();
            } catch (SQLException e) { System.out.println("DB error: " + e.getMessage()); }
        }
        StringBuilder sb = new StringBuilder();
        for (String[] h : mockHorario) {
            if (!h[1].equalsIgnoreCase(curso) || !h[2].equalsIgnoreCase(clase)) continue;
            if (sb.length() > 0) sb.append(REC);
            sb.append(h[0]).append(FLD).append(h[3]).append(FLD).append(h[4]).append(FLD)
              .append(h[5]).append(FLD).append(h[6]).append(FLD).append(h[7]);
        }
        return sb.toString();
    }

    public boolean addHorarioSlot(String curso, String clase, String dia, String hi, String hf, String asig, String aula) {
        if (connected) {
            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO horarios(curso,clase,dia,hora_inicio,hora_fin,asignatura,aula) VALUES(?,?,?,?,?,?,?)")) {
                ps.setString(1, curso); ps.setString(2, clase); ps.setString(3, dia);
                ps.setString(4, hi);   ps.setString(5, hf);    ps.setString(6, asig); ps.setString(7, aula);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) { System.out.println("DB error: " + e.getMessage()); return false; }
        }
        mockHorario.add(new String[]{String.valueOf(nextHorarioId++), curso, clase, dia, hi, hf, asig, aula});
        return true;
    }

    public boolean deleteHorarioSlot(int id) {
        if (connected) {
            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = c.prepareStatement("DELETE FROM horarios WHERE id=?")) {
                ps.setInt(1, id); return ps.executeUpdate() > 0;
            } catch (SQLException e) { System.out.println("DB error: " + e.getMessage()); return false; }
        }
        return mockHorario.removeIf(h -> h[0].equals(String.valueOf(id)));
    }

    // ── Utilidades ──────────────────────────────────────────────────────

    public static String hash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest((password + ":" + salt).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return password; }
    }
}
