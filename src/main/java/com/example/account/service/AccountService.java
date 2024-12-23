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

    private final AccountRepository accountRepository;
    private final AccountUserRepository accountUserRepository;

    @Transactional
    public AccountDto createAccount(Long userId, Long initBalance) {

        AccountUser accountUser = accountUserRepository.findById(userId).orElseThrow(() -> new AccountException(USER_NOT_FOUND));

        validateCreateAccount(accountUser);
        String newAccountNumber = accountRepository.findFirstByOrderByIdDesc().map(account -> (Integer.parseInt(account.getAccountNumber()) + 1 + "")) // 여기까지 return값은 Optional
                // ? Optional타입을 문자열로 바꾸는 방법은 .toString()도 있지만, 위와 같이 + ""를 해줄 수도 있음
                .orElse("1000000000");

        // ? .save() : JpaRepository 인터페이스에서 제공하는 기능/ save하면서 save 된 Account를 반환한다.
        return AccountDto.fromEntity(
            accountRepository.save(Account.builder().accountUser(accountUser).accountStatus(IN_USE).accountNumber(newAccountNumber).balance(initBalance).registeredAt(LocalDateTime.now()).build()
                ));
    }

    private void validateCreateAccount(AccountUser accountUser) {
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

        validateDeleteAccount(accountUser, account);

        account.setAccountStatus(UNREGISTERED);
        account.setUnRegisteredAt(LocalDateTime.now());

        accountRepository.save(account);

        return AccountDto.fromEntity(account);
    }

    public void validateDeleteAccount(AccountUser accountUser, Account account) {
        if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(USER_ACCOUNT_UN_MATCH);
        }

        if (account.getAccountStatus() == UNREGISTERED) {
            throw new AccountException(ACCOUNT_ALREADY_UNREGISTERED);
        }

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
    }
}
