package org.example;

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
    private static final String API_SECRET_KEY = "key123_a_very_bad_secret_dont_use_it";
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
    }

    private void legacyEncrypt() throws NoSuchAlgorithmException, NoSuchPaddingException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    }
}