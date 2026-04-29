#language: en
#Author: ty.pixelplane@gmail.com

@239 @WindowsOnly
Feature: 239 WinAppDriver Windows Desktop Automation
  As a user I want to be able to automate Windows 11 OS,
  so that I can automate things that cannot be automated in the browser.

  @239A
  Scenario: 239A Use Calculator - basic arithmetic
    Given I open the Calc App
    When I click the Button One
      And I click the Plus Button
      And I click the Button Two
      And I click the Equals Button
    Then I verify the Result Display contains the text "3"
    When I click the Clear Button
    Then I verify the Result Display contains the text "0"
      And I verify the nonexistent element does not exist

  @239B
  Scenario: 239B Use Calculator - chained calculation
    Given I switch to the Calc App
    When I click the Button Three
      And I click the Plus Button
      And I click the Button Three
      And I click the Equals Button
    Then I verify the Result Display contains the text "6"

  @239D
  Scenario: 239D Switch Between Active Tabs and Pages Together
    Open a New Tab and Ensures it Opens
    Switch to the Previous Tab and Ensure We Have Switched
    Open a New Window and Ensures it Opens
    Switch to the New Tab and Ensures We Have Switched
    Switch to the New Window and Ensures We Have Switched
    Switch to the Original Window and Ensures We Have Switched

    Given I am on the Encode DNA Home Page
    When I click the Open New Tab Button
    Then I verify a new tab opens to the Encode DNA New Tab Page
      And I verify the Header contains the text "Window Opened in a New Tab"
    When I switch to the Encode DNA Home Page on the previous tab
      And I click the Open New Window Button
    Then I verify a new window opens the Encode DNA PopUp Window
      And I verify the Header contains the text "A New Popup Window"
    When I switch to the Encode DNA New Tab Page in the previous window
    Then I verify the Header contains the text "Window Opened in a New Tab"
    When I switch to the Encode DNA PopUp Window in the next window
    Then I verify the Header contains the text "A New Popup Window"
    When I switch to the Encode DNA New Tab Page in the previous window
    Then I verify the Header contains the text "Window Opened in a New Tab"
