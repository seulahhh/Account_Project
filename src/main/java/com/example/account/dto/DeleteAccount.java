package com.example.account.dto;

import lombok.*;

import javax.validation.constraints.*;
import java.time.LocalDateTime;

public class DeleteAccount {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        @NotNull
        @Min(1)
        private Long userId;

        @NotBlank
        @Size(min = 10, max = 10)
        private String accountNumber;
//        @Size @NotNull, @NotBlank, @Min 등은 Controller에서 요청을 받아서 Valid 를 수행할때 작동된다.

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long userId;
        private String accountNumber;
        private LocalDateTime unregisteredAt;

        public static Response from(AccountDto accountDto) {
            return Response.builder()
                    .userId(accountDto.getUserId())
                    .accountNumber(accountDto.getAccountNumber())
                    .unregisteredAt(accountDto.getUnRegisteredAt())
                    .build();
        }
    }
}