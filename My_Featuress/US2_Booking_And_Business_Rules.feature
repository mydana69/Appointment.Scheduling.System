Feature: Sprint 2 - Booking and Business Rules
  As a user, I want reliable booking with business-rule validation.

  Scenario: US2.1 Book appointment successfully
    Given a schedule contains free slot "A"
    And user "U-10" wants appointment "AP-200"
    When the user books slot "A" with valid data
    Then the appointment should be saved
    And the appointment status should be "CONFIRMED"

  Scenario: US2.2 Reject appointment duration beyond maximum
    Given maximum allowed duration is 2 hours
    When the user attempts to book an appointment longer than 2 hours
    Then booking should be rejected
    And an error message about duration should be shown

  Scenario: US2.3 Reject participants above capacity
    Given maximum participants per appointment is 8
    When the user attempts to book with 9 participants
    Then booking should be rejected
    And an error message about participant limit should be shown

