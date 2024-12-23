package com.example.account.controller;

import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.dto.CreateAccount;
import com.example.account.dto.DeleteAccount;
import com.example.account.service.AccountService;
import com.example.account.service.RedisTestService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {
    @MockBean
    private AccountService accountService;

    @MockBean
    private RedisTestService redisTestService;

    // ? WebMvcTest: Web 요청-응답 테스트이다. 실제로 요청을 일일히 하고 응답을 받기 번거로우므로
    // ? 어떠한 요청을 보냈을 때 어떠한 응답이 올 지 테스트코드를 이용해서 테스트해볼 수 있게 해준다.
    // @MockBean을 통해 모킹된 빈들이 진짜 AccountController안에 주입되어 test컨테이너에 올라가게 됨
    // 최종적으로 test 컨테이너에는
    // 진짜 AccountController, 가짜 AccountServce, 가짜 RedisTestService가 합쳐져서
    // webMvcTest대상으로 올라가게 됨.

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void successCreateAccount() throws Exception {
        //given
        // 상황을 가정하는 로직을 작성
        given(accountService.createAccount(anyLong(), anyLong()))
                .willReturn(AccountDto.builder()
                        .userId(1L)
                        .accountNumber("123456789")
                        .registeredAt(LocalDateTime.now())
                        .unRegisteredAt(LocalDateTime.now())
                        .build());
        // 테스트이기 때문에 해당 AccuontDto의 필드가 정확히 들어갔는지 확인하기 위해서
        // AccountDto를 builder로 필드를 직접 입력하여 생성함으로써 비교객체를 만들어 주는 것
        // ? anyLong(): Java에서 Mockito 테스트 프레임워크에서 사용하는 메서드

        //when,
        //then
        // 상황을 가정한 로직을 perform시켜보기
        mockMvc.perform(post("/account")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new CreateAccount.Request(1L, 100L)
                                // 여기에는 다른 값이 들어와도 무방하다.
                                // 테스트 할 흐름은 AccountDto ~ Response이다.
                                // 그냥 아무 Request나 던져줬다는 것을 가정한 것.
                                // 위의 given ~ willReturn내부의 AccountDto객체와 값만 같으면 됨
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.accountNumber").value("123456789"))
                .andDo(print());

        // ? MockMvc.perform({수행할 웹 동작(POST,GET등의 요청 수행을 메서드로 가지고 있다})
        // ? .andExpect({예상결과})
        // ? .content(): Request body의 내용

        // ? ObjectMapper: Spring에서 기본적으로 제공하는 기능으로, 문자열로 바꿔주는 기능을 한다. Jackson라이브러리에서 제공되는 기능.
        // ? Spring Container에 이미 등록되어 있으므로(jackson) @AutoWired를 통해 주입받 수 있다.

        // ? jsonPath :  JSON 데이터를 쉽게 탐색하고 검증할 수 있게 해주는 기능/MockMvc에서 Http응답의
        // ? Json결과를 검증할 때 자주 사용된다. 응답 본문에서 특정 값을 추출하거나 검증할 수 있음
        // ? "$.userId" - 응답 JSON 루트에서 userId필드를 찾는다.
        // ? .value(1L) - 그 값이 1L인지 확인
        // ? jsonPath 기타용법예시: $.data[0].id: data배열의 번째 요소에서 id필드를 찾는다.

        // ? andDo() : mockWebMvc에서 최종수행할 행동
        // ObjectMapper의 기능(writeValueAsString 등)을 사용할 수 없다면, jackson클래스를
        // import 해주는 것 뿐 아니라 gradle에서 의존성 설정도 해주어야 한다.

        //objectMapper.writeValueAsString()에서 JsonProcessingException이 발생하거나,
        // perform() 메서드에서 HTTP 요청 처리 과정에서 Exception이 발생할 수 있기 때문에
        // 따라서 테스트 메서드에서 이러한 예외들을 처리하거나 선언하여 코드가 실행될 수 있도록 해야 함
    }


    @Test
    void successDeleteAccount() throws Exception {
        //given
        given(accountService.deleteAccount(anyLong(), anyString()))
                .willReturn(AccountDto.builder()
                        .userId(1L)
                        .accountNumber("1234056789")
                        .registeredAt(LocalDateTime.now())
                        .unRegisteredAt(LocalDateTime.now())
                        .build());
        //when
        //then
        mockMvc.perform(delete("/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DeleteAccount.Request(1L, "1000000000")
                                // given 으로 이미 어떤 값을 넣어 deleteAccount 를 호출해도
                                // 동일한 결과가 나오는 환경을 설정했으므로
                                // 여기서 어떻게 정의하던지 위에서 mocking 한대로 동작할것이기때문에
                                // 이쪽에서 Request에 넣어주는 값은 의미 없음.
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.accountNumber").value("1234056789"))
                .andDo(print());
    }
}