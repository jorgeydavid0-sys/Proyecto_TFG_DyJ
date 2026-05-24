import java.security.MessageDigest;
import java.sql.*;
import java.util.*;

public class DBManager {

    // ── Configuración de BD ─────────────────────────────────────────────
    private static final String DB_URL  = "jdbc:postgresql://localhost:5432/escuela_interactiva";
    private static final String DB_USER = "postgres";
    private static final String DB_PASS = "postgres";

    private boolean connected = false;

    // ── Separadores para serialización sin dependencias ─────────────────
    public static final String REC = "<<REC>>";
    public static final String FLD = "<<FLD>>";

    // ── Datos en memoria (fallback) ─────────────────────────────────────
    private final List<String[]> mockUsuarios = new ArrayList<>();
    private final List<String[]> mockAnuncios = new ArrayList<>();
    private final List<String[]> mockHorario  = new ArrayList<>();
    private final List<String[]> mockMenu     = new ArrayList<>();
    private final List<String[]> mockNotas    = new ArrayList<>();
    private int nextUserId   = 5;
    private int nextAnuncioId = 5;
    private int nextNotaId   = 8;
    private int nextHorarioId = 12;
    private int nextMenuId   = 5;

    public DBManager() {
        // [id, nombre, passHash, color, rol]
        mockUsuarios.add(new String[]{"1","Jorge",  hash("1234","jorge"),  "#4A90D9","alumno"});
        mockUsuarios.add(new String[]{"2","Maria",  hash("1234","maria"),  "#E74C3C","profesor"});
        mockUsuarios.add(new String[]{"3","Carlos", hash("1234","carlos"), "#2ECC71","admin"});
        mockUsuarios.add(new String[]{"4","Ana",    hash("1234","ana"),    "#F39C12","alumno"});

        // [id, titulo, contenido, autor, fecha, importante]
        mockAnuncios.add(new String[]{"1","Bienvenidos al nuevo curso","Bienvenidos al curso 2025-2026 de DAM. Revisad los horarios.","Direccion","2026-05-20","1"});
        mockAnuncios.add(new String[]{"2","Concurso de Programacion","Inscripcion abierta para el concurso anual. Limite: 15 de junio.","Dpto. Informatica","2026-05-18","0"});
        mockAnuncios.add(new String[]{"3","Entrega TFG","La fecha limite para la entrega del TFG es el 30 de junio.","Secretaria","2026-05-17","1"});
        mockAnuncios.add(new String[]{"4","Torneo de eSports","El club de gaming organiza un torneo. Inscripciones abiertas.","Club Gaming","2026-05-15","0"});

        // [id, idAlumno, dia, horaInicio, horaFin, asignatura, aula]
        mockHorario.add(new String[]{"1","1","Lunes",   "08:30","10:15","Programacion",         "Aula 201"});
        mockHorario.add(new String[]{"2","1","Lunes",   "10:30","12:15","Base de Datos",        "Aula 203"});
        mockHorario.add(new String[]{"3","1","Lunes",   "12:30","14:15","Entornos de Desarrollo","Lab 1"});
        mockHorario.add(new String[]{"4","1","Martes",  "08:30","10:15","Desarrollo Web",       "Lab 2"});
        mockHorario.add(new String[]{"5","1","Martes",  "10:30","12:15","Sistemas Informaticos","Aula 105"});
        mockHorario.add(new String[]{"6","1","Miercoles","08:30","10:15","Programacion",        "Aula 201"});
        mockHorario.add(new String[]{"7","1","Miercoles","10:30","12:15","Base de Datos",       "Aula 203"});
        mockHorario.add(new String[]{"8","1","Jueves",  "08:30","10:15","Desarrollo Web",       "Lab 2"});
        mockHorario.add(new String[]{"9","1","Jueves",  "10:30","12:15","Entornos de Desarrollo","Lab 1"});
        mockHorario.add(new String[]{"10","1","Viernes","08:30","10:15","Sistemas Informaticos","Aula 105"});
        mockHorario.add(new String[]{"11","1","Viernes","10:30","12:15","Programacion",         "Aula 201"});

        // [id, fecha, primerPlato, segundoPlato, postre, alergenos]
        mockMenu.add(new String[]{"1","2026-05-26","Crema de calabaza","Pollo al horno","Fruta de temporada","Lacteos"});
        mockMenu.add(new String[]{"2","2026-05-27","Ensalada mixta","Merluza a la plancha","Yogur natural","Pescado, Lacteos"});
        mockMenu.add(new String[]{"3","2026-05-28","Lentejas estofadas","Tortilla espanola","Helado","Huevo, Lacteos"});
        mockMenu.add(new String[]{"4","2026-05-29","Sopa de pollo","Lomo a la plancha","Natillas","Lacteos"});

        // [id, idAlumno, materia, calificacion, tipo, comentario, fecha]
        mockNotas.add(new String[]{"1","1","Programacion",        "8.5", "Examen",  "Buen trabajo en POO",        "2026-05-15"});
        mockNotas.add(new String[]{"2","1","Base de Datos",       "9.0", "Examen",  "Excelente diseno de esquema","2026-05-10"});
        mockNotas.add(new String[]{"3","1","Sistemas Informaticos","7.25","Practica","Mejorar documentacion",      "2026-05-05"});
        mockNotas.add(new String[]{"4","1","Entornos de Desarrollo","8.75","Proyecto","Proyecto TFG sobresaliente","2026-05-01"});
        mockNotas.add(new String[]{"5","1","Desarrollo Web",      "9.5", "Examen",  "Dominio frontend y backend", "2026-04-20"});
        mockNotas.add(new String[]{"6","4","Programacion",        "7.0", "Examen",  "Revisar estructuras de datos","2026-05-15"});
        mockNotas.add(new String[]{"7","4","Base de Datos",       "8.25","Examen",  "Buenas consultas SQL",       "2026-05-10"});
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

    // Returns [id, nombre, color, rol] or null
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

    // Returns [id, nombre, color, rol] or null
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
        for (String[] u : mockUsuarios) {
            if (u[1].equalsIgnoreCase(nombre)) return null; // ya existe
        }
        String id = String.valueOf(nextUserId++);
        String[] u = {id, nombre, hash(password, nombre.toLowerCase()), color, "alumno"};
        mockUsuarios.add(u);
        return new String[]{id, nombre, color, "alumno"};
    }

    // ── Datos del juego ─────────────────────────────────────────────────

    public String getAnunciosSerial() {
        if (connected) {
            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(
                     "SELECT titulo,contenido,autor,fecha::text,importante FROM anuncios ORDER BY importante DESC, fecha DESC LIMIT 20")) {
                StringBuilder sb = new StringBuilder();
                while (rs.next()) {
                    if (sb.length() > 0) sb.append(REC);
                    sb.append(rs.getString(1)).append(FLD)
                      .append(rs.getString(2)).append(FLD)
                      .append(rs.getString(3)).append(FLD)
                      .append(rs.getString(4)).append(FLD)
                      .append(rs.getBoolean(5) ? "1" : "0");
                }
                return sb.toString();
            } catch (SQLException e) { System.out.println("DB error: " + e.getMessage()); }
        }
        StringBuilder sb = new StringBuilder();
        List<String[]> sorted = new ArrayList<>(mockAnuncios);
        sorted.sort((a, b) -> {
            int cmp = b[5].compareTo(a[5]);
            return cmp != 0 ? cmp : b[4].compareTo(a[4]);
        });
        for (String[] a : sorted) {
            if (sb.length() > 0) sb.append(REC);
            sb.append(a[1]).append(FLD).append(a[2]).append(FLD)
              .append(a[3]).append(FLD).append(a[4]).append(FLD).append(a[5]);
        }
        return sb.toString();
    }

    public String getHorarioSerial(int userId) {
        if (connected) {
            try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT dia,hora_inicio,hora_fin,asignatura,aula FROM horarios WHERE id_alumno=? " +
                     "ORDER BY CASE dia WHEN 'Lunes' THEN 1 WHEN 'Martes' THEN 2 WHEN 'Miercoles' THEN 3 " +
                     "WHEN 'Jueves' THEN 4 WHEN 'Viernes' THEN 5 END, hora_inicio")) {
                ps.setInt(1, userId);
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
        String uid = String.valueOf(userId);
        List<String[]> list = new ArrayList<>();
        for (String[] h : mockHorario) if (h[1].equals(uid)) list.add(h);
        if (list.isEmpty()) for (String[] h : mockHorario) if (h[1].equals("1")) list.add(h);
        Map<String,Integer> order = new HashMap<>();
        order.put("Lunes",1); order.put("Martes",2); order.put("Miercoles",3);
        order.put("Jueves",4); order.put("Viernes",5);
        list.sort((a,b) -> {
            int d = order.getOrDefault(a[2],9) - order.getOrDefault(b[2],9);
            return d != 0 ? d : a[3].compareTo(b[3]);
        });
        StringBuilder sb = new StringBuilder();
        for (String[] h : list) {
            if (sb.length() > 0) sb.append(REC);
            sb.append(h[2]).append(FLD).append(h[3]).append(FLD)
              .append(h[4]).append(FLD).append(h[5]).append(FLD).append(h[6]);
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
            sb.append(m[1]).append(FLD).append(m[2]).append(FLD)
              .append(m[3]).append(FLD).append(m[4]).append(FLD).append(m[5]);
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
                      .append(rs.getString(3)).append(FLD).append(rs.getString(4)).append(FLD)
                      .append(rs.getString(5));
                }
                return sb.toString();
            } catch (SQLException e) { System.out.println("DB error: " + e.getMessage()); }
        }
        String uid = String.valueOf(userId);
        StringBuilder sb = new StringBuilder();
        for (String[] n : mockNotas) {
            if (!n[1].equals(uid)) continue;
            if (sb.length() > 0) sb.append(REC);
            sb.append(n[2]).append(FLD).append(n[3]).append(FLD)
              .append(n[4]).append(FLD).append(n[5]).append(FLD).append(n[6]);
        }
        return sb.toString();
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
