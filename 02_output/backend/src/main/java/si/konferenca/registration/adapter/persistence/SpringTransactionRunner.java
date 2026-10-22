package si.konferenca.registration.adapter.persistence;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import si.konferenca.registration.application.TransactionRunner;

/** {@link TransactionRunner} with Spring's transaction manager. */
public class SpringTransactionRunner implements TransactionRunner {

  private final TransactionTemplate template;

  public SpringTransactionRunner(PlatformTransactionManager transactionManager) {
    this.template = new TransactionTemplate(transactionManager);
  }

  @Override
  public void inTransaction(Runnable work) {
    template.executeWithoutResult(status -> work.run());
  }
}
