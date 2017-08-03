package com.mymensorar.cognitoclient;

import android.util.Log;


/**
 * This class is used to parse the response of the Login request of the sample
 * Cognito developer authentication and convert it into LoginResponse object
 */
public class LoginResponseHandler extends ResponseHandler {
    /*
    private final String decryptionKey;

    public LoginResponseHandler(final String decryptionKey) {
        this.decryptionKey = decryptionKey;
    }
    */

    public Response handleResponse(int responseCode, String responseBody) {
        if (responseCode == 200) {
            try {
                String json = responseBody;
                Log.d("LoginResponseHandler","MyMensor User Group: "+Utilities.extractElement(json, "usergroup"));
                return new LoginResponse(Utilities.extractElement(json, "key"),Utilities.extractElement(json, "usergroup"));
            } catch (Exception exception) {
                return new LoginResponse(500, exception.getMessage());
            }
        } else {
            return new LoginResponse(responseCode, responseBody);
        }
    }
}
