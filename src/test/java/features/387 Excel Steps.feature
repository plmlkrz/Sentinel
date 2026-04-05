#language: en
@387 @excel
Feature: 387 Excel Steps
  As a QA engineer working with a job application tracker,
  I need to be able to verify Excel file contents,
  so that I can confirm data integrity across tracking spreadsheets.

  @387A @excel
  Scenario: 387A Verify all column headers exist in the job tracker
    When I open src/test/resources/excel/test_excel.xlsx as an Excel file with 1 header row
    Then I verify the Excel file has the column headers Company, Date Applied, Position, Notes, Status

  @387B @excel
  Scenario: 387B Verify the Excel file has the correct number of data rows
    When I open src/test/resources/excel/test_excel.xlsx as an Excel file with 1 header row
    Then I verify the Excel file has 13 data rows

  @387C @excel
  Scenario: 387C Verify exact company name in the first data row
    When I open src/test/resources/excel/test_excel.xlsx as an Excel file with 1 header row
    Then I verify the Excel file has the value Labcorp in the Company column and the 1st row

  @387D @excel
  Scenario: 387D Verify exact position title in the last data row
    When I open src/test/resources/excel/test_excel.xlsx as an Excel file with 1 header row
    Then I verify the Excel file has the value Software Engineer III - Automation & Functional Testing Lead (A0840079) in the Position column and the 13th row

  @387E @excel
  Scenario: 387E Verify Rejected status does not appear in rows with empty status
    When I open src/test/resources/excel/test_excel.xlsx as an Excel file with 1 header row
    Then I verify the Excel file does not have the value Rejected in the Status column and the 3rd row
      And I verify the Excel file does not have the value Rejected in the Status column and the 13th row

  @387F @excel
  Scenario: 387F Verify partial text match in the Notes column
    When I open src/test/resources/excel/test_excel.xlsx as an Excel file with 1 header row
    Then I verify the Excel file contains the value Randstad in the Notes column and the 3rd row
      And I verify the Excel file contains the value Position in the Notes column and the 6th row

  @387G @excel
  Scenario: 387G Verify the Status column contains Rejected somewhere in the data
    When I open src/test/resources/excel/test_excel.xlsx as an Excel file with 1 header row
    Then I verify the Status column of the excel has the text Rejected

  @387H @excel
  Scenario: 387H Verify the data does not contain values absent from the tracker
    When I open src/test/resources/excel/test_excel.xlsx as an Excel file with 1 header row
    Then I verify the Notes column of the excel does not have the text Interview scheduled
      And I verify the Status column of the excel does not have the text Hired

  @387I @excel
  Scenario: 387I Store a company name and verify it appears in the column
    When I open src/test/resources/excel/test_excel.xlsx as an Excel file with 1 header row
      And I store the cell value in the 12th row of the Company column in the excel as lastRejectedCompany
    Then I verify the Company column of the excel contains the same text used for the lastRejectedCompany

  @387J @excel
  Scenario: 387J Verify Company column access by ordinal index matches named access
    When I open src/test/resources/excel/test_excel.xlsx as an Excel file with 1 header row
    Then I verify the Excel file has the value Labcorp in the 1st column and the 1st row
