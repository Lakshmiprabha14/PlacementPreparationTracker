
package com.placementtracker;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;
import org.bson.types.ObjectId;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class TrackerGUI extends JFrame {

    private MongoClient client;
    private MongoCollection<Document> collection;

    private JTextField topicField;
    private JComboBox<String> categoryBox;
    private JTable topicTable;
    private DefaultTableModel tableModel;

    private JLabel totalLabel;
    private JLabel progressLabel;
    private JLabel completedLabel;
    private JLabel messageLabel;

    // Colour theme
    private final Color PRIMARY = new Color(101, 85, 217);
    private final Color DARK = new Color(37, 32, 74);
    private final Color BACKGROUND = new Color(245, 244, 251);
    private final Color SUCCESS = new Color(22, 163, 100);
    private final Color INFO = new Color(14, 165, 233);
    private final Color DANGER = new Color(220, 65, 80);
    private final Color WHITE = Color.WHITE;
    private final Color MUTED = new Color(110, 106, 130);

    public TrackerGUI() {
        String uri = System.getenv("MONGODB_URI");

        if (uri == null || uri.isBlank()) {
            throw new IllegalStateException(
                    "Please configure MONGODB_URI in Run Configurations."
            );
        }

        client = MongoClients.create(uri);

        MongoDatabase database =
                client.getDatabase("PlacementTrackerDB");

        collection = database.getCollection("topics");

        setTitle("Placement Preparation Tracker");
        setSize(1000, 680);
        setMinimumSize(new Dimension(800, 550));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        buildUI();
        loadTopics();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                client.close();
            }
        });
    }

    private void buildUI() {
        getContentPane().setBackground(BACKGROUND);
        setLayout(new BorderLayout());

        // ===== HEADER =====
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(DARK);
        header.setBorder(new EmptyBorder(22, 28, 22, 28));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 5));
        titlePanel.setOpaque(false);

        JLabel heading = new JLabel("Placement Preparation Tracker");
        heading.setFont(new Font("SansSerif", Font.BOLD, 25));
        heading.setForeground(WHITE);

        JLabel subtitle = new JLabel(
                "Organize your learning. Track your progress."
        );
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(new Color(215, 210, 245));

        titlePanel.add(heading);
        titlePanel.add(subtitle);
        header.add(titlePanel, BorderLayout.WEST);

        JLabel icon = new JLabel("\u2605");
        icon.setFont(new Font("SansSerif", Font.BOLD, 34));
        icon.setForeground(new Color(196, 181, 253));
        header.add(icon, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // ===== MAIN CONTENT =====
        JPanel mainPanel = new JPanel(new BorderLayout(0, 18));
        mainPanel.setBackground(BACKGROUND);
        mainPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        // ===== SUMMARY CARDS =====
        JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        summaryPanel.setOpaque(false);

        totalLabel = new JLabel("0");
        progressLabel = new JLabel("0");
        completedLabel = new JLabel("0");

        summaryPanel.add(createSummaryCard(
                "TOTAL TOPICS", totalLabel,
                new Color(229, 231, 255),
                new Color(79, 70, 229)
        ));

        summaryPanel.add(createSummaryCard(
                "IN PROGRESS", progressLabel,
                new Color(219, 242, 255),
                new Color(2, 132, 199)
        ));

        summaryPanel.add(createSummaryCard(
                "COMPLETED", completedLabel,
                new Color(217, 251, 232),
                new Color(4, 120, 87)
        ));

        // ===== ADD TOPIC FORM =====
        JPanel formCard = new JPanel(new BorderLayout(0, 14));
        formCard.setBackground(WHITE);
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        new Color(231, 228, 241)
                ),
                new EmptyBorder(18, 20, 18, 20)
        ));

        JLabel formHeading = new JLabel("Add a New Topic");
        formHeading.setFont(new Font("SansSerif", Font.BOLD, 18));
        formHeading.setForeground(DARK);

        JPanel fieldsPanel = new JPanel(new GridLayout(2, 2, 15, 8));
        fieldsPanel.setOpaque(false);

        topicField = new JTextField();
        topicField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        topicField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        new Color(215, 211, 231)
                ),
                new EmptyBorder(8, 10, 8, 10)
        ));

        categoryBox = new JComboBox<>(new String[]{
                "Java", "SQL", "Aptitude", "Python",
                "Web Development", "Other"
        });
        categoryBox.setFont(new Font("SansSerif", Font.PLAIN, 14));

        fieldsPanel.add(createFieldLabel("Topic Name"));
        fieldsPanel.add(createFieldLabel("Category"));
        fieldsPanel.add(topicField);
        fieldsPanel.add(categoryBox);

        JButton addButton = createButton("+ Add Topic", PRIMARY);
        JButton refreshButton = createButton("Refresh", INFO);
        JButton updateButton = createButton("Update Status", SUCCESS);
        JButton deleteButton = createButton("Delete Topic", DANGER);

        JPanel buttonsPanel = new JPanel(new FlowLayout(
                FlowLayout.LEFT, 10, 0
        ));
        buttonsPanel.setOpaque(false);

        buttonsPanel.add(addButton);
        buttonsPanel.add(refreshButton);
        buttonsPanel.add(updateButton);
        buttonsPanel.add(deleteButton);

        JPanel formContent = new JPanel(new BorderLayout(0, 14));
        formContent.setOpaque(false);
        formContent.add(formHeading, BorderLayout.NORTH);
        formContent.add(fieldsPanel, BorderLayout.CENTER);
        formContent.add(buttonsPanel, BorderLayout.SOUTH);

        formCard.add(formContent, BorderLayout.CENTER);

        // ===== TOPICS TABLE =====
        String[] columns = {
                "Topic Name", "Category", "Status", "Database ID"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        topicTable = new JTable(tableModel);
        topicTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        topicTable.setRowHeight(34);
        topicTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
        );
        topicTable.setShowVerticalLines(false);
        topicTable.setGridColor(new Color(237, 234, 245));
        topicTable.setSelectionBackground(new Color(229, 225, 255));
        topicTable.setSelectionForeground(DARK);
        topicTable.setFillsViewportHeight(true);

        JTableHeader tableHeader = topicTable.getTableHeader();
        tableHeader.setFont(
                new Font("SansSerif", Font.BOLD, 13)
        );
        tableHeader.setBackground(new Color(236, 233, 248));
        tableHeader.setForeground(DARK);
        tableHeader.setPreferredSize(new Dimension(100, 38));

        // Status column colour
        topicTable.getColumnModel().getColumn(2)
                .setCellRenderer(new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(
                            JTable table, Object value,
                            boolean isSelected, boolean hasFocus,
                            int row, int column) {

                        JLabel label = (JLabel)
                                super.getTableCellRendererComponent(
                                        table, value, isSelected,
                                        hasFocus, row, column
                                );

                        label.setHorizontalAlignment(
                                SwingConstants.CENTER
                        );
                        label.setFont(
                                new Font("SansSerif", Font.BOLD, 12)
                        );

                        if (!isSelected) {
                            String status = value == null
                                    ? "" : value.toString();

                            if (status.equals("Completed")) {
                                label.setForeground(
                                        new Color(4, 120, 87)
                                );
                                label.setBackground(
                                        new Color(217, 251, 232)
                                );
                            } else if (status.equals("In Progress")) {
                                label.setForeground(
                                        new Color(2, 132, 199)
                                );
                                label.setBackground(
                                        new Color(219, 242, 255)
                                );
                            } else {
                                label.setForeground(MUTED);
                                label.setBackground(
                                        new Color(243, 242, 248)
                                );
                            }
                        }

                        return label;
                    }
                });

        // Hide database ID column from user, but retain its model value.
        topicTable.removeColumn(
                topicTable.getColumnModel().getColumn(3)
        );

        JScrollPane scrollPane = new JScrollPane(topicTable);
        scrollPane.getViewport().setBackground(WHITE);
        scrollPane.setBorder(BorderFactory.createLineBorder(
                new Color(231, 228, 241)
        ));

        JPanel tableCard = new JPanel(new BorderLayout(0, 12));
        tableCard.setBackground(WHITE);
        tableCard.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel tableHeading = new JLabel("My Preparation Topics");
        tableHeading.setFont(new Font("SansSerif", Font.BOLD, 18));
        tableHeading.setForeground(DARK);

        tableCard.add(tableHeading, BorderLayout.NORTH);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        messageLabel = new JLabel("Ready");
        messageLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        messageLabel.setForeground(MUTED);
        tableCard.add(messageLabel, BorderLayout.SOUTH);

        JPanel contentPanel = new JPanel(new BorderLayout(0, 18));
        contentPanel.setOpaque(false);
        contentPanel.add(summaryPanel, BorderLayout.NORTH);

        JPanel middlePanel = new JPanel(new BorderLayout(0, 18));
        middlePanel.setOpaque(false);
        middlePanel.add(formCard, BorderLayout.NORTH);
        middlePanel.add(tableCard, BorderLayout.CENTER);

        contentPanel.add(middlePanel, BorderLayout.CENTER);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);

        // ===== BUTTON ACTIONS =====
        addButton.addActionListener(e -> addTopic());
        refreshButton.addActionListener(e -> loadTopics());
        updateButton.addActionListener(e -> updateStatus());
        deleteButton.addActionListener(e -> deleteTopic());
    }

    private JPanel createSummaryCard(
            String title, JLabel numberLabel,
            Color cardColor, Color numberColor) {

        JPanel card = new JPanel(new GridLayout(2, 1, 0, 8));
        card.setBackground(cardColor);
        card.setBorder(new EmptyBorder(15, 18, 15, 18));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        titleLabel.setForeground(numberColor);

        numberLabel.setFont(new Font("SansSerif", Font.BOLD, 27));
        numberLabel.setForeground(numberColor);

        card.add(titleLabel);
        card.add(numberLabel);

        return card;
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(DARK);
        return label;
    }

    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(WHITE);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setBorder(new EmptyBorder(10, 14, 10, 14));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    // ===== ADD TOPIC =====
    private void addTopic() {
        String name = topicField.getText().trim();
        String category = (String) categoryBox.getSelectedItem();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter a topic name.",
                    "Missing Topic",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            Document topic = new Document("topicName", name)
                    .append("category", category)
                    .append("status", "Not Started");

            collection.insertOne(topic);

            topicField.setText("");
            loadTopics();

            messageLabel.setText("Topic added successfully!");

        } catch (Exception ex) {
            showError(ex);
        }
    }

    // ===== VIEW / REFRESH TOPICS =====
    private void loadTopics() {
        try {
            tableModel.setRowCount(0);

            int total = 0;
            int inProgress = 0;
            int completed = 0;

            for (Document doc : collection.find()) {
                String status = doc.getString("status");

                tableModel.addRow(new Object[]{
                        doc.getString("topicName"),
                        doc.getString("category"),
                        status,
                        doc.getObjectId("_id")
                });

                total++;

                if ("In Progress".equals(status)) {
                    inProgress++;
                }

                if ("Completed".equals(status)) {
                    completed++;
                }
            }

            totalLabel.setText(String.valueOf(total));
            progressLabel.setText(String.valueOf(inProgress));
            completedLabel.setText(String.valueOf(completed));

            messageLabel.setText(
                    "Showing " + total + " topic(s)"
            );

        } catch (Exception ex) {
            showError(ex);
        }
    }

    // ===== UPDATE STATUS =====
    private void updateStatus() {
        int row = topicTable.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a topic from the table first."
            );
            return;
        }

        ObjectId id = (ObjectId) tableModel.getValueAt(row, 3);

        String[] statuses = {
                "Not Started", "In Progress", "Completed"
        };

        String currentStatus =
                (String) tableModel.getValueAt(row, 2);

        String newStatus = (String) JOptionPane.showInputDialog(
                this,
                "Choose the new preparation status:",
                "Update Status",
                JOptionPane.QUESTION_MESSAGE,
                null,
                statuses,
                currentStatus
        );

        if (newStatus == null) {
            return;
        }

        try {
            collection.updateOne(
                    new Document("_id", id),
                    new Document("$set",
                            new Document("status", newStatus))
            );

            loadTopics();
            messageLabel.setText("Status updated successfully!");

        } catch (Exception ex) {
            showError(ex);
        }
    }

    // ===== DELETE TOPIC =====
    private void deleteTopic() {
        int row = topicTable.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a topic to delete."
            );
            return;
        }

        ObjectId id = (ObjectId) tableModel.getValueAt(row, 3);
        String name = (String) tableModel.getValueAt(row, 0);

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete \"" + name + "\"?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            collection.deleteOne(new Document("_id", id));
            loadTopics();
            messageLabel.setText("Topic deleted successfully!");

        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(
                this,
                "Something went wrong:\n" + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                TrackerGUI app = new TrackerGUI();
                app.setVisible(true);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        null,
                        "Could not start the application.\n"
                                + ex.getMessage(),
                        "Startup Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }
}
