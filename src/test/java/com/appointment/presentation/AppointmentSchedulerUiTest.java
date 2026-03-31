package com.appointment.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appointment.application.service.AdminAuthService;
import com.appointment.application.service.AppointmentService;
import com.appointment.domain.model.Appointment;
import com.appointment.domain.model.AppointmentType;
import com.appointment.domain.model.BookingRequest;
import com.appointment.domain.model.TimeSlot;
import com.appointment.domain.model.User;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.Test;

class AppointmentSchedulerUiTest {

    @Test
    void constructorBuildsUiAndLoadsInitialData() throws Exception {
        AppointmentSchedulerUi ui = createUi();
        try {
            JComboBox<?> bookingSlotBox = getField(ui, "bookingSlotBox", JComboBox.class);
            JTextArea notificationsArea = getField(ui, "notificationsArea", JTextArea.class);

            assertTrue(bookingSlotBox.getItemCount() > 0);
            assertTrue(notificationsArea.getText().contains("No reminders were sent yet."));
        } finally {
            disposeUi(ui);
        }
    }

    @Test
    void fillManageFormsFromSelectionCopiesSelectedRowValues() throws Exception {
        AppointmentSchedulerUi ui = createUi();
        try {
            AppointmentService service = getField(ui, "appointmentService", AppointmentService.class);
            TimeSlot slot = service.viewAvailableSlots().get(0);
            service.bookAppointment(new BookingRequest(
                    "AP-UI-1",
                    new User("U-UI-1", "Lina"),
                    AppointmentType.INDIVIDUAL,
                    slot.getStart(),
                    slot.getEnd(),
                    1));

            AdminAuthService authService = getField(ui, "adminAuthService", AdminAuthService.class);
            authService.login("admin", "admin123");

            invoke(ui, "refreshAllViews");

            JTable table = getField(ui, "appointmentsTable", JTable.class);
            DefaultTableModel tableModel = getField(ui, "appointmentsTableModel", DefaultTableModel.class);

            assertTrue(tableModel.getRowCount() > 0);
            table.setRowSelectionInterval(0, 0);
            invoke(ui, "fillManageFormsFromSelection");

            JTextField userId = getField(ui, "userManageAppointmentIdField", JTextField.class);
            JTextField adminId = getField(ui, "adminManageAppointmentIdField", JTextField.class);
            JTextField userStart = getField(ui, "userManageStartField", JTextField.class);
            JTextField adminStart = getField(ui, "adminManageStartField", JTextField.class);

            assertEquals("AP-UI-1", userId.getText());
            assertEquals("AP-UI-1", adminId.getText());
            assertFalse(userStart.getText().isEmpty());
            assertEquals(userStart.getText(), adminStart.getText());
        } finally {
            disposeUi(ui);
        }
    }

    @Test
    void cancelledAppointmentsAreNotShownInTable() throws Exception {
        AppointmentSchedulerUi ui = createUi();
        try {
            AppointmentService service = getField(ui, "appointmentService", AppointmentService.class);
            TimeSlot slot = service.viewAvailableSlots().get(0);
            Appointment booked = service.bookAppointment(new BookingRequest(
                    "AP-UI-DEL-1",
                    new User("U-UI-DEL", "Lina"),
                    AppointmentType.INDIVIDUAL,
                    slot.getStart(),
                    slot.getEnd(),
                    1));
            service.cancelAppointment("AP-UI-DEL-1", booked.getAccessCode());

            invoke(ui, "refreshAllViews");

            DefaultTableModel tableModel = getField(ui, "appointmentsTableModel", DefaultTableModel.class);
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String id = String.valueOf(tableModel.getValueAt(i, 0));
                assertFalse("AP-UI-DEL-1".equals(id));
            }
        } finally {
            disposeUi(ui);
        }
    }

    @Test
    void parseDateAndRequiredTextValidateInput() throws Exception {
        AppointmentSchedulerUi ui = createUi();
        try {
            LocalDateTime parsed = invoke(
                    ui,
                    "parseDate",
                    new Class<?>[]{String.class},
                    new Object[]{"2026-03-28 11:00"},
                    LocalDateTime.class);
            assertEquals(2026, parsed.getYear());
            assertEquals(11, parsed.getHour());

            assertThrows(IllegalArgumentException.class,
                    () -> invoke(ui, "parseDate", new Class<?>[]{String.class}, new Object[]{"bad-date"}, LocalDateTime.class));

            JTextField valid = new JTextField("  value  ");
            String trimmed = invoke(
                    ui,
                    "requiredText",
                    new Class<?>[]{JTextField.class, String.class},
                    new Object[]{valid, "required"},
                    String.class);
            assertEquals("value", trimmed);

            JTextField blank = new JTextField("   ");
            assertThrows(IllegalArgumentException.class,
                    () -> invoke(ui, "requiredText", new Class<?>[]{JTextField.class, String.class}, new Object[]{blank, "required"}, String.class));
        } finally {
            disposeUi(ui);
        }
    }

    @Test
    void buildManageRequestCreatesUpdatedRequest() throws Exception {
        AppointmentSchedulerUi ui = createUi();
        try {
            AppointmentService service = getField(ui, "appointmentService", AppointmentService.class);
            TimeSlot first = service.viewAvailableSlots().get(0);
            TimeSlot second = service.viewAvailableSlots().get(1);

            service.bookAppointment(new BookingRequest(
                    "AP-UI-2",
                    new User("U-UI-2", "Rana"),
                    AppointmentType.INDIVIDUAL,
                    first.getStart(),
                    first.getEnd(),
                    1));

            JComboBox<AppointmentType> typeBox = getField(ui, "userManageTypeBox", JComboBox.class);
            JTextField startField = getField(ui, "userManageStartField", JTextField.class);
            JTextField endField = getField(ui, "userManageEndField", JTextField.class);
            JSpinner participants = getField(ui, "userManageParticipantsSpinner", JSpinner.class);

            typeBox.setSelectedItem(AppointmentType.GROUP);
            startField.setText(format(second.getStart()));
            endField.setText(format(second.getEnd()));
            participants.setValue(2);

            BookingRequest request = invoke(
                    ui,
                    "buildManageRequest",
                    new Class<?>[]{String.class, JComboBox.class, JTextField.class, JTextField.class, JSpinner.class},
                    new Object[]{"AP-UI-2", typeBox, startField, endField, participants},
                    BookingRequest.class);

            assertEquals("U-UI-2", request.getUser().getId());
            assertEquals(AppointmentType.GROUP, request.getType());
            assertEquals(2, request.getParticipants());
        } finally {
            disposeUi(ui);
        }
    }

    @Test
    void helperFactoriesReturnConfiguredComponents() throws Exception {
        AppointmentSchedulerUi ui = createUi();
        try {
            Object primary = invoke(ui, "createPrimaryButton", new Class<?>[]{String.class}, new Object[]{"Book"}, Object.class);
            Object secondary = invoke(ui, "createSecondaryButton", new Class<?>[]{String.class}, new Object[]{"Cancel"}, Object.class);
            Object card = invoke(ui, "createCardPanel", new Class<?>[]{String.class}, new Object[]{"Card"}, Object.class);

            assertNotNull(primary);
            assertNotNull(secondary);
            assertNotNull(card);
        } finally {
            disposeUi(ui);
        }
    }

    private static String format(LocalDateTime value) {
        return String.format("%04d-%02d-%02d %02d:%02d",
                value.getYear(), value.getMonthValue(), value.getDayOfMonth(), value.getHour(), value.getMinute());
    }

    private static AppointmentSchedulerUi createUi() throws Exception {
        Path tempDir = Files.createTempDirectory("appointment-ui-test-" + UUID.randomUUID());
        System.setProperty("appointment.storage.file", tempDir.resolve("system.db").toString());

        final AppointmentSchedulerUi[] holder = new AppointmentSchedulerUi[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new AppointmentSchedulerUi());
        return holder[0];
    }

    private static void disposeUi(AppointmentSchedulerUi ui) throws Exception {
        JFrame frame = getField(ui, "frame", JFrame.class);
        SwingUtilities.invokeAndWait(frame::dispose);
    }

    private static Object invoke(AppointmentSchedulerUi ui, String methodName) throws Exception {
        return invoke(ui, methodName, new Class<?>[0], new Object[0], Object.class);
    }

    private static <T> T invoke(
            AppointmentSchedulerUi ui,
            String methodName,
            Class<?>[] parameterTypes,
            Object[] args,
            Class<T> returnType) throws Exception {
        Method method = AppointmentSchedulerUi.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        try {
            Object value = method.invoke(ui, args);
            return returnType.cast(value);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof RuntimeException) {
                throw (RuntimeException) ex.getCause();
            }
            throw ex;
        }
    }

    private static <T> T getField(AppointmentSchedulerUi ui, String fieldName, Class<T> type) throws Exception {
        Field field = AppointmentSchedulerUi.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return type.cast(field.get(ui));
    }
}
