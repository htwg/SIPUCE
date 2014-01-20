package de.fhkn.in.uce.sip.core;

import java.util.UUID;

/**
 * This Class represents one instance of a client. 
 * <p>
 * Each UA MUST have an Instance Identifier Uniform Resource Name (URN)
 * that uniquely identifies the device. Usage of a URN
 * provides a persistent and unique name for the UA instance.  It also
 * provides an easy way to guarantee uniqueness within the AOR.  This
 * URN MUST be persistent across power cycles of the device.  The
 * instance ID MUST NOT change as the device moves from one network to
 * another.<br/>
 * See <a href="http://tools.ietf.org/html/rfc5626#section-4.1">rfc5626</a>
 * </p>
 * Since we use to instances in the same folder, i'd suggest that we store a
 * UUID per user to avoid conflicts.
 * @author Felix
 *
 */
public class SipUAInstance {
	
	/** The instance name. */
	private String instanceName;
	
	/** Instance Identifier Uniform Resource Name (URN) */
	private UUID instanceId;

	/**
	 * Creates a new SipClientInstance creating a random instance id.
	 * @param instanceName the instance name
	 */
	public SipUAInstance(String instanceName) {
		super();
		this.instanceName = instanceName;
		this.instanceId = UUID.randomUUID();
	}

	/**
	 * Creates a new SipClientInstance with the given instance id and name.
	 * @param instanceName
	 * @param instanceId
	 */
	public SipUAInstance(String instanceName, UUID instanceId) {
		super();
		this.instanceName = instanceName;
		this.instanceId = instanceId;
	}
	
	/**
	 * @return the instanceName
	 */
	public final String getInstanceName() {
		return instanceName;
	}

	/**
	 * @param instanceName the instanceName to set
	 */
	public final void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	/**
	 * @return the instanceId
	 */
	public final UUID getInstanceId() {
		return instanceId;
	}

	/**
	 * @param instanceId the instanceId to set
	 */
	public final void setInstanceId(UUID instanceId) {
		this.instanceId = instanceId;
	}


	
}
