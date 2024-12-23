package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccountSuccess() {
        //given
        AccountUser accountUser =
                AccountUser.builder().id(12L).name("Pobi").build();

        given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(accountUser));

        given(accountRepository.findFirstByOrderByIdDesc()).willReturn(Optional.of(Account.builder().accountNumber("1000000011").build()));

        given(accountRepository.save(any())).willReturn(Account.builder().accountUser(accountUser).accountNumber("1000000013").build());

        ArgumentCaptor<Account> captor =
                ArgumentCaptor.forClass(Account.class);// captor 생성
        //when
        AccountDto accountDto = accountService.createAccount(1L, 100098L);

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000012", captor.getValue().getAccountNumber());
    }

    @Test
    @DisplayName("맨 처음 계좌를 생성할 때")
    void createFirstAccountSuccess() {
        //given
        AccountUser accountUser =
                AccountUser.builder().id(15L).name("Pobi").build();

        given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(accountUser));

        given(accountRepository.findFirstByOrderByIdDesc()).willReturn(Optional.empty());
        // ? Optional.empty() : Optional.value == null인 Optional객체를 생성함

        given(accountRepository.save(any())).willReturn(Account.builder().accountUser(accountUser).accountNumber("1000000011").build());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        //when
        AccountDto accountDto = accountService.createAccount(1L, 100098L);

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        System.out.println(captor.getValue());
        assertEquals(15L, accountDto.getUserId());
        assertEquals("1000000000", captor.getValue().getAccountNumber());
    }


    @Test
    @DisplayName("해당하는 유저가 없을때 - 계좌 생성 실패")
    void createAccount_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong())).willReturn(Optional.empty());

        //when
        AccountException accountException =
                assertThrows(AccountException.class,
                        () -> accountService.createAccount(1L, 100098L));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, accountException.getErrorCode());
    }

    @Test
    @DisplayName("계정에 계좌가 10개인 경우 - 계좌 생성 실패")
    void createAccount_maxAccountIs10() {
        //given
        given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(AccountUser.builder().id(10L).build()));
        given(accountRepository.countByAccountUser(any())).willReturn(10);

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(10L, 10001L));

        //then
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10,
                exception.getErrorCode());
    }


    @Test
    void deleteAccountSuccess() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .id(12L).name("Pobi").build();

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        given(accountRepository.findByAccountNumber(anyString())).willReturn(
                Optional.of(Account.builder()
                        .accountUser(accountUser)
                        .accountNumber("1000000011")
                        .balance(0L)
                        .build())
        );

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);// captor 생성

        //when
        AccountDto accountDto = accountService.deleteAccount(12L, "10000000000");

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000011", captor.getValue().getAccountNumber());
        assertEquals(AccountStatus.UNREGISTERED, captor.getValue().getAccountStatus());
    }

    @Test
    @DisplayName("해당하는 유저가 없을때 - 계좌 해지")
    void deleteAccount_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong())).willReturn(Optional.empty());

        //when
        AccountException accountException =
                assertThrows(AccountException.class,
                        () -> accountService.deleteAccount(1L, "1000000100"));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, accountException.getErrorCode());
        // accountService 실행 시 던져진 AccountException을 받아서 두번째 검사 진행
    }


    @Test
    @DisplayName("검색한 사용자의 계좌가 없을때 - 계좌 해지 실패")
    void deleteAccount_userAccountNotFound() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .id(12L).name("Pobi").build();
        given(accountUserRepository.findById(any())).willReturn(Optional.of(accountUser));
        given(accountRepository.findByAccountNumber(anyString())).willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(10L, "10000000000"));

        //then
        assertEquals(ErrorCode.USER_ACCOUNT_NOT_FOUND,
                exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 소유주가 다를때 - 계좌 해지 실패")
    void deleteAccount_userAccountUnMatch() {
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
                () -> accountService.deleteAccount(1L, "1000000013"));

        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH,
                exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌의 잔액이 남아있을때 - 계좌 해지 실패")
    void deleteAccount_balanceNotEmpty() {
        //given
        AccountUser userPobi = AccountUser.builder()
                .id(12L).name("Pobi").build();

        given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(userPobi));
        given(accountRepository.findByAccountNumber(anyString())).willReturn(Optional.of(
                Account.builder()
                        .accountUser(userPobi)
                        .balance(100L)
                        .accountNumber("1000000012")
                        .build()
        ));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1000000013"));

        //then
        assertEquals(ErrorCode.BALANCE_NOT_EMPTY,
                exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌가 이미 해지 상태인 경우 - 계좌 해지 실패")
    void deleteAccount_accountAlreadyUnregisterd() {
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
                () -> accountService.deleteAccount(1L, "1000000013"));

        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED,
                exception.getErrorCode());
    }

    @Test
    void successGetAccountsByUserId() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .id(12L).name("Pobi").build();
        List<Account> accounts = Arrays.asList(
                Account.builder().accountUser(accountUser)
                        .accountNumber("1111111111")
                        .balance(1000L)
                        .build(),
                Account.builder().accountUser(accountUser)
                        .accountNumber("2222222222")
                        .balance(2000L)
                        .build(),
                Account.builder().accountUser(accountUser)
                        .accountNumber("3333333333")
                        .balance(3000L)
                        .build()
        );

        given(accountUserRepository.findById(any()))
                .willReturn(Optional.of(accountUser));
        given(accountRepository.findByAccountUser(any()))
                .willReturn(accounts);
        //when
        List<AccountDto> accountDtos =
                accountService.getAccountsByUserId(accountUser.getId());
        //then
        assertEquals(3, accountDtos.size());
        assertEquals("1111111111", accountDtos.get(0).getAccountNumber());
        assertEquals(1000L, accountDtos.get(0).getBalance());
    }

    @Test
    @DisplayName("사용자가 존재하지 않을 경우 - 계좌번호 조회 실패")
    void failedToGetAccounts() {
        //given
        AccountUser accountUser = AccountUser.builder()
                .id(12L).name("Pobi").build();
        given(accountUserRepository.findById(anyLong())).willReturn(Optional.empty());
        //when
        AccountException accountException = assertThrows(AccountException.class, () -> accountService.getAccountsByUserId(accountUser.getId()));
        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, accountException.getErrorCode());
    }

}