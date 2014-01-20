package de.fhkn.in.uce.sip.ucesip.message;

public interface IUCESipMessageData {
    String serialize();

    void deserialize(String data);
}