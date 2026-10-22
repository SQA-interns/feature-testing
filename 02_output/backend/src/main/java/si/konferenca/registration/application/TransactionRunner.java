package si.konferenca.registration.application;

/** Port: runs work in one database transaction, rolled back when the work throws. */
public interface TransactionRunner {

  void inTransaction(Runnable work);
}
