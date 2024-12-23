package com.example.account.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountInfo {
    //Account DTO와 가장 구조가 비슷
    // Account Dto 는 서비스에서 사용할 의도로 생성 (Controller ↔️Service)
    // AccountInfo는 응답 반환 용도로 생성 (Controller ↔️Client)

    private String accountNumber;
    private Long balance;

    // 참고 추가 : 각 전용 dto를 만들어서 로직 동작 시 dto를 여러개 사용하는 이유
    // 복잡한 상황 발생 가능성 up -> 장애 발생 가능성 up
    // 필요에 따라 딱 정해진 용도로만 사용하도록 생성하고 최적화하는것이 중요
}

