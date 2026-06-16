package outreach_accounting.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ClaudeService {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${anthropic.api-key}")
    private String apiKey;

    @Value("${anthropic.model:claude-sonnet-4-6}")
    private String model;

    public ReceiptAnalysisResult analyze(MultipartFile image, String description) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(image.getBytes());
            String mediaType = resolveMediaType(image.getContentType());

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "max_tokens", 1024,
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", List.of(
                                    Map.of(
                                            "type", "image",
                                            "source", Map.of(
                                                    "type", "base64",
                                                    "media_type", mediaType,
                                                    "data", base64Image
                                            )
                                    ),
                                    Map.of(
                                            "type", "text",
                                            "text", buildPrompt(description)
                                    )
                            )
                    ))
            );

            String responseBody = restClient.post()
                    .uri(API_URL)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseAnalysisResult(responseBody);
        } catch (Exception e) {
            log.error("Claude 영수증 분석 실패, 기본값으로 대체합니다.", e);
            return new ReceiptAnalysisResult(description, 0, LocalDateTime.now());
        }
    }

    private String buildPrompt(String description) {
        return """
                다음 영수증 이미지를 분석해서 아래 JSON 형식으로만 응답해.
                다른 설명 없이 JSON 객체 하나만 출력해.

                {"title": "지출 항목명", "amount": 숫자(원 단위, 쉼표/단위 없이), "usedAt": "yyyy-MM-dd"}

                참고할 사용자 설명: %s
                """.formatted(description == null ? "" : description);
    }

    private String resolveMediaType(String contentType) {
        if (contentType == null) {
            return "image/jpeg";
        }
        return switch (contentType) {
            case "image/png" -> "image/png";
            case "image/jpeg", "image/jpg" -> "image/jpeg";
            default -> "image/jpeg";
        };
    }

    private ReceiptAnalysisResult parseAnalysisResult(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String text = root.path("content").path(0).path("text").asText("");

        String json = extractJsonObject(text);
        JsonNode parsed = objectMapper.readTree(json);

        String title = parsed.path("title").asText(null);
        Integer amount = parsed.path("amount").isMissingNode() ? null : parsed.path("amount").asInt();
        LocalDateTime usedAt = parseUsedAt(parsed.path("usedAt").asText(null));

        return new ReceiptAnalysisResult(
                title != null ? title : "영수증",
                amount != null ? amount : 0,
                usedAt != null ? usedAt : LocalDateTime.now()
        );
    }

    private String extractJsonObject(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start == -1 || end == -1 || end < start) {
            return "{}";
        }
        return text.substring(start, end + 1);
    }

    private LocalDateTime parseUsedAt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ignored) {
            // fall through to date-only parsing
        }
        try {
            return LocalDate.parse(value).atStartOfDay();
        } catch (Exception ignored) {
            return null;
        }
    }
}
