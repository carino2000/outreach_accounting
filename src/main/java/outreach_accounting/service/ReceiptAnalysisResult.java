package outreach_accounting.service;

import java.time.LocalDateTime;

public record ReceiptAnalysisResult(String title, Integer amount, LocalDateTime usedAt) {
}
