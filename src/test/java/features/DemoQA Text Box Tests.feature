#Author: Sentinel Framework
@demoqa @text
Feature: DemoQA Text Box Tests
  Validates text entry, form submission, and keyboard interactions using
  the DemoQA Text Box page — a purpose-built automation practice site.

  @demoqa-textbox
  Scenario: Fill out and submit the text box form
    Given I am on the Demo QA Text Box Page
    When I type "John Doe" in the Full Name Textbox
      And I type "john.doe@example.com" in the Email Textbox
      And I type "123 Main Street" in the Current Address Textarea
      And I type "456 Oak Avenue" in the Permanent Address Textarea
      And I click the Submit Button
    Then I verify the Output Section is displayed

  @demoqa-keys
  Scenario: Key press interactions on the text box page
    Given I am on the Demo QA Text Box Page
    When I click the Full Name Textbox
      And I type "Test Entry" in the Full Name Textbox
    Then I verify the Full Name Textbox contains the text "Test Entry"
    When I press the tab key in the Full Name Textbox
      And I press the escape key
