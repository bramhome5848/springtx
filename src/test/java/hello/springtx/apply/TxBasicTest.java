package hello.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 트랜잭션 적용 확인
 - @Transactional 을 통해 선언적 트랜잭션 방식 사용시 단순히 어노테이션 하나로 트랜잭션 적용 가능
 -> 트랜잭션 관련 코드가 눈에 보이지 않고 AOP 기반으로 동작하기 때문에 실제 트랜잭션이 적용되었는지 확인하기 쉽지 않음

 * @Transactional
 - 해당 어노테이션이 메서드나 클래스에 하나라도 있을 경우 해당 객체는 트랜잭션 AOP 적용 대상이 됨
 - 실제 객체 대신 트랜잭션을 처리해주는 프록시 객체가 스프링 빈에 등록되고, 주입을 받을 때도 실제 객체 대신 프록시 객체가 주입됨

 * 구조
 - txBasicTest -- 참조 --> basicService$$CGLIB(proxy) -- 참조 --> basicService(실제)
 - Proxy 는 실제 객체를 상속받아 만들어져 있기 때문에 basicService 대신에 Proxy 인 basicService$$CGLIB 를 주입할 수 있음

 * 호출 순서
 - tx() -> proxy 의 tx() -> 트랜잭션 적용 확인 -> 실제 tx()
 - nonTx() -> proxy 의 nonTx() -> 트랜잭션 적용 확인 -> 실제 nonTx()
 -> 트랜잭션 적용 확인 단계에서 트랜잭션 적용 대상인 경우(@Transactional) 트랜잭션을 시작하고, 실제 호출이 마무리 된 후 트랜잭션을 종요함
 */
@Slf4j
@SpringBootTest
class TxBasicTest {

    @Autowired
    BasicService basicService;

    @Test
    void proxyCheck() {
        log.info("aop class = {}", basicService.getClass());
        assertThat(AopUtils.isAopProxy(basicService)).isTrue();
    }

    @Test
    void txTest() {
        basicService.tx();
        basicService.nonTx();
    }

    @TestConfiguration
    static class TxApplyBasicConfig {
        @Bean
        BasicService basicService() {
            return new BasicService();
        }
    }

    @Service
    static class BasicService {

        @Transactional
        public void tx() {
            log.info("call tx");
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active = {}", txActive);
        }

        public void nonTx() {
            log.info("call nonTx");
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active = {}", txActive);
        }
    }
}
