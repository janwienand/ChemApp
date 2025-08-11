package org.example;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.NoSuchAlgorithmException;
import static spark.Spark.*;

public class ChemCompoundDB {

    private static final String DB_URL = "jdbc:h2:mem:chemdb;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "very_secret_and_complex_password";
    private static final String API_SECRET_KEY = "key123_a_very_bad_secret_dont_use";
    private static final String SALT_FOR_HASHING = "static_salt_for_every_user";


    public static void main(String[] args) {
        initializeDatabase();
        port(8080);

        get("/", (req, res) -> {
            StringBuilder html = new StringBuilder();
            html.append("<h1>ChemCompound DB</h1>");
            html.append("<h3>Search for a Compound</h3>");
            html.append("<form action='/search' method='get'>");
            html.append("Compound Name: <input type='text' name='name'>");
            html.append("<input type='submit' value='Search'>");
            html.append("</form>");
            html.append("<hr>");
            html.append("<h3>Add a Research Note</h3>");
            html.append("<form action='/addnote' method='post'>");
            html.append("<textarea name='note' rows='4' cols='50'></textarea><br>");
            html.append("<input type='submit' value='Save Note'>");
            html.append("</form>");
            html.append("<hr>");
            html.append("<h3>View Logs</h3>");
            html.append("<form action='/logs' method='get'>");
            html.append("Log file: <input type='text' name='file' value='system.log'>");
            html.append("<input type='submit' value='View Log'>");
            html.append("</form>");
            html.append("<hr>");
            html.append("<h3>Run Diagnostics</h3>");
            html.append("<form action='/diag' method='get'>");
            html.append("Host to ping: <input type='text' name='host' value='localhost'>");
            html.append("<input type='submit' value='Run Ping'>");
            html.append("</form>");
            html.append("<hr>");
            html.append("<h2>Research Notes</h2>");

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT content FROM notes")) {

                while (rs.next()) {
                    html.append("<div>").append(rs.getString("content")).append("</div>");
                }
            } catch (SQLException e) {
                return "Error fetching notes: " + e.getMessage();
            }

            return html.toString();
        });

        get("/search", (req, res) -> {
            String compoundName = req.queryParams("name");
            if (compoundName == null || compoundName.isEmpty()) {
                return "Please provide a compound name.";
            }

            StringBuilder result = new StringBuilder("<h2>Search Results for: " + compoundName + "</h2>");

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                String sql = "SELECT * FROM compounds WHERE name = '" + compoundName + "'";

                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                    if (!rs.isBeforeFirst()) {
                        result.append("<p>No compound found.</p>");
                    } else {
                        while (rs.next()) {
                            result.append("<p><b>ID:</b> ").append(rs.getInt("id"));
                            result.append(", <b>Name:</b> ").append(rs.getString("name"));
                            result.append(", <b>Formula:</b> ").append(rs.getString("formula")).append("</p>");
                        }
                    }
                }
            } catch (SQLException e) {
                return "Error executing query: " + e.getMessage();
            }
            return result.toString();
        });

        post("/addnote", (req, res) -> {
            String note = req.queryParams("note");

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 Statement stmt = conn.createStatement()) {
                String sql = "INSERT INTO notes (content) VALUES ('" + note + "')";
                stmt.executeUpdate(sql);
            } catch (SQLException e) {
                return "Error saving note: " + e.getMessage();
            }

            res.redirect("/");
            return "";
        });

        get("/logs", (req, res) -> {
            String logFile = req.queryParams("file");
            String logDirectory = "logs/";

            // VULNERABILITY: Path Manipulation
            Path path = Paths.get(logDirectory + logFile);

            try {
                String content = new String(Files.readAllBytes(path));
                res.type("text/plain");
                return content;
            } catch (IOException e) {
                return "Error reading log file: " + e.getMessage();
            }
        });

        get("/diag", (req, res) -> {
            String host = req.queryParams("host");
            String command;

            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                command = "ping -n 4 " + host;
            } else {
                command = "ping -c 4 " + host;
            }

            StringBuilder output = new StringBuilder();
            output.append("<h2>Diagnostic Output for: ").append(host).append("</h2>");
            output.append("<pre>");

            try {
                Process process = Runtime.getRuntime().exec(command);

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                process.waitFor();
                reader.close();

            } catch (IOException | InterruptedException e) {
                output.append("Error executing command: ").append(e.getMessage());
            }
            output.append("</pre>");
            return output.toString();
        });
    }

    private static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE compounds (id INT PRIMARY KEY, name VARCHAR(255), formula VARCHAR(255))");
            stmt.execute("CREATE TABLE notes (id INT AUTO_INCREMENT PRIMARY KEY, content VARCHAR(1000))");

            stmt.execute("INSERT INTO compounds VALUES (1, 'Water', 'H2O')");
            stmt.execute("INSERT INTO compounds VALUES (2, 'Benzene', 'C6H6')");
            stmt.execute("INSERT INTO compounds VALUES (3, 'Ethanol', 'C2H5OH')");
            stmt.execute("INSERT INTO notes VALUES (1, 'Initial note: Remember to check the stability of Benzene rings.')");

        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        try {
            File logDir = new File("logs");
            if (!logDir.exists()) {
                logDir.mkdir();
            }
            try (PrintWriter writer = new PrintWriter("logs/system.log", "UTF-8")) {
                writer.println("INFO: Application initialized successfully.");
                writer.println("WARN: Demo mode is active. This application is not secure.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void legacyEncrypt() throws NoSuchAlgorithmException, NoSuchPaddingException {
        Cipher desCipher = Cipher.getInstance("DES");
    }
}