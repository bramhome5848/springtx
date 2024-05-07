package hello.springtx.propagation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MemberService
 - 회원을 등록하면서 동시에 회원 등록에 대한 DB 로그도 함께 남김
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final LogRepository logRepository;

    /**
     * joinV1
     - 회원과 DB 로그를 함께 남기는 비즈니스 로직
     - 현재 별도의 트랜잭션 설정은 하지 않음
     */
    @Transactional  //추가
    public void joinV1(String username) {
        Member member = new Member(username);
        Log logMessage = new Log(username);

        log.info("== memberRepository 호출 시작 ==");
        memberRepository.save(member);
        log.info("== memberRepository 호출 종료 ==");

        log.info("== logRepository 호출 시작 ==");
        logRepository.save(logMessage);
        log.info("== logRepository 호출 종료 ==");
    }

    /**
     * joinV2
     - joinV1() 과 같은 기능을 수행
     - DB 로그 저장시 예외가 발생하면 예외를 복구
     - 현재 별도의 트랜잭션 설정은 하지 않음
     */
    @Transactional
    public void joinV2(String username) {
        Member member = new Member(username);
        Log logMessage = new Log(username);

        log.info("== memberRepository 호출 시작 ==");
        memberRepository.save(member);
        log.info("== memberRepository 호출 종료 ==");

        log.info("== logRepository 호출 시작 ==");
        try {
            logRepository.save(logMessage);
        } catch (RuntimeException e) {
            log.info("log 저장에 실패했습니다. logMessage = {}", logMessage.getMessage());
            log.info("정상 흐름 변환");
        }

        log.info("== logRepository 호출 종료 == ");
    }
}
