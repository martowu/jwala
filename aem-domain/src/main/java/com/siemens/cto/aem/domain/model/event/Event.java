package com.siemens.cto.aem.domain.model.event;

import java.io.Serializable;

import com.siemens.cto.aem.domain.model.app.UploadWebArchiveCommand;
import com.siemens.cto.aem.domain.model.audit.AuditEvent;

public class Event<T> implements Serializable {

	private static final long serialVersionUID = 1L;

	private final T command;
	private final AuditEvent auditEvent;

	public Event(final T theCommand, final AuditEvent theAuditEvent) {
		command = theCommand;
		auditEvent = theAuditEvent;
	}

	public T getCommand() {
		return command;
	}

	public AuditEvent getAuditEvent() {
		return auditEvent;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final Event<?> event = (Event<?>) o;

		if (auditEvent != null ? !auditEvent.equals(event.auditEvent)
				: event.auditEvent != null) {
			return false;
		}
		if (command != null ? !command.equals(event.command)
				: event.command != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = command != null ? command.hashCode() : 0;
		result = 31 * result + (auditEvent != null ? auditEvent.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Event{" + "command=" + command + ", auditEvent=" + auditEvent
				+ '}';
	}

    public static <T> Event<T> create(T cmd, AuditEvent ae) {
        return new Event<T>(cmd, ae);
    }
}
