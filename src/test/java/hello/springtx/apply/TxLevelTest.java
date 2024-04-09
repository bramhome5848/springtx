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
 * 트랜잭션 적용 위치
 - 스프링에서 우선순위는 더 구체적이고 자세한 것이 더 높음
 -> 메서드와 클래스에 어노테이션을 붙일 수 있다면 더 구체적인 메서드가 더 높은 우선순위를 가짐
 -> 인터페이스와 구현 클래스에 어노테이션을 붙일 수 있다면 더 구체적인 클래스가 더 높은 우선순위를 가짐

 * @Transactional 의 두 가지 규칙
 1. 우선 순위
 - 구체적이고 자세한 것이 우선순위가 높음으로 해당 규칙을 따름
 2. 클래스에 적용시 메서드는 자동 적용
 - 메서드에 @Transactional 이 없고 클래스에 적용되어 있는 경우 상위 존재의 설정을 따른다

 * 인터페이스에 @Transactional 적용 우선순위
 1. 클래스의 메서드
 2. 클래스의 타입
 3. 인터페이스의 메서드
 4. 인터페이스의 타입
 - 스프링 공식 메뉴얼에서는 인터페이스에 어노테이션을 둘 경우 AOP 적용이 되지 않는 경우도 있기 때문에 구체 클래스에 사용할 것을 권장
 */
@SpringBootTest
class TxLevelTest {

    @Autowired
    LevelService service;

    @TestConfiguration
    static class TxApplyLevelConfig {
        @Bean
        LevelService levelService() {
            return new LevelService();
        }
    }
    @Test
    void orderTest() {
        service.write();
        service.read();
    }

    @Slf4j
    @Transactional(readOnly = true) //읽기전용 transaction
    static class LevelService {

        @Transactional(readOnly = false)
        public void write() {
            log.info("call write");
            printTxInfo();
        }

        public void read() {
            log.info("call read");
            printTxInfo();
        }

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();

            log.info("tx active = {}", txActive);
            log.info("tx readOnly = {}", readOnly);
        }
    }
}
