package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountRepository;
import com.example.account.repository.AccountUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.example.account.type.AccountStatus.IN_USE;
import static com.example.account.type.AccountStatus.UNREGISTERED;
import static com.example.account.type.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository; // 자동주입됨
    private final AccountUserRepository accountUserRepository; // 자동주입

    // * createAccount()
    // * 1. validation 진행(사용자가 있는지 / 계좌 개수가 10개 미만인지)
    // * 2. 계좌번호 생성
    // * 3. 계좌 저장, 그 정보를 넘겨줌 (해당 정보 = Response에 필요한 정보)
    @Transactional
    public AccountDto createAccount(Long userId, Long initBalance) {
        // 사용자가 있는지 확인

        AccountUser accountUser = accountUserRepository.findById(userId).orElseThrow(() -> new AccountException(USER_NOT_FOUND));
        // ↪︎ accountUser가 있으면 accountUser로 받고, 없으면 .orElseThrow가 동작해서 예외를
        // 던져주게 된다.

        // 계좌 개수가 10개 미만인지 확인
        validateCreateAccount(accountUser);

        // 2. 계좌번호 생성
        // 10자리의 숫자로 이루어진 값을 사용할것임
        // 새로 생성할 계좌번호는 제일 마지막에 생성되어 저장된 계좌번호보다 1 더 큰 숫자
        // ? .map() 은 Optional 타입에서 제공하는 기능 / map을 사용하여 Optional 내부 값을 변환하는데 사용
        String newAccountNumber = accountRepository.findFirstByOrderByIdDesc().map(account -> (Integer.parseInt(account.getAccountNumber()) + 1 + "")) // 여기까지 return값은 Optional
                // ? Optional타입을 문자열로 바꾸는 방법은 .toString()도 있지만, 위와 같이 + ""를 해줄 수도 있음
                .orElse("1000000000");// findFirstByOrderByIdDesc()의 결과값이 없을때 (= Optional.value == null)
        // Optional 타입은 값이 없는 경우 매핑함수(.map())은 호출되지 않으며 Optional.emty()를 반환한다.

        // 3. 계좌 저장, AccountDto객체 생성, AccountDto 반환
        // ? .save() : JpaRepository 인터페이스에서 제공하는 기능/ save하면서 save 된 Account를 반환한다.
        return AccountDto.fromEntity(
                // --
            accountRepository.save(Account.builder().accountUser(accountUser).accountStatus(IN_USE).accountNumber(newAccountNumber).balance(initBalance).registeredAt(LocalDateTime.now()).build()
                // -- Account 반환
                ));
    }

    private void validateCreateAccount(AccountUser accountUser) {
        // 계좌가 10개 미만인지 확인
        if (accountRepository.countByAccountUser(accountUser) == 10) {
            throw new AccountException(MAX_ACCOUNT_PER_USER_10);
        }
    }

    @Transactional
    public Account getAccount(Long id) {
        if (id < 0) {
            throw new RuntimeException("Minus");
        }
        return accountRepository.findById(id).get();
    }

    @Transactional
    public AccountDto deleteAccount(Long userId, String accountNumber) {
        AccountUser accountUser = accountUserRepository.findById(userId).orElseThrow(() -> new AccountException(USER_NOT_FOUND));
        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new AccountException(USER_ACCOUNT_NOT_FOUND));

        // 응답 정책에 따른 validation 처리.
        validateDeleteAccount(accountUser, account);

        // account를 해지상태로 update
        account.setAccountStatus(UNREGISTERED);
        account.setUnRegisteredAt(LocalDateTime.now());

        accountRepository.save(account);

        // @@ 계좌를 새로 생성하거나 아예 삭제하는 것이 아니라 unregistred로 update하는 것이다! @@
        // 따라서 account를 새로 build해서 넣어줄 필요 없이 기존 account 의 필드를 setter로 업데이트 하고
        // 해당 account를 바탕으로 AccountDto를 생성하여 넘겨주면 됨
        return AccountDto.fromEntity(account);
    }

    public void validateDeleteAccount(AccountUser accountUser, Account account) {

        // [case] 사용자와 계좌의 소유주가 다른 경우
        // accountUser : 요청으로 전달받은 "id" 로 검색한 사용자 정보
        // account.getAccountUser() : 요청을 전달받은 "계좌번호"를 통해 검색한 account에 저장된 사용자 정보
        if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(USER_ACCOUNT_UN_MATCH);
        }

        // [case] 계좌가 이미 해지 상태인 경우
        if (account.getAccountStatus() == UNREGISTERED) {
            throw new AccountException(ACCOUNT_ALREADY_UNREGISTERED);
        }

        // [case] 계좌에 잔액이 있는 경우
        if (account.getBalance() > 0) {
            throw new AccountException(BALANCE_NOT_EMPTY);
        }
    }

    @Transactional
    public List<AccountDto> getAccountsByUserId(Long userId) {

        AccountUser accountUser =
                accountUserRepository.findById(userId).orElseThrow(() -> new AccountException(USER_NOT_FOUND));

        List<Account> accounts = accountRepository.findByAccountUser(accountUser);

        return accounts.stream().map(AccountDto::fromEntity).collect(Collectors.toList());
        // == return accounts.stream().map(account -> AccountDto.fromEntity(account)).collect(Collectors.toList());
    }
}
