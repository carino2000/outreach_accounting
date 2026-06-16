package outreach_accounting.dto;

import lombok.Builder;
import lombok.Getter;
import outreach_accounting.entity.Receipt;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReceiptResponseDto {

    private Long id;
    private String name;
    private String team;
    private String title;
    private Integer amount;
    private LocalDateTime usedAt;
    private String description;
    private String imagePath;
    private Boolean sheetsSynced;
    private LocalDateTime createdAt;

    public static ReceiptResponseDto from(Receipt receipt) {
        return ReceiptResponseDto.builder()
                .id(receipt.getId())
                .name(receipt.getName())
                .team(receipt.getTeam())
                .title(receipt.getTitle())
                .amount(receipt.getAmount())
                .usedAt(receipt.getUsedAt())
                .description(receipt.getDescription())
                .imagePath(receipt.getImagePath())
                .sheetsSynced(receipt.getSheetsSynced())
                .createdAt(receipt.getCreatedAt())
                .build();
    }
}
