package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest
class MemberServiceTest {

    @Autowired
    MemberService memberService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    LogRepository logRepository;

    /**
     * MemberService        @Transactional : OFF
     * MemberRepository     @Transactional : ON
     * LogRepository        @Transactional : ON

     * 상황
     - 서비스 계층에 트랜잭션이 없음
     - 회원, 로그 레포지토리가 각각 트랜잭션을 가지고 있음
     - 회원, 로그 레포지토리가 둘다 커밋에 성공
     */
    @Test
    void outerTxOff_success() {
        //given
        String username = "outerTxOff_success";

        //when
        memberService.joinV1(username);

        //then : 모든 데이터가 정상 저장
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }

    /**
     * MemberService        @Transactional : OFF
     * MemberRepository     @Transactional : ON
     * LogRepository        @Transactional : ON Exception

     * 상황
     - 서비스 계층에 트랜잭션이 없음
     - 회원, 로그 레포지토리가 각각 트랜잭션을 가지고 있음
     - 회원 레포지토리가 정상 동작하지만 로그 레포지토리에서 예외가 발생
     */
    @Test
    void outerTxOff_fail() {
        //given
        String username = "로그예외_outerTxOff_fail";

        //when
        assertThatThrownBy(() -> memberService.joinV1(username))
                .isInstanceOf(RuntimeException.class);

        //then : 완전히 롤백되지 않고, member 데이터가 남아서 저장
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isEmpty());
    }
}