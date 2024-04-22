package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

import javax.sql.DataSource;

@Slf4j
@SpringBootTest
class BasicTxTest {

    @Autowired
    PlatformTransactionManager txManager;

    @TestConfiguration
    static class Config {
        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }
    }

    @Test
    void commit() {
        log.info("트랜잭션 시작");
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("트랜잭션 커밋 시작");
        txManager.commit(status);
        log.info("트랜잭션 커밋 완료");
    }

    /**
     * 트랜잭션 두 번 사용
     - 트랜잭션1 을 시작하고, 커넥션 풀에서 conn0 커넥션 획득 -> 트랜잭션1 을 커밋하고, 커넥션 풀에 conn0 커넥션 반납
     - 트랜잭션2 을 시작하고, 커넥션 풀에서 conn0 커넥션 획득 -> 트랜잭션2 을 커밋하고, 커넥션 풀에 conn0 커넥션 반납
     -> 로그를 보면 같은 커넥션(conn0)을 사용중이지만 중간에 커넥션 풀에 반납 후 다시 획득했기 때문에 다른 커넥션으로 보는 것이 맞음!

     * 커넥션 구분
     - 히카리 커넥션 풀에서 커넥션 획득시 커넥션을 그대로 반환하는 것이 아닌 히카리 프록시 커넥션이라는 객체를 생성하고 그 안에 실제 커넥션을 포함시켜 반환
     -> 객체의 주소를 확인하면 커넥션 풀에서 획득한 커넥션을 구분할 수 있음
     ex) 트랜잭션1 : Acquired Connection [HikariProxyConnection@5072587 wrapping conn0]
         트랜잭션2 : Acquired Connection [HikariProxyConnection@574999722 wrapping conn0]
     */
    @Test
    void rollback() {
        log.info("트랜잭션 시작");
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("트랜잭션 롤백 시작");
        txManager.rollback(status);
        log.info("트랜잭션 롤백 완료");
    }

    @Test
    void double_commit() {
        log.info("트랜잭션1 시작");
        TransactionStatus tx1 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션1 커밋");
        txManager.commit(tx1);

        log.info("트랜잭션2 시작");
        TransactionStatus tx2 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션2 커밋");
        txManager.commit(tx2);
    }

    /**
     * 전체 트랜잭션을 묶지 않고 각각 관리했기 때문에, 트랜잭션1 에서 저장한 데이터는 커밋되고, 트랜잭션2 에서 저장한 데이터는 롤백
     */
    @Test
    void double_commit_rollback() {
        log.info("트랜잭션1 시작");
        TransactionStatus tx1 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션1 커밋");
        txManager.commit(tx1);

        log.info("트랜잭션2 시작");
        TransactionStatus tx2 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜잭션2 롤백");
        txManager.rollback(tx2);
    }
}
