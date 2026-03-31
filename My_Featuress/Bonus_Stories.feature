Feature: Bonus Stories for Better User Experience
  Additional stories to improve usability and reliability.

  Scenario: Show clear validation message for invalid date format
    Given the user enters date "03/29/2026 11:00"
    When the user submits a modify request
    Then the request should be rejected
    And the message "Invalid date format. Use yyyy-MM-dd HH:mm" should be shown

  Scenario: Auto-fill management forms when selecting an appointment row
    Given the appointments table contains at least one appointment
    When the user selects an appointment row
    Then user manage fields should be pre-filled with that appointment data
    And admin manage fields should be pre-filled with that appointment data

  Scenario: Display notification history in GUI
    Given reminders were sent successfully
    When the user opens the notifications tab
    Then all sent reminders should be listed in the history area

