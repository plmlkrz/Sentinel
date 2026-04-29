#Author: Doug Noel

@228
Feature: 228 Open New Window or Tab.feature
  Open a New Window and Ensure it Opens
  Close the Window and ensure we go back to the previous window
  Open a New Tab and ensure it opens
  Close the tab and ensure we go back to the previous window

  @228A
  Scenario: 228A Open New Window
    Given I am on the Demo QA Browser Windows Page
    When I click the New Window Button
    Then I verify a new window opens the Demo QA Sample Page
      And I verify the Heading contains the text "This is a sample page"
    When I close the browser window
    Then I am redirected to the Demo QA Browser Windows Page
    Then I verify the New Window Button is displayed

  @228B
  Scenario: 228B Open New Tab
    Given I am on the Demo QA Browser Windows Page
    When I click the New Tab Button
    Then I verify a new tab opens to the Demo QA Sample Page
      And I verify the Heading contains the text "This is a sample page"
    When I close the browser tab
    Then I am redirected to the Demo QA Browser Windows Page
    Then I verify the New Tab Button is displayed

  @228C
  Scenario: 228C Switch to new window that contains partial title
    Given I am on the Demo QA Browser Windows Page
    When I initialize the configuration values as follows
    """
    partial_tab_title: sample
    """
      And I click the New Window Button
    Then I look for and switch to a window on the Demo QA Sample Page with a title that contains the same text used in the partial_tab_title
      And I verify the Heading contains the text "This is a sample page"
    When I close the browser window
    Then I am redirected to the Demo QA Browser Windows Page
    Then I verify the New Window Button is displayed
