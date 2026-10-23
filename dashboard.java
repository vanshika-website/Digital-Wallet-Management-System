import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class Dashboard extends JFrame {

    String username;
    JLabel balanceLabel;
    JTable table;

    public Dashboard(String username) {
        this.username = username;

        setTitle("Digital Wallet - Dashboard");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JPanel header = new JPanel(new BorderLayout());

        JLabel title = new JLabel("DIGITAL WALLET");
        title.setFont(new Font("Arial", Font.BOLD, 28));

        JLabel userLabel = new JLabel("Welcome, " + username);
        userLabel.setFont(new Font("Arial", Font.PLAIN, 16));

        header.add(title, BorderLayout.WEST);
        header.add(userLabel, BorderLayout.EAST);

        // Balance
        JPanel balancePanel = new JPanel();
        balancePanel.setBorder(BorderFactory.createTitledBorder("Wallet Balance"));

        balanceLabel = new JLabel("₹ 0.00");
        balanceLabel.setFont(new Font("Arial", Font.BOLD, 30));

        balancePanel.add(balanceLabel);

        // Buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 10, 10));

        JButton addMoney = new JButton("Add Money");
        JButton sendMoney = new JButton("Send Money");
        JButton transactions = new JButton("Transactions");
        JButton logout = new JButton("Logout");

        buttonPanel.add(addMoney);
        buttonPanel.add(sendMoney);
        buttonPanel.add(transactions);
        buttonPanel.add(logout);

        // Table
        table = new JTable();
        JScrollPane scrollPane = new JScrollPane(table);

        mainPanel.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.add(balancePanel, BorderLayout.NORTH);
        center.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(center, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        loadBalance();
        loadTransactions();

        addMoney.addActionListener(e -> addMoney());

        sendMoney.addActionListener(e -> sendMoney());

        transactions.addActionListener(e -> loadTransactions());

        logout.addActionListener(e -> {
            dispose();
            new WalletApp();
        });
    }

    // LOAD BALANCE
    private void loadBalance() {

        try {
            Connection con = DBConnection.getConnection();

            String sql = "SELECT balance FROM users WHERE username=?";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, username);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                double balance = rs.getDouble("balance");
                balanceLabel.setText("₹ " + String.format("%.2f", balance));
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Unable to load balance\n" + e.getMessage());
        }
    }

    // ADD MONEY
    private void addMoney() {

        String input = JOptionPane.showInputDialog(
                this,
                "Enter amount to add:"
        );

        if (input == null) return;

        try {

            double amount = Double.parseDouble(input);

            if (amount <= 0) {
                JOptionPane.showMessageDialog(this,
                        "Enter a valid amount.");
                return;
            }

            Connection con = DBConnection.getConnection();

            String sql =
                    "UPDATE users SET balance = balance + ? WHERE username=?";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setDouble(1, amount);
            ps.setString(2, username);

            ps.executeUpdate();

            JOptionPane.showMessageDialog(this,
                    "₹" + amount + " added successfully!");

            loadBalance();

        } catch (Exception e) {

            JOptionPane.showMessageDialog(this,
                    "Error: " + e.getMessage());
        }
    }

    // SEND MONEY
    private void sendMoney() {

        JTextField receiverField = new JTextField();
        JTextField amountField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));

        panel.add(new JLabel("Receiver Username:"));
        panel.add(receiverField);

        panel.add(new JLabel("Amount:"));
        panel.add(amountField);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "Send Money",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (result != JOptionPane.OK_OPTION)
            return;

        String receiver = receiverField.getText();
        String amountText = amountField.getText();

        try {

            double amount = Double.parseDouble(amountText);

            if (amount <= 0) {
                JOptionPane.showMessageDialog(this,
                        "Enter a valid amount.");
                return;
            }

            Connection con = DBConnection.getConnection();

            con.setAutoCommit(false);

            // Check sender balance
            String balanceSQL =
                    "SELECT id, balance FROM users WHERE username=?";

            PreparedStatement ps1 =
                    con.prepareStatement(balanceSQL);

            ps1.setString(1, username);

            ResultSet rs1 = ps1.executeQuery();

            if (!rs1.next()) {
                JOptionPane.showMessageDialog(this,
                        "Sender not found.");
                con.rollback();
                return;
            }

            int senderId = rs1.getInt("id");
            double balance = rs1.getDouble("balance");

            if (balance < amount) {
                JOptionPane.showMessageDialog(this,
                        "Insufficient balance!");
                con.rollback();
                return;
            }

            // Find receiver
            String receiverSQL =
                    "SELECT id FROM users WHERE username=?";

            PreparedStatement ps2 =
                    con.prepareStatement(receiverSQL);

            ps2.setString(1, receiver);

            ResultSet rs2 = ps2.executeQuery();

            if (!rs2.next()) {
                JOptionPane.showMessageDialog(this,
                        "Receiver does not exist.");
                con.rollback();
                return;
            }

            int receiverId = rs2.getInt("id");

            if (senderId == receiverId) {
                JOptionPane.showMessageDialog(this,
                        "You cannot send money to yourself.");
                con.rollback();
                return;
            }

            // Deduct sender balance
            String deductSQL =
                    "UPDATE users SET balance = balance - ? WHERE id=?";

            PreparedStatement ps3 =
                    con.prepareStatement(deductSQL);

            ps3.setDouble(1, amount);
            ps3.setInt(2, senderId);

            ps3.executeUpdate();

            // Add receiver balance
            String addSQL =
                    "UPDATE users SET balance = balance + ? WHERE id=?";

            PreparedStatement ps4 =
                    con.prepareStatement(addSQL);

            ps4.setDouble(1, amount);
            ps4.setInt(2, receiverId);

            ps4.executeUpdate();

            // Transaction record
            String transactionSQL =
                    "INSERT INTO transactions " +
                    "(sender_id, receiver_id, amount) " +
                    "VALUES (?, ?, ?)";

            PreparedStatement ps5 =
                    con.prepareStatement(transactionSQL);

            ps5.setInt(1, senderId);
            ps5.setInt(2, receiverId);
            ps5.setDouble(3, amount);

            ps5.executeUpdate();

            con.commit();

            JOptionPane.showMessageDialog(this,
                    "₹" + amount +
                    " sent successfully to " + receiver + "!");

            loadBalance();
            loadTransactions();

        } catch (Exception e) {

            JOptionPane.showMessageDialog(this,
                    "Transaction failed:\n" + e.getMessage());
        }
    }

    // TRANSACTIONS
    private void loadTransactions() {

        try {

            Connection con = DBConnection.getConnection();

            String sql =
                    "SELECT " +
                    "t.id, " +
                    "u1.username AS sender, " +
                    "u2.username AS receiver, " +
                    "t.amount, " +
                    "t.transaction_date " +
                    "FROM transactions t " +
                    "JOIN users u1 ON t.sender_id = u1.id " +
                    "JOIN users u2 ON t.receiver_id = u2.id " +
                    "WHERE u1.username=? OR u2.username=? " +
                    "ORDER BY t.transaction_date DESC";

            PreparedStatement ps =
                    con.prepareStatement(sql);

            ps.setString(1, username);
            ps.setString(2, username);

            ResultSet rs = ps.executeQuery();

            DefaultTableModel model =
                    new DefaultTableModel();

            model.addColumn("ID");
            model.addColumn("Sender");
            model.addColumn("Receiver");
            model.addColumn("Amount");
            model.addColumn("Date");

            while (rs.next()) {

                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("sender"),
                        rs.getString("receiver"),
                        "₹ " + rs.getDouble("amount"),
                        rs.getTimestamp("transaction_date")
                });
            }

            table.setModel(model);

        } catch (Exception e) {

            JOptionPane.showMessageDialog(this,
                    "Unable to load transactions\n" +
                    e.getMessage());
        }
    }
}