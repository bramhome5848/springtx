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

    /**
     * MemberService        @Transactional : ON
     * MemberRepository     @Transactional : OFF
     * LogRepository        @Transactional : OFF

     * 상황
     - 서비스 계층에 트랜잭션 사용
     - 회원, 로그 레포지토리에 있던 트랜잭션 주석 처리
     -> 회원, 로그 레포지토리를 하나의 트랜잭션으로 간단하게 묶음

     * 설명
     - @Transactional 이 MemberService 에만 붙어있기 때문에 여기에만 트랜잭션 AOP 가 적용
     -> MemberRepository, LogRepository 는 트랜잭션 AOP 가 적용되지 않음
     - MemberService 의 시작부터 끝까지, 관련 로직은 해당 트랜잭션이 생성한 커넥션을 사용
     -> MemberService 가 호출하는 MemberRepository, LogRepository 도 같은 커넥션을 사용하면서 트랜잭션 범위에 포함
     */
    @Test
    void singleTx() {
        //given
        String username = "singleTx";

        //when
        memberService.joinV1(username);

        //then
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }

    /**
     * MemberService        @Transactional : ON
     * MemberRepository     @Transactional : ON
     * LogRepository        @Transactional : ON
     */
    @Test
    void outerTxOn_success() {
        //given
        String username = "outerTxOn_success";

        //when
        memberService.joinV1(username);

        //then : 모든 데이터가 정상 저장됨
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }

    /**
     * MemberService        @Transactional : ON
     * MemberRepository     @Transactional : ON
     * LogRepository        @Transactional : ON Exception
     - Participating transaction failed - marking existing transaction as rollback-only 확인가능

     * 참고
     - 해당 과정의 경우 MemberService 에서도 런타임 예외를 받게 되는데, 로직에서 해당 런타임 예외를 처리하지 않고 밖으로 던짐
     - 트랜잭션 AOP(MemberService Proxy) 는 런타임 예외가 발생했으므로 트랜잭션 매니저에 롤백을 요청, 이 경우 신규 트랜잭션이므로 물리 롤백 호출
     - 어차피 롤백 되기 때문에 rollbackOnly 설정을 참고하지 않음
     */
    @Test
    void outerTxOn_fail() {
        //given
        String username = "로그예외_outerTxOn_success";

        //when
        assertThatThrownBy(() -> memberService.joinV1(username))
                .isInstanceOf(RuntimeException.class);

        //then : 모든 데이터가 롤백됨
        assertTrue(memberRepository.find(username).isEmpty());
        assertTrue(logRepository.find(username).isEmpty());
    }
}