#language: en
@90 @api
Feature: 90 API Testing - Extended
  Extended coverage for new Sentinel API step definitions using the JSONPlaceholder fake REST API.
  https://jsonplaceholder.typicode.com
  Note: Retry, SSL trust-all, and cookie assertions require infrastructure not
  available via the public JSONPlaceholder API and are covered by unit tests only.

  # ── JSONPath ──────────────────────────────────────────────────────────────

  @90A @api
  Scenario: 90A JSONPath Extraction and Variable Reuse
    Given I use the API named JSONPlaceholder API
      And I add an Accept header with the value application/json
    When I send a GET request to the posts/1 endpoint
    Then I verify the response code equals 200
      And I store the response field "$.userId" as "storedUserId"
    Given I add an Accept header with the value application/json
      And I add a userId parameter with the value {storedUserId}
    When I send a GET request to the posts endpoint
    Then I verify the response code equals 200
      And I validate the response contains the text "userId"

  @90B @api
  Scenario: 90B JSONPath Field Assertions
    Given I use the API named JSONPlaceholder API
      And I add an Accept header with the value application/json
    When I send a GET request to the posts/1 endpoint
    Then I verify the response code equals 200
      And the response field "$.id" should equal "1"
      And the response field "$.userId" should contain "1"
      And the response field "$.id" should not equal "999"
      And the response field "$.userId" should not contain "99"

  @90C @api
  Scenario: 90C JSONPath Array Size Assertion
    Given I use the API named JSONPlaceholder API
      And I add an Accept header with the value application/json
    When I send a GET request to the posts/1/comments endpoint
    Then I verify the response code equals 200
      And the response should have 5 items at "$[*]"

  # ── Response headers ──────────────────────────────────────────────────────

  @90D @api
  Scenario: 90D Response Header Assertions
    Given I use the API named JSONPlaceholder API
      And I add an Accept header with the value application/json
    When I send a GET request to the posts endpoint
    Then I verify the response code equals 200
      And the response header "Content-Type" should contain "application/json"
      And the response header "Content-Type" should not equal "text/html"

  # ── Authentication ────────────────────────────────────────────────────────

  @90E @api
  Scenario: 90E Bearer Token Authentication
    Given I use the API named JSONPlaceholder API
      And I set the bearer token to test-sentinel-bearer-token
      And I add an Accept header with the value application/json
    When I send a GET request to the posts endpoint
    Then I verify the response code equals 200

  @90F @api
  Scenario: 90F Basic Authentication
    Given I use the API named JSONPlaceholder API
      And I set basic authentication with username sentinel and password testpass
      And I add an Accept header with the value application/json
    When I send a GET request to the posts endpoint
    Then I verify the response code equals 200

  @90G @api
  Scenario: 90G API Key Header Step
    Given I use the API named JSONPlaceholder API
      And I set the api_key API key header to special-sentinel-key
      And I add an Accept header with the value application/json
    When I DELETE record 1 from the posts endpoint
    Then I verify the response code equals 200

  # ── HTTP verbs ────────────────────────────────────────────────────────────

  @90H @api
  Scenario: 90H PATCH Request
    Given I use the API named JSONPlaceholder API
      And I add an Accept header with the value application/json
      And I add a Content-Type header with the value application/json
    When I set the request body to
    """
    {"title": "patched title"}
    """
      And I send a PATCH request to the posts/1 endpoint
    Then I verify the response code equals 200

  @90I @api
  Scenario: 90I HEAD Request
    Given I use the API named JSONPlaceholder API
    When I send a HEAD request to the posts endpoint
    Then I verify the response code equals 200

  @90J @api
  Scenario: 90J OPTIONS Request
    Given I use the API named JSONPlaceholder API
    When I send a OPTIONS request to the posts endpoint
    Then I verify the response code equals 204

  # ── JSON Schema validation ────────────────────────────────────────────────

  @90K @api
  Scenario: 90K JSON Schema Validation
    Given I use the API named JSONPlaceholder API
      And I add an Accept header with the value application/json
    When I send a GET request to the posts/1 endpoint
    Then I verify the response code equals 200
      And I validate the response matches the schema "schemas/post.json"

  # ── Logging ───────────────────────────────────────────────────────────────

  @90L @api
  Scenario: 90L Request and Response Logging
    Given I use the API named JSONPlaceholder API
      And I add an Accept header with the value application/json
    When I send a GET request to the posts endpoint
    Then I verify the response code equals 200
      And I log the last request as a curl command
      And I log the last response
