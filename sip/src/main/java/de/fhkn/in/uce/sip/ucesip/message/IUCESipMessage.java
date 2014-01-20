package de.fhkn.in.uce.sip.ucesip.message;

import java.util.Set;

import de.fhkn.in.uce.sip.ucesip.exception.UCESipException;

public interface IUCESipMessage {
    void addMessageData(String messageKey, IUCESipMessageData data);

    Set<String> getMessageKeys();

    <T extends IUCESipMessageData> T getMessageData(String messageKey,
            Class<T> classOfT) throws UCESipException;

    String serialize();
}
