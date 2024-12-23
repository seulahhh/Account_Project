package com.example.account.dto;

import com.example.account.domain.Account;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountDto {
    Long userId;
    private String accountNumber;
    private Long balance;

    private LocalDateTime registeredAt;
    private LocalDateTime unRegisteredAt;

    // ? 특정 Entity -> 특정 Dto로 변환해줄 때, 해당 Dto 내에 static메서드를 정의하여 AccountDto의
    // ? 생성자를 사용하지 않고 satic 메서드를 통해 해당 dto를 생성하면 깔끔하게 생성할 수 있다
    // dto는 Entity를 통해서 만들어 지는 경우가 더 많으므로 이렇게 생성한다.
    public static AccountDto fromEntity(Account account) {
        return AccountDto.builder()
                .userId(account.getAccountUser().getId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .registeredAt(account.getRegisteredAt())
                .unRegisteredAt(account.getUnRegisteredAt())
                .build();
    }
}
