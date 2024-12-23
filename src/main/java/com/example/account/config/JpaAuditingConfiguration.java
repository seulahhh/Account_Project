package com.example.account.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration // component의 일종 == 자동으로 빈으로 등록이 됨
@EnableJpaAuditing // JpaAuditing 이 켜진 상태로 어플리케이션이 패키징됨.
public class JpaAuditingConfiguration {
    // Auditing : 회계
    // Jpa 자동 회계에 관련된 configuration을 정의 해 줄 클래스
}
