package hello.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 트랜잭션 AOP 주의 사항 - 프록시 내부 호출
 - @Transactional 적용시 프록시 객체가 요청을 먼저 받아 트랜잭션을 처리하고, 실제 객체를 호출함
 - 트랜잭션을 적용하려면 항상 프록시를 통해 대상 객체를 호출해야 함
 - 만약 프록시를 거치지 않고 대상 객체를 직접 호출하게 되면 AOP 가 적용되지 않고, 트랜잭션도 적용되지 않음
 - AOP 를 적용하면 스프링은 대상 객체 대신 프록시를 스프링빈으로 등록하기 때문에 프록시 객체가 주입되어 대상 객체를 직접 호출하는 문제는 일반적으로 발생하지 않음

 * 대상 객체의 내부에서 메서드 호출이 발생하면 프록시를 거치지 않고 대상 객체를 직접 호출하는 문제가 발생
 - 이러한 상황에서는 @Transactional 이 있어도 트랜잭션이 적용되지 않음
 - 실무에서 반드시 한번씨 만나는 문제로 고생할 수 있음!!

 * externalCall()
 - internal() 을 내부에서 호출하여 트랜잭션이 적용되는 것 처럼 보임 -> internal() 에 @Transactional 이 있으므로
 - 하지만 트랜잭션이 적용되지 않음!!

 * 트랜잭션이 적용되지 않는 문제 원인
 - 자바 언어에서 별도의 참조자가 없는 경우 -> this 의 의미로 자기 자신의 인스턴스를 가르킴
 - internal() 은 자신의 내부 메서드를 호출하는 this.internal() 이 됨
 - this 는 자기 자신을 가리키므로, 프록시가 아닌 실제 대상 객체(target)의 인스턴스를 가르킴
 - 결과적으로 내부 호출(target 에 있는 internal() 을 직접 호출)은 프록시를 거치지 않기 때문에 트랜잭션을 적용할 수 없음
 */
@Slf4j
@SpringBootTest
class InternalCallV1Test {

    @Autowired
    CallService callService;

    @Test
    void printProxy() {
        log.info("callService class = {}", callService.getClass());
    }

    @Test
    void internalCall() {
        callService.internal();
    }

    @Test
    void externalCall() {
        callService.external();
    }

    @TestConfiguration
    static class InternalCallV1Config {
        @Bean
        CallService callService() {
            return new CallService();
        }
    }

    @Slf4j
    static class CallService {

        public void external() {
            log.info("call external");
            printTxInfo();
            internal();
        }

        @Transactional
        public void internal() {
            log.info("call internal");
            printTxInfo();
        }

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active = {}", txActive);
        }
    }
}
