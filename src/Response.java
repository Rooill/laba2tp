package src;
import java.util.HashMap;
import java.util.Map;

public class Response {
    private final String NEW_LINE = "\r\n";
    private final String VERSION = "HTTP/1.1";
    private String message;
    private Map<String, String> headers = new HashMap<>();
    private String body;
    private String statusLine;

    void addHeader(String header, String value) {
        headers.put(header, value);
    }

    void addBody(String body) {
        this.body = body;
        String valueContentLength = Integer.toString(body.length());
        headers.put("Content-Length", valueContentLength);
    }

    void addResponseCode(String code, String meaning) {
        StringBuffer buff = new StringBuffer();
        buff.append(VERSION)
                .append(" ")
                .append(code)
                .append(" ")
                .append(meaning);
        statusLine = buff.toString();
    }

    String message() {
        StringBuffer message = new StringBuffer();
        message.append(statusLine)
                .append(NEW_LINE);

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            message.append(entry.getKey())
                    .append(" : ")
                    .append(entry.getValue())
                    .append(NEW_LINE);
        }

        message.append(NEW_LINE)
                .append(body);

        return message.toString();
    }
}
