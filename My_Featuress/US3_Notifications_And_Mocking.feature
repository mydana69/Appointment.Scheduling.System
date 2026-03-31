Feature: Sprint 3 - Appointment Reminders and Mocking
  As the system, I want to notify users before upcoming appointments.

  Scenario: US3.1 Send reminder for upcoming appointment
    Given a confirmed appointment starts within 1 hour
    And notification observers are registered
    When reminders are dispatched
    Then a reminder message should be generated
    And the mock/in-memory notification service should record the sent message

  Scenario: Do not remind cancelled appointments
    Given a cancelled appointment starts within 1 hour
    When reminders are dispatched
    Then no reminder should be sent for that appointment

