package de.fhkn.in.uce.sip.ucesip.client;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.text.ParseException;

import javax.sdp.SdpException;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.message.Request;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import de.fhkn.in.uce.sip.core.ISipManager;
import de.fhkn.in.uce.sip.core.SipUser;
import de.fhkn.in.uce.sip.core.message.OkMessage;
import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;
import de.fhkn.in.uce.sip.ucesip.message.UCESipMessage;
import de.fhkn.in.uce.sip.ucesip.settings.UCESipClientSettings;

public class UCESipUserAgentClientImpliedRegisterMockTest {

	private UCESipUserAgentClient sut;
	private ISipManager sipManagerMock;

	@Before
	public void setup() throws UCESipException {
		sipManagerMock = createSipManagerDefaultMock();
		
		sut = new UCESipUserAgentClient("janosch", "hallo", "tscho", "hallo", new UCESipClientSettings(new InetSocketAddress("localhost",
				1234)), sipManagerMock);
	}

	@Test
	public void testInviteUCESipUAS() throws UCESipException, InterruptedException {
		sut.inviteUCESipUAS(new UCESipMessage());
	}

	@Test(expected = IllegalStateException.class)
	public void testDoubleInviteUCESipUAS() throws UCESipException, InterruptedException {
		sut.inviteUCESipUAS(new UCESipMessage());
		sut.inviteUCESipUAS(new UCESipMessage());
	}
	
	@Test
	public void testDeregisterUCESipUAC() throws UCESipException, InterruptedException {
		sut.inviteUCESipUAS(new UCESipMessage());
		sut.shutdown();
	}

	

	
	private ISipManager createSipManagerDefaultMock() {
		ISipManager sipManager = mock(ISipManager.class);

		try {
			
			Answer<Object> answ = new Answer<Object>() {

				@Override
				public Object answer(InvocationOnMock invocation) throws Throwable {

					OkMessage tMessage = mock(OkMessage.class);
					when(tMessage.getMessage()).thenReturn("as" + (new UCESipMessage()).serialize());
					when(tMessage.getFromUser()).thenReturn((SipUser) invocation.getArguments()[1]);
					when(tMessage.getRequestMethod()).thenReturn(Request.INVITE);

					final OkMessage tMsg = tMessage;

					Thread t = new Thread(new Runnable() {
						@Override
						public void run() {
							sut.onOk(tMsg);
						}
					});

					t.start();

					return null;
				}
			};
			doAnswer(answ).when(sipManager).sendInvite((SipUser) any(), (SipUser) any());
			doAnswer(answ).when(sipManager).sendInvite((SipUser) any(), (SipUser) any(), anyString());
			
		} catch (ParseException | InvalidArgumentException | SipException | SdpException e) {
			throw new RuntimeException(e);
		}

		return sipManager;
	}

}
