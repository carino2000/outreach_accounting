package outreach_accounting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import outreach_accounting.entity.Receipt;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
}
