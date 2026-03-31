Feature: Administrator Login
  As an administrator
  I want to log into the system using my credentials
  So that I can manage schedules and reservations

  Scenario: Login succeeds with valid credentials
    Given the administrator is on the login form
    When the administrator enters username "admin" and password "admin123"
    And clicks the login button
    Then login should succeed
    And the admin session should be active

  Scenario: Login fails with invalid credentials
    Given the administrator is on the login form
    When the administrator enters username "admin" and password "wrong-password"
    And clicks the login button
    Then login should fail
    And an error message "Invalid admin credentials." should be shown

