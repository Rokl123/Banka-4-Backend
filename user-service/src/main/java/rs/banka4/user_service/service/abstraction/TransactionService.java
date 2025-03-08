package rs.banka4.user_service.service.abstraction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import rs.banka4.user_service.domain.transaction.dtos.TransactionDto;
import rs.banka4.user_service.domain.transaction.db.TransactionStatus;
import rs.banka4.user_service.domain.transaction.dtos.CreatePaymentDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface TransactionService {
    TransactionDto createPayment(Authentication authentication, CreatePaymentDto createPaymentDto);
    TransactionDto createTransfer(Authentication authentication, CreatePaymentDto createPaymentDto);
    Page<TransactionDto> getAllTransactionsForClient(String token, TransactionStatus paymentStatus, BigDecimal amount, LocalDate paymentDate, String accountNumber, PageRequest pageRequest);
    TransactionDto getTransactionById(String token, UUID transactionId);
}
