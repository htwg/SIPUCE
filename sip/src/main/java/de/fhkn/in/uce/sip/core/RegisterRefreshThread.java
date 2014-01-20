package de.fhkn.in.uce.sip.core;

import java.text.ParseException;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterRefreshThread extends Thread {
	
	private final SipUser sipUser;
	private final ISipManager sipMessageHandler;
	private  boolean terminate;

	// Time in seconds to send register before binding expired
	private static final int REFRESH = 5;
	private static final int FACTOR = 5;
	private static final int TIMEOUT = 1000;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(RegisterRefreshThread.class);

	public RegisterRefreshThread(
			final ISipManager iSipManager,
			final SipUser sipUser) {
		this.sipUser = sipUser;
		this.sipMessageHandler = iSipManager;
	}

	@Override
	public void run() {
		LOGGER.debug("SIP - RegisterRefreshHandler Thread started.");
		int expires = sipMessageHandler.getPreferences().getExpireTime();
		LOGGER.debug("Updates every " +(expires - REFRESH) + "s");

		if (expires - FACTOR < REFRESH) {
			throw new IllegalArgumentException(
					"SIP - Expire Time is smaller than 10 seconds.");
		}

		try {
			Thread.sleep((expires - REFRESH) * TIMEOUT);
			while (!terminate) {
				sipMessageHandler.register(sipUser);
				LOGGER.debug("SIP - Registration of User: '"
						+ sipUser.getUsername() + "' refreshed.");
				Thread.sleep((expires - REFRESH) * TIMEOUT);
			}

		} catch (ParseException | InvalidArgumentException | SipException e) {
			LOGGER.warn(e.getMessage());
		} catch (IllegalStateException e) {
			//Called when Sip core is shutdown.
			LOGGER.warn(e.getMessage());
		} catch (InterruptedException e) {
			// This exception occurs if the thread is interrupted while waiting
			// (wait or sleep) - It is ok
		}
		LOGGER.debug("SIP - RegisterRefreshHandler Thread exited.");
	}

	public void terminate() {
		terminate = true;
	}
}
