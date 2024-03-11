package org.exactlearner.connection;
import java.net.MalformedURLException;
import java.net.URL;

public interface Bridge {

    boolean checkConnection(String ip, int port, String key);

    boolean checkConnection(String stringURL, String key);

    String ask(String message, String key);

    URL getURL(String ip, int port) throws MalformedURLException;

    URL getURL(String stringURL) throws MalformedURLException;

}


abstract class BasicBridge implements Bridge {

    public URL getURL(String ip, int port) throws MalformedURLException {
        return new URL(ip.concat(":".concat(String.valueOf(port))));
    }

    public URL getURL(String stringURL) throws MalformedURLException {
        return new URL(stringURL);
    }
}
