#Author: Sentinel Framework
@API @reqres
Feature: 91 API Testing - ReqRes
  Tests Sentinel's API capabilities against ReqRes (reqres.in) — a
  purpose-built REST API that supports auth token flows and pagination.

  Background:
    Given I use the API named ReqRes API

  @reqres-get
  Scenario: Get a single user
    When I send a GET request to the users/2 endpoint
    Then I verify the response code equals 200
      And the response field "$.data.id" should equal "2"
      And the response field "$.data.email" should contain "reqres.in"

  @reqres-list
  Scenario: List users with pagination parameter
    When I add a page parameter with the value 2
      And I send a GET request to the users endpoint
    Then I verify the response code equals 200
      And the response field "$.page" should equal "2"
      And the response should have 6 items at "$.data"

  @reqres-post
  Scenario: Create a new user
    Given I load new_user to use as the request body
    When I send a POST request to the users endpoint
    Then I verify the response code equals 201
      And the response field "$.name" should equal "sentinel test user"
      And the response field "$.job" should equal "automation engineer"
      And I store the response field "$.id" as "createdUserId"

  @reqres-put
  Scenario: Update a user with PUT
    Given I load updated_user to use as the request body
    When I send a PUT request to the users/2 endpoint
    Then I verify the response code equals 200
      And the response field "$.name" should equal "sentinel updated user"

  @reqres-delete
  Scenario: Delete a user
    When I send a DELETE request to the users/2 endpoint
    Then I verify the response code equals 204

  @reqres-auth
  Scenario: Login and use returned token in a subsequent request
    Given I load login to use as the request body
    When I send a POST request to the login endpoint
    Then I verify the response code equals 200
      And I store the response field "$.token" as "authToken"
    When I apply the stored bearer token "authToken"
      And I send a GET request to the users/1 endpoint
    Then I verify the response code equals 200
