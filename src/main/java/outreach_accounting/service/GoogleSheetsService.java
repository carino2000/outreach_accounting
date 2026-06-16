package outreach_accounting.service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import outreach_accounting.entity.Receipt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoogleSheetsService {

    private static final String SHEET_NAME = "★ 회계장부";
    private static final String DATA_START_ROW = "9";
    private static final String COLUMN_A_RANGE = SHEET_NAME + "!A" + DATA_START_ROW + ":A";
    private static final String APPEND_RANGE = SHEET_NAME + "!A" + DATA_START_ROW;

    private final Sheets sheetsService;

    @Value("${google.sheets.spreadsheet-id}")
    private String spreadsheetId;

    public void appendReceipt(Receipt receipt) throws IOException {
        int nextNumber = findNextRowNumber();

        List<Object> row = new ArrayList<>();
        row.add(nextNumber);
        row.add(receipt.getUsedAt().getMonthValue());
        row.add(receipt.getUsedAt().getDayOfMonth());
        row.add(receipt.getTitle() != null ? receipt.getTitle() : "");
        row.add(receipt.getId());
        row.add("");
        row.add(receipt.getAmount());
        row.add("");
        row.add(receipt.getDescription() != null ? receipt.getDescription() : "");

        ValueRange valueRange = new ValueRange().setValues(List.of(row));

        sheetsService.spreadsheets().values()
                .append(spreadsheetId, APPEND_RANGE, valueRange)
                .setValueInputOption("USER_ENTERED")
                .setInsertDataOption("INSERT_ROWS")
                .execute();
    }

    private int findNextRowNumber() throws IOException {
        ValueRange existing = sheetsService.spreadsheets().values()
                .get(spreadsheetId, COLUMN_A_RANGE)
                .execute();

        List<List<Object>> values = existing.getValues();
        int filledRows = values == null ? 0 : values.size();
        return filledRows + 1;
    }
}
