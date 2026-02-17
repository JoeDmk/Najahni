package services;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.io.InputStream;
import java.util.Properties;

public class TwilioService {
    private final String accountSid;
    private final String authToken;
    private final String twilioPhoneNumber;

    public TwilioService() {
        Properties props = loadSecrets();
        this.accountSid = props.getProperty("twilio.account_sid", "");
        this.authToken = props.getProperty("twilio.auth_token", "");
        this.twilioPhoneNumber = props.getProperty("twilio.phone_number", "");
        Twilio.init(accountSid, authToken);
    }

    private Properties loadSecrets() {
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/secrets.properties")) {
            if (is != null) props.load(is);
        } catch (Exception e) {
            System.err.println("Could not load secrets.properties: " + e.getMessage());
        }
        return props;
    }

    public void sendSms(String to, String message) {
        PhoneNumber toNumber = new PhoneNumber(to);
        PhoneNumber fromNumber = new PhoneNumber(twilioPhoneNumber);
        Message.creator(toNumber, fromNumber, message).create();
    }
}
