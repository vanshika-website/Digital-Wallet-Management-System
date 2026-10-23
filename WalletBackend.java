import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class WalletBackend {

    static final String URL =
        "jdbc:mysql://localhost:3306/digital_wallet";
    static final String USER = "root";
    static final String PASS = "MYSQL@1234";

    static Connection db() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static void main(String[] args) throws Exception {

        Class.forName("com.mysql.cj.jdbc.Driver");

        HttpServer s = HttpServer.create(
            new InetSocketAddress(8080), 0);

        s.createContext("/api/login", WalletBackend::login);
        s.createContext("/api/balance", WalletBackend::balance);
        s.createContext("/api/send", WalletBackend::sendMoney);

        s.start();

        System.out.println(
            "Digital Wallet Backend running on port 8080");
    }

    // LOGIN
    static void login(HttpExchange e) throws IOException {

        try {
            String b = body(e);
            String u = value(b, "username");
            String p = value(b, "password");

            Connection c = db();

            PreparedStatement ps = c.prepareStatement(
                "SELECT id,name,balance FROM users " +
                "WHERE username=? AND password=?");

            ps.setString(1, u);
            ps.setString(2, p);

            ResultSet r = ps.executeQuery();

            if (r.next())
                reply(e, 200,
                    "{\"success\":true,\"id\":" +
                    r.getInt("id") +
                    ",\"balance\":" +
                    r.getDouble("balance") + "}");
            else
                reply(e, 401,
                    "{\"success\":false}");

            c.close();

        } catch (Exception ex) {
            reply(e, 500,
                "{\"error\":\"Database error\"}");
        }
    }

    // BALANCE
    static void balance(HttpExchange e) throws IOException {

        try {
            String id = query(e);

            Connection c = db();

            PreparedStatement ps = c.prepareStatement(
                "SELECT balance FROM users WHERE id=?");

            ps.setInt(1, Integer.parseInt(id));

            ResultSet r = ps.executeQuery();

            if (r.next())
                reply(e, 200,
                    "{\"balance\":" +
                    r.getDouble("balance") + "}");

            c.close();

        } catch (Exception ex) {
            reply(e, 500,
                "{\"error\":\"Database error\"}");
        }
    }

    // SEND MONEY
    static void sendMoney(HttpExchange e) throws IOException {

        try {
            String b = body(e);

            int sender =
                Integer.parseInt(value(b, "senderId"));

            int receiver =
                Integer.parseInt(value(b, "receiverId"));

            double amount =
                Double.parseDouble(value(b, "amount"));

            Connection c = db();
            c.setAutoCommit(false);

            PreparedStatement check = c.prepareStatement(
                "SELECT balance FROM users WHERE id=?");

            check.setInt(1, sender);

            ResultSet r = check.executeQuery();

            if (!r.next() ||
                r.getDouble("balance") < amount) {

                c.rollback();
                reply(e, 400,
                    "{\"error\":\"Insufficient balance\"}");
                return;
            }

            PreparedStatement deduct = c.prepareStatement(
                "UPDATE users SET balance=balance-? WHERE id=?");

            deduct.setDouble(1, amount);
            deduct.setInt(2, sender);
            deduct.executeUpdate();

            PreparedStatement add = c.prepareStatement(
                "UPDATE users SET balance=balance+? WHERE id=?");

            add.setDouble(1, amount);
            add.setInt(2, receiver);
            add.executeUpdate();

            PreparedStatement t = c.prepareStatement(
                "INSERT INTO transactions " +
                "(sender_id,receiver_id,amount,transaction_date) " +
                "VALUES(?,?,?,NOW())");

            t.setInt(1, sender);
            t.setInt(2, receiver);
            t.setDouble(3, amount);
            t.executeUpdate();

            c.commit();
            c.close();

            reply(e, 200,
                "{\"message\":\"Money sent successfully\"}");

        } catch (Exception ex) {
            reply(e, 500,
                "{\"error\":\"Transaction failed\"}");
        }
    }

    // HELPERS

    static String body(HttpExchange e) throws IOException {
        return new String(
            e.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8);
    }

    static String value(String json, String key) {
        String k = "\"" + key + "\":\"";
        int a = json.indexOf(k) + k.length();
        int b = json.indexOf("\"", a);
        return json.substring(a, b);
    }

    static String query(HttpExchange e) {
        String q = e.getRequestURI().getQuery();
        return q.substring(q.indexOf("=") + 1);
    }

    static void reply(
        HttpExchange e, int code, String text)
        throws IOException {

        byte[] data =
            text.getBytes(StandardCharsets.UTF_8);

        e.getResponseHeaders().set(
            "Content-Type", "application/json");

        e.sendResponseHeaders(code, data.length);
        e.getResponseBody().write(data);
        e.close();
    }
}