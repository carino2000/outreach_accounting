package outreach_accounting.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReceiptManualRequestDto {

    private String name;
    private String team;
    private String title;
    private Integer amount;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime usedAt;

    private String description;

    private MultipartFile image;
}
