package com.appointment.presentation;

import com.appointment.application.port.TimeProvider;
import com.appointment.application.service.AdminAuthService;
import com.appointment.application.service.AppointmentService;
import com.appointment.domain.model.Appointment;
import com.appointment.domain.model.AppointmentStatus;
import com.appointment.domain.model.AppointmentType;
import com.appointment.domain.model.BookingRequest;
import com.appointment.domain.model.NotificationMessage;
import com.appointment.domain.model.Schedule;
import com.appointment.domain.model.TimeSlot;
import com.appointment.domain.model.User;
import com.appointment.domain.observer.AppointmentEventPublisher;
import com.appointment.domain.observer.NotificationObserver;
import com.appointment.domain.strategy.BookingRuleStrategy;
import com.appointment.domain.strategy.DurationRuleStrategy;
import com.appointment.domain.strategy.ParticipantLimitRuleStrategy;
import com.appointment.domain.strategy.TypeSpecificRuleStrategy;
import com.appointment.infrastructure.notification.FileNotificationService;
import com.appointment.infrastructure.time.SystemTimeProvider;
import com.appointment.persistence.FileAppointmentRepository;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import javax.swing.border.Border;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

public class AppointmentSchedulerUi {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Color PRIMARY = new Color(46, 125, 140);

    private static final Color ACCENT = new Color(245, 171, 53);

    private static final Color BACKGROUND = new Color(245, 247, 250);

    private static final Color SUCCESS = new Color(46, 125, 50);

    private static final Color WARNING = new Color(230, 155, 25);

    private static final Color DANGER = new Color(198, 40, 40);

    private static final Color SECONDARY = new Color(225, 231, 239);

    private final AppointmentService appointmentService;
    private final AdminAuthService adminAuthService;
    private final FileNotificationService notificationService;
    private final JFrame frame;
    private final DefaultTableModel appointmentsTableModel;
    private final JTable appointmentsTable;
    private final DefaultComboBoxModel<SlotOption> availableSlotsModel;
    private final JList<String> availableSlotsList;
    private final JTextField bookingAppointmentIdField;
    private final JTextField bookingUserIdField;
    private final JTextField bookingUserNameField;
    private final JComboBox<AppointmentType> bookingTypeBox;
    private final JSpinner bookingParticipantsSpinner;
    private final JComboBox<SlotOption> bookingSlotBox;
    private final JTextField bookingAccessCodeField;
    private final JTextField appointmentsAccessCodeField;
    private final JTextField userManageAppointmentIdField;
    private final JTextField userManageStartField;
    private final JTextField userManageEndField;
    private final JComboBox<AppointmentType> userManageTypeBox;
    private final JSpinner userManageParticipantsSpinner;
    private final JTextField userManageAccessCodeField;
    private final JTextField adminUserNameField;
    private final JPasswordField adminPasswordField;
    private final JLabel adminStatusLabel;
    private final JTextField adminManageAppointmentIdField;
    private final JTextField adminManageStartField;
    private final JTextField adminManageEndField;
    private final JComboBox<AppointmentType> adminManageTypeBox;
    private final JSpinner adminManageParticipantsSpinner;
    private final JTextArea notificationsArea;
    private final JTextField notificationCodeField;

    public AppointmentSchedulerUi() {
        AppRuntime runtime = createRuntime();
        this.appointmentService = runtime.appointmentService;
        this.adminAuthService = runtime.adminAuthService;
        this.notificationService = runtime.notificationService;

        this.frame = new JFrame("Appointment Scheduling System - GUI");
        this.appointmentsTableModel = new DefaultTableModel(
                new Object[]{"ID", "User", "Type", "Start", "End", "Participants", "Status"},
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.appointmentsTable = new JTable(appointmentsTableModel);

        this.availableSlotsModel = new DefaultComboBoxModel<>();
        this.availableSlotsList = new JList<>();

        this.bookingAppointmentIdField = new JTextField();
        this.bookingUserIdField = new JTextField();
        this.bookingUserNameField = new JTextField();
        this.bookingTypeBox = new JComboBox<>(AppointmentType.values());
        this.bookingParticipantsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
        this.bookingSlotBox = new JComboBox<>(availableSlotsModel);
        this.bookingAccessCodeField = new JTextField();
        this.bookingAccessCodeField.setEditable(false);
        this.appointmentsAccessCodeField = new JTextField();

        this.userManageAppointmentIdField = new JTextField();
        this.userManageStartField = new JTextField();
        this.userManageEndField = new JTextField();
        this.userManageTypeBox = new JComboBox<>(AppointmentType.values());
        this.userManageParticipantsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
        this.userManageAccessCodeField = new JTextField();

        this.adminUserNameField = new JTextField();
        this.adminPasswordField = new JPasswordField();
        this.adminStatusLabel = new JLabel("Admin session: NOT LOGGED IN");

        this.adminManageAppointmentIdField = new JTextField();
        this.adminManageStartField = new JTextField();
        this.adminManageEndField = new JTextField();
        this.adminManageTypeBox = new JComboBox<>(AppointmentType.values());
        this.adminManageParticipantsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));

        this.notificationsArea = new JTextArea();
        this.notificationCodeField = new JTextField();

        initializeFrame();
        refreshAllViews();
    }

    public void show() {
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
            }
            new AppointmentSchedulerUi().show();
        });
    }

    private static AppRuntime createRuntime() {
        LocalDateTime now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        List<TimeSlot> slots = Arrays.asList(
                new TimeSlot(now.plusHours(1), now.plusHours(2)),
                new TimeSlot(now.plusHours(2), now.plusHours(3)),
                new TimeSlot(now.plusHours(3), now.plusHours(4)),
                new TimeSlot(now.plusHours(4), now.plusHours(5)),
                new TimeSlot(now.plusHours(24), now.plusHours(25)),
                new TimeSlot(now.plusHours(25), now.plusHours(26)));

        FileAppointmentRepository repository = new FileAppointmentRepository();
        FileNotificationService notificationService = new FileNotificationService();
        AdminAuthService authService = new AdminAuthService();
        TimeProvider timeProvider = new SystemTimeProvider();

        AppointmentEventPublisher publisher = new AppointmentEventPublisher();
        publisher.register(new NotificationObserver(notificationService, "EMAIL"));
        publisher.register(new NotificationObserver(notificationService, "SMS"));

        List<BookingRuleStrategy> rules = new ArrayList<>();
        rules.add(new DurationRuleStrategy(Duration.ofHours(2)));
        rules.add(new ParticipantLimitRuleStrategy(8));
        rules.add(new TypeSpecificRuleStrategy());

        AppointmentService service = new AppointmentService(
                repository,
                new Schedule(slots),
                rules,
                publisher,
                timeProvider,
                authService);

        return new AppRuntime(service, authService, notificationService);
    }

    private void initializeFrame() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1180, 760));
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().setBackground(BACKGROUND);

        JPanel header = buildHeaderPanel();
        JSplitPane content = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildLeftPanel(), buildRightPanel());
        content.setDividerLocation(640);
        content.setResizeWeight(0.52);

        frame.getContentPane().add(header, BorderLayout.NORTH);
        frame.getContentPane().add(content, BorderLayout.CENTER);
    }

    private JPanel buildHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PRIMARY);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JLabel title = new JLabel("Appointment Scheduling System");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("");
        subtitle.setForeground(new Color(220, 240, 244));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(title);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(subtitle);

        JButton refreshButton = createPrimaryButton("Refresh Data");
        applyActionStyle(refreshButton, WARNING, new Color(40, 40, 40));
        refreshButton.addActionListener(e -> refreshAllViews());

        panel.add(textPanel, BorderLayout.WEST);
        panel.add(refreshButton, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildLeftPanel() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 8));

        JPanel visibilityPanel = new JPanel(new BorderLayout(8, 8));
        visibilityPanel.setOpaque(false);
        visibilityPanel.add(createLabeledField("My Access Code", appointmentsAccessCodeField), BorderLayout.CENTER);

        JPanel visibilityActions = createActionPanel();
        JButton showMineButton = createPrimaryButton("Show My Appointment");
        showMineButton.addActionListener(e -> handleShowMyAppointments());
        JButton clearButton = createSecondaryButton("Clear Filter");
        clearButton.addActionListener(e -> {
            appointmentsAccessCodeField.setText("");
            refreshAppointmentsTable();
        });
        visibilityActions.add(showMineButton);
        visibilityActions.add(clearButton);
        visibilityPanel.add(visibilityActions, BorderLayout.EAST);

        JPanel appointmentsPanel = createCardPanel("All Appointments");
        appointmentsPanel.add(visibilityPanel, BorderLayout.NORTH);
        appointmentsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        appointmentsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                fillManageFormsFromSelection();
            }
        });
        appointmentsPanel.add(new JScrollPane(appointmentsTable), BorderLayout.CENTER);

        JPanel slotsPanel = createCardPanel("Available Slots");
        slotsPanel.setPreferredSize(new Dimension(400, 190));
        slotsPanel.add(new JScrollPane(availableSlotsList), BorderLayout.CENTER);

        root.add(appointmentsPanel, BorderLayout.CENTER);
        root.add(slotsPanel, BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildRightPanel() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 12));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Book", buildBookingTab());
        tabs.addTab("User Manage", buildUserManageTab());
        tabs.addTab("Admin", buildAdminTab());
        tabs.addTab("Notifications", buildNotificationsTab());

        root.add(tabs, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildBookingTab() {
        JPanel card = createCardPanel("Book Appointment (US2.1)");

        JPanel form = createFormPanel();
        form.add(createLabeledField("Appointment ID", bookingAppointmentIdField));
        form.add(createLabeledField("User ID", bookingUserIdField));
        form.add(createLabeledField("User Name", bookingUserNameField));
        form.add(createLabeledField("Type", bookingTypeBox));
        form.add(createLabeledField("Participants", bookingParticipantsSpinner));
        form.add(createLabeledField("Available Slot", bookingSlotBox));
        form.add(createLabeledField("Access Code", bookingAccessCodeField));

        JPanel actions = createActionPanel();
        JButton bookButton = createPrimaryButton("Book Appointment");
        bookButton.addActionListener(e -> handleBookAppointment());
        JButton copyCodeButton = createSuccessButton("Copy Code");
        copyCodeButton.addActionListener(e -> handleCopyAccessCode());
        actions.add(bookButton);
        actions.add(copyCodeButton);

        card.add(form, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildUserManageTab() {
        JPanel card = createCardPanel("Modify / Cancel Your Appointment (US4.1)");

        JPanel note = new JPanel(new FlowLayout(FlowLayout.LEFT));
        note.setOpaque(false);
        JLabel noteLabel = new JLabel("Date format: yyyy-MM-dd HH:mm. Select a row from appointments table to auto-fill.");
        noteLabel.setForeground(new Color(70, 70, 70));
        note.add(noteLabel);

        JPanel form = createFormPanel();
        form.add(createLabeledField("Appointment ID", userManageAppointmentIdField));
        form.add(createLabeledField("Access Code", userManageAccessCodeField));
        form.add(createLabeledField("New Start", userManageStartField));
        form.add(createLabeledField("New End", userManageEndField));
        form.add(createLabeledField("New Type", userManageTypeBox));
        form.add(createLabeledField("Participants", userManageParticipantsSpinner));

        JPanel actions = createActionPanel();
        JButton modifyButton = createPrimaryButton("Modify Appointment");
        modifyButton.addActionListener(e -> handleUserModify());
        JButton cancelButton = createDangerButton("Delete Appointment");
        cancelButton.addActionListener(e -> handleUserCancel());
        actions.add(modifyButton);
        actions.add(cancelButton);

        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);
        container.add(note, BorderLayout.NORTH);
        container.add(form, BorderLayout.CENTER);

        card.add(container, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildAdminTab() {
        JPanel card = createCardPanel("Administrator Actions (US1.1, US1.2, US4.2)");

        JLabel credentialsHint = new JLabel("Enter credentials manually (demo: username=admin, password=admin123)");
        credentialsHint.setForeground(new Color(90, 90, 90));
        credentialsHint.setBorder(BorderFactory.createEmptyBorder(0, 4, 6, 4));

        JPanel authForm = createFormPanel();
        authForm.add(createLabeledField("Username", adminUserNameField));
        authForm.add(createLabeledField("Password", adminPasswordField));

        JPanel authActions = createActionPanel();
        JButton loginButton = createSuccessButton("Login");
        loginButton.addActionListener(e -> handleAdminLogin());
        JButton logoutButton = createDangerButton("Logout");
        logoutButton.addActionListener(e -> handleAdminLogout());
        authActions.add(loginButton);
        authActions.add(logoutButton);

        JPanel authCard = createCardPanel("Admin Authentication");
        adminStatusLabel.setForeground(new Color(120, 28, 28));
        JPanel authBody = new JPanel(new BorderLayout());
        authBody.setOpaque(false);
        authBody.add(credentialsHint, BorderLayout.NORTH);
        authBody.add(authForm, BorderLayout.CENTER);
        authCard.add(authBody, BorderLayout.CENTER);
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setOpaque(false);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(6, 8, 8, 8));
        statusPanel.add(adminStatusLabel, BorderLayout.WEST);
        statusPanel.add(authActions, BorderLayout.EAST);
        authCard.add(statusPanel, BorderLayout.SOUTH);

        JPanel manageForm = createFormPanel();
        manageForm.add(createLabeledField("Appointment ID", adminManageAppointmentIdField));
        manageForm.add(createLabeledField("New Start", adminManageStartField));
        manageForm.add(createLabeledField("New End", adminManageEndField));
        manageForm.add(createLabeledField("New Type", adminManageTypeBox));
        manageForm.add(createLabeledField("Participants", adminManageParticipantsSpinner));

        JPanel manageActions = createActionPanel();
        JButton adminModifyButton = createPrimaryButton("Admin Modify");
        adminModifyButton.addActionListener(e -> handleAdminModify());
        JButton adminCancelButton = createDangerButton("Admin Cancel");
        adminCancelButton.addActionListener(e -> handleAdminCancel());
        JButton remindersButton = createWarningButton("Send Upcoming Reminders");
        remindersButton.addActionListener(e -> handleSendReminders());
        manageActions.add(adminModifyButton);
        manageActions.add(adminCancelButton);
        manageActions.add(remindersButton);

        JPanel manageCard = createCardPanel("Admin Reservation Management");
        manageCard.add(manageForm, BorderLayout.CENTER);
        manageCard.add(manageActions, BorderLayout.SOUTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(authCard);
        body.add(Box.createVerticalStrut(10));
        body.add(manageCard);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildNotificationsTab() {
        JPanel card = createCardPanel("Reminder History (US3.1)");
        JPanel filterPanel = createFormPanel();
        filterPanel.add(createLabeledField("Access Code", notificationCodeField));

        JPanel actions = createActionPanel();
        JButton viewMineButton = createPrimaryButton("View My Notifications");
        viewMineButton.addActionListener(e -> handleViewNotificationsByCode());
        JButton viewAllButton = createSuccessButton("View All (Admin)");
        viewAllButton.addActionListener(e -> handleViewAllNotificationsForAdmin());
        actions.add(viewMineButton);
        actions.add(viewAllButton);

        notificationsArea.setEditable(false);
        notificationsArea.setLineWrap(true);
        notificationsArea.setWrapStyleWord(true);
        notificationsArea.setFont(new Font("Consolas", Font.PLAIN, 13));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(filterPanel, BorderLayout.CENTER);
        top.add(actions, BorderLayout.SOUTH);

        card.add(top, BorderLayout.NORTH);
        card.add(new JScrollPane(notificationsArea), BorderLayout.CENTER);
        return card;
    }

    private void handleBookAppointment() {
        try {
            SlotOption selectedSlot = Objects.requireNonNull((SlotOption) bookingSlotBox.getSelectedItem(),
                    "Please select a slot.");
            BookingRequest request = new BookingRequest(
                    requiredText(bookingAppointmentIdField, "Appointment ID is required."),
                    new User(
                            requiredText(bookingUserIdField, "User ID is required."),
                            requiredText(bookingUserNameField, "User name is required.")),
                    (AppointmentType) bookingTypeBox.getSelectedItem(),
                    selectedSlot.start,
                    selectedSlot.end,
                    (Integer) bookingParticipantsSpinner.getValue());

            Appointment appointment = appointmentService.bookAppointment(request);
            showInfo("Appointment booked successfully. Your access code: " + appointment.getAccessCode());
            bookingAccessCodeField.setText(appointment.getAccessCode());
            appointmentsAccessCodeField.setText(appointment.getAccessCode());
            userManageAccessCodeField.setText(appointment.getAccessCode());
            notificationCodeField.setText(appointment.getAccessCode());
            bookingAppointmentIdField.setText("");
            bookingUserIdField.setText("");
            bookingUserNameField.setText("");
            bookingTypeBox.setSelectedIndex(0);
            bookingParticipantsSpinner.setValue(1);
            refreshAllViews();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void handleUserModify() {
        try {
            String appointmentId = requiredText(userManageAppointmentIdField, "Appointment ID is required.");
            String accessCode = requiredText(userManageAccessCodeField, "Access code is required.");
            BookingRequest request = buildManageRequest(
                    appointmentId,
                    userManageTypeBox,
                    userManageStartField,
                    userManageEndField,
                    userManageParticipantsSpinner);
            appointmentService.modifyAppointment(appointmentId, accessCode, request);
            showInfo("Appointment updated successfully.");
            refreshAllViews();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void handleUserCancel() {
        try {
            String appointmentId = requiredText(userManageAppointmentIdField, "Appointment ID is required.");
            String accessCode = requiredText(userManageAccessCodeField, "Access code is required.");
            appointmentService.cancelAppointment(appointmentId, accessCode);
            showInfo("Appointment cancelled successfully.");
            refreshAllViews();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void handleCopyAccessCode() {
        String code = bookingAccessCodeField.getText().trim();
        if (code.isEmpty()) {
            showError("No access code available to copy yet.");
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(code), null);
        bookingAccessCodeField.setText("");
        showInfo("Access code copied to clipboard.");
    }

    private void handleShowMyAppointments() {
        try {
            requiredText(appointmentsAccessCodeField, "Access code is required.");
            refreshAppointmentsTable();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void handleViewNotificationsByCode() {
        try {
            String code = requiredText(notificationCodeField, "Access code is required.").trim().toUpperCase();
            String token = "[CODE:" + code + "]";

            StringBuilder builder = new StringBuilder();
            for (NotificationMessage message : notificationService.getSentMessages()) {
                if (message.getMessage().contains(token)) {
                    builder.append("- To ")
                            .append(message.getRecipient().getName())
                            .append(" [")
                            .append(message.getRecipient().getId())
                            .append("]: ")
                            .append(message.getMessage())
                            .append(System.lineSeparator());
                }
            }

            if (builder.length() == 0) {
                notificationsArea.setText("No notifications found for this code.");
            } else {
                notificationsArea.setText(builder.toString());
            }
            notificationsArea.setCaretPosition(0);
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void handleViewAllNotificationsForAdmin() {
        if (!adminAuthService.isLoggedIn()) {
            showError("Administrator must log in first.");
            return;
        }
        StringBuilder builder = new StringBuilder();
        List<NotificationMessage> messages = notificationService.getSentMessages();
        if (messages.isEmpty()) {
            builder.append("No reminders were sent yet.");
        } else {
            for (NotificationMessage message : messages) {
                builder.append("- To ")
                        .append(message.getRecipient().getName())
                        .append(" [")
                        .append(message.getRecipient().getId())
                        .append("]: ")
                        .append(message.getMessage())
                        .append(System.lineSeparator());
            }
        }
        notificationsArea.setText(builder.toString());
        notificationsArea.setCaretPosition(0);
    }

    private void handleAdminLogin() {
        String username = adminUserNameField.getText().trim();
        String password = new String(adminPasswordField.getPassword());
        boolean loggedIn = adminAuthService.login(username, password);
        if (loggedIn) {
            adminStatusLabel.setText("Admin session: LOGGED IN as " + username);
            adminStatusLabel.setForeground(new Color(25, 116, 57));
            showInfo("Admin login successful.");
            refreshAllViews();
        } else {
            adminStatusLabel.setText("Admin session: NOT LOGGED IN");
            adminStatusLabel.setForeground(new Color(120, 28, 28));
            showError("Invalid admin credentials.");
        }
    }

    private void handleAdminLogout() {
        adminAuthService.logout();
        adminStatusLabel.setText("Admin session: NOT LOGGED IN");
        adminStatusLabel.setForeground(new Color(120, 28, 28));
        showInfo("Admin logout completed.");
        refreshAllViews();
    }

    private void handleAdminModify() {
        try {
            String appointmentId = requiredText(adminManageAppointmentIdField, "Appointment ID is required.");
            BookingRequest request = buildManageRequest(
                    appointmentId,
                    adminManageTypeBox,
                    adminManageStartField,
                    adminManageEndField,
                    adminManageParticipantsSpinner);
            appointmentService.adminModifyReservation(appointmentId, request);
            showInfo("Admin updated the reservation.");
            refreshAllViews();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void handleAdminCancel() {
        try {
            String appointmentId = requiredText(adminManageAppointmentIdField, "Appointment ID is required.");
            appointmentService.adminCancelReservation(appointmentId);
            showInfo("Admin cancelled the reservation.");
            refreshAllViews();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void handleSendReminders() {
        try {
            appointmentService.sendUpcomingReminders();
            showInfo("Upcoming reminders were dispatched.");
            refreshAllViews();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void refreshAllViews() {
        refreshAppointmentsTable();
        refreshAvailableSlots();
        refreshNotifications();
    }

    private void refreshAppointmentsTable() {
        appointmentsTableModel.setRowCount(0);
        List<Appointment> appointments = new ArrayList<>(appointmentService.viewAppointments());
        appointments.sort(Comparator.comparing(a -> a.getTimeSlot().getStart()));
        String filteredCode = appointmentsAccessCodeField.getText().trim().toUpperCase();
        for (Appointment appointment : appointments) {
            if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
                continue;
            }
            if (!adminAuthService.isLoggedIn()) {
                if (filteredCode.isEmpty() || !appointment.getAccessCode().equalsIgnoreCase(filteredCode)) {
                    continue;
                }
            }
            appointmentsTableModel.addRow(new Object[]{
                appointment.getId(),
                appointment.getUser().getName() + " (" + appointment.getUser().getId() + ")",
                appointment.getType(),
                DATE_FORMAT.format(appointment.getTimeSlot().getStart()),
                DATE_FORMAT.format(appointment.getTimeSlot().getEnd()),
                appointment.getParticipants(),
                appointment.getStatus()
            });
        }
    }

    private void refreshAvailableSlots() {
        availableSlotsModel.removeAllElements();
        List<String> lines = new ArrayList<>();

        List<TimeSlot> slots = new ArrayList<>(appointmentService.viewAvailableSlots());
        slots.sort(Comparator.comparing(TimeSlot::getStart));

        for (TimeSlot slot : slots) {
            SlotOption option = new SlotOption(slot.getStart(), slot.getEnd());
            availableSlotsModel.addElement(option);
            lines.add(option.toString());
        }

        availableSlotsList.setListData(lines.toArray(new String[0]));
    }

    private void refreshNotifications() {
        List<NotificationMessage> messages = notificationService.getSentMessages();
        if (messages.isEmpty()) {
            notificationsArea.setText("No reminders were sent yet.");
        } else {
            notificationsArea.setText("Enter your access code then click 'View My Notifications'. "
                    + "Admins can click 'View All (Admin)'.");
        }
        notificationsArea.setCaretPosition(0);
    }

    private void fillManageFormsFromSelection() {
        int row = appointmentsTable.getSelectedRow();
        if (row < 0) {
            return;
        }

        String appointmentId = String.valueOf(appointmentsTableModel.getValueAt(row, 0));
        String typeName = String.valueOf(appointmentsTableModel.getValueAt(row, 2));
        String start = String.valueOf(appointmentsTableModel.getValueAt(row, 3));
        String end = String.valueOf(appointmentsTableModel.getValueAt(row, 4));
        Integer participants = Integer.valueOf(String.valueOf(appointmentsTableModel.getValueAt(row, 5)));

        userManageAppointmentIdField.setText(appointmentId);
        userManageStartField.setText(start);
        userManageEndField.setText(end);
        userManageTypeBox.setSelectedItem(AppointmentType.valueOf(typeName));
        userManageParticipantsSpinner.setValue(participants);

        adminManageAppointmentIdField.setText(appointmentId);
        adminManageStartField.setText(start);
        adminManageEndField.setText(end);
        adminManageTypeBox.setSelectedItem(AppointmentType.valueOf(typeName));
        adminManageParticipantsSpinner.setValue(participants);
    }

    private BookingRequest buildManageRequest(
            String appointmentId,
            JComboBox<AppointmentType> typeBox,
            JTextField startField,
            JTextField endField,
            JSpinner participantsSpinner) {
        Appointment appointment = appointmentService.viewAppointments().stream()
                .filter(a -> a.getId().equals(appointmentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));

        LocalDateTime start = parseDate(startField.getText());
        LocalDateTime end = parseDate(endField.getText());

        return new BookingRequest(
                appointmentId,
                appointment.getUser(),
                (AppointmentType) typeBox.getSelectedItem(),
                start,
                end,
                (Integer) participantsSpinner.getValue());
    }

    private LocalDateTime parseDate(String text) {
        try {
            return LocalDateTime.parse(text.trim(), DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid date format. Use yyyy-MM-dd HH:mm");
        }
    }

    private String requiredText(JTextField field, String errorMessage) {
        String value = field.getText().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value;
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(frame, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private JPanel createCardPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 232)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(PRIMARY.darker());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        panel.add(titleLabel, BorderLayout.NORTH);
        return panel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setOpaque(false);
        return panel;
    }

    private JPanel createLabeledField(String labelText, Component input) {
        JPanel row = new JPanel(new BorderLayout(8, 8));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        JLabel label = new JLabel(labelText);
        label.setPreferredSize(new Dimension(120, 28));
        row.add(label, BorderLayout.WEST);
        row.add(input, BorderLayout.CENTER);
        return row;
    }

    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        applyActionStyle(button, PRIMARY, Color.WHITE);
        return button;
    }

    private JButton createSuccessButton(String text) {
        JButton button = new JButton(text);
        applyActionStyle(button, SUCCESS, Color.WHITE);
        return button;
    }

    private JButton createWarningButton(String text) {
        JButton button = new JButton(text);
        applyActionStyle(button, WARNING, new Color(40, 40, 40));
        return button;
    }

    private JButton createDangerButton(String text) {
        JButton button = new JButton(text);
        applyActionStyle(button, DANGER, Color.WHITE);
        return button;
    }

    private JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        applyActionStyle(button, SECONDARY, new Color(35, 45, 60));
        return button;
    }

    private void applyActionStyle(JButton button, Color background, Color foreground) {
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBackground(background);
        button.setForeground(Color.BLACK);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        Border border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(background.darker()),
                BorderFactory.createEmptyBorder(6, 12, 6, 12));
        button.setBorder(border);
    }

    private static class SlotOption {

        private final LocalDateTime start;
        private final LocalDateTime end;

        private SlotOption(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return DATE_FORMAT.format(start) + " -> " + DATE_FORMAT.format(end);
        }
    }

    private static class AppRuntime {

        private final AppointmentService appointmentService;
        private final AdminAuthService adminAuthService;
        private final FileNotificationService notificationService;

        private AppRuntime(
                AppointmentService appointmentService,
                AdminAuthService adminAuthService,
                FileNotificationService notificationService) {
            this.appointmentService = appointmentService;
            this.adminAuthService = adminAuthService;
            this.notificationService = notificationService;
        }
    }
}

