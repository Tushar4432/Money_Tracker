package com.tracker.MoneyTracker.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileParser")
class FileParserTest {

    private FileParser sut;

    @BeforeEach
    void setUp() {
        sut = new FileParser();
    }

    @Nested
    @DisplayName("parseExcel")
    class ParseExcelTests {

        @Test
        @DisplayName("Should parse Excel file and return transactions when valid file provided")
        void shouldParseExcelFile_WhenValidFileProvided() throws Exception {
            // Arrange
            File file = new File("src/test/resources/test-statement.xlsx");

            // Act
            List<Map<String, String>> result = sut.parseExcel(file);

            // Assert
            assertThat(result).isNotEmpty();
            assertThat(result).hasSize(323);
        }

        @Test
        void inspectExcel() throws Exception {
            File file = new File("src/test/resources/test-statement.xlsx");
            try (java.io.InputStream is = new java.io.FileInputStream(file);
                 org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(is)) {
                org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
                int idx = 0;
                for (org.apache.poi.ss.usermodel.Row row : sheet) {
                    java.util.List<String> rowData = new java.util.ArrayList<>();
                    for (int cn = 0; cn < row.getLastCellNum(); cn++) {
                        org.apache.poi.ss.usermodel.Cell cell = row.getCell(cn);
                        rowData.add(cell == null ? "" : cell.toString());
                    }
                    System.out.println("RAW_ROW " + idx + ": " + rowData);
                    idx++;
                }
            }
        }

        @Test
        @DisplayName("Should auto-detect header row when header is not in first row")
        void shouldAutoDetectHeaderRow_WhenHeaderNotInFirstRow() throws Exception {
            // Arrange
            File file = new File("src/test/resources/test-statement.xlsx");

            // Act
            List<Map<String, String>> result = sut.parseExcel(file);

            // Assert
            assertThat(result).isNotEmpty();
            // First transaction should have parsed date, not header text
            Map<String, String> first = result.get(0);
            assertThat(first).containsKey("date");
            assertThat(first.get("date")).isNotEqualTo("Date");
        }

        @Test
        @DisplayName("Should map Date column correctly")
        void shouldMapDateColumnCorrectly() throws Exception {
            // Arrange
            File file = new File("src/test/resources/test-statement.xlsx");

            // Act
            List<Map<String, String>> result = sut.parseExcel(file);

            // Assert
            Map<String, String> first = result.get(0);
            assertThat(first.get("date")).isEqualTo("01/03/2026");
        }

        @Test
        @DisplayName("Should map Details column correctly")
        void shouldMapDetailsColumnCorrectly() throws Exception {
            // Arrange
            File file = new File("src/test/resources/test-statement.xlsx");

            // Act
            List<Map<String, String>> result = sut.parseExcel(file);

            // Assert
            Map<String, String> first = result.get(0);
            assertThat(first.get("details")).isNotBlank();
            assertThat(first.get("details")).contains("UPI");
        }

        @Test
        @DisplayName("Should map Debit column correctly")
        void shouldMapDebitColumnCorrectly() throws Exception {
            // Arrange
            File file = new File("src/test/resources/test-statement.xlsx");

            // Act
            List<Map<String, String>> result = sut.parseExcel(file);

            // Assert
            Map<String, String> first = result.get(0);
            assertThat(first.get("debit")).isNotBlank();
        }

        @Test
        @DisplayName("Should map Credit column correctly")
        void shouldMapCreditColumnCorrectly() throws Exception {
            // Arrange
            File file = new File("src/test/resources/test-statement.xlsx");

            // Act
            List<Map<String, String>> result = sut.parseExcel(file);

            // Assert — find a row with credit value
            Map<String, String> creditRow = result.stream()
                    .filter(r -> r.get("credit") != null && !r.get("credit").isBlank())
                    .findFirst()
                    .orElseThrow();
            assertThat(creditRow.get("credit")).isNotBlank();
        }

        @Test
        @DisplayName("Should normalize column names to lowercase with underscores")
        void shouldNormalizeColumnNames() throws Exception {
            // Arrange
            File file = new File("src/test/resources/test-statement.xlsx");

            // Act
            List<Map<String, String>> result = sut.parseExcel(file);

            // Assert
            Map<String, String> first = result.get(0);
            assertThat(first.keySet()).allMatch(key -> key.equals(key.toLowerCase()));
        }

        @Test
        @DisplayName("Should skip empty rows")
        void shouldSkipEmptyRows() throws Exception {
            // Arrange
            File file = new File("src/test/resources/test-statement.xlsx");

            // Act
            List<Map<String, String>> result = sut.parseExcel(file);

            // Assert — no row should have all blank values
            assertThat(result).allMatch(row ->
                    row.values().stream().anyMatch(v -> v != null && !v.isBlank())
            );
        }

        @Test
        @DisplayName("Should skip summary/footer rows at end of file")
        void shouldSkipSummaryFooterRows() throws Exception {
            // Arrange
            File file = new File("src/test/resources/test-statement.xlsx");

            // Act
            List<Map<String, String>> result = sut.parseExcel(file);

            // Assert — last row should not contain summary text like "CR" balance
            Map<String, String> last = result.get(result.size() - 1);
            assertThat(last.get("date")).doesNotContain("CR");
        }

        @Test
        @DisplayName("Should throw exception when file is null")
        void shouldThrowException_WhenFileIsNull() {
            assertThatThrownBy(() -> sut.parseExcel(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should throw exception when file does not exist")
        void shouldThrowException_WhenFileDoesNotExist() {
            File file = new File("src/test/resources/nonexistent.xlsx");
            assertThatThrownBy(() -> sut.parseExcel(file))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("parseCsv")
    class ParseCsvTests {

        @Test
        @DisplayName("Should parse CSV file and return transactions when valid file provided")
        void shouldParseCsvFile_WhenValidFileProvided() throws Exception {
            // Arrange — create a minimal CSV for testing
            java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("test", ".csv");
            java.nio.file.Files.writeString(tempFile,
                    "Date,Details,Debit,Credit\n01/01/2026,Test Payment,100.00,\n");

            // Act
            List<Map<String, String>> result = sut.parseCsv(tempFile.toFile());

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).get("date")).isEqualTo("01/01/2026");

            // Cleanup
            java.nio.file.Files.deleteIfExists(tempFile);
        }

        @Test
        @DisplayName("Should auto-detect header row in CSV")
        void shouldAutoDetectHeaderRow_InCsv() throws Exception {
            // Arrange
            java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("test", ".csv");
            java.nio.file.Files.writeString(tempFile,
                    "Some preamble line\nDate,Details,Debit,Credit\n01/01/2026,Test,100.00,\n");

            // Act
            List<Map<String, String>> result = sut.parseCsv(tempFile.toFile());

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).get("date")).isEqualTo("01/01/2026");

            // Cleanup
            java.nio.file.Files.deleteIfExists(tempFile);
        }

        @Test
        @DisplayName("Should throw exception when CSV file is null")
        void shouldThrowException_WhenCsvFileIsNull() {
            assertThatThrownBy(() -> sut.parseCsv(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("detectHeaderRow")
    class DetectHeaderRowTests {

        @Test
        @DisplayName("Should detect header row containing date-like keyword")
        void shouldDetectHeaderRow_ContainingDateKeyword() {
            // Arrange
            List<List<String>> rows = List.of(
                    List.of("", "", "", "", "", ""),
                    List.of("Date", "Details", "Ref No", "Debit", "Credit", "Balance"),
                    List.of("01/01/2026", "Test", "", "100.00", "", "1000.00")
            );

            // Act
            int headerRow = sut.detectHeaderRow(rows);

            // Assert
            assertThat(headerRow).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return -1 when no header row found")
        void shouldReturnMinus1_WhenNoHeaderFound() {
            // Arrange
            List<List<String>> rows = List.of(
                    List.of("1", "2", "3"),
                    List.of("4", "5", "6")
            );

            // Act
            int headerRow = sut.detectHeaderRow(rows);

            // Assert
            assertThat(headerRow).isEqualTo(-1);
        }
    }

    @Nested
    @DisplayName("normalizeHeader")
    class NormalizeHeaderTests {

        @Test
        @DisplayName("Should normalize 'Date' to 'date'")
        void shouldNormalizeDate() {
            assertThat(sut.normalizeHeader("Date")).isEqualTo("date");
        }

        @Test
        @DisplayName("Should normalize 'Ref No/Cheque No' to 'ref_no'")
        void shouldNormalizeRefNo() {
            assertThat(sut.normalizeHeader("Ref No/Cheque No")).isEqualTo("ref_no");
        }

        @Test
        @DisplayName("Should normalize 'Debit' to 'debit'")
        void shouldNormalizeDebit() {
            assertThat(sut.normalizeHeader("Debit")).isEqualTo("debit");
        }

        @Test
        @DisplayName("Should normalize 'Credit' to 'credit'")
        void shouldNormalizeCredit() {
            assertThat(sut.normalizeHeader("Credit")).isEqualTo("credit");
        }

        @Test
        @DisplayName("Should normalize 'Balance' to 'balance'")
        void shouldNormalizeBalance() {
            assertThat(sut.normalizeHeader("Balance")).isEqualTo("balance");
        }

        @Test
        @DisplayName("Should normalize 'Details' to 'details'")
        void shouldNormalizeDetails() {
            assertThat(sut.normalizeHeader("Details")).isEqualTo("details");
        }

        @Test
        @DisplayName("Should return empty string when header is null")
        void shouldReturnEmpty_WhenHeaderIsNull() {
            assertThat(sut.normalizeHeader(null)).isEmpty();
        }

        @Test
        @DisplayName("Should return empty string when header is blank")
        void shouldReturnEmpty_WhenHeaderIsBlank() {
            assertThat(sut.normalizeHeader("  ")).isEmpty();
        }
    }
}
