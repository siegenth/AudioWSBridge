package wasdev.sample.websocket.backward;
/**
 * Getting messages back from Steams and forwarding them onto
 * the original connector. 
 * @author siegenth
 *
 */

public interface BackwardResponse {
	void responseFromStreams(String msg);
	void shutdownFromStreams();
}
