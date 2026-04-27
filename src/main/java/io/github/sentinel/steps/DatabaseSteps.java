package io.github.sentinel.steps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.sentinel.databases.DatabaseConnection;
import io.github.sentinel.strings.SentinelStringUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Cucumber step definitions for database verification.
 * Requires a JDBC driver for your database (MySQL, PostgreSQL, etc.) on the classpath.
 * Configure connections in sentinel.yml using the db.&lt;name&gt;.url / username / password keys.
 * <p>
 * Example sentinel.yml:
 * <pre>
 *   db.mydb.url: jdbc:postgresql://localhost:5432/testdb
 *   db.mydb.username: testuser
 *   db.mydb.password: testpass
 * </pre>
 */
public class DatabaseSteps {

    /**
     * Opens a database connection using the named configuration from sentinel.yml.
     * <p><b>Gherkin Example:</b> Given I connect to the database named mydb
     * @param name String the database config name
     */
    @Given("^I connect to the database named (.*?)$")
    public static void connectToDatabase(String name) {
        DatabaseConnection.getConnection(name);
    }

    /**
     * Closes the current database connection.
     * <p><b>Gherkin Example:</b> When I close the database connection
     */
    @When("^I close the database connection$")
    public static void closeDatabaseConnection() {
        DatabaseConnection.closeConnection();
    }

    /**
     * Verifies that a table has a row where a column equals a value.
     * <p><b>Gherkin Example:</b> Then I verify the users table has a row where email equals "test@example.com"
     * @param table String the table name
     * @param column String the column name
     * @param value String the expected value
     */
    @Then("^I verify the (.*?) table has a row where (.*?) equals \"(.*?)\"$")
    public static void verifyRowExists(String table, String column, String value) {
        String sql = SentinelStringUtils.format(
                "SELECT COUNT(*) FROM {} WHERE {} = '{}'", table, column, value);
        int count = DatabaseConnection.queryRowCount(sql);
        String msg = SentinelStringUtils.format(
                "Expected at least one row in {} where {} = '{}', but found none.", table, column, value);
        assertTrue(msg, count > 0);
    }

    /**
     * Verifies that a table has a specific row count.
     * <p><b>Gherkin Example:</b> Then I verify the orders table has 5 rows
     * @param table String the table name
     * @param expectedCount int the expected number of rows
     */
    @Then("^I verify the (.*?) table has (\\d+) rows?$")
    public static void verifyRowCount(String table, int expectedCount) {
        String sql = SentinelStringUtils.format("SELECT COUNT(*) FROM {}", table);
        int actual = DatabaseConnection.queryRowCount(sql);
        String msg = SentinelStringUtils.format(
                "Expected {} rows in {}, but found {}.", expectedCount, table, actual);
        assertEquals(msg, expectedCount, actual);
    }

    /**
     * Verifies that a column value in a row matches an expected value.
     * <p><b>Gherkin Example:</b> Then I verify the value of status in users where id equals "1" is "active"
     * @param selectColumn String column to read
     * @param table String table name
     * @param whereColumn String filter column
     * @param whereValue String filter value
     * @param expected String expected value
     */
    @Then("^I verify the value of (.*?) in (.*?) where (.*?) equals \"(.*?)\" is \"(.*?)\"$")
    public static void verifyColumnValue(String selectColumn, String table, String whereColumn, String whereValue, String expected) {
        String sql = SentinelStringUtils.format(
                "SELECT {} FROM {} WHERE {} = '{}'", selectColumn, table, whereColumn, whereValue);
        String actual = DatabaseConnection.querySingleValue(sql);
        String msg = SentinelStringUtils.format(
                "Expected {} in {} where {} = '{}' to be '{}', but was '{}'.",
                selectColumn, table, whereColumn, whereValue, expected, actual);
        assertTrue(msg, StringUtils.equals(expected, actual));
    }

    /**
     * Verifies that a column value in a row is null.
     * <p><b>Gherkin Example:</b> Then I verify the value of deleted_at in users where id equals "1" is null
     * @param selectColumn String column to read
     * @param table String table name
     * @param whereColumn String filter column
     * @param whereValue String filter value
     */
    @Then("^I verify the value of (.*?) in (.*?) where (.*?) equals \"(.*?)\" is null$")
    public static void verifyColumnIsNull(String selectColumn, String table, String whereColumn, String whereValue) {
        String sql = SentinelStringUtils.format(
                "SELECT {} FROM {} WHERE {} = '{}'", selectColumn, table, whereColumn, whereValue);
        String actual = DatabaseConnection.querySingleValue(sql);
        String msg = SentinelStringUtils.format(
                "Expected {} in {} where {} = '{}' to be null, but was '{}'.",
                selectColumn, table, whereColumn, whereValue, actual);
        assertNull(msg, actual);
    }

    /**
     * Verifies a raw SQL query returns the expected single value.
     * <p><b>Gherkin Example:</b> Then I verify the SQL query "SELECT status FROM orders WHERE id=1" returns "shipped"
     * @param sql String the SQL query to execute
     * @param expected String the expected value
     */
    @Then("^I verify the SQL query \"(.*?)\" returns \"(.*?)\"$")
    public static void verifySQLQueryResult(String sql, String expected) {
        String actual = DatabaseConnection.querySingleValue(sql);
        String msg = SentinelStringUtils.format(
                "Expected SQL query to return '{}', but got '{}'.", expected, actual);
        assertTrue(msg, StringUtils.equals(expected, actual));
    }

    /**
     * Verifies that a table has no row matching a column value.
     * <p><b>Gherkin Example:</b> Then I verify the users table has no row where email equals "deleted@example.com"
     * @param table String the table name
     * @param column String the column name
     * @param value String the value that should not exist
     */
    @Then("^I verify the (.*?) table has no row where (.*?) equals \"(.*?)\"$")
    public static void verifyRowDoesNotExist(String table, String column, String value) {
        String sql = SentinelStringUtils.format(
                "SELECT COUNT(*) FROM {} WHERE {} = '{}'", table, column, value);
        int count = DatabaseConnection.queryRowCount(sql);
        String msg = SentinelStringUtils.format(
                "Expected no rows in {} where {} = '{}', but found {}.", table, column, value, count);
        assertEquals(msg, 0, count);
    }
}
