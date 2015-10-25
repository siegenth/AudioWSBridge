/*License (MIT)

Copyright Â© 2013 Matt Diamond

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
documentation files (the "Software"), to deal in the Software without restriction, including without limitation 
the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and 
to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of 
the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO 
THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
DEALINGS IN THE SOFTWARE.
*/

(function(window){

  var AUDIO_WORKER_PATH =   'js/recorderjs/recorderWorker.js';
  var WS_WORKER_PATH = 'js/recorderjs/wsWorker.js';
  var streamsCnt = 0;

  var Recorder = function(source, cfg){
    var config = cfg || {};
    var bufferLen = config.bufferLen || 4096;
    this.context = source.context;
    if(!this.context.createScriptProcessor){
       this.node = this.context.createJavaScriptNode(bufferLen, 2, 2);
    } else {
       this.node = this.context.createScriptProcessor(bufferLen, 2, 2);
    }

    // Setup WS thread'
    // TODO get the IP address from web context, Voci does this
    var wsWorker ;
    if ("WebSocket" in window) {
       var nodePort;                  // not debugging IP
      if (!nodePort) {
        nodePort = window.document.location.host;
      }
      var webSocketForwarderUrl  = nodePort + "/WebsocketApp/Forward";

      console.log("Connection using webSocketForwarderUrl:" + webSocketForwarderUrl);
      console.log("Connection using SessionId:" + SessionId);      
      wsWorker = new Worker(WS_WORKER_PATH);
      wsWorker.postMessage({
        command: 'init',
        config: {
          url: webSocketForwarderUrl,
          senderId: SessionId,
          audioType: "audio/wav",
          chunkSize: 1000
        }
      });
      console.log("setup wsWorker");
    }
    /// setup Audio thread
    var audioWorker = new Worker(config.workerPath || AUDIO_WORKER_PATH);
    audioWorker.postMessage({
      command: 'init',
      config: {
        sampleRate: this.context.sampleRate
      }
    });
    var recording = false, wsTransmitting = false,
      currCallback, wsCallback;

    this.node.onaudioprocess = function(e){
      if (!recording) return;
      if (wsTransmitting) {
//        console.log("recording:" + e.inputBuffer.getChannelData(0).length)
        wsWorker.postMessage({
          command: "chunk",
          chunkBuffer: [e.inputBuffer.getChannelData(0)]
        });
      }
      audioWorker.postMessage({
        command: 'record',
        buffer: [
          e.inputBuffer.getChannelData(0),
          e.inputBuffer.getChannelData(1)
        ]
      });
    }
  // TODO - what is this mapping?
    this.configure = function(cfg){
      for (var prop in cfg){
        if (cfg.hasOwnProperty(prop)){
          config[prop] = cfg[prop];
        }
      }
    }


    // send the data to Streams via webSockets
    this.wsTransmit = function(buffer){
      console.log("wsTransmit@recorder:" + buffer.length);
      wsWorker.postMessage({
        command: 'transmit',
        transmitBuffer: buffer
      });
    }
	console.log(document.getElementById("StreamsIP").value + "@recorder.js");

    this.wsStartChunking = function() {
    	console.log("start chunking");
      wsWorker.postMessage({
        command: "startChunking", 
        streamsIP: document.getElementById("StreamsIP").value,
        senderId: document.getElementById("StreamsID").value                
      });
      wsTransmitting = true;
    }

    this.wsStopChunking = function() {

      wsWorker.postMessage({
          command:"stopChunking"
      });
      wsTransmitting = false;
    }

    this.record = function(){
      recording = true;
    }

    this.stop = function(){
      recording = false;
    }

    this.clear = function(){
      audioWorker.postMessage({ command: 'clear' });
    }

    this.getBuffers = function(cb) {
      currCallback = cb || config.callback;
      audioWorker.postMessage({ command: 'getBuffers' });
    }

    this.exportWAV = function(cb, type){
      currCallback = cb || config.callback;
      type = type || config.type || 'audio/wav';
      if (!currCallback) throw new Error('Callback not set');
      audioWorker.postMessage({
        command: 'exportWAV',
        type: type
      });
    }

    this.exportMonoWAV = function(cb, type){
      currCallback = cb || config.callback;
      type = type || config.type || 'audio/wav';
      if (!currCallback) throw new Error('Callback not set');
      audioWorker.postMessage({
        command: 'exportMonoWAV',
        type: type
      });
    }

    this.exportSocketWAV = function(cb, type) {
      currCallback = cb || config.callback;
      type = type || config.type || 'audio/wav';
      if (!currCallback) throw new Error('Callback not set');
      audioWorker.postMessage({
        command: 'exportSocketWAV',
        type: type
      });
    }

    this.sendWebSocket = function(cb, type) {
      currCallBack = cb;
      wsWorker.postMessage({
        command: 'transmit',
        size: 1234
      });
    }


    audioWorker.onmessage = function(e){   // This is where the data is returned.
      var blob = e.data;
      currCallback(blob);
    }

    wsWorker.onmessage = function(e){   // This is where the data is returned.
      var command = e.data;
      console.log("wsWorker.onmessage : " + command);
      document.getElementById("StreamsCnt").value = streamsCnt++;    
      wsCallback && wsCallback(command);
    }

    source.connect(this.node);
    this.node.connect(this.context.destination);   // if the script node is not connected to an output the "onaudioprocess" event is not triggered in chrome.
  };// End of the Recorder object definition

  Recorder.setupDownload = function(blob, filename) {
    var url = (window.URL || window.webkitURL).createObjectURL(blob);
    var link = document.getElementById("save");
    link.href = url;
    link.download = filename || 'output.wav';
  }

  window.Recorder = Recorder;

})(window)
