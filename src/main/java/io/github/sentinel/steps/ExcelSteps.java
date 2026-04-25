package io.github.sentinel.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.sentinel.configurations.Configuration;
import io.github.sentinel.exceptions.FileException;
import io.github.sentinel.files.ExcelFile;
import io.github.sentinel.strings.SentinelStringUtils;
import io.github.sentinel.system.FileManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.nio.file.Path;

import static org.junit.Assert.*;

/**
 * Cucumber step definitions for verifying Excel (.xlsx) file contents.
 * Mirrors {@link CsvSteps} in structure and naming conventions.
 * <p>
 * The active Excel file is stored via {@link FileManager#setCurrentTestFile} and
 * retrieved in subsequent steps via {@link FileManager#getCurrentTestFile}.
 */
public class ExcelSteps {

    private static final Logger log = LogManager.getLogger(ExcelSteps.class.getName());
    private static final String CONTAIN = "contain";

    // ── Open steps ────────────────────────────────────────────────────────────

    /**
     * Opens a specific Excel file for use in subsequent steps.
     * The file location may be a literal path or a test-data key resolvable via
     * {@code Configuration.getTestData(key, "fileLocation")}.
     */
    @When("^I open (.*) as an? (?:Excel|excel|EXCEL) file with (\\d+) header rows?$")
    public static void openSpecificFileAsExcel(String fileLocation, int numberOfHeaderRows)
            throws FileNotFoundException {
        String filePath;
        try {
            filePath = Configuration.getTestData(fileLocation.trim(), "fileLocation");
        } catch (FileException | NullPointerException e) {
            filePath = fileLocation;
        }
        FileManager.setCurrentTestFile(new ExcelFile(Path.of(filePath), numberOfHeaderRows));
    }

    /**
     * Opens the most recently downloaded Excel file.
     */
    @When("^I find and open the last downloaded (?:Excel|excel|EXCEL) file with (\\d+) header rows?$")
    public static void openMostRecentlyDownloadedFileAsExcel(int numberOfHeaderRows)
            throws FileNotFoundException {
        FileManager.setCurrentTestFile(new ExcelFile(numberOfHeaderRows));
    }

    // ── Header verification ───────────────────────────────────────────────────

    /**
     * Verifies that a comma-separated list of column headers all exist in the Excel file.
     * Example: {@code I verify the Excel file has the column headers Company, Date Applied, Position}
     */
    @Then("^I verify the (?:Excel|excel|EXCEL) file has the column headers? (.*)$")
    public static void verifyExcelColumnHeadersExist(String columnHeaders) {
        ExcelFile file = (ExcelFile) FileManager.getCurrentTestFile();
        String[] headers = columnHeaders.split(",");
        for (String header : headers) {
            String trimmed = header.trim();
            String expectedResult = SentinelStringUtils.format(
                    "Expected the Excel file to have the column header \"{}\".", trimmed);
            assertTrue(expectedResult, file.verifyColumnHeaderEquals(trimmed, false));
        }
    }

    // ── Cell verification ─────────────────────────────────────────────────────

    /**
     * Verifies that a cell in the Excel file has (or does not have) a specific value.
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code I verify the Excel file has the value Labcorp in the Company column and the 1st row}</li>
     *   <li>{@code I verify the Excel file does not have the value ACME in the Company column and the 1st row}</li>
     *   <li>{@code I verify the Excel file contains the value Rejected in the Status column and the last row}</li>
     * </ul>
     */
    @Then("^I verify the (?:Excel|excel|EXCEL) file( do(?:es)? not)? (has|have|contains?) the value (.*) in the (.*) column and the (la|\\d+)(?:st|nd|rd|th) row$")
    public static void verifyExcelCellHasValue(String assertion, String matchType,
                                               String textToMatch, String column, String rowNum) {
        ExcelFile file = (ExcelFile) FileManager.getCurrentTestFile();
        boolean negate = !StringUtils.isEmpty(assertion);
        int rowIndex = rowNum.equals("la") ? file.getNumberOfDataRows() : Integer.parseInt(rowNum);
        boolean partialMatch = matchType.contains(CONTAIN);

        var expectedResult = SentinelStringUtils.format(
                "Expected the cell in the {} row and the {} column of the Excel file to {}contain the text {}.",
                SentinelStringUtils.ordinal(rowIndex), column, (negate ? "not " : ""), textToMatch);
        log.trace(expectedResult);

        if (StringUtils.isNumeric(column.substring(0, 1))) {
            int colIdx = SentinelStringUtils.parseOrdinal(column);
            if (negate)
                assertNotNull(expectedResult, file.verifyCellDataContains(rowIndex, colIdx, textToMatch, partialMatch));
            else
                assertNull(expectedResult, file.verifyCellDataContains(rowIndex, colIdx, textToMatch, partialMatch));
        } else {
            if (negate)
                assertNotNull(expectedResult, file.verifyCellDataContains(rowIndex, column, textToMatch, partialMatch));
            else
                assertNull(expectedResult, file.verifyCellDataContains(rowIndex, column, textToMatch, partialMatch));
        }
    }

    // ── Row count verification ────────────────────────────────────────────────

    /**
     * Verifies the number of data rows in the Excel file (excluding header rows).
     * Example: {@code I verify the Excel file has 13 data rows}
     */
    @Then("^I verify the (?:Excel|excel|EXCEL) file has (\\d+) data rows?$")
    public static void verifyNumberOfExcelRows(int numRows) {
        ExcelFile file = (ExcelFile) FileManager.getCurrentTestFile();
        String errorMessage = SentinelStringUtils.format(
                "Expected the Excel file to have {} data rows, not including headers, but found {}.",
                numRows, file.getNumberOfDataRows());
        assertEquals(errorMessage, numRows, file.getNumberOfDataRows());
    }

    // ── Column content verification ───────────────────────────────────────────

    /**
     * Verifies that any cell in a column does (or does not) contain the given text.
     * Example: {@code I verify the Status column of the excel has the text Rejected}
     */
    @Then("^I verify the (.*?) column of the (?:excel|Excel|EXCEL)( do(?:es)? not)? (has|have|contains?) the text (.*)$")
    public static void verifyTextAppearsInExcelColumn(String column, String assertion,
                                                      String matchType, String textToMatch) {
        ExcelFile file = (ExcelFile) FileManager.getCurrentTestFile();
        boolean negate = !StringUtils.isEmpty(assertion);
        boolean partialMatch = matchType.contains(CONTAIN);

        String errorMessage = SentinelStringUtils.format(
                "Expected the {} column of the Excel to {}contain cells with the text {}.",
                column, (negate ? "not " : ""), textToMatch);

        if (StringUtils.isNumeric(column.substring(0, 1))) {
            int colIdx = SentinelStringUtils.parseOrdinal(column);
            if (negate)
                assertFalse(errorMessage, file.verifyAnyColumnCellContains(colIdx, textToMatch, partialMatch));
            else
                assertTrue(errorMessage, file.verifyAnyColumnCellContains(colIdx, textToMatch, partialMatch));
        } else {
            if (negate)
                assertFalse(errorMessage, file.verifyAnyColumnCellContains(column, textToMatch, partialMatch));
            else
                assertTrue(errorMessage, file.verifyAnyColumnCellContains(column, textToMatch, partialMatch));
        }
    }

    /**
     * Verifies that a column contains the same text previously stored under a key via
     * {@code Configuration.update(key, value)}.
     * Example: {@code I verify the Company column of the excel contains the same text used for the storedCompany}
     */
    @Then("^I verify the (.*?) column of the (?:excel|Excel|EXCEL)( do(?:es)? not)? (has|have|contains?) the same text (?:entered|selected|used) for the (.*)$")
    public static void verifyStoredTextAppearsInExcelColumn(String column, String assertion,
                                                            String matchType, String key) {
        var textToMatch = Configuration.toString(key);
        String errorMessage = SentinelStringUtils.format(
                "No previously stored text was found for the \"{}\" key.", key);
        assertNotNull(errorMessage, textToMatch);
        verifyTextAppearsInExcelColumn(column, assertion, matchType, textToMatch);
    }

    // ── Value storage ─────────────────────────────────────────────────────────

    /**
     * Reads a cell value and stores it in {@link Configuration} for later retrieval.
     * Example: {@code I store the cell value in the 1st row of the Company column in the excel as storedCompany}
     */
    @Then("^I store the cell value in the (la|\\d+)(?:st|nd|rd|th) row of the (.*?) column in the (?:excel|Excel|EXCEL) as (.*?)$")
    public static void storeExcelValue(String rowNum, String column, String storageKey) {
        ExcelFile file = (ExcelFile) FileManager.getCurrentTestFile();
        int rowIndex = rowNum.equals("la") ? file.getNumberOfDataRows() : Integer.parseInt(rowNum);

        String storageValue;
        if (StringUtils.isNumeric(column.substring(0, 1))) {
            storageValue = file.readCellData(SentinelStringUtils.parseOrdinal(column), rowIndex);
        } else {
            storageValue = file.readCellData(column, rowIndex);
        }
        Configuration.update(storageKey, storageValue);
    }
}
