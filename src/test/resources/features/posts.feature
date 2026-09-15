@posts
Feature: Posts API
  As an API consumer
  I want to create, read, update and delete posts
  So that content can be managed programmatically

  Background:
    Given I use the "default" API

  @smoke @regression @positive
  Scenario: Create a post, then read the author it belongs to
    Given I set the request body to:
      | userId | ${data:USER_ID}                 |
      | title  | Sarvada BDD ${random:string}    |
      | body   | Created by the API framework    |
    When I send a POST request to "POSTS"
    Then the response status code should be 201
    And the response should match the schema "schemas/post.schema.json"
    And the response field "userId" should be "${data:USER_ID}"
    And I save the response field "id" as "newPostId"
    And I save the response field "userId" as "authorId"
    Given I add "id" as "${saved:authorId}" in "PATH"
    When I send a GET request to "USER_BY_ID"
    Then the response status code should be 200
    And the response field "id" should equal the saved "authorId"
    And the "POSTS" response field "userId" should equal the "USER_BY_ID" response field "id"

  @regression @positive
  Scenario: Posts can be filtered by author
    Given I add "userId" as "${data:USER_ID}" in "QUERY"
    When I send a GET request to "POSTS"
    Then the response status code should be 200
    And the response field "$" should be a non-empty list
    And every item in "$" should have "userId" equal to "${data:USER_ID}"

  @regression @positive
  Scenario: Update a post with a JSON document
    Given I add "CORRECT" "POST_ID" in "PATH" as "id"
    And I set the request body:
      """
      {
        "id": ${data:POST_ID},
        "userId": ${data:USER_ID},
        "title": "Updated title",
        "body": "Updated by ${random:email}"
      }
      """
    When I send a PUT request to "POST_BY_ID"
    Then the response status code should be 200
    And the response field "title" should be "Updated title"
    And the response field "body" should contain "Updated by qa."

  @regression @positive
  Scenario: Partially update a post
    Given I add "CORRECT" "POST_ID" in "PATH" as "id"
    And I set the request body to:
      | title | Patched title |
    When I send a PATCH request to "POST_BY_ID"
    Then the response status code should be 200
    And the response field "title" should be "Patched title"
    And the response field "userId" should exist

  @regression @positive
  Scenario: Delete a post
    Given I add "CORRECT" "POST_ID" in "PATH" as "id"
    When I send a DELETE request to "POST_BY_ID"
    Then the response status code should be 200

  @regression @positive
  Scenario: Read the comments of a post
    Given I add "CORRECT" "POST_ID" in "PATH" as "id"
    When I send a GET request to "POST_COMMENTS"
    Then the response status code should be 200
    And the response field "$" should be a non-empty list
    And every item in "$" should have "postId" equal to "${data:POST_ID}"
    And the response field "[0].email" should contain "@"

  @regression @negative
  Scenario: Requests with an API key header are accepted whatever the key state
    Given I add "CORRECT" "API_KEY" in "HEADER" as "X-Api-Key"
    When I send a GET request to "POSTS"
    Then the response status code should be 200
    Given I add "EXPIRED" "API_KEY" in "HEADER" as "X-Api-Key"
    When I send a GET request to "POSTS"
    Then the response status code should be 200
