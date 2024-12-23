package com.example.account.controller;

import com.example.account.dto.AccountDto;
import com.example.account.dto.AccountInfo;
import com.example.account.dto.CreateAccount;
import com.example.account.dto.DeleteAccount;
import com.example.account.service.AccountService;
import com.example.account.service.RedisTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    private final RedisTestService redisTestService;

    // 계좌 생성
    @PostMapping("/account")
    public CreateAccount.Response createAccount(@RequestBody @Valid CreateAccount.Request request) {
        AccountDto accountDto =
                accountService.createAccount(request.getUserId(),
                        request.getInitBalance());
        // 받은 accountDto를 가지고 Response 객체 만들기
        // => Reponse객체에 해당 메서드 정의 (public static Response from(AccountDto
        // accountDto))
        // 위 accountDto와 같은 일회용 변수는 사용하지 않고 바로 inline valriable 해버려도 되지만 현재
        // 명시적으로 나타내기 위해 남겨두었음

        return CreateAccount.Response.from(accountDto);
    }

    // 계좌해지
    @DeleteMapping("/account")
    public DeleteAccount.Response deleteAccount(@RequestBody @Valid DeleteAccount.Request request) {
        AccountDto accountDto = accountService.deleteAccount(request.getUserId(),request.getAccountNumber());

        return DeleteAccount.Response.from(accountDto);
    }

    // 계좌 확인
    @GetMapping("/account")
    public List<AccountInfo> getAccountByUserId(@RequestParam("user_id") Long userId) {

        List<AccountDto> accountDtos = accountService.getAccountsByUserId(userId);

        return accountDtos.stream().map(accountDto ->
                AccountInfo.builder()
                        .accountNumber(accountDto.getAccountNumber())
                        .balance(accountDto.getBalance())
                        .build())
                .collect(Collectors.toList());
    }
}
