package outreach_accounting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import outreach_accounting.dto.ReceiptManualRequestDto;
import outreach_accounting.dto.ReceiptRequestDto;
import outreach_accounting.dto.ReceiptResponseDto;
import outreach_accounting.entity.Receipt;
import outreach_accounting.repository.ReceiptRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    private final ReceiptRepository receiptRepository;
    private final ClaudeService claudeService;
    private final GoogleSheetsService googleSheetsService;

    @Value("${app.upload-dir:src/main/resources/static/uploads/}")
    private String uploadDir;

    @Transactional
    public ReceiptResponseDto create(ReceiptRequestDto request) {
        String imagePath = saveImage(request.getImage());

        ReceiptAnalysisResult analysis = claudeService.analyze(request.getImage(), request.getDescription());

        Receipt receipt = Receipt.builder()
                .name(request.getName())
                .team(request.getTeam())
                .title(analysis.title())
                .amount(analysis.amount())
                .usedAt(analysis.usedAt())
                .description(request.getDescription())
                .imagePath(imagePath)
                .sheetsSynced(false)
                .build();

        receipt = receiptRepository.save(receipt);
        trySyncToSheets(receipt);

        return ReceiptResponseDto.from(receipt);
    }

    @Transactional
    public ReceiptResponseDto createManual(ReceiptManualRequestDto request) {
        validateManualRequest(request);

        MultipartFile image = request.getImage();
        String imagePath = (image != null && !image.isEmpty()) ? saveImage(image) : null;

        Receipt receipt = Receipt.builder()
                .name(request.getName())
                .team(request.getTeam())
                .title(request.getTitle())
                .amount(request.getAmount())
                .usedAt(request.getUsedAt())
                .description(request.getDescription())
                .imagePath(imagePath)
                .sheetsSynced(false)
                .build();

        receipt = receiptRepository.save(receipt);
        trySyncToSheets(receipt);

        return ReceiptResponseDto.from(receipt);
    }

    public List<ReceiptResponseDto> getAll() {
        return receiptRepository.findAll().stream()
                .map(ReceiptResponseDto::from)
                .toList();
    }

    public ReceiptResponseDto getById(Long id) {
        return ReceiptResponseDto.from(findReceipt(id));
    }

    public void delete(Long id) {
        if (!receiptRepository.existsById(id)) {
            throw new NoSuchElementException("영수증을 찾을 수 없습니다: id=" + id);
        }
        receiptRepository.deleteById(id);
    }

    @Transactional
    public ReceiptResponseDto syncToSheets(Long id) {
        Receipt receipt = findReceipt(id);
        if (!Boolean.TRUE.equals(receipt.getSheetsSynced())) {
            trySyncToSheets(receipt);
        }
        return ReceiptResponseDto.from(receipt);
    }

    private void trySyncToSheets(Receipt receipt) {
        try {
            googleSheetsService.appendReceipt(receipt);
            receipt.setSheetsSynced(true);
            receiptRepository.save(receipt);
        } catch (Exception e) {
            log.error("Google Sheets 동기화 실패: receiptId={}", receipt.getId(), e);
        }
    }

    private void validateManualRequest(ReceiptManualRequestDto request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("이름은 필수입니다.");
        }
        if (request.getAmount() == null) {
            throw new IllegalArgumentException("금액은 필수입니다.");
        }
        if (request.getUsedAt() == null) {
            throw new IllegalArgumentException("사용 날짜는 필수입니다.");
        }
    }

    private Receipt findReceipt(Long id) {
        return receiptRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("영수증을 찾을 수 없습니다: id=" + id));
    }

    private String saveImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("영수증 이미지가 필요합니다.");
        }

        String extension = extractExtension(image.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다: " + extension);
        }

        try {
            Path uploadPath = Path.of(uploadDir);
            Files.createDirectories(uploadPath);

            String fileName = UUID.randomUUID() + "." + extension;
            Path targetPath = uploadPath.resolve(fileName);
            image.transferTo(targetPath);

            return targetPath.toString();
        } catch (IOException e) {
            throw new IllegalStateException("영수증 이미지 저장에 실패했습니다.", e);
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("파일 확장자를 확인할 수 없습니다.");
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1);
    }
}
