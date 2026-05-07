#Author: Doug Noël
# Original tests used saucelabs.com/test/guinea-pig (removed by SauceLabs).
# Migrated to DemoQA Text Box page — a purpose-built automation practice site.
@example @demoqa
Feature: Example Feature
  This is an example of a test using Cucumber.

  Scenario: Basic form interaction
    Given I am on the Demo QA Text Box Page
    And I verify the URL contains the text "demoqa.com"
    When I type "Hello Sentinel" in the Full Name Textbox
    Then I verify the Full Name Textbox contains the text "Hello Sentinel"

  @#87
  Scenario: 87 Add the ability to press keys using a cucumber step
    Given I am on the Demo QA Text Box Page
    When I press the enter key
      And I press the return key
      And I press the tab key
      And I press the escape key
      And I press the enter key on the Submit Button
      And I press the tab key in the Full Name Textbox
      And I press the escape key to the Full Name Textbox
      And I press the page up key to the Full Name Textbox
      And I press the page down key to the Full Name Textbox
      And I press the page up key
      And I press the page down key
