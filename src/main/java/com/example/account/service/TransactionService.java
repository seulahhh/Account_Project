package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.domain.Transaction;
import com.example.account.dto.TransactionDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.repository.TransactionRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import com.example.account.type.TransactionResultType;
import com.example.account.type.TransactionType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.example.account.type.ErrorCode.*;
import static com.example.account.type.TransactionResultType.F;
import static com.example.account.type.TransactionResultType.S;
import static com.example.account.type.TransactionType.CANCEL;

@Slf4j
@AllArgsConstructor
@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountUserRepository accountUserRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public TransactionDto useBalance(Long userId, String accountNumber, Long amount) {
        AccountUser accountUser = accountUserRepository.findById(userId).orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));
        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new AccountException(ErrorCode.USER_ACCOUNT_NOT_FOUND));

        validateUseBalance(accountUser, account, amount);

        account.useBalance(amount);

        return TransactionDto.fromEntity(saveAndGetTransaction(TransactionType.USE, S, account, amount));
    }

    private void validateUseBalance(AccountUser accountUser, Account account, Long amount) {
        // 사용자가 없는 경우는 accountUser를 찾아오는 과정에서 이미 filtering 되었음

        // [case] 사용자와 계좌의 소유주가 다른 경우
        if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(USER_ACCOUNT_UN_MATCH);
        }

        // [case] 계좌가 이미 해지 상태인 경우
        if (account.getAccountStatus() != AccountStatus.IN_USE) {
            throw new AccountException(ACCOUNT_ALREADY_UNREGISTERED);
        }

        // [case] 거래금액이 잔액보다 큰 경우
        if (account.getBalance() < amount) {
            throw new AccountException(AMOUNT_EXCEED_BALANCE);
        }
    }

    // 개발도중, 혹은 프로그램 실행 도중 바뀔 수 있는 것들은 @Transactional 애노테이션을 달아줌
    @Transactional
    public void saveFailedUseTransaction(String accountNumber, Long amount) {
        Account account =  accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(USER_ACCOUNT_NOT_FOUND));

        saveAndGetTransaction(TransactionType.USE, F, account, amount);
    }

    // Transaction 객체를 get/save 하는 부분을 메서드로 따로 뽑아서 공통화 해줌
    private Transaction saveAndGetTransaction(TransactionType transactionType, TransactionResultType transactionResultType, Account account, Long amount) {
        return transactionRepository.save(
                Transaction.builder()
                        .transactedAt(LocalDateTime.now())
                .transactionType(transactionType)
                .transactionResultType(transactionResultType)
                .account(account)
                .amount(amount)
                .balanceSnapshot(account.getBalance())
                .transactionId(UUID.randomUUID()
                        .toString()
                        .replace("-", ""))
                .build());
    }

    @Transactional
    public TransactionDto cancelBalance(String transactionId, String accountNumber, Long amount) {
        Transaction transaction = transactionRepository
                .findByTransactionId(transactionId).orElseThrow(
                        () -> new AccountException(TRANSACTION_NOT_FOUND));
        Account account = accountRepository
                .findByAccountNumber(accountNumber).orElseThrow(
                        () -> new AccountException(ErrorCode.USER_ACCOUNT_NOT_FOUND));

        validateCancelBalance(transaction, account, amount);

        account.cancelBalance(amount);

        return TransactionDto.fromEntity(
                saveAndGetTransaction(CANCEL, S, account, amount)
        );
    }

    private void validateCancelBalance(Transaction transaction, Account account, Long amount) {
        // [case] 사용자와 계좌의 소유주가 다른 경우
        if (!Objects.equals(transaction.getAccount().getId(), account.getId())) {
            throw new AccountException(TRANSACTION_ACCOUNT_UN_MATCH);
        }

        // [case] 요청으로 받은 취소금액과, 사용했던 금액이 일치하지 않는 경우(== 부분취소 시도)
        if (!Objects.equals(transaction.getAmount(), amount)) {
            throw new AccountException(CANCEL_MUST_FULLY);
        }

        // [case] 거래날짜가 1년이 지난 경우
        if (transaction.getTransactedAt().isBefore(LocalDateTime.now().minusYears(1)) ) {
            throw new AccountException(TOO_OLD_ORDER_TO_CANCEL);
        }
        // ? isBefore: LocalDateTime의 기능. parameter로 넣어주는 시간이 LocalDateTime객체보다
        // ? 이전인지 아닌지 확인하는 메서
    }

    @Transactional
    public void saveFailedCancelTransaction(String accountNumber, Long amount) {
        Account account =  accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(USER_ACCOUNT_NOT_FOUND));

        saveAndGetTransaction(CANCEL, F, account, amount);
    }

    public TransactionDto queryTransaction(String transactionId) {

        return TransactionDto.fromEntity(
            transactionRepository
                .findByTransactionId(transactionId).orElseThrow(
                    () -> new AccountException(TRANSACTION_NOT_FOUND))
        );
    }
}
