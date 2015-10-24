package wasdev.sample.websocket.forward;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;

class ClientSocket extends WebSocketClient {

    ClientSocket(URI uri) {
        super(uri);
        this.connect();
        System.out.println("connected@ClientSocket callback");
    }

    @Override
    public void onMessage(String message) {
        System.out.println("onMessage@ClientSocket:" + message);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("opened connection@ClientSocket");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("closed connection@ClientSocket");
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }
}