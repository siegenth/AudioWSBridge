/*License (MIT)

 Send data out over websocket interface.
    - Sends whole file (many buffers)
    - Streams live data out, one buffer at time.

 */
// TODO - need to state what type of data is being sent.
// TODO - init should have type - and any other constant
var recLength = 0, recBuffersL = [], recBuffersR = [], wsUrl, senderId, audioType, wsSnd, window;

this.onmessage = function(e) {
	console.log("wsWorker dispatcher:" + e.data.command);
	switch (e.data.command) {
	case 'init':
		init(e.data.config);
		break;
	case 'transmit':
		transmit(e.data.transmitBuffer);
		break;
	// in realtime send chunk of data
	case 'chunk':
		transmitChunk(e.data.chunkBuffer);
		break;
	case 'startChunking':
		senderId = e.data.senderId.replace('#', '_');
		transmitCommand("START", e.data.streamsIP); // IPAddress of Streams
													// instance.
		break;
	case 'stopChunking':
		transmitCommand("STOP");
		break;
	case 'close':
		break;
	}
};
/**
 * Init - setup the WS connection. - store the id, cannot use the '#' remove it.
 * 
 * @param config
 */

function init(config) {
	console.log("recorderWorker - init");
	senderId = config.senderId.replace('#', '_');
	audioType = config.audioType;

	var wsSoc = "ws://" + config.url;

	// connection to Streams
	console.log("Connecting to : " + wsSoc);
	wsSnd = new WebSocket(wsSoc);
	wsSnd.onopen = function() {
		// Web Socket is connected, send data using send()
		console.log("WS control sent");
	};

	wsSnd.onmessage = function(evt) {
		var received_msg = evt.data;
		// sndMsg.innerHTML = "Alert:<b>" + received_msg + "</b>";
	};
	wsSnd.onclose = function() {
		// sndMsg.innerHTML = "Connection closed....";
		this.postMessage("closed!");
		wsSnd = null;
	};
	// setInterval(buildMessage, 100);
}

function buildMessage(command, message) {
	console.log("buildMessage");
	return ("#" + senderId + "#" + command + "#" + (message || ""));
}

function transmitCommand(command, message) {
	wsSnd.send(buildMessage(command, message));
}

// TODO Send header state what we are sending - 
function transmit(fullBuffer) {
	wsSnd.send("<DATALENGTH>" + fullBuffer.length + "</DATALENGTH>");
	var siz = 2048;
	var idx, tmpBuf;
	for (idx = 0; idx <= fullBuffer.length; idx += siz) {
		tmpBuf = fullBuffer.slice(idx, idx + siz);
		wsSnd.send(tmpBuf);
	}
	if (idx != fullBuffer.length) {
		tmpBuf = fullBuffer.slice(idx, fullBuffer.length);
		wsSnd.send(tmpBuf);
	}
	wsSnd.send("<END></END>");
	console.log("TRANSMIT START");
}

/**
 * Convert the outbound audio data to ascii text, add a header to beginning of
 * each message. Receiving uses this to keep track of he Audio source coming in.
 * 
 * @param audioBlob
 */

function transmitAudioBytes(audioBlob) {
	var myReader = new FileReaderSync(); // FIX - Firefox does not support
											// FileReader()
	var arrayBuffer = myReader.readAsArrayBuffer(audioBlob);
	var binaryData = buildMessage("DATA", "");
	var view = new DataView(arrayBuffer);
	var viewLen = view.byteLength;
	for (var i = 0; i < viewLen; binaryData += ("00" + view.getUint8(i++)
			.toString(16)).substr(-2).toUpperCase())
		;
	console.log("socketSend@main " + binaryData.length);
	wsSnd.send(binaryData);
}

function floatTo16BitPCM(output, offset, input) {
	for (var i = 0; i < input.length; i++, offset += 2) {
		var s = Math.max(-1, Math.min(1, input[i]));
		output.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7FFF, true);
	}
}

function encodeChunk(samples, mono) {
	var buffer = new ArrayBuffer(samples.length * 2);
	var view = new DataView(buffer);

	// We are not sending the wav header, it is the responsiblity of the
	// receiver to build the .wav header since we do not know how much
	// Refer to the wavBuild script file that add on header.

	floatTo16BitPCM(view, 0, samples);

	return view; // this is where the audio file is setup. can we push this
					// to streams here?
	// we have multiple saves, where are the others happening.
}
// DOC - what is this doing???
function mergeFloat(recBuffers, recLength) {
	var result = new Float32Array(recLength);
	result.set(recBuffers, 0);
	return result;
}

//
// Prep chunk for transmission and send. 
function transmitChunk(chunkBuffer) {
	var bufFloat = mergeFloat(chunkBuffer[0], chunkBuffer[0].length);
	var dataview = encodeChunk(bufFloat, true);
	var audioBlob = new Blob([ dataview ], {
		type : audioType
	});
	transmitAudioBytes(audioBlob);
	this.postMessage("next");
}
