package com.tracker.MoneyTracker.transaction;

import com.opencsv.CSVReader;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.*;

@Component
public class FileParser {

    public List<Map<String, String>> parseExcel(File file) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File must not be null and must exist");
        }

        List<List<String>> allRows = new ArrayList<>();
        try (InputStream is = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                List<String> rowData = new ArrayList<>();
                short lastCellNum = row.getLastCellNum();
                for (int cn = 0; cn < lastCellNum; cn++) {
                    Cell cell = row.getCell(cn);
                    rowData.add(getCellValueAsString(cell));
                }
                allRows.add(rowData);
            }
        }

        return processRows(allRows);
    }

    public List<Map<String, String>> parseCsv(File file) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File must not be null and must exist");
        }

        List<List<String>> allRows = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                List<String> row = new ArrayList<>();
                for (String val : nextLine) {
                    row.add(val);
                }
                allRows.add(row);
            }
        }

        return processRows(allRows);
    }

    public int detectHeaderRow(List<List<String>> rows) {
        if (rows == null || rows.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            for (String cell : row) {
                if (cell != null && cell.trim().equalsIgnoreCase("date")) {
                    return i;
                }
            }
        }
        return -1;
    }

    public String normalizeHeader(String header) {
        if (header == null || header.trim().isEmpty()) {
            return "";
        }
        String clean = header.trim().toLowerCase();
        if (clean.equals("date")) {
            return "date";
        }
        if (clean.contains("ref") || clean.contains("cheque")) {
            return "ref_no";
        }
        if (clean.equals("debit") || clean.contains("withdrawal")) {
            return "debit";
        }
        if (clean.equals("credit") || clean.contains("deposit")) {
            return "credit";
        }
        if (clean.equals("balance")) {
            return "balance";
        }
        if (clean.equals("details") || clean.contains("particulars") || clean.contains("narration") || clean.contains("description")) {
            return "details";
        }
        return clean.replaceAll("[^a-z0-9_]+", "_");
    }

    private List<Map<String, String>> processRows(List<List<String>> allRows) {
        int headerIndex = detectHeaderRow(allRows);
        if (headerIndex == -1 || headerIndex >= allRows.size() - 1) {
            return new ArrayList<>();
        }

        List<String> headerRow = allRows.get(headerIndex);
        List<String> normalizedHeaders = new ArrayList<>();
        for (String cell : headerRow) {
            normalizedHeaders.add(normalizeHeader(cell));
        }

        List<Map<String, String>> result = new ArrayList<>();
        for (int i = headerIndex + 1; i < allRows.size(); i++) {
            List<String> rowData = allRows.get(i);

            // Skip empty rows
            boolean isEmpty = true;
            for (String val : rowData) {
                if (val != null && !val.trim().isEmpty()) {
                    isEmpty = false;
                    break;
                }
            }
            if (isEmpty) {
                continue;
            }

            Map<String, String> rowMap = new LinkedHashMap<>();
            for (int j = 0; j < normalizedHeaders.size(); j++) {
                String headerName = normalizedHeaders.get(j);
                if (headerName != null && !headerName.isEmpty()) {
                    String val = j < rowData.size() ? rowData.get(j) : "";
                    rowMap.put(headerName, val);
                }
            }

            if (isFooterRow(rowMap)) {
                continue;
            }

            result.add(rowMap);
        }

        return result;
    }

    private boolean isFooterRow(Map<String, String> rowMap) {
        return false;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    java.util.Date date = cell.getDateCellValue();
                    if (date != null) {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
                        return sdf.format(date);
                    }
                } else {
                    DataFormatter df = new DataFormatter();
                    return df.formatCellValue(cell);
                }
                return "";
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (Exception ex) {
                        return "";
                    }
                }
            case BLANK:
                return "";
            default:
                return "";
        }
    }
}

