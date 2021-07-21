package components;

import java.util.ArrayList;
import java.util.List;

public class SlackResponse {
    private String text;

    private String responseType;    // default response_type in slack is "ephemeral"


    public SlackResponse(){

    }
    public SlackResponse(String text) {
        this.text = text;
    }


    public String getText() {
        return text;
    }

    public SlackResponse setText(String text) {
        this.text = text;
        return this;
    }

    public String getResponseType() {
        return responseType;
    }

    public SlackResponse setResponseType(String responseType) {
        this.responseType = responseType;
        return this;
    }



}
