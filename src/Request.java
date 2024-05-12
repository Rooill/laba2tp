package src;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Request {
    private final String body;
    private final String url;
    private final Method method;
    private final static String DELIMITER = "\r\n\r\n";
    private final static String NEW_LINE = "\r\n";
    private final static String HEADER_DELIMITER = ":";

    private final Map<String, String> headers;

    public Request(String message){
        String[] parts = message.split(DELIMITER);
        
        String[] headers = parts[0].split(NEW_LINE);
        
        String[] firstLine = headers[0].split(" ");
        this.method = Method.valueOf(firstLine[0]);

        this.url = firstLine[1]; 

        this.headers = Collections.unmodifiableMap(new HashMap<>(){
            {
                for(int i =1; i<headers.length; i++) {
                    String[] headerPart = headers[i].split(HEADER_DELIMITER,2);
                    put(headerPart[0].trim(), headerPart[1].trim());
                }
            }
        });


        String valueContentLength = this.headers.get("Content-Length");
        int contentLength = valueContentLength == null ? 0 : Integer.parseInt(valueContentLength);
        this.body = parts.length > 1 ? parts[1].substring(0, contentLength) : "";
    }

    public String getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Method getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }
}
