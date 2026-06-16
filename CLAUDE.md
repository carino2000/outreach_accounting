# outreach_accounting - Backend CLAUDE.md

## 프로젝트 개요
아웃리치 활동 회계 관리 웹앱의 백엔드 서버.
영수증 사진 + 설명을 입력받아 Claude AI로 분석 후 Google Sheets에 자동 기록.

---

## 기술 스택
- **Language**: Java 17+
- **Framework**: Spring Boot
- **Build Tool**: Maven
- **Database**: MySQL
- **ORM**: Spring Data JPA
- **Port**: 8081

---

## 외부 연동
- **Google Sheets API v4** — 회계 내역 기록
- **Claude API (Anthropic)** — 영수증 이미지 분석 (항목, 금액, 날짜 추출)

---

## application.yml 구조

```yaml
spring:
  application:
    name: outreach_accounting
  devtools:
    restart:
      enabled: true
  datasource:
    url: jdbc:mysql://localhost:3306/outreach_accounting
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: qowlgns9231
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: true
  jackson:
    default-property-inclusion: NON_NULL

server:
  port: 8081

google:
  sheets:
    client-id: YOUR_CLIENT_ID
    client-secret: YOUR_CLIENT_SECRET
    refresh-token: YOUR_REFRESH_TOKEN
    spreadsheet-id: 1NoQwppfyff5clBi4a5gvaZrWJM9RF2807xjcl7tXPPw
```

---

## Maven 의존성 (pom.xml에 추가된 것)

```xml
<!-- Google Sheets API -->
<dependency>
    <groupId>com.google.api-client</groupId>
    <artifactId>google-api-client</artifactId>
    <version>2.2.0</version>
</dependency>
<dependency>
    <groupId>com.google.oauth-client</groupId>
    <artifactId>google-oauth-client-jetty</artifactId>
    <version>1.34.1</version>
</dependency>
<dependency>
    <groupId>com.google.apis</groupId>
    <artifactId>google-api-services-sheets</artifactId>
    <version>v4-rev20220927-2.0.0</version>
</dependency>
```

---

## DB 스키마 (MySQL: outreach_accounting)

```sql
-- 영수증 내역 테이블
CREATE TABLE receipt (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(255),           -- AI가 추출한 항목명
    amount      INT NOT NULL,           -- 금액 (원)
    used_at     DATE,                   -- 사용 날짜
    description TEXT,                   -- 사용자가 입력한 설명
    image_path  VARCHAR(500),           -- 업로드된 영수증 이미지 경로
    category    VARCHAR(100),           -- 분류 (식비, 교통비 등)
    sheets_synced BOOLEAN DEFAULT FALSE, -- Sheets 기록 여부
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## 패키지 구조

```
src/main/java/com/example/outreach_accounting/
├── config/
│   └── GoogleSheetsConfig.java       # Sheets API 인증 설정
├── controller/
│   └── ReceiptController.java        # REST API 엔드포인트
├── service/
│   ├── ReceiptService.java           # 비즈니스 로직
│   ├── GoogleSheetsService.java      # Sheets 행 추가 로직
│   └── ClaudeService.java            # Claude API 호출 (영수증 분석)
├── entity/
│   └── Receipt.java                  # JPA 엔티티
├── repository/
│   └── ReceiptRepository.java        # JPA Repository
└── dto/
    ├── ReceiptRequestDto.java         # 클라이언트 요청 DTO
    └── ReceiptResponseDto.java        # 응답 DTO
```

---

## API 엔드포인트 설계

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/receipts` | 영수증 등록 (이미지 + 설명 업로드) |
| GET | `/api/receipts` | 전체 내역 조회 |
| GET | `/api/receipts/{id}` | 단건 조회 |
| DELETE | `/api/receipts/{id}` | 내역 삭제 |
| POST | `/api/receipts/{id}/sync` | 특정 내역을 Sheets에 수동 동기화 |

---

## Google Sheets 연동 흐름

```
1. 사용자가 영수증 이미지 + 설명 업로드
2. ClaudeService → Claude API로 이미지 분석 (항목, 금액, 날짜 추출)
3. ReceiptService → MySQL에 저장
4. GoogleSheetsService → Sheets에 행 추가
   - 기록 열 순서: [날짜, 항목, 금액, 분류, 설명, 등록일시]
```

---

## Google Sheets 인증 방식
- OAuth2 방식 (refresh token 기반)
- 최초 인증: OAuth 2.0 Playground에서 refresh token 발급 완료
- 이후: refresh token → access token 자동 갱신

### GoogleSheetsConfig.java 핵심 로직
```java
@Configuration
public class GoogleSheetsConfig {

    @Value("${google.sheets.client-id}")
    private String clientId;

    @Value("${google.sheets.client-secret}")
    private String clientSecret;

    @Value("${google.sheets.refresh-token}")
    private String refreshToken;

    @Bean
    public Sheets sheetsService() throws Exception {
        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(GoogleNetHttpTransport.newTrustedTransport())
                .setJsonFactory(GsonFactory.getDefaultInstance())
                .setClientSecrets(clientId, clientSecret)
                .build()
                .setRefreshToken(refreshToken);

        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("outreach-accounting")
                .build();
    }
}
```

---

## 스프레드시트 정보
- **이름**: 2026 한남제일 아웃리치
- **ID**: `1NoQwppfyff5clBi4a5gvaZrWJM9RF2807xjcl7tXPPw`
- **대상 시트**: `회계장부` 탭
- **기록 시작 행**: 데이터가 있는 마지막 행 다음에 append

---

## 이미지 업로드
- 저장 경로: `src/main/resources/static/uploads/` 또는 외부 경로
- 파일명: UUID로 rename하여 중복 방지
- 허용 확장자: jpg, jpeg, png
- Multipart 설정 필요 (application.yml에 추가):

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

---

## CORS 설정
프론트엔드(Next.js)가 `http://localhost:3000`에서 실행되므로 CORS 허용 필요.

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "DELETE");
    }
}
```

---

## 대화 진행 규칙 (Claude 작업 흐름)
- 사용자가 **"작업 시작하자"**라고 말하면 → 이전에 진행했던 작업을 간단히 요약해서 알려준다.
- 사용자가 **"작업 마무리해"**라고 말하면 → 오늘 진행했던 작업을 일기처럼 정리해서 작성한다.

## 주의사항
- `application.yml`의 민감 정보(client-secret, refresh-token, DB password)는 `.gitignore`에 추가할 것
- refresh token은 6개월 미사용 시 만료되므로 정기적으로 앱 사용 필요
- Google Sheets API 무료 할당량: 분당 300회 (교회 회계 용도로는 충분)
