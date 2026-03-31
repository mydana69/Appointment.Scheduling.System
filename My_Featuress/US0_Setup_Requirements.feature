Feature: Setup Requirements (Week 1)
  The student prepares the development environment and project tools.

  Scenario: US0.1 Install JDK
    Given Java is installed on the machine
    When the student runs command "javac -version"
    Then the reported version should be greater than or equal to "1.8"

  Scenario: US0.2 Install IDE
    Given an IDE for Java development is installed
    When the student opens the IDE
    Then the IDE should launch successfully
    And the student should be able to create a Maven project

  Scenario: US0.3 Create Maven project
    Given the student is inside the project directory
    When the student runs command "mvn test"
    Then a file named "pom.xml" should exist
    And Maven tests should run successfully

