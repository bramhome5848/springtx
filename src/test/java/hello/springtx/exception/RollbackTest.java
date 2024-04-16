package hello.springtx.exception;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 예외와 트랜잭션 커밋, 롤백 - 기본
 - 예외가 발생했는데 내부에서 처리하지 못하고 트랜잭션 범위(@Transactional 가 적용된 AOP) 밖으로 예외를 던질 경우
 1. Repository - 예외발생, service 로 예외 던짐
 2. Service - AOP Proxy 로 예외 던짐
 3. @Transactional AOP Proxy - 예외와 트랜잭션 커밋, 롤백 정책 적용(런타임 예외 : 롤백, 체크 예외 : 커밋), Controller 로 예외 던짐
 4. Controller

 * 예외 발생시 스프링 트랜잭션 AOP 는 예외 종류에 따라 트랜잭션을 커밋하거나 롤백
 - 언체크 예외인 RuntimeException, Error 와 그 하위 예외가 발생할 경우 트랜잭션을 롤백
 - 체크 예외인 Exception 과 그 하위 예외가 발생하면 트랜잭션을 커밋
 - 정상 응답(리턴) 하면 트랜잭션을 커밋
 */
@SpringBootTest
class RollbackTest {

    @Autowired
    RollbackService service;

    @Test
    void runtimeException() {
        assertThatThrownBy(() -> service.runtimeException())
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void checkedException() {
        assertThatThrownBy(() -> service.checkedException())
                .isInstanceOf(MyException.class);
    }

    @Test
    void rollbackFor() {
        assertThatThrownBy(() -> service.rollbackFor())
                .isInstanceOf(MyException.class);
    }

    @TestConfiguration
    static class RollbackTestConfig {
        @Bean
        RollbackService rollbackService() {
            return new RollbackService();
        }
    }

    @Slf4j
    static class RollbackService {

        //런타임 예외 발생 : 롤백
        @Transactional
        public void runtimeException() {
            log.info("call runtimeException");
            throw new RuntimeException();
        }

        //체크 예외 발생 : 커밋
        @Transactional
        public void checkedException() throws MyException {
            log.info("call checkedException");
            throw new MyException();
        }

        //체크 예외 rollbackFor 지정 : 롤백, 체크 예외인 Exception(자식 타입 포함) 이 발생해도 커밋 되신 롤백 됨
        @Transactional(rollbackFor = MyException.class)
        public void rollbackFor() throws MyException {
            log.info("call rollbackFor");
            throw new MyException();
        }
    }

    static class MyException extends Exception {
    }
}
