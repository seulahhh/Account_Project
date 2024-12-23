package com.example.account.repository;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    // ? find fisrt / by / orderby /id / desc
    // ? DB 쿼리 형식에 맞게 메서드 이름을 지정해 주면, 자동으로 해당 쿼리를 사용할 수 있게 해준다!!!!!
    Optional<Account> findFirstByOrderByIdDesc();
    // 처음 생성 하는 등의 상황에서는 찾았을 때 값이 ull일 수 있으므로 Optional타입을 사용


    Integer countByAccountUser(AccountUser accountUser);

    Optional<Account> findByAccountNumber(String accountNumber);
    // Account객체가 AccountUser 필드를 가짐
    // ==> DB관점에서, Account 테이블의 column에 AccountUser가 있다는 의미
    // 따라서 accountUser를 기준으로 Account 개수를 조회할 수 있다.

//    List<Account> getAllByUpdatedAtNotEmpty(LocalDateTime updatedAt);
}

