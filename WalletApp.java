import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class WalletApp {

    static JFrame frame;
    static String loggedInUsername;
    static int loggedInUserId;
    static JLabel balanceLabel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> showLogin());
    }

    // ================= LOGIN =================

    static void showLogin() {
        frame = new JFrame("Digital Wallet - Login");
        frame.setSize(450, 350);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(235, 242, 255));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("DIGITAL WALLET", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 28));

        JLabel userLabel = new JLabel("Username:");
        JTextField username = new JTextField(18);

        JLabel passLabel = new JLabel("Password:");
        JPasswordField password = new JPasswordField(18);

        JButton login = new JButton("LOGIN");
        JButton register = new JButton("CREATE ACCOUNT");

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        panel.add(userLabel, gbc);

        gbc.gridx = 1;
        panel.add(username, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(passLabel, gbc);

        gbc.gridx = 1;
        panel.add(password, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(login, gbc);

        gbc.gridx = 1;
        panel.add(register, gbc);

        login.addActionListener(e ->
                loginUser(username.getText(), new String(password.getPassword()))
        );

        register.addActionListener(e -> showRegister());

        frame.add(panel);
        frame.setVisible(true);
    }

    // ================= REGISTER =================

    static void showRegister() {
        JFrame registerFrame = new JFrame("Digital Wallet - Create Account");
        registerFrame.setSize(450, 400);
        registerFrame.setLocationRelativeTo(frame);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(240, 255, 240));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("CREATE ACCOUNT", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));

        JTextField name = new JTextField(18);
        JTextField username = new JTextField(18);
        JPasswordField password = new JPasswordField(18);

        JButton create = new JButton("REGISTER");
        JButton back = new JButton("BACK");

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridwidth = 1;

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        panel.add(name, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        panel.add(username, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        panel.add(password, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(create, gbc);
        gbc.gridx = 1;
        panel.add(back, gbc);

        create.addActionListener(e -> {
            String n = name.getText().trim();
            String u = username.getText().trim();
            String p = new String(password.getPassword());

            if (n.isEmpty() || u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(registerFrame,
                        "Please fill all fields.");
                return;
            }

            try (Connection con = DBConnection.getConnection()) {

                String sql = "INSERT INTO users(name, username, password, balance) VALUES (?, ?, ?, 0)";

                PreparedStatement ps = con.prepareStatement(sql);
                ps.setString(1, n);
                ps.setString(2, u);
                ps.setString(3, p);

                ps.executeUpdate();

                JOptionPane.showMessageDialog(registerFrame,
                        "Account created successfully!");

                registerFrame.dispose();

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(registerFrame,
                        "Registration failed:\n" + ex.getMessage());
            }
        });

        back.addActionListener(e -> registerFrame.dispose());

        registerFrame.add(panel);
        registerFrame.setVisible(true);
    }

    // ================= LOGIN DATABASE =================

    static void loginUser(String username, String password) {

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "Please enter username and password.");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {

            String sql = "SELECT id, name, balance FROM users WHERE username=? AND password=?";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                loggedInUserId = rs.getInt("id");
                loggedInUsername = username;

                frame.dispose();
                showDashboard();

            } else {
                JOptionPane.showMessageDialog(frame,
                        "Invalid username or password!");
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Database error:\n" + ex.getMessage());
        }
    }

    // ================= DASHBOARD =================

    static void showDashboard() {

        frame = new JFrame("Digital Wallet - Dashboard");
        frame.setSize(650, 500);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel main = new JPanel(new BorderLayout(15, 15));
        main.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("DIGITAL WALLET", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 28));

        JLabel welcome = new JLabel("Welcome, " + loggedInUsername,
                SwingConstants.CENTER);
        welcome.setFont(new Font("Arial", Font.PLAIN, 18));

        JPanel top = new JPanel(new GridLayout(2, 1));
        top.add(title);
        top.add(welcome);

        balanceLabel = new JLabel();
        balanceLabel.setFont(new Font("Arial", Font.BOLD, 22));
        balanceLabel.setHorizontalAlignment(SwingConstants.CENTER);

        updateBalance();

        JPanel buttons = new JPanel(new GridLayout(2, 3, 15, 15));

        JButton addMoney = new JButton("ADD MONEY");
        JButton sendMoney = new JButton("SEND MONEY");
        JButton history = new JButton("TRANSACTION HISTORY");
        JButton refresh = new JButton("REFRESH BALANCE");
        JButton logout = new JButton("LOGOUT");

        buttons.add(addMoney);
        buttons.add(sendMoney);
        buttons.add(history);
        buttons.add(refresh);
        buttons.add(logout);

        addMoney.addActionListener(e -> addMoney());
        sendMoney.addActionListener(e -> sendMoney());
        history.addActionListener(e -> showHistory());
        refresh.addActionListener(e -> updateBalance());

        logout.addActionListener(e -> {
            frame.dispose();
            showLogin();
        });

        main.add(top, BorderLayout.NORTH);
        main.add(balanceLabel, BorderLayout.CENTER);
        main.add(buttons, BorderLayout.SOUTH);

        frame.add(main);
        frame.setVisible(true);
    }

    // ================= BALANCE =================

    static void updateBalance() {

        try (Connection con = DBConnection.getConnection()) {

            String sql = "SELECT balance FROM users WHERE id=?";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, loggedInUserId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                double balance = rs.getDouble("balance");

                balanceLabel.setText(
                        "Current Balance: ₹" +
                        String.format("%.2f", balance)
                );
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame,
                    "Unable to load balance.");
        }
    }

    // ================= ADD MONEY =================

    static void addMoney() {

        String input = JOptionPane.showInputDialog(
                frame,
                "Enter amount to add:"
        );

        if (input == null) return;

        try {

            double amount = Double.parseDouble(input);

            if (amount <= 0) {
                JOptionPane.showMessageDialog(frame,
                        "Enter a valid amount.");
                return;
            }

            try (Connection con = DBConnection.getConnection()) {

                String sql =
                        "UPDATE users SET balance = balance + ? WHERE id=?";

                PreparedStatement ps = con.prepareStatement(sql);
                ps.setDouble(1, amount);
                ps.setInt(2, loggedInUserId);

                ps.executeUpdate();
            }

            JOptionPane.showMessageDialog(frame,
                    "₹" + amount + " added successfully!");

            updateBalance();

        } catch (NumberFormatException ex) {

            JOptionPane.showMessageDialog(frame,
                    "Please enter a valid number.");

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(frame,
                    "Transaction failed:\n" + ex.getMessage());
        }
    }

    // ================= SEND MONEY =================

    static void sendMoney() {

        JTextField receiver = new JTextField();
        JTextField amount = new JTextField();

        Object[] fields = {
                "Receiver Username:", receiver,
                "Amount:", amount
        };

        int option = JOptionPane.showConfirmDialog(
                frame,
                fields,
                "Send Money",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (option != JOptionPane.OK_OPTION) return;

        try {

            double money = Double.parseDouble(amount.getText());

            if (money <= 0) {
                JOptionPane.showMessageDialog(frame,
                        "Enter a valid amount.");
                return;
            }

            try (Connection con = DBConnection.getConnection()) {

                con.setAutoCommit(false);

                // Find receiver
                String find =
                        "SELECT id FROM users WHERE username=?";

                PreparedStatement findPs =
                        con.prepareStatement(find);

                findPs.setString(1, receiver.getText());

                ResultSet rs = findPs.executeQuery();

                if (!rs.next()) {
                    JOptionPane.showMessageDialog(frame,
                            "Receiver not found!");
                    con.rollback();
                    return;
                }

                int receiverId = rs.getInt("id");

                if (receiverId == loggedInUserId) {
                    JOptionPane.showMessageDialog(frame,
                            "You cannot send money to yourself.");
                    con.rollback();
                    return;
                }

                // Check balance
                String balanceQuery =
                        "SELECT balance FROM users WHERE id=?";

                PreparedStatement balancePs =
                        con.prepareStatement(balanceQuery);

                balancePs.setInt(1, loggedInUserId);

                ResultSet br = balancePs.executeQuery();
                br.next();

                double currentBalance = br.getDouble("balance");

                if (currentBalance < money) {
                    JOptionPane.showMessageDialog(frame,
                            "Insufficient balance!");
                    con.rollback();
                    return;
                }

                // Deduct sender balance
                String deduct =
                        "UPDATE users SET balance=balance-? WHERE id=?";

                PreparedStatement deductPs =
                        con.prepareStatement(deduct);

                deductPs.setDouble(1, money);
                deductPs.setInt(2, loggedInUserId);
                deductPs.executeUpdate();

                // Add receiver balance
                String add =
                        "UPDATE users SET balance=balance+? WHERE id=?";

                PreparedStatement addPs =
                        con.prepareStatement(add);

                addPs.setDouble(1, money);
                addPs.setInt(2, receiverId);
                addPs.executeUpdate();

                // Record transaction
                String transaction =
                        "INSERT INTO transactions(sender_id, receiver_id, amount) VALUES (?, ?, ?)";

                PreparedStatement transactionPs =
                        con.prepareStatement(transaction);

                transactionPs.setInt(1, loggedInUserId);
                transactionPs.setInt(2, receiverId);
                transactionPs.setDouble(3, money);

                transactionPs.executeUpdate();

                con.commit();
            }

            JOptionPane.showMessageDialog(frame,
                    "Money sent successfully!");

            updateBalance();

        } catch (NumberFormatException ex) {

            JOptionPane.showMessageDialog(frame,
                    "Invalid amount!");

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(frame,
                    "Transfer failed:\n" + ex.getMessage());
        }
    }

    // ================= TRANSACTION HISTORY =================

    static void showHistory() {

        JFrame historyFrame =
                new JFrame("Transaction History");

        historyFrame.setSize(650, 400);
        historyFrame.setLocationRelativeTo(frame);

        String[] columns = {
                "ID",
                "Sender",
                "Receiver",
                "Amount",
                "Date"
        };

        DefaultTableModel model =
                new DefaultTableModel(columns, 0);

        JTable table = new JTable(model);

        try (Connection con = DBConnection.getConnection()) {

            String sql =
                    "SELECT t.id, " +
                    "s.username AS sender, " +
                    "r.username AS receiver, " +
                    "t.amount, t.transaction_date " +
                    "FROM transactions t " +
                    "JOIN users s ON t.sender_id=s.id " +
                    "JOIN users r ON t.receiver_id=r.id " +
                    "WHERE t.sender_id=? OR t.receiver_id=? " +
                    "ORDER BY t.transaction_date DESC";

            PreparedStatement ps =
                    con.prepareStatement(sql);

            ps.setInt(1, loggedInUserId);
            ps.setInt(2, loggedInUserId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("sender"),
                        rs.getString("receiver"),
                        rs.getDouble("amount"),
                        rs.getTimestamp("transaction_date")
                });
            }

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(frame,
                    "Unable to load history:\n" + ex.getMessage());
        }

        historyFrame.add(new JScrollPane(table));
        historyFrame.setVisible(true);
    }
}