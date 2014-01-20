package de.fhkn.in.uce.sip.core;

import java.text.ParseException;

import javax.sip.PeerUnavailableException;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.ToHeader;

/**
 * 
 * @author Felix
 *
 */
public class SipUser implements Comparable<SipUser>{
	private String username;
	private String domain;


	/**
	 * Creates a new SipUser with the specified username and domain.
	 * it creates a
	 * @param username
	 * @param domain
	 * @param tag
	 */
	public SipUser(final String username, final String domain) {
		super();
		this.username = username;
		this.domain = domain;
	}

	/**
	 * @return the username
	 */
	public final String getUsername() {
		return username;
	}
	/**
	 * @param username the username to set
	 */
	public final void setUsername(final String username) {
		//TODO Validation
		this.username = username;
	}
	/**
	 * @return the domain
	 */
	public final String getDomain() {
		return domain;
	}
	/**
	 * @param domain the domain to set
	 */
	public final void setDomain(final String domain) {
		//TODO Validation
		this.domain = domain;
	}

	/**
	 * Create a javax.sip.header.FromHeader for this SipUser
	 * 
	 * @param addressFactory
	 *            an instance of an AddressFactory, created by the SipManager
	 * @param headerFactory
	 *            an instance of a HeaderFactory, created by the SipManager
	 * @return the FromHeader for this LocalSipProfile
	 * @throws PeerUnavailableException
	 * @throws ParseException
	 */
	public FromHeader getFromHeader(
			final AddressFactory addressFactory,
			final HeaderFactory headerFactory,
			final String tag)
					throws PeerUnavailableException, ParseException {
		SipURI fromAddress = addressFactory.createSipURI(username, domain);
		Address fromNameAddress = addressFactory.createAddress(fromAddress);
		fromNameAddress.setDisplayName(username);

		String nTag = tag;
		if (tag == null) {
			// TODO: globally unique (cryptographically) RFC 3261 (19.3)
			nTag = String.valueOf(Math.random() * Long.MAX_VALUE);
		}
		return headerFactory.createFromHeader(fromNameAddress, nTag);
	}

	/**
	 * Create a javax.sip.header.ToHeader for this SipUser
	 * 
	 * @param addressFactory
	 *            an instance of an AddressFactory, created by the SipManager
	 * @param headerFactory
	 *            an instance of a HeaderFactory, created by the SipManager
	 * @return the ToHeader created for this LocalSipProfile
	 * @throws PeerUnavailableException
	 * @throws ParseException
	 */
	public ToHeader getToHeader(final AddressFactory addressFactory, final HeaderFactory headerFactory)
			throws PeerUnavailableException, ParseException {
		SipURI fromAddress = addressFactory.createSipURI(username, domain);
		Address fromNameAddress = addressFactory.createAddress(fromAddress);
		fromNameAddress.setDisplayName(username);
		return headerFactory.createToHeader(fromNameAddress, null);
	}

	@Override
	public String toString() {
		return String.format("<sip:%s@%s>", username, domain);
	}

	public static SipUser parseFromHeader(final ToHeader to) {
		URI uri = to.getAddress().getURI();
		if (uri.isSipURI()) {
			SipURI sipUri = (SipURI) uri;
			sipUri.getUser();
			SipUser user = new SipUser(sipUri.getUser(), sipUri.getHost());
			//TODO more information in SipURI than in SipUser
			return user;
		}
		return null;
	}

	public static SipUser parseFromHeader(final FromHeader from) {
		URI uri = from.getAddress().getURI();
		if (uri.isSipURI()) {
			SipURI sipUri = (SipURI) uri;
			sipUri.getUser();
			SipUser user = new SipUser(sipUri.getUser(), sipUri.getHost());
			//TODO more information in SipURI than in SipUser
			return user;
		}
		return null;
	}

	@Override
	public int compareTo(final SipUser o) {
		return toString().compareTo(o.toString());
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SipUser)) {
			return false;
		}
		SipUser other = (SipUser)obj;
		return this.username.equals(other.getUsername())
				&& this.domain.equals(other.getDomain());
	}

	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
}
