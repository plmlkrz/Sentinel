#Author: Sentinel Framework
@API @httpbin
Feature: 92 API Testing - HttpBin
  Tests Sentinel's request construction accuracy against httpbin.org,
  which reflects back exactly what was sent — headers, body, auth.

  Background:
    Given I use the API named Http Bin API

  @httpbin-get
  Scenario: Verify GET request headers are sent correctly
    When I add a X-Sentinel-Test header with the value framework-test
      And I send a GET request to the get endpoint
    Then I verify the response code equals 200
      And the response field "$.headers.X-Sentinel-Test" should equal "framework-test"

  @httpbin-post
  Scenario: Verify POST body is sent correctly
    Given I load sample_body to use as the request body
    When I add a Content-Type header with the value application/json
      And I send a POST request to the post endpoint
    Then I verify the response code equals 200
      And the response field "$.json.framework" should equal "sentinel"

  @httpbin-auth-bearer
  Scenario: Verify bearer token header is sent correctly
    When I set the bearer token to sentinel-test-token
      And I send a GET request to the bearer endpoint
    Then I verify the response code equals 200
      And the response field "$.authenticated" should equal "true"
      And the response field "$.token" should equal "sentinel-test-token"

  @httpbin-auth-basic
  Scenario: Verify basic auth header is sent correctly
    When I set basic authentication with username sentinel and password secret
      And I send a GET request to the basic-auth/sentinel/secret endpoint
    Then I verify the response code equals 200
      And the response field "$.authenticated" should equal "true"

  @httpbin-status
  Scenario: Request a specific HTTP status code
    When I send a GET request to the status/418 endpoint
    Then I verify the response code equals 418

  @httpbin-response-time
  Scenario: Verify response within acceptable time
    When I send a GET request to the get endpoint
    Then I verify the response was received in less than 5 seconds
