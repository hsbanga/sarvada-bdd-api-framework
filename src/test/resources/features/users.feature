@users
Feature: Users API
  As an API consumer
  I want to read user records
  So that I can display account details

  Background:
    Given I use the "default" API

  @smoke @regression @positive
  Scenario: List all users
    When I send a GET request to "USERS"
    Then the response status code should be 200
    And the response header "Content-Type" should contain "application/json"
    And the response should be a list of 10 items
    And the response time should be under 5000 ms

  @smoke @regression @positive
  Scenario: Fetch a single user by id and validate its contract
    Given I add "CORRECT" "USER_ID" in "PATH" as "id"
    When I send a GET request to "USER_BY_ID"
    Then the response status code should be 200
    And the response should match the schema "schemas/user.schema.json"
    And the response field "id" should be "${data:USER_ID}"
    And the response field "email" should be "${data:USER_EMAIL}"
    And the response field "website" should exist

  @regression @positive
  Scenario: Filter users by email
    Given I add "email" as "${data:USER_EMAIL}" in "QUERY"
    When I send a GET request to "USERS"
    Then the response status code should be 200
    And the response should be a list of 1 items
    And the response field "[0].email" should be "${data:USER_EMAIL}"

  @regression @negative
  Scenario Outline: Unknown user ids return 404
    Given I add "<condition>" "USER_ID" in "PATH" as "id"
    When I send a GET request to "USER_BY_ID"
    Then the response status code should be 404
    And the response body should be empty

    Examples:
      | condition |
      | INVALID   |
