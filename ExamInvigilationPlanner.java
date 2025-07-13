import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

public class ExamInvigilationPlanner extends JFrame {

    // --------------------- Data Classes ---------------------
    static class Exam {
        int id;
        String name;
        String timeSlot;
        Set<Integer> conflicts;

        public Exam(int id, String name, String timeSlot) {
            this.id = id;
            this.name = name;
            this.timeSlot = timeSlot;
            this.conflicts = new HashSet<>();
        }

        public boolean isConflict(Exam other) {
            return this.timeSlot.equals(other.timeSlot);
        }
    }

    static class Teacher {
        int id;
        String name;
        Set<String> unavailableSlots;

        public Teacher(int id, String name, Set<String> unavailableSlots) {
            this.id = id;
            this.name = name;
            this.unavailableSlots = unavailableSlots;
        }

        public boolean isAvailable(String timeSlot) {
            return !unavailableSlots.contains(timeSlot);
        }
    }

    static class ScheduleRecord {
        String timestamp;
        String scheduleDetails;
        Map<Integer, Integer> assignments;

        public ScheduleRecord(String details, Map<Integer, Integer> assignments) {
            this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            this.scheduleDetails = details;
            this.assignments = new HashMap<>(assignments);
        }

        @Override
        public String toString() {
            return timestamp + " - " + scheduleDetails.split("\n")[0];
        }

        public String toCSV() {
            StringBuilder sb = new StringBuilder();
            sb.append("\"").append(timestamp).append("\",\"");
            sb.append(scheduleDetails.replace("\"", "\"\"")).append("\"");

            if (!assignments.isEmpty()) {
                sb.append(",\"");
                boolean first = true;
                for (Map.Entry<Integer, Integer> entry : assignments.entrySet()) {
                    if (!first) sb.append(";");
                    sb.append(entry.getKey()).append(":").append(entry.getValue());
                    first = false;
                }
                sb.append("\"");
            }

            return sb.toString();
        }

        public static ScheduleRecord fromCSV(String csvLine) {
            try {
                // Split while handling quoted fields
                String[] parts = csvLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                if (parts.length < 2) return null;

                // Remove surrounding quotes and clean data
                String timestamp = parts[0].replaceAll("^\"|\"$", "").trim();
                String details = parts[1].replaceAll("^\"|\"$", "").trim();

                Map<Integer, Integer> assignments = new HashMap<>();
                if (parts.length > 2 && !parts[2].isEmpty()) {
                    String assignmentsStr = parts[2].replaceAll("^\"|\"$", "").trim();
                    if (!assignmentsStr.isEmpty()) {
                        String[] pairs = assignmentsStr.split(";");
                        for (String pair : pairs) {
                            String[] kv = pair.split(":");
                            if (kv.length == 2) {
                                try {
                                    assignments.put(Integer.parseInt(kv[0].trim()),
                                            Integer.parseInt(kv[1].trim()));
                                } catch (NumberFormatException e) {
                                    System.err.println("Error parsing assignment: " + pair);
                                }
                            }
                        }
                    }
                }

                ScheduleRecord record = new ScheduleRecord(details, assignments);
                record.timestamp = timestamp; // Preserve original timestamp
                return record;
            } catch (Exception e) {
                System.err.println("Error parsing CSV line: " + csvLine);
                e.printStackTrace();
                return null;
            }
        }
    }

    // --------------------- GUI Components ---------------------
    private final Color BACKGROUND_COLOR = new Color(18, 18, 18);
    private final Color PRIMARY_COLOR = new Color(0, 150, 255);
    private final Color SECONDARY_COLOR = new Color(30, 30, 30);
    private final Color TEXT_COLOR = new Color(220, 220, 220);

    private final JTextField examNameField = createFuturisticTextField();
    private final JTextField examSlotField = createFuturisticTextField();
    private final JButton addExamButton = createEnhancedButton("➕ Add Exam", PRIMARY_COLOR);

    private final JTextField teacherNameField = createFuturisticTextField();
    private final JTextField unavailableField = createFuturisticTextField();
    private final JButton addTeacherButton = createEnhancedButton("👤 Add Teacher", PRIMARY_COLOR);

    private final JButton scheduleButton = createEnhancedButton("🚀 Generate Schedule", new Color(0, 180, 120));
    private final JButton printButton = createEnhancedButton("🖨️ Print Schedule", new Color(180, 100, 0));
    private final JButton clearButton = createEnhancedButton("🧹 Clear All", new Color(150, 0, 150));
    private final JButton saveHistoryButton = createEnhancedButton("💾 Save History", new Color(100, 0, 150));
    private final JButton loadHistoryButton = createEnhancedButton("📂 Load History", new Color(0, 150, 150));
    private final JButton openFileButton = createEnhancedButton("📂 Open File Location", new Color(0, 100, 150));

    private final JTextArea outputArea = createFuturisticTextArea();
    private final JList<ScheduleRecord> historyList = new JList<>();
    private final DefaultListModel<ScheduleRecord> historyModel = new DefaultListModel<>();

    private final List<Exam> examList = new ArrayList<>();
    private final List<Teacher> teacherList = new ArrayList<>();
    private final List<ScheduleRecord> scheduleHistory = new ArrayList<>();
    private int examCounter = 0;
    private int teacherCounter = 0;

    private File currentHistoryFile;

    public ExamInvigilationPlanner() {
        setTitle("🚀 Exam Invigilation Planner");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BACKGROUND_COLOR);

        // Initialize history file in user's home directory
        currentHistoryFile = new File(System.getProperty("user.home"), "exam_schedule_history.csv");

        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Input panel
        JPanel inputPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        inputPanel.setBackground(BACKGROUND_COLOR);

        // Exam Panel
        JPanel examPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        examPanel.setBackground(SECONDARY_COLOR);
        examPanel.add(createFuturisticLabel("Exam Name:"));
        examPanel.add(examNameField);
        examPanel.add(createFuturisticLabel("Time Slot:"));
        examPanel.add(examSlotField);
        examPanel.add(addExamButton);

        // Teacher Panel
        JPanel teacherPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        teacherPanel.setBackground(SECONDARY_COLOR);
        teacherPanel.add(createFuturisticLabel("Teacher Name:"));
        teacherPanel.add(teacherNameField);
        teacherPanel.add(createFuturisticLabel("Unavailable Slots (comma):"));
        teacherPanel.add(unavailableField);
        teacherPanel.add(addTeacherButton);

        inputPanel.add(examPanel);
        inputPanel.add(teacherPanel);

        // Action Buttons Panel
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        actionPanel.setBackground(BACKGROUND_COLOR);
        actionPanel.add(scheduleButton);
        actionPanel.add(printButton);
        actionPanel.add(clearButton);
        actionPanel.add(saveHistoryButton);
        actionPanel.add(loadHistoryButton);
        actionPanel.add(openFileButton);

        // Output Panel
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(createTitledBorder("📋 Schedule Output"));
        outputPanel.setBackground(SECONDARY_COLOR);

        outputArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setBorder(BorderFactory.createEmptyBorder());
        outputScroll.getViewport().setBackground(SECONDARY_COLOR);
        outputPanel.add(outputScroll, BorderLayout.CENTER);

        // History Panel
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBorder(createTitledBorder("⏳ Schedule History"));
        historyPanel.setBackground(SECONDARY_COLOR);

        historyList.setModel(historyModel);
        historyList.setCellRenderer(new HistoryListRenderer());
        historyList.setBackground(SECONDARY_COLOR);
        historyList.setForeground(TEXT_COLOR);
        historyList.setSelectionBackground(PRIMARY_COLOR);
        historyList.setSelectionForeground(Color.WHITE);
        historyList.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JScrollPane historyScroll = new JScrollPane(historyList);
        historyScroll.setBorder(BorderFactory.createEmptyBorder());
        historyPanel.add(historyScroll, BorderLayout.CENTER);

        // Add components to main panel
        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(actionPanel, BorderLayout.CENTER);
        mainPanel.add(outputPanel, BorderLayout.SOUTH);

        // Add history panel to the right
        add(mainPanel, BorderLayout.CENTER);
        add(historyPanel, BorderLayout.EAST);

        // Button Actions
        addExamButton.addActionListener(e -> addExam());
        addTeacherButton.addActionListener(e -> addTeacher());
        scheduleButton.addActionListener(e -> generateSchedule());
        printButton.addActionListener(e -> printSchedule());
        clearButton.addActionListener(e -> clearAll());
        saveHistoryButton.addActionListener(e -> saveHistoryToCSV());
        loadHistoryButton.addActionListener(e -> loadHistoryFromCSV());
        openFileButton.addActionListener(e -> openFileLocation());

        // History list selection listener
        historyList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ScheduleRecord record = historyList.getSelectedValue();
                if (record != null) {
                    outputArea.setText(record.scheduleDetails);
                }
            }
        });

        // Load history on startup
        loadHistoryFromCSV();

        // Set window properties
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // --------------------- Helper Methods ---------------------
    private Border createTitledBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(PRIMARY_COLOR, 2),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14),
                TEXT_COLOR);
    }

    private JLabel createFuturisticLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(TEXT_COLOR);
        return label;
    }

    private JTextField createFuturisticTextField() {
        JTextField field = new JTextField(15);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setForeground(TEXT_COLOR);
        field.setBackground(new Color(50, 50, 50));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 80)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        field.setCaretColor(PRIMARY_COLOR);
        return field;
    }

    private JTextArea createFuturisticTextArea() {
        JTextArea area = new JTextArea();
        area.setFont(new Font("Consolas", Font.PLAIN, 14));
        area.setForeground(new Color(200, 255, 200));
        area.setBackground(new Color(30, 30, 30));
        area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        return area;
    }

    private JButton createEnhancedButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createRaisedBevelBorder(),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(brighter(bgColor, 1.3f));
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLoweredBevelBorder(),
                        BorderFactory.createEmptyBorder(8, 15, 8, 15)));
            }

            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createRaisedBevelBorder(),
                        BorderFactory.createEmptyBorder(8, 15, 8, 15)));
            }
        });

        return button;
    }

    private Color brighter(Color color, float factor) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        r = Math.min((int)(r * factor), 255);
        g = Math.min((int)(g * factor), 255);
        b = Math.min((int)(b * factor), 255);

        return new Color(r, g, b);
    }

    // --------------------- Action Methods ---------------------
    private void addExam() {
        String name = examNameField.getText().trim();
        String slot = examSlotField.getText().trim();
        if (!name.isEmpty() && !slot.isEmpty()) {
            examList.add(new Exam(examCounter++, name, slot));
            outputArea.append("✅ Exam added: " + name + " at " + slot + "\n");
            examNameField.setText("");
            examSlotField.setText("");
            examNameField.requestFocus();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Please enter exam name and time slot.",
                    "Input Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void addTeacher() {
        String name = teacherNameField.getText().trim();
        String slots = unavailableField.getText().trim();
        if (!name.isEmpty()) {
            Set<String> set = new HashSet<>();
            if (!slots.isEmpty()) {
                String[] arr = slots.split(",");
                for (String s : arr) set.add(s.trim());
            }
            teacherList.add(new Teacher(teacherCounter++, name, set));
            outputArea.append("✅ Teacher added: " + name + ", Unavailable: " + set + "\n");
            teacherNameField.setText("");
            unavailableField.setText("");
            teacherNameField.requestFocus();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Please enter teacher name.",
                    "Input Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void generateSchedule() {
        if (examList.isEmpty() || teacherList.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please add at least one exam and one teacher first.",
                    "Scheduling Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        InvigilationScheduler scheduler = new InvigilationScheduler(examList, teacherList);
        boolean success = scheduler.assignTeachers();

        String scheduleText = scheduler.getScheduleString();
        outputArea.setText(scheduleText);

        if (success) {
            ScheduleRecord record = new ScheduleRecord(scheduleText, scheduler.examTeacherMap);
            scheduleHistory.add(record);
            historyModel.addElement(record);
            JOptionPane.showMessageDialog(this,
                    "Schedule generated successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "No valid schedule found with current constraints.",
                    "Scheduling Failed", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void printSchedule() {
        if (outputArea.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No schedule to print. Please generate a schedule first.",
                    "Print Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            boolean complete = outputArea.print();
            if (complete) {
                JOptionPane.showMessageDialog(this,
                        "Printing complete", "Print Status",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Printing cancelled", "Print Status",
                        JOptionPane.WARNING_MESSAGE);
            }
        } catch (PrinterException pe) {
            JOptionPane.showMessageDialog(this,
                    "Printing error: " + pe.getMessage(),
                    "Print Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearAll() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to clear all data?",
                "Confirm Clear", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            examList.clear();
            teacherList.clear();
            scheduleHistory.clear();
            historyModel.clear();
            outputArea.setText("");
            examCounter = 0;
            teacherCounter = 0;

            examNameField.setText("");
            examSlotField.setText("");
            teacherNameField.setText("");
            unavailableField.setText("");
        }
    }

    private void saveHistoryToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(currentHistoryFile);
        fileChooser.setDialogTitle("Save Schedule History");

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentHistoryFile = fileChooser.getSelectedFile();
            try {
                currentHistoryFile.getParentFile().mkdirs();

                try (PrintWriter writer = new PrintWriter(new FileWriter(currentHistoryFile))) {
                    writer.println("\"Timestamp\",\"Schedule Details\",\"Assignments\"");

                    for (ScheduleRecord record : scheduleHistory) {
                        writer.println(record.toCSV());
                    }
                    JOptionPane.showMessageDialog(this,
                            "History saved to:\n" + currentHistoryFile.getAbsolutePath(),
                            "Save Successful", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Error saving history: " + e.getMessage(),
                        "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadHistoryFromCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(currentHistoryFile);
        fileChooser.setDialogTitle("Load Schedule History");

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentHistoryFile = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(currentHistoryFile))) {
                historyModel.clear();
                scheduleHistory.clear();

                String line;
                boolean firstLine = true;
                while ((line = reader.readLine()) != null) {
                    if (firstLine) {
                        firstLine = false;
                        continue;
                    }
                    if (line.trim().isEmpty()) continue;

                    ScheduleRecord record = ScheduleRecord.fromCSV(line);
                    if (record != null) {
                        scheduleHistory.add(record);
                        historyModel.addElement(record);
                    }
                }

                if (!scheduleHistory.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "Loaded " + scheduleHistory.size() + " history records",
                            "Load Successful", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "History file is empty",
                            "Load Status", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Error loading history: " + e.getMessage(),
                        "Load Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openFileLocation() {
        if (currentHistoryFile.exists()) {
            try {
                Desktop.getDesktop().open(currentHistoryFile.getParentFile());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Could not open file location: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "No history file exists yet. Please save first.",
                    "File Not Found", JOptionPane.WARNING_MESSAGE);
        }
    }

    class HistoryListRenderer extends DefaultListCellRenderer {
        private final Color SELECTION_BG = new Color(0, 120, 215);
        private final Color ODD_ROW = new Color(40, 40, 40);
        private final Color EVEN_ROW = new Color(50, 50, 50);

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            setFont(new Font("Segoe UI", Font.PLAIN, 12));

            if (isSelected) {
                setBackground(SELECTION_BG);
                setForeground(Color.WHITE);
            } else {
                setBackground(index % 2 == 0 ? ODD_ROW : EVEN_ROW);
                setForeground(TEXT_COLOR);
            }

            return this;
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new ExamInvigilationPlanner();
        });
    }

    static class InvigilationScheduler {
        List<Exam> exams;
        List<Teacher> teachers;
        Map<Integer, Integer> examTeacherMap;

        public InvigilationScheduler(List<Exam> exams, List<Teacher> teachers) {
            this.exams = exams;
            this.teachers = teachers;
            this.examTeacherMap = new HashMap<>();
            buildConflictGraph();
        }

        private void buildConflictGraph() {
            for (int i = 0; i < exams.size(); i++) {
                for (int j = i + 1; j < exams.size(); j++) {
                    Exam e1 = exams.get(i);
                    Exam e2 = exams.get(j);
                    if (e1.isConflict(e2)) {
                        e1.conflicts.add(e2.id);
                        e2.conflicts.add(e1.id);
                    }
                }
            }
        }

        public boolean assignTeachers() {
            return backtrack(0);
        }

        private boolean backtrack(int examIndex) {
            if (examIndex == exams.size()) return true;

            Exam exam = exams.get(examIndex);
            for (int t = 0; t < teachers.size(); t++) {
                Teacher teacher = teachers.get(t);
                if (teacher.isAvailable(exam.timeSlot) && isSafe(exam, t)) {
                    examTeacherMap.put(exam.id, t);
                    if (backtrack(examIndex + 1)) return true;
                    examTeacherMap.remove(exam.id);
                }
            }
            return false;
        }

        private boolean isSafe(Exam exam, int teacherId) {
            for (int conflictId : exam.conflicts) {
                if (examTeacherMap.getOrDefault(conflictId, -1) == teacherId)
                    return false;
            }
            return true;
        }

        public String getScheduleString() {
            if (examTeacherMap.isEmpty()) return "❌ No valid schedule found.";

            StringBuilder sb = new StringBuilder("✅ Teacher assignment successful:\n\n");
            for (Exam exam : exams) {
                int teacherId = examTeacherMap.get(exam.id);
                Teacher teacher = teachers.get(teacherId);
                sb.append("📘 Exam: ").append(exam.name)
                        .append(" (").append(exam.timeSlot)
                        .append(") → 👨‍🏫 Teacher: ").append(teacher.name).append("\n");
            }
            return sb.toString();
        }
    }
}