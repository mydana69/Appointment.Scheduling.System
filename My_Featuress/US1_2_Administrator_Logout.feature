Feature: Administrator Logout
  As an administrator
  I want to log out
  So that my session is closed securely

  Background:
    Given the administrator is logged in with username "admin" and password "admin123"

  Scenario: Logout closes the active session
    When the administrator clicks the logout button
    Then the admin session should be inactive

  Scenario: Admin actions require re-login after logout
    Given an appointment exists with id "AP-S1-1"
    And the administrator clicks the logout button
    When the administrator attempts to cancel reservation "AP-S1-1"
    Then the action should be denied
    And the error message "Administrator must log in first." should be shown

