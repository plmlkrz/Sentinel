package io.github.sentinel.files;

import io.github.sentinel.strings.SentinelStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Provides read access to Excel (.xlsx) files for use in Sentinel test scenarios.
 * Mirrors the structure and API of {@link CsvFile} so that step definitions and
 * test patterns are consistent between CSV and Excel handling.
 * <p>
 * Data is loaded into memory on construction as a {@code List<List<String>>}.
 * Row indexing is 1-based and excludes header rows, matching the CSV convention.
 * Column indexing is also 1-based.
 * <p>
 * Only the first worksheet is loaded by default; use the
 * {@link #ExcelFile(Path, int, int)} constructor to specify a different sheet.
 */
@SuppressWarnings("serial")
public class ExcelFile extends TestFile {

    private static final Logger log = LogManager.getLogger(ExcelFile.class.getName());
    public static final String IOEXCEPTION_CAUGHT_WHILE_PARSING_EXCEL_FILE =
            "IOException caught while parsing Excel file {}.";

    private final int numHeaderRows;
    private final int sheetIndex;
    private List<List<String>> excelContents;

    // ── Constructors ──────────────────────────────────────────────────────────

    /**
     * Creates an ExcelFile from the most recently downloaded file with 1 header row.
     *
     * @throws FileNotFoundException if no file has been downloaded in this test session.
     */
    public ExcelFile() throws FileNotFoundException {
        this(1);
    }

    /**
     * Creates an ExcelFile from the specified path with 1 header row.
     *
     * @param pathToFile path to the .xlsx file
     * @throws FileNotFoundException if the file does not exist at the given path
     */
    public ExcelFile(Path pathToFile) throws FileNotFoundException {
        super(pathToFile);
        numHeaderRows = 1;
        sheetIndex = 0;
        loadExcelFile();
    }

    /**
     * Creates an ExcelFile from the most recently downloaded file.
     *
     * @param numberOfHeaderRows number of header rows at the top of the sheet
     * @throws FileNotFoundException if no file has been downloaded in this test session
     */
    public ExcelFile(int numberOfHeaderRows) throws FileNotFoundException {
        super();
        numHeaderRows = numberOfHeaderRows;
        sheetIndex = 0;
        loadExcelFile();
    }

    /**
     * Creates an ExcelFile from the specified path. This is the primary constructor.
     *
     * @param pathToFile         path to the .xlsx file
     * @param numberOfHeaderRows number of header rows at the top of the sheet
     * @throws FileNotFoundException if the file does not exist at the given path
     */
    public ExcelFile(Path pathToFile, int numberOfHeaderRows) throws FileNotFoundException {
        super(pathToFile);
        numHeaderRows = numberOfHeaderRows;
        sheetIndex = 0;
        loadExcelFile();
    }

    /**
     * Creates an ExcelFile from the specified path and sheet index.
     *
     * @param pathToFile         path to the .xlsx file
     * @param numberOfHeaderRows number of header rows at the top of the sheet
     * @param sheetIndex         zero-based index of the worksheet to load
     * @throws FileNotFoundException if the file does not exist at the given path
     */
    public ExcelFile(Path pathToFile, int numberOfHeaderRows, int sheetIndex)
            throws FileNotFoundException {
        super(pathToFile);
        numHeaderRows = numberOfHeaderRows;
        this.sheetIndex = sheetIndex;
        loadExcelFile();
    }

    // ── Loading ───────────────────────────────────────────────────────────────

    private void loadExcelFile() {
        excelContents = readAllFileContents();
    }

    /**
     * Reads all rows from the configured worksheet into memory as strings.
     * Formula cells are evaluated before conversion.
     *
     * @return every row (including header rows) as a list of string values
     */
    public List<List<String>> readAllFileContents() {
        List<List<String>> allFileContents = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(this);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(sheetIndex);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            for (Row row : sheet) {
                List<String> rowData = new ArrayList<>();
                int lastCellNum = row.getLastCellNum();
                for (int colIdx = 0; colIdx < lastCellNum; colIdx++) {
                    Cell cell = row.getCell(colIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    rowData.add(cellToString(cell, evaluator));
                }
                allFileContents.add(rowData);
            }
            return allFileContents;

        } catch (IOException ioe) {
            log.trace(SentinelStringUtils.format(
                    IOEXCEPTION_CAUGHT_WHILE_PARSING_EXCEL_FILE, toPath()));
            return new ArrayList<>();
        }
    }

    private String cellToString(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) return "";

        CellType effectiveType = cell.getCellType();

        if (effectiveType == CellType.FORMULA) {
            CellValue evaluated = evaluator.evaluate(cell);
            return switch (evaluated.getCellType()) {
                case STRING  -> evaluated.getStringValue();
                case NUMERIC -> formatNumericValue(cell, evaluated.getNumberValue());
                case BOOLEAN -> String.valueOf(evaluated.getBooleanValue());
                default      -> "";
            };
        }

        return switch (effectiveType) {
            case STRING  -> cell.getStringCellValue();
            case NUMERIC -> formatNumericValue(cell, cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> "";
        };
    }

    private String formatNumericValue(Cell cell, double value) {
        if (DateUtil.isCellDateFormatted(cell)) {
            return new DataFormatter().formatCellValue(cell);
        }
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    // ── Read methods ──────────────────────────────────────────────────────────

    /**
     * Returns the total number of rows including header rows.
     *
     * @return total row count
     */
    public int getNumberOfTotalRows() {
        return excelContents.size();
    }

    /**
     * Returns the number of data rows (total rows minus header rows).
     *
     * @return data row count
     */
    public int getNumberOfDataRows() {
        return excelContents.size() - numHeaderRows;
    }

    /**
     * Returns the last header row as a list of column name strings.
     *
     * @return header values
     * @throws IndexOutOfBoundsException if the file has no header rows
     */
    public List<String> readHeaders() {
        if (numHeaderRows < 1) {
            throw new IndexOutOfBoundsException(
                    "readHeaders() is undefined for Excel files without header rows.");
        }
        return excelContents.get(numHeaderRows - 1);
    }

    /**
     * Returns the 1-based column index for the given header name.
     *
     * @param columnHeader the column header text to find
     * @return 1-based column index, or 0 if the header is not found
     */
    public int getColumnIndex(String columnHeader) {
        return readHeaders().indexOf(columnHeader) + 1;
    }

    /**
     * Returns a single row of data values. Row 1 is the first data row after headers.
     *
     * @param rowIndex 1-based row index (excludes header rows)
     * @return list of cell values for that row
     */
    public List<String> readRowData(int rowIndex) {
        int actualRowIndex = rowIndex - 1 + numHeaderRows;
        return excelContents.get(actualRowIndex);
    }

    /**
     * Returns the value of a cell by 1-based column and row index.
     *
     * @param columnIndex 1-based column index
     * @param rowIndex    1-based data row index (excludes header rows)
     * @return cell value as a String
     */
    public String readCellData(int columnIndex, int rowIndex) {
        return readRowData(rowIndex).get(columnIndex - 1);
    }

    /**
     * Returns the value of a cell by column header name and 1-based row index.
     *
     * @param columnHeader column header text
     * @param rowIndex     1-based data row index (excludes header rows)
     * @return cell value as a String
     */
    public String readCellData(String columnHeader, int rowIndex) {
        return readCellData(getColumnIndex(columnHeader), rowIndex);
    }

    /**
     * Returns all data cell values in a column by 1-based index.
     *
     * @param columnIndex 1-based column index
     * @return list of cell values (excludes header rows)
     */
    public List<String> readAllCellDataForColumn(int columnIndex) {
        int adjustedColumnIndex = columnIndex - 1;
        List<String> result = new ArrayList<>();
        excelContents.stream().skip(numHeaderRows)
                .forEach(row -> result.add(
                        adjustedColumnIndex < row.size() ? row.get(adjustedColumnIndex) : ""));
        return result;
    }

    /**
     * Returns all data cell values in a column by header name.
     *
     * @param columnHeader column header text
     * @return list of cell values (excludes header rows)
     */
    public List<String> readAllCellDataForColumn(String columnHeader) {
        return readAllCellDataForColumn(getColumnIndex(columnHeader));
    }

    // ── Verification methods ──────────────────────────────────────────────────

    /**
     * Verifies that a column header exists in the header row.
     *
     * @param columnHeader the header name to find
     * @param partialMatch if true, checks whether any header contains the value;
     *                     if false, checks for an exact match
     * @return true if the header is found, false otherwise
     */
    public boolean verifyColumnHeaderEquals(String columnHeader, boolean partialMatch) {
        var headers = readHeaders();
        if (partialMatch)
            return headers.stream().anyMatch(h -> StringUtils.contains(h, columnHeader));
        else
            return headers.stream().anyMatch(h -> StringUtils.equals(h, columnHeader));
    }

    /**
     * Verifies that a cell contains the expected text. Returns null on success (assertion
     * passed) or the actual cell value on failure, consistent with the CSV convention.
     *
     * @param rowIndex     1-based data row index
     * @param columnIndex  1-based column index
     * @param textToMatch  text to check for
     * @param partialMatch if true, uses contains; if false, uses exact match
     * @return null if assertion passes, the actual cell value if it fails
     */
    public String verifyCellDataContains(int rowIndex, int columnIndex,
                                         String textToMatch, boolean partialMatch) {
        var cell = readCellData(columnIndex, rowIndex);
        if (partialMatch)
            return cell.contains(textToMatch) ? null : cell;
        else
            return StringUtils.equals(cell, textToMatch) ? null : cell;
    }

    /**
     * Verifies that a cell contains the expected text, looking up the column by name.
     *
     * @param rowIndex     1-based data row index
     * @param columnHeader column header text
     * @param textToMatch  text to check for
     * @param partialMatch if true, uses contains; if false, uses exact match
     * @return null if assertion passes, the actual cell value if it fails
     */
    public String verifyCellDataContains(int rowIndex, String columnHeader,
                                         String textToMatch, boolean partialMatch) {
        return verifyCellDataContains(rowIndex, getColumnIndex(columnHeader), textToMatch, partialMatch);
    }

    /**
     * Verifies that all data cells in a column match the expected text.
     *
     * @param columnIndex  1-based column index
     * @param textToMatch  text to check for
     * @param partialMatch if true, uses contains; if false, uses exact match
     * @return true if all cells match, false otherwise
     */
    public boolean verifyAllColumnCellsContain(int columnIndex,
                                               String textToMatch, boolean partialMatch) {
        var data = readAllCellDataForColumn(columnIndex);
        if (partialMatch)
            return data.stream().allMatch(c -> StringUtils.contains(c, textToMatch));
        else
            return data.stream().allMatch(c -> StringUtils.equals(c, textToMatch));
    }

    /**
     * Verifies that all data cells in a column match the expected text, looking up
     * the column by name.
     *
     * @param columnHeader column header text
     * @param textToMatch  text to check for
     * @param partialMatch if true, uses contains; if false, uses exact match
     * @return true if all cells match, false otherwise
     */
    public boolean verifyAllColumnCellsContain(String columnHeader,
                                               String textToMatch, boolean partialMatch) {
        return verifyAllColumnCellsContain(getColumnIndex(columnHeader), textToMatch, partialMatch);
    }

    /**
     * Verifies that at least one data cell in a column matches the expected text.
     *
     * @param columnIndex  1-based column index
     * @param textToMatch  text to check for
     * @param partialMatch if true, uses contains; if false, uses exact match
     * @return true if any cell matches, false otherwise
     */
    public boolean verifyAnyColumnCellContains(int columnIndex,
                                               String textToMatch, boolean partialMatch) {
        var data = readAllCellDataForColumn(columnIndex);
        if (partialMatch)
            return data.stream().anyMatch(c -> StringUtils.contains(c, textToMatch));
        else
            return data.stream().anyMatch(c -> StringUtils.equals(c, textToMatch));
    }

    /**
     * Verifies that at least one data cell in a column matches the expected text,
     * looking up the column by name.
     *
     * @param columnHeader column header text
     * @param textToMatch  text to check for
     * @param partialMatch if true, uses contains; if false, uses exact match
     * @return true if any cell matches, false otherwise
     */
    public boolean verifyAnyColumnCellContains(String columnHeader,
                                               String textToMatch, boolean partialMatch) {
        return verifyAnyColumnCellContains(getColumnIndex(columnHeader), textToMatch, partialMatch);
    }

    // ── Object overrides ──────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ExcelFile excelFile = (ExcelFile) o;
        return numHeaderRows == excelFile.numHeaderRows
                && sheetIndex == excelFile.sheetIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), numHeaderRows, sheetIndex);
    }
}
