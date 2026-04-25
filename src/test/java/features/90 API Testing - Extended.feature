#language: en
@90
Feature: 90 API Testing - Extended
  Extended coverage for new Sentinel API features using the Swagger Pet Store.
  https://petstore3.swagger.io/
  Note: Retry, SSL trust-all, and cookie assertions require infrastructure not
  available via the public Petstore API and are covered by unit tests only.

  # ── JSONPath ──────────────────────────────────────────────────────────────

  @90A
  Scenario: 90A JSONPath Extraction and Variable Reuse
    Given I use the API named Pet Store API
      And I add an Accept header with the value application/json
      And I add a Content-Type header with the value application/json
    When I load puppydata to use as the request body
      And I send a POST request to the pet endpoint
    Then I verify the response code equals 200
      And I store the response field "$.id" as "newPetId"
    Given I add an Accept header with the value application/json
    When I send a GET request to the pet/{newPetId} endpoint
    Then I verify the response code equals 200
      And I validate the response contains the text "doggie"

  @90B
  Scenario: 90B JSONPath Field Assertions
    Given I use the API named Pet Store API
      And I add an Accept header with the value application/json
      And I add a Content-Type header with the value application/json
    When I load puppydata to use as the request body
      And I send a POST request to the pet endpoint
    Then I verify the response code equals 200
      And the response field "$.name" should equal "doggie"
      And the response field "$.status" should contain "avail"
      And the response field "$.name" should not equal "cat"
      And the response field "$.status" should not contain "sold"

  @90C
  Scenario: 90C JSONPath Array Size Assertion
    Given I use the API named Pet Store API
      And I add an Accept header with the value application/json
      And I add a Content-Type header with the value application/json
    When I load puppydata to use as the request body
      And I send a POST request to the pet endpoint
    Then I verify the response code equals 200
      And the response should have 1 items at "$.photoUrls"

  # ── Response headers ──────────────────────────────────────────────────────

  @90D
  Scenario: 90D Response Header Assertions
    Given I use the API named Pet Store API
      And I add an Accept header with the value application/json
      And I add a status parameter with the value available
    When I send a GET request to the pet/findByStatus endpoint
    Then I verify the response code equals 200
      And the response header "Content-Type" should contain "application/json"
      And the response header "Content-Type" should not equal "text/html"

  # ── Authentication ────────────────────────────────────────────────────────

  @90E
  Scenario: 90E Bearer Token Authentication
    Given I use the API named Pet Store API
      And I set the bearer token to test-sentinel-bearer-token
      And I add an Accept header with the value application/json
      And I add a status parameter with the value available
    When I send a GET request to the pet/findByStatus endpoint
    Then I verify the response code equals 200

  @90F
  Scenario: 90F Basic Authentication
    Given I use the API named Pet Store API
      And I set basic authentication with username sentinel and password testpass
      And I add an Accept header with the value application/json
      And I add a status parameter with the value available
    When I send a GET request to the pet/findByStatus endpoint
    Then I verify the response code equals 200

  @90G
  Scenario: 90G API Key Header Step
    Given I use the API named Pet Store API
      And I set the api_key API key header to special-sentinel-key
      And I add an Accept header with the value application/json
    When I DELETE record 10 from the pet endpoint
    Then I verify the response code equals 200

  # ── HTTP verbs ────────────────────────────────────────────────────────────

  @90H
  Scenario: 90H PATCH Request
    Given I use the API named Pet Store API
      And I add an Accept header with the value application/json
      And I add a Content-Type header with the value application/json
    When I set the request body to
    """
    {"id": 10, "name": "patchedpuppy", "status": "available", "photoUrls": []}
    """
      And I send a PATCH request to the pet endpoint
    Then I verify the response code equals 405

  @90I
  Scenario: 90I HEAD Request
    Given I use the API named Pet Store API
      And I add a status parameter with the value available
    When I send a HEAD request to the pet/findByStatus endpoint
    Then I verify the response code equals 200

  @90J
  Scenario: 90J OPTIONS Request
    Given I use the API named Pet Store API
    When I send a OPTIONS request to the pet endpoint
    Then I verify the response code equals 200

  # ── JSON Schema validation ────────────────────────────────────────────────

  @90K
  Scenario: 90K JSON Schema Validation
    Given I use the API named Pet Store API
      And I add an Accept header with the value application/json
      And I add a Content-Type header with the value application/json
    When I load puppydata to use as the request body
      And I send a POST request to the pet endpoint
    Then I verify the response code equals 200
      And I validate the response matches the schema "schemas/pet.json"

  # ── Logging ───────────────────────────────────────────────────────────────

  @90L
  Scenario: 90L Request and Response Logging
    Given I use the API named Pet Store API
      And I add an Accept header with the value application/json
      And I add a status parameter with the value available
    When I send a GET request to the pet/findByStatus endpoint
    Then I verify the response code equals 200
      And I log the last request as a curl command
      And I log the last response
