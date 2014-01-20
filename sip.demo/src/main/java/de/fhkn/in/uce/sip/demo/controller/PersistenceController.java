package de.fhkn.in.uce.sip.demo.controller;

import java.util.List;

import com.db4o.Db4oEmbedded;
import com.db4o.ObjectContainer;
import com.db4o.query.Predicate;

import de.fhkn.in.uce.sip.core.SipUAInstance;

/**
 * Controls the persistent access to SipInstanceIds.
 * @author Felix
 *
 */
public class PersistenceController {

	private ObjectContainer db;

	/**
	 * Stores a SipClientInstance.
	 * @param instance
	 */
	public void storeInstance(final SipUAInstance instance) {
		db.store(instance);
	}

	/**
	 * Returns an SipClientInstance by the given name.
	 * @param instanceName the instance name
	 * @return the SipClientInstance with its URN
	 */
	public SipUAInstance getInstanceByName(final String instanceName) {
		List<SipUAInstance> instances = db.query(new Predicate<SipUAInstance>() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean match(final SipUAInstance instance) {
				return instance.getInstanceName().equals(instanceName);
			}
		});

		if (instances.isEmpty()) {
			return null;
		}
		return instances.get(0);
	}

	/**
	 * Opens a connection to the database.
	 * @param database the location of the database
	 */
	public void open(final String database) {
		db = Db4oEmbedded.openFile(Db4oEmbedded.newConfiguration(), database);
	}

	/**
	 * Closes the connection to the database.
	 */
	public void close() {
		db.close();
	}
}
