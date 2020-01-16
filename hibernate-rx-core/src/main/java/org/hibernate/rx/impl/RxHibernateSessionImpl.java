package org.hibernate.rx.impl;

import java.util.function.Consumer;

import org.hibernate.engine.spi.ExceptionConverter;
import org.hibernate.engine.spi.SessionDelegatorBaseImpl;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.ExceptionMapperStandardImpl;
import org.hibernate.resource.transaction.backend.jta.internal.synchronization.ExceptionMapper;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.rx.RxHibernateSession;
import org.hibernate.rx.RxSession;
import org.hibernate.rx.engine.spi.RxActionQueue;
import org.hibernate.rx.engine.spi.RxHibernateSessionFactoryImplementor;

public class RxHibernateSessionImpl extends SessionDelegatorBaseImpl implements RxHibernateSession, EventSource {

	private final RxHibernateSessionFactoryImplementor factory;
	private transient RxActionQueue rxActionQueue;

	public RxHibernateSessionImpl(RxHibernateSessionFactoryImplementor factory, SessionImplementor delegate) {
		super( delegate );
		this.factory = factory;
		this.rxActionQueue = new RxActionQueue( this );
	}

	@Override
	public TransactionCoordinator getTransactionCoordinator() {
		return super.getTransactionCoordinator();
	}

	@Override
	public RxHibernateSessionFactoryImplementor getSessionFactory() {
		return factory;
	}

	@Override
	public RxActionQueue getRxActionQueue() {
		return rxActionQueue;
	}

	public SessionImplementor delegate() {
		return super.delegate();
	}

	private boolean isTransactionFlushable() {
		if ( getCurrentTransaction() == null ) {
			// assume it is flushable - CMT, auto-commit, etc
			return true;
		}
		final TransactionStatus status = getCurrentTransaction().getStatus();
		return status == TransactionStatus.ACTIVE || status == TransactionStatus.COMMITTING;
	}

	// SessionImpl
	protected TransactionImplementor getCurrentTransaction() {
		return (TransactionImplementor) getTransaction();
	}

	@Override
	public RxSession reactive() {
		return new RxSessionImpl( factory, this );
	}

	@Override
	public void reactive(Consumer<RxSession> consumer) {
		consumer.accept( new RxSessionImpl( factory, this ) );
	}

	@Override
	public <T> T unwrap(Class<T> clazz) {
		if ( RxHibernateSession.class.isAssignableFrom( clazz ) ) {
			return (T) this;
		}
		return super.unwrap( clazz );
	}
}

