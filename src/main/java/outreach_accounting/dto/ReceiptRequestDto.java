package outreach_accounting.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class ReceiptRequestDto {

    private String name;

    private String team;

    private String description;

    private MultipartFile image;
}
