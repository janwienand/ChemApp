package org.example;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.security.NoSuchAlgorithmException;
import org.xml.sax.InputSource;
import java.io.StringReader;
import static spark.Spark.*;

public class ChemCompoundDB {

    private static final String DB_URL = "jdbc:h2:mem:chemdb;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "very_secret_and_complex_password";
    private static final String API_SECRET_KEY = "key123_a_very_bad_secret_dont_use";
    private static final String SALT_FOR_HASHING = "static_salt_for_every_user";
    private static final String UPLOAD_DIR = "uploads";

    public static void main(String[] args) {
        initializeDatabase();
        port(8080);

        File uploadDir = new File(UPLOAD_DIR);
        uploadDir.mkdir();
        staticFiles.externalLocation(UPLOAD_DIR);

        get("/", (req, res) -> {
            return "<h1>ChemCompound DB</h1>"
                    + "<h3>Search for a Compound (SQLi)</h3>"
                    + "<form action='/search' method='get'><input type='text' name='name'><input type='submit' value='Search'></form><hr>"

                    + "<h3>Add a Research Note (Stored XSS)</h3>"
                    + "<form action='/addnote' method='post'><textarea name='note' rows='4' cols='50'></textarea><br><input type='submit' value='Save Note'></form><hr>"

                    + "<h3>Run Diagnostics (Command Injection)</h3>"
                    + "<form action='/diag' method='get'><input type='text' name='host' value='localhost'><input type='submit' value='Run Ping'></form><hr>"

                    + "<h3>Process Serialized Data (Insecure Deserialization)</h3>"
                    + "<form action='/deserialize' method='post'><textarea name='data' rows='4' cols='50'></textarea><br><input type='submit' value='Process'></form><hr>"

                    + "<h3>Parse XML Configuration (XXE)</h3>"
                    + "<form action='/parse-xml' method='post'><textarea name='xml' rows='4' cols='50'></textarea><br><input type='submit' value='Parse'></form><hr>"

                    + "<h3>Upload Lab Results (Unsafe File Upload)</h3>"
                    + "<form action='/upload' method='post' enctype='multipart/form-data'><input type='file' name='labfile'><input type='submit' value='Upload'></form><hr>"

                    + "<h3>Redirect to Partner Site (Open Redirect)</h3>"
                    + "<form action='/redirect' method='get'><input type='text' name='url' value='http://example.com'><input type='submit' value='Go'></form><hr>"

                    + "<h2>Research Notes</h2>" + getNotes();
        });

        get("/search", (req, res) -> {
            String compoundName = req.queryParams("name");
            StringBuilder result = new StringBuilder("<h2>Search Results</h2>");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                // VULNERABILITY: SQL Injection
                String sql = "SELECT * FROM compounds WHERE name = '" + compoundName + "'";
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        result.append("<p>").append(rs.getString("name")).append("</p>");
                    }
                }
            }
            return result.toString();
        });

        post("/addnote", (req, res) -> {
            String note = req.queryParams("note");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD); Statement stmt = conn.createStatement()) {
                // VULNERABILITY: Stored XSS
                String sql = "INSERT INTO notes (content) VALUES ('" + note + "')";
                stmt.executeUpdate(sql);
            }
            res.redirect("/");
            return "";
        });

        get("/diag", (req, res) -> {
            String host = req.queryParams("host");
            String command = System.getProperty("os.name").toLowerCase().contains("win") ? "ping -n 1 " + host : "ping -c 1 " + host;
            StringBuilder output = new StringBuilder("<pre>");
            try {
                // VULNERABILITY: OS Command Injection
                Process process = Runtime.getRuntime().exec(command);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }
            } catch (IOException e) {
                output.append("Error: ").append(e.getMessage());
            }
            return output.append("</pre>").toString();
        });

        post("/deserialize", (req, res) -> {
            try {
                byte[] data = Base64.getDecoder().decode(req.queryParams("data"));
                // VULNERABILITY: Insecure Deserialization
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
                Object obj = ois.readObject();
                ois.close();
                return "Object deserialized: " + obj.toString();
            } catch (Exception e) {
                return "Deserialization failed: " + e.getMessage();
            }
        });

        post("/parse-xml", (req, res) -> {
            String xml = req.queryParams("xml");
            try {
                // VULNERABILITY: XML External Entity (XXE)
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                builder.parse(new InputSource(new StringReader(xml)));
                return "XML parsed successfully!";
            } catch (Exception e) {
                return "XML parsing failed: " + e.getMessage();
            }
        });

        post("/upload", (req, res) -> {
            req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
            try {
                Part filePart = req.raw().getPart("labfile");
                // VULNERABILITY: Unsafe File Upload / Path Manipulation
                String fileName = filePart.getSubmittedFileName();
                Path targetPath = Paths.get(UPLOAD_DIR).resolve(fileName);
                Files.copy(filePart.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                return "File uploaded to: " + targetPath.toString();
            } catch (Exception e) {
                return "File upload failed: " + e.getMessage();
            }
        });

        get("/redirect", (req, res) -> {
            String url = req.queryParams("url");
            // VULNERABILITY: Open Redirect
            res.redirect(url);
            return null;
        });
    }

    private static String getNotes() {
        StringBuilder notesHtml = new StringBuilder();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT content FROM notes")) {
            while (rs.next()) {
                notesHtml.append("<div>").append(rs.getString("content")).append("</div>");
            }
        } catch (SQLException e) {
            return "Error fetching notes.";
        }
        return notesHtml.toString();
    }

    private static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS compounds (id INT PRIMARY KEY, name VARCHAR(255), formula VARCHAR(255))");
            stmt.execute("CREATE TABLE IF NOT EXISTS notes (id INT AUTO_INCREMENT PRIMARY KEY, content VARCHAR(1000))");
            stmt.execute("MERGE INTO compounds KEY(id) VALUES (1, 'Water', 'H2O'), (2, 'Benzene', 'C6H6')");
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void legacyEncrypt() throws NoSuchAlgorithmException, NoSuchPaddingException {
        // VULNERABILITY: Weak Encryption
        Cipher desCipher = Cipher.getInstance("DES");
    }
}