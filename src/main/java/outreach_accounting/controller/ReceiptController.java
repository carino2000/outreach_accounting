package outreach_accounting.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import outreach_accounting.dto.ReceiptManualRequestDto;
import outreach_accounting.dto.ReceiptRequestDto;
import outreach_accounting.dto.ReceiptResponseDto;
import outreach_accounting.service.ReceiptService;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    @PostMapping
    public ResponseEntity<ReceiptResponseDto> create(@ModelAttribute ReceiptRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(receiptService.create(request));
    }

    @PostMapping("/manual")
    public ResponseEntity<ReceiptResponseDto> createManual(@ModelAttribute ReceiptManualRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(receiptService.createManual(request));
    }

    @GetMapping
    public ResponseEntity<List<ReceiptResponseDto>> getAll() {
        return ResponseEntity.ok(receiptService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReceiptResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(receiptService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        receiptService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<ReceiptResponseDto> sync(@PathVariable Long id) {
        return ResponseEntity.ok(receiptService.syncToSheets(id));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
