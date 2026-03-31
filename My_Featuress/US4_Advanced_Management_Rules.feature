Feature: Sprint 4 - Advanced Management Rules
  Appointment management must be safe for both users and administrators.

  Scenario: US4.1 User modifies future appointment
    Given user appointment "AP-300" is in the future
    When the user modifies the appointment details with valid data
    Then the appointment should be updated successfully

  Scenario: US4.1 User cannot modify past appointment
    Given user appointment "AP-301" is in the past
    When the user tries to modify or cancel the appointment
    Then the action should be rejected
    And an error message should explain that only future appointments are allowed

  Scenario: US4.1 Cancellation releases slot
    Given appointment "AP-302" is confirmed in slot "B"
    When the user cancels appointment "AP-302"
    Then slot "B" should become available again

  Scenario: US4.2 Only administrator can manage reservations
    Given no admin session is active
    When a reservation management action is attempted through admin endpoints
    Then the action should be denied
    And the error message "Administrator must log in first." should be shown

