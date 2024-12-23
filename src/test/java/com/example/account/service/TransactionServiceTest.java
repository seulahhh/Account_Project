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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.account.type.AccountStatus.IN_USE;
import static com.example.account.type.TransactionResultType.F;
import static com.example.account.type.TransactionResultType.S;
import static com.example.account.type.TransactionType.CANCEL;
import static com.example.account.type.TransactionType.USE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void successUseBalance() {
        //given
        AccountUser pobi = AccountUser.builder().id(12L).name("Pobi").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));

        Account account = Account.builder()
                .accountUser(pobi)
                .accountStatus(IN_USE)
                .balance(30000L)
                .accountNumber("1000000012")
                .build();

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any())).willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResultType(S)
                        .transactionId("transactionId")
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                .build()
        );

        //when
        TransactionDto transactionDto = transactionService.useBalance(1L, "1000000000", 1000L);

        ArgumentCaptor<Transaction> captor =
                ArgumentCaptor.forClass(Transaction.class);


        //then
        Transaction transaction = verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(29000L, captor.getValue().getBalanceSnapshot());

        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(9000L, transactionDto.getBalanceSnapshot());
        assertEquals(USE, transactionDto.getTransactionType());
    }

    @Test
    @DisplayName("해당하는 유저가 없을때 - 금액 사용 실패")
    void useBalance_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong())).willReturn(Optional.empty());

        //when
        AccountException accountException =
                assertThrows(AccountException.class,
                        () -> transactionService.useBalance(1L, "1000000000", 1000L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("검색한 사용자의 계좌가 없을때 - 잔액 사용 실패")
    void useBalance_userAccountNotFound() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .id(12L).name("Pobi").build();
        given(accountUserRepository.findById(any())).willReturn(Optional.of(accountUser));
        given(accountRepository.findByAccountNumber(anyString())).willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 1000L));

        //then
        assertEquals(ErrorCode.USER_ACCOUNT_NOT_FOUND,
                exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주가 다를때 - 잔액 사용 실패")
    void useBalance_userAccountUnMatch() {
        //given
        AccountUser userPobi = AccountUser.builder()
                .id(12L).name("Pobi").build();
        AccountUser userHerry = AccountUser.builder()
                .id(13L).name("Herry").build();
        given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(userPobi));
        given(accountRepository.findByAccountNumber(anyString())).willReturn(Optional.of(
                Account.builder()
                        .accountUser(userHerry)
                        .balance(0L)
                        .accountNumber("1000000012")
                        .build()
        ));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 1000L));

        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH,
                exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌가 해지 상태인 경우 - 잔액 사용 실패")
    void useBalance_accountAlreadyUnregisterd() {
        //given
        AccountUser userPobi = AccountUser.builder()
                .id(12L).name("Pobi").build();

        given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(userPobi));
        given(accountRepository.findByAccountNumber(anyString())).willReturn(Optional.of(
                Account.builder()
                        .accountUser(userPobi)
                        .balance(0L)
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .accountNumber("1000000012")
                        .build()
        ));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 1000L));

        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED,
                exception.getErrorCode());
    }

    @Test
    @DisplayName("거래 금액이 잔액보다 큰 경우 - 잔액 사용 실패")
    void useBalance_amountExceedBalance() {
        //given
        AccountUser pobi = AccountUser.builder().id(12L).name("Pobi").build();
        Account account = Account.builder()
                .accountUser(pobi)
                .accountStatus(IN_USE)
                .balance(30000L)
                .accountNumber("1000000012")
                .build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));

        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        //when
        //then
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 100000L));

        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
        verify(transactionRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("실패 트렌젝션 저장 성공")
    void successSaveFailedUseTransaction() {
        //given
        AccountUser pobi = AccountUser.builder().id(12L).name("Pobi").build();
        Account account = Account.builder()
                .accountUser(pobi)
                .accountStatus(IN_USE)
                .balance(10000L)
                .accountNumber("1000000012").build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any())).willReturn(Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(F)
                .transactionId("transactionId")
                .amount(1000L)
                .balanceSnapshot(9000L)
                .build()
        );
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        //when
        transactionService.saveFailedUseTransaction("1000000000", 200L);

        //then
        verify(transactionRepository, times(1)).save(captor.capture());

        assertEquals(200L, captor.getValue().getAmount());
        assertEquals(F, captor.getValue().getTransactionResultType());
        assertEquals(10000L, captor.getValue().getBalanceSnapshot());
    }

    @Test
    void successCancelUseBalance() {
        //given
        AccountUser pobi = AccountUser.builder().id(12L).name("Pobi").build();

        Account account = Account.builder()
                .accountUser(pobi)
                .accountStatus(IN_USE)
                .balance(30000L)
                .accountNumber("1000000012")
                .build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactedAt(LocalDateTime.now())
                .transactionId("transactionId")
                .amount(30000L)
                .balanceSnapshot(9000L)
                .build();


        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any())).willReturn(Transaction.builder()
                .account(account)
                .transactionType(CANCEL)
                .transactionResultType(S)
                .transactionId("transactionIdForCancel")
                .amount(30000L)
                .balanceSnapshot(9000L)
                .build()
        );

        //when
        TransactionDto transactionDto = transactionService.cancelBalance("transactionId", "1000000000", 30000L);

        ArgumentCaptor<Transaction> captor =
                ArgumentCaptor.forClass(Transaction.class);

        //then
        Transaction transactionCap = verify(transactionRepository, times(1)).save(captor.capture());

        assertEquals( 30000L, captor.getValue().getAmount());
        assertEquals(60000L, captor.getValue().getBalanceSnapshot());
        assertEquals(S, transactionDto.getTransactionResultType());


        assertEquals(9000L, transactionDto.getBalanceSnapshot());
        assertEquals(CANCEL, transactionDto.getTransactionType());
    }

    @Test
    @DisplayName("검색한 사용자의 계좌가 없을때 - 잔액 취소 실패")
    void cancelBalance_userAccountNotFound() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .id(12L).name("Pobi").build();
        given(transactionRepository.findByTransactionId(anyString())).willReturn(Optional.of(Transaction.builder()
                        .transactionType(USE)
                        .transactionResultType(S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                .build()));
        given(accountRepository.findByAccountNumber(anyString())).willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1000000000", 1000L));

        //then
        assertEquals(ErrorCode.USER_ACCOUNT_NOT_FOUND,
                exception.getErrorCode());
    }

    @Test
    @DisplayName("해당 거래가 없을때 - 잔액 취소 실패")
    void cancelBalance_transactionNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString())).willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1000000000", 1000L));

        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND,
                exception.getErrorCode());
    }

    @Test
    @DisplayName("거래와 계좌가 매칭실패 - 잔액 취소 실패")
    void cancelBalance_transactionAccountUnMatch() {
        AccountUser pobi = AccountUser.builder().id(12L).name("Pobi").build();

        //given
        Account account = Account.builder()
                .id(1L)
                .accountUser(pobi)
                .accountStatus(IN_USE)
                .balance(30000L)
                .accountNumber("1000000012")
                .build();
        Account accountNotUse = Account.builder()
                .id(2L)
                .accountUser(pobi)
                .accountStatus(IN_USE)
                .balance(30000L)
                .accountNumber("1000000013")
                .build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(1000L)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString())).willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString())).willReturn(Optional.of(accountNotUse));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1000000012", 1000L));

        //then
        assertEquals(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH,
                exception.getErrorCode());
    }

    @Test
    @DisplayName("거래 금액과 취소 금액이 다를때 - 잔액 취소 실패")
    void cancelBalance_cancelMustFully() {
        AccountUser pobi = AccountUser.builder().id(12L).name("Pobi").build();

        //given
        Account account = Account.builder()
                .id(1L)
                .accountUser(pobi)
                .accountStatus(IN_USE)
                .balance(30000L)
                .accountNumber("1000000012")
                .build();

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(21000L)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString())).willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString())).willReturn(Optional.of(account));
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1000000012", 1000L));

        //then
        assertEquals(ErrorCode.CANCEL_MUST_FULLY,
                exception.getErrorCode());
    }

    @Test
    @DisplayName("거래 날짜가 1년이 넘었을 때 - 잔액 취소 실패")
    void cancelBalance_tooOldOrderedCancel() {
        AccountUser pobi = AccountUser.builder().id(12L).name("Pobi").build();

        //given
        Account account = Account.builder()
                .id(1L)
                .accountUser(pobi)
                .accountStatus(IN_USE)
                .balance(30000L)
                .accountNumber("1000000012")
                .build();

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResultType(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1).minusDays(1))
                .amount(20000L)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString())).willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString())).willReturn(Optional.of(account));
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1000000012", 20000L));

        //then
        assertEquals(ErrorCode.TOO_OLD_ORDER_TO_CANCEL,
                exception.getErrorCode());
    }

    @Test
    void successQueryTransaction() {
        //given
        AccountUser pobi = AccountUser.builder().id(12L).name("Pobi").build();
        Account account =  Account.builder()
                .id(1L)
                .accountUser(pobi)
                .accountStatus(IN_USE)
                .balance(30000L)
                .accountNumber("1000000012")
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(
                            Transaction.builder()
                                        .account(account)
                                        .transactionType(USE)
                                        .transactionResultType(S)
                                        .transactionId("transactionId")
                                        .transactedAt(LocalDateTime.now().minusYears(1).minusDays(1))
                                        .amount(20000L)
                                        .balanceSnapshot(9000L)
                                        .build()
        ));
        //when
        TransactionDto transactionDto = transactionService.queryTransaction("trxId");

        //then
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(S, transactionDto.getTransactionResultType());
        assertEquals(20000L, transactionDto.getAmount());
        assertEquals("transactionId", transactionDto.getTransactionId());
    }

    @Test
    @DisplayName("원거래 없음 - 거래 조회 실패")
    void queryTransaction_TransactionNotFound() {
        //given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException accountException = assertThrows(AccountException.class, () -> transactionService.queryTransaction("transactionId"));

        //then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, accountException.getErrorCode());
    }
}