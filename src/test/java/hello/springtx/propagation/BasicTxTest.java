package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
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

    /**
     * 스프링 트랜잭션 전파(트랜잭션 전파 옵션 REQUIRED)
     - 외부 트랜잭션 수행중, 내부 트랜잭션 추가로 수행
     - outer -> true, inner -> false
     - 내부 트랜잭션을 시작하는 시점에 이미 외부 트랜잭션은 진행중이며 이 경우 내부 트랜잭션이 외부 트랜잭션에 참여
     -> 내부 트랜잭션이 외부 트랜잭션을 그대로 이어 받아 따름
     -> 외부 트래잭션(논리 트랜잭션1)과 내부 트랜잭션(논리 트랜잭션2)이 하나의 물리 트랜잭션으로 묶이는 것

     * 커밋
     - 아래 코드의 경우 커밋을 두 번 호출
     - 트랜잭션을 생각해보면 하나의 커넥션에 커밋을 한 번만 호출 할 수 있음 -> 커밋이나 롤백을 하면 해당 트랜잭션은 끝남
     - 로그 확인 결과 내부 트랜잭션 커밋의 경우 DB 커넥션을 통해 커밋하는 로그를 확인할 수 없음
     -> 외부 트랜잭션만 물리 트랜잭션을 시작하고, 커밋
     -> 내부 트랜잭션이 실제 물리 트랜잭션을 커밋하면 트랜잭션이 끝나버리기 때문에, 처음 시작한 외부 트랜잭션까지 이어갈 수 없음
     -> 내부 트랜잭션은 DB 커넥션을 통한 물리 트랜잭션은 커밋하면 안됨
     -> 스프링은 여러 트랜잭션이 함께 사용될 경우 처음 트랜잭션을 시작한 외부 트랜잭션이 실제 물리 트랜잭션을 관리하도록 함

     * 정리
     - 트랜잭션 매니저에 커밋을 호출한다고 해서 항상 실제 커넥션에 물리 커밋이 발생하지는 않음
     - 신규 트랜잭션인 경우에만 실제 커넥션을 사용해서 물리 커밋과 롤백을 수행, 신규 트랜잭션이 아닌 경우 실제 물리 커넥션을 사용하지 않음
     - 트랜잭션이 내부에서 추가로 사용되면 트랜잭션 매니저에 커밋하는 것이 항상 물리 커밋으로 이어지지 않음
     -> 논리 트랜잭션과 물리 트랜잭션으로 나누어 설명
     - 트랜잭션이 내부에서 추가로 사용되면 트랜잭션 매니저를 통해 논리 트랜잭션을 관리하고, 모든 논리 트랜잭션이 커밋되면 물리 트랜잭션이 커밋!
     */
    @Test
    void inner_commit() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outer.isNewTransaction() = {}", outer.isNewTransaction());

        log.info("내부 트랜잭션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("inner.isNewTransaction() = {}", inner.isNewTransaction());

        log.info("내부 트랜잭션 커밋");
        txManager.commit(inner);

        log.info("외부 트랜잭션 커밋");
        txManager.commit(outer);
    }

    /**
     * 외부 롤백
     - 논리트랜잭션이 하나라도 롤백되면 전체 물리 트랜잭션을 롤백 됨
     -> 내부 트랜잭션이 커밋했어도, 내부 트랜잭션 안에서 저장한 데이터도 모두 롤백 됨
     */
    @Test
    void outer_rollback() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("내부 트랜잭션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("내부 트랜잭션 커밋");
        txManager.commit(inner);

        log.info("외부 트랜잭션 롤백");
        txManager.rollback(outer);
    }

    /**
     * 내부 롤백
     - 내부 트랜잭션이 롤백했지만 내부 트랜잭션은 물리 트랜잭션에 영향을 주지 않음, 그런데 외부 트랜잭션은 커밋을 해버림
     -> 지금까지 보면 외부 트랜잭션만 물리 트랜잭션에 영향을 주기 때문에 물리 트랜잭션이 커밋될 것 같음

     * 정리
     - 논리 트랜잭션이 하라나도 롤백되면 물리 트랜잭션은 롤백
     - 내부 트랜잭션이 롤백되면 롤백 전용 마크를 표시(rollbackOnly = true)
     - 외부 트랜잭션을 커밋할 때 롤백 전용 마크를 확인하고 마크 표시가 있으면 트랜잭션을 롤백하고, UnexpectedRollbackException 예외를 던짐
     */
    @Test
    void inner_rollback() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("내부 트랜잭션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("내부 트랜잭션 롤백");
        txManager.rollback(inner);  //rollback-only 표시

        log.info("외부 트랜잭션 커밋");
        Assertions.assertThatThrownBy(() -> txManager.commit(outer))
                .isInstanceOf(UnexpectedRollbackException.class);
    }

    /**
     * REQUIRES_NEW
     - 외부 트랜잭션과 내부 트랜잭션을 완전히 분리해서 각각 별도의 물리 트랜잭션을 사용하는 방법 -> 커밋과 롤백도 각각 별도로 이루어짐
     - 해당 방법은 내부 트랜잭션에 문제가 발생해서 롤백해도, 외부 트랜잭션에 영향을 주지 않음
     - 외부 트랜잭션에 문제가 발생해도 내부 트랜잭션에 영향을 주지 않음

     * 정리
     - REQUIRES_NEW 옵션을 사용하면 물리 트랜잭션이 명확하게 분리
     - REQUIRES_NEW 옵션을 사용하면 커넥션이 동시에 2개 사용된다는 점을 주의

     * 다양한 전파 옵션
     1. REQUIRED : 가장 많이 사용하는 기본 설정으로 기존 트랜잭션이 없으면 생성하고, 있으면 참여
     2. REQUIRES_NEW : 항상 새로운 트랜잭션을 생성
     3. SUPPORT : 트랜잭션을 지원한다는 뜻으로, 기존 트랜잭션이 없으면 없는대로 진행하고 있으면 참여
     4. NOT_SUPPORT : 트랜잭션을 지원하지 않는다는 뜻으로, 기존 트랜잭션이 유무에 관계 없이 트랜잭션 없이 진행(기존 트랜잭션은 보류)
     5. MANDATORY : 트랜잭션이 반드시 있어야함(기존 트랜잭션에 참여), 기존 트랜잭션이 없는 경우 예외(IllegalTransactionStateException) 발생
     6. NEVER : 트랜잭션을 사용하지 않음, 기존 트랜잭션이 있는 경우 예외(IllegalTransactionStateException) 발생
     7. NESTED : 기존 트랜잭션이 없으면 새로운 트랜잭션을 생성, 기존 트랜잭션이 있는 경우 중첨 트랜잭션을 만듦
     - 중첩 트랜잭션은 외부 트랜잭션의 영향을 받지만, 외부에 영향을 주지 않음
     - 중첩 트랜잭션은 롤백되어도 외부 트랜잭션은 커밋할 수 있음
     - 외부 트랜잭션이 롤백 되면 중첩 트랜잭션도 함께 롤백

     * 참고
     - isolation, timeout, readOnly 는 트랜잭션이 처음 시작될 때만 적용되고, 트랜잭션에 참여하는 경우에는 적용되지 않음
     */
    @Test
    void inner_rollback_requires_new() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outer.isNewTransaction() = {}", outer.isNewTransaction());

        log.info("내부 트랜잭션 시작");
        DefaultTransactionAttribute definition = new DefaultTransactionAttribute();
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        TransactionStatus inner = txManager.getTransaction(definition);
        log.info("inner.isNewTransaction() = {}", inner.isNewTransaction());

        log.info("내부 트랜잭션 롤백");
        txManager.rollback(inner);  //롤백

        log.info("외부 트랜잭션 커밋");
        txManager.commit(outer);    //커밋
    }
}
