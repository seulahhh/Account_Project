package com.example.account.domain;

import com.example.account.type.AccountStatus;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Account {
    @Id
    @GeneratedValue
    private Long id;

    // ? @ManyToOne: 여러개의 자식이 하나의 부모 엔티티와 연결되는 다대일 관계를 정의하는대 사용
    // ? 외래키를 이용하여 부모-자식 관계를 데이터베이스 테이블에 매핑할 수 있다.
    // ? @OneToMany와 함께 사용하여 양방항 관계를 정의할 수 있다.
    // 한 AccountUser당 여러 Account를 가질 수 있음.
    // 앞! 이 해당 클래스, to 가 대응관계에 있는 클래스
    @ManyToOne
    private AccountUser accountUser;
    private String accountNumber;

    //? @Enumerated - Enum을 0,1,2,3의 형태로 저장하지 않고 정의된 문자 그대로 저장 할 수 있게 함
    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus;
    private Long balance;

    private LocalDateTime registeredAt;
    private LocalDateTime unRegisteredAt;

    // ? @CreatedDate와 @LastModifiedDate, @EntityListeners(AuditingEntityListener.class)
    // ? 를 사용하면 매번 레코드를 생성할 때 마다 생성, 업데이트 일자를 채워주지 않아도 된다. 자동으로 채워준다.
    // ? (application 전체 config 파일에 해당 설정을 넣어줘야 함)
    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
