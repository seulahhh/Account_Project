package com.example.account.exception;

import com.example.account.type.ErrorCode;
import lombok.*;

// Custom Exception
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountException extends RuntimeException {
    //요즘에는 편의를 위해 기본적으로
    // RuntimeException을 기반으로 예외를 만든디.

    private ErrorCode errorCode;
    private String errorMessage;

    public AccountException(ErrorCode errorCode) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
        this.errorMessage = errorCode.getDescription();
    }
}