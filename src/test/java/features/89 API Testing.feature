#language: en
@89 @api
Feature: 89 API Testing
  Tests using the JSONPlaceholder fake REST API.
  https://jsonplaceholder.typicode.com

  @89A @api
  Scenario: 89A POST JSONPlaceholder Test
    Given I use the API named JSONPlaceholder API
      And I add an Accept header with the value application/json
      And I add a Content-Type header with the value application/json
    When I load postdata to use as the request body
      And I send a POST request to the posts endpoint
    Then I verify the response code equals 201
      And I verify the response was received in less than 2 seconds
      And I validate the response contains the text "sentinel"

  @89B @api
  Scenario: 89B GET JSONPlaceholder Test
    Given I use the API named JSONPlaceholder API
      And I add an Accept header with the value application/json
    When I GET record {test_id} from the posts endpoint
    Then I verify the response code equals 200
      And I verify the response was received in less than 2 seconds
      And I validate the response contains the text "userId"

  @89C @api
  Scenario: 89C PUT JSONPlaceholder Test
    Given I use the API named JSONPlaceholder API
      And I add an Accept header with the value application/json
      And I add a Content-Type header with the value application/json
    When I set the request body to
    """
    {
      "id": 1,
      "title": "sentinel updated post",
      "body": "updated by sentinel framework",
      "userId": 1
    }
    """
    And I send a PUT request to the posts/1 endpoint
    Then I verify the response code equals 200
      And I validate the response contains the text "sentinel"

  @89D @api
  Scenario: 89D Parameter JSONPlaceholder Test
    Given I use the API named JSONPlaceholder API
      And I add an Accept header with the value application/json
      And I add a userId parameter with the value 1
    When I send a GET request to the posts endpoint
    Then I verify the response code equals 200
      And I validate the response contains the text "userId"

  @89E @api
  Scenario: 89E DELETE JSONPlaceholder Test
    Given I use the API named JSONPlaceholder API
      And I add an Accept header with the value application/json
    When I DELETE record 1 from the posts endpoint
    Then I verify the response code equals 200
    Given I add an Accept header with the value application/json
    When I GET record 9999 from the posts endpoint
    Then I verify the response code equals 404

  @89F @api
  Scenario: 89F DELETE Header JSONPlaceholder Test
    Given I use the API named JSONPlaceholder API
      And I add an Accept header with the value application/json
      And I add an Authorization header with the value Bearer test-token
    When I DELETE record 1 from the posts endpoint
    Then I verify the response code equals 200

  @89G @api
  Scenario: 89G Body With Parameters Test
    Given I use the API named JSONPlaceholder API
      And I add an Accept header with the value application/json
      And I add a Content-Type header with the value application/json
    When I initialize the configuration values as follows
    """
    userId: 5
    post_title: sentinel parameterized post
    """
    When I set the request body to
    """
    {
      "title": "{post_title}",
      "body": "test body",
      "userId": {userId}
    }
    """
    And I send a POST request to the posts endpoint
    Then I verify the response code equals 201
      And I validate the response contains the text "sentinel"

  @89H @api
  Scenario: 89H URL With Parameter Test
    Given I use the API named JSONPlaceholder API
      And I add an Accept header with the value application/json
    When I initialize the configuration values as follows
    """
    id: 5
    """
      And I send a GET request to the posts/{id} endpoint
    Then I verify the response code equals 200

  @89I @api
  Scenario: 89I Query String Stored Parameter Test
    Given I use the API named JSONPlaceholder API
      And I add an Accept header with the value application/json
    When I initialize the configuration values as follows
    """
    user_id: 3
    """
      And I add a userId parameter with the value {user_id}
    When I send a GET request to the posts endpoint
    Then I verify the response code equals 200
      And I validate the response contains the text "userId"
