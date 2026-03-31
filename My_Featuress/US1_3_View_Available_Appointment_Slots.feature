Feature: View Available Appointment Slots
  As a user
  I want to view available appointment slots
  So that I can select a suitable time

  Background:
    Given a schedule has the following slots
      | slotId | start             | end               |
      | A      | 2026-03-29 11:00  | 2026-03-29 12:00  |
      | B      | 2026-03-29 12:00  | 2026-03-29 13:00  |

  Scenario: Only available slots are displayed
    Given slot "A" is already booked with status "CONFIRMED"
    When the user opens the available slots view
    Then slot "A" should not be displayed
    And slot "B" should be displayed

  Scenario: Fully booked slots are not selectable
    Given slot "A" is already booked with status "CONFIRMED"
    When the user tries to book slot "A"
    Then booking should be rejected
    And the error message "Selected slot is fully booked." should be shown

