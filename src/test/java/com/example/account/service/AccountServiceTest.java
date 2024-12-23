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
    // @Mock을 통해 AccountRepository, AccountUserRepository 의 Mock을 생성한다.

    // 위에서 생성한 두 Mock 을 AccountService 생성 시 주입함.
    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccountSuccess() {
        //given
        AccountUser accountUser =
                AccountUser.builder().id(12L).name("Pobi").build();

        given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(accountUser));

        given(accountRepository.findFirstByOrderByIdDesc()).willReturn(Optional.of(Account.builder().accountNumber("1000000011").build()));
        // findById와 findFirstByOrderByIdDesc는 내부적으로 Optional 타입을 return 하도록
        // 되어 있기 때문에 여기에서도 willReturn 값이 Optional 값이 되는 것

        given(accountRepository.save(any())).willReturn(Account.builder().accountUser(accountUser).accountNumber("1000000013").build());

        // accountRepository에 save 하는 대상 확인해보기
        ArgumentCaptor<Account> captor =
                ArgumentCaptor.forClass(Account.class);// captor 생성
        //when
        AccountDto accountDto = accountService.createAccount(1L, 100098L);
        // given 에서 주어진 findById, findFirstByOrderByIdDesc, save 모두 발생.
        // ==> willReturn의 상황도 모두 발생함

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        // [when]에서 accountService.createAccount()할 때
        // verify - accountRepository가 save()메서드를 1번 호출했는지를 검사하고,
        // ArgumentCaptor.capture() - save()메서드 호출 시 넘겨준 인자를 검사한다.
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000012", captor.getValue().getAccountNumber());
        // findFirstByOrderByIdDesc()의 AccountNumber가 1000000011이므로, save할 때
        // 넘겨주는 인자는
        // +1을 해준 1000000012 가 되는게 맞다.
    }

    @Test
    @DisplayName("맨 처음 계좌를 생성할 때")
    void createFirstAccountSuccess() {
        //given
        AccountUser accountUser =
                AccountUser.builder().id(15L).name("Pobi").build();

        given(accountUserRepository.findById(anyLong())).willReturn(Optional.of(accountUser));

        // accountRepository에 아무 계좌도 없는 상황
        // accountRepositorty에 등록한 계좌가 없다면 기본 값인 1000000000으로 계좌번호가 생성되게 됨
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
        // 해당하는 유저가 존재하지 않을 경우
        given(accountUserRepository.findById(anyLong())).willReturn(Optional.empty());

        //when
        // 첫번째 검사와 동시에 두번째 검사에 이용할 값을 반환
        AccountException accountException =
                assertThrows(AccountException.class,
                        () -> accountService.createAccount(1L, 100098L));
        // assertThorws(Exception타입.class, 실행할 로직)
        // {실행할 로직}을 실행했을 때, {Exception타입}의 Exception 발생 여부 검사

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, accountException.getErrorCode());
        // accountService 실행 시 던져진 AccountException을 받아서 두번째 검사 진행
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
        // 다른테스트들과 같은 이유로, deleteAccount의 인자로 무엇을 넣어 주던지
        // given 에서 mocking한 메서드가 호출 되면 willReturn 값이 호출된다.

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        // ↪︎ 실제 deleteAccount 서비스 로직내부에 accountRepository.save(account); 구문을 작성하여 <테스트>를 함.
        // (업데이트된 account가 captor에 capture될 것이기 때문에 제대로 업데이트가 되었는지 확인해보는 용도)
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000011", captor.getValue().getAccountNumber());
        assertEquals(AccountStatus.UNREGISTERED, captor.getValue().getAccountStatus());
    }

    @Test
    @DisplayName("해당하는 유저가 없을때 - 계좌 해지")
    void deleteAccount_UserNotFound() {
        //given
        // 해당하는 유저가 존재하지 않는 환경 설정
        given(accountUserRepository.findById(anyLong())).willReturn(Optional.empty());

        //when
        // 첫번째 검사와 동시에 두번째 검사에 이용할 값을 반환
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
}