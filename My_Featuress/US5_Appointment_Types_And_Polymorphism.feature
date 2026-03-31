Feature: Sprint 5 - Appointment Types and Polymorphism
  Type-based behavior should remain flexible and extendable.

  Scenario: US5.1 Store appointment type correctly
    Given a user chooses type "URGENT"
    When the appointment is booked successfully
    Then the stored appointment type should be "URGENT"

  Scenario: US5.1 Support full type catalog
    Given the system supports appointment types
      | type        |
      | URGENT      |
      | FOLLOW_UP   |
      | ASSESSMENT  |
      | VIRTUAL     |
      | IN_PERSON   |
      | INDIVIDUAL  |
      | GROUP       |
    Then each type should be selectable for booking

  Scenario: US5.2 Apply INDIVIDUAL rule
    Given appointment type is "INDIVIDUAL"
    When participants count is not equal to 1
    Then booking should be rejected with a type-specific rule error

  Scenario: US5.2 Apply GROUP rule
    Given appointment type is "GROUP"
    When participants count is less than 2
    Then booking should be rejected with a type-specific rule error

  Scenario: US5.2 Apply VIRTUAL rule
    Given appointment type is "VIRTUAL"
    When participants count is greater than 10
    Then booking should be rejected with a type-specific rule error

