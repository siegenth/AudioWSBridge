/* Copyright 2013 Chris Wilson

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

window.AudioContext = window.AudioContext || window.webkitAudioContext;

var audioContext = new AudioContext();
var audioInput = null,
    realAudioInput = null,
    inputPoint = null,
    audioRecorder = null;
var rafID = null;
var analyserContext = null;
var canvasWidth, canvasHeight;
var recIndex = 0;
var saveAs = "file";
var sendBlob = "";


/* TODO: overview
 * Ootion mono/stereo
 * Option disk/socket
   * Field for node/port
*/

function saveSocket() {
    audioRecorder.exportSocketWAV(doneEncodingSocket);
}
function doneEncodingSocket( blobSocket ) {
    console.log("SEND socket interface : " + blobSocket.length);
}
function saveAudio() {
    audioRecorder.exportMonoWAV( doneEncoding );
    // could get mono instead by saying
    // audioRecorder.exportMonoWAV( doneEncoding );
    // could get stereo by saying
    // audioRecorder.exportWAV(doneEncoding);
}

/*
*
 */

function gotBuffers( buffers ) {
    var canvas = document.getElementById( "wavedisplay" );

    drawBuffer( canvas.width, canvas.height, canvas.getContext('2d'), buffers[0] );

    // the ONLY time gotBuffers is called is right after a new recording is completed - 
    // so here's where we should set up the download.
    audioRecorder.exportMonoWAV( doneEncoding );
}


function doneEncoding( blob ) {
    // setup the filename of the file that will be downloaded (saved).
    // TODO * blob is what we want to send to the sebsockets - or part of it...
    console.log(blob.get);
    sendBlob = blob.slice();
    console.log("blob:" + blob.size);
    console.log("sendBlob:" + sendBlob.size);
    Recorder.setupDownload( blob, "myRecording" + ((recIndex<10)?"0":"") + recIndex + ".wav" );
    recIndex++;   // Note : This is where the file index is incremented.
}

// TODO : remove this, you only need the socketSend() from the web page.
//        already removed the entry that invoked this from .html
function toggleSaveAs( e ) {
    if (saveAs == "file") {
        saveAs = "socket";
    } else {
        saveAs = "file";
    }
    e.text = saveAs;
    console.log("New saveAs:" + saveAs);
}


function toggleRecording( e ) {
    if (e.classList.contains("recording")) {
        // stop recording
        audioRecorder.stop();
        e.classList.remove("recording");
        audioRecorder.getBuffers( gotBuffers );
    } else {
        // start recording
        if (!audioRecorder)
            return;
        e.classList.add("recording");
        audioRecorder.clear();
        audioRecorder.record();
    }
}
/**
 * Turn on and off the sending of data to Streams.
 *
 * @param e
 */
function toggleStreams( e ) {
    if (e.classList.contains("recording")) {
        // stop recording
        audioRecorder.wsStopChunking();
        e.classList.remove("recording");
//        audioRecorder.getBuffers( gotBuffers );
    } else {
        // start recording
        if (!audioRecorder)
            return;
        e.classList.add("recording");
//        audioRecorder.clear();
        audioRecorder.wsStartChunking();
    }
}



/**
 * Send the captured audio down the socket interface.
 * This is for captured buffer that can be sent down
 * to the file system as well.
 *
 * Convert the blob that has been captured into
 * hex strings. Use the FileReader to extract data
 * from the blob and covert it to an ArrayBuffer.
 * Process the ArrayBuffer using the DataView that
 * allows extracting data in 'strict' data format.
 * (ie: not use Integer by uint8).
 *
 * Convert the Uint8 array into
 *
 * @param e
 */
function socketSend(e) {
    var myReader = new FileReader();
    myReader.addEventListener("loadend", function(e) {
        //console.log(e.srcElement.result);
        var arrayBuffer = e.target.result;
//        e.srcElement.length
        // TODO - move DataView into worker thread
        var binaryData = '';
        var view = new DataView(arrayBuffer);
        var viewLen = view.byteLength;
        // TODO - move convert into the worker thread.
        for (var i = 0; i < viewLen; binaryData += ("00" + view.getUint8(i++).toString(16)).substr(-2).toUpperCase());
        console.log("socketSend@main " + binaryData.length);
        audioRecorder.wsTransmit(binaryData);
    });
    myReader.readAsArrayBuffer(sendBlob);
    //myReader.readAsBinaryString(sendBlob);
}
function convertToMono( input ) {
    var splitter = audioContext.createChannelSplitter(2);
    var merger = audioContext.createChannelMerger(2);

    input.connect( splitter );
    splitter.connect( merger, 0, 0 );
    splitter.connect( merger, 0, 1 );
    return merger;
}

function cancelAnalyserUpdates() {
    window.cancelAnimationFrame( rafID );
    rafID = null;
}

function updateAnalysers(time) {
    if (!analyserContext) {
        var canvas = document.getElementById("analyser");
        canvasWidth = canvas.width;
        canvasHeight = canvas.height;
        analyserContext = canvas.getContext('2d');
    }

    // analyzer draw code here
    {
        var SPACING = 3;
        var BAR_WIDTH = 1;
        var numBars = Math.round(canvasWidth / SPACING);
        var freqByteData = new Uint8Array(analyserNode.frequencyBinCount);

        analyserNode.getByteFrequencyData(freqByteData); 

        analyserContext.clearRect(0, 0, canvasWidth, canvasHeight);
        analyserContext.fillStyle = '#F6D565';
        analyserContext.lineCap = 'round';
        var multiplier = analyserNode.frequencyBinCount / numBars;

        // Draw rectangle for each frequency bin.
        for (var i = 0; i < numBars; ++i) {
            var magnitude = 0;
            var offset = Math.floor( i * multiplier );
            // gotta sum/average the block, or we miss narrow-bandwidth spikes
            for (var j = 0; j< multiplier; j++)
                magnitude += freqByteData[offset + j];
            magnitude = magnitude / multiplier;
            var magnitude2 = freqByteData[i * multiplier];
            analyserContext.fillStyle = "hsl( " + Math.round((i*360)/numBars) + ", 100%, 50%)";
            analyserContext.fillRect(i * SPACING, canvasHeight, BAR_WIDTH, -magnitude);
        }
    }
    
    rafID = window.requestAnimationFrame( updateAnalysers );
}

function toggleMono() {
    if (audioInput != realAudioInput) {
        audioInput.disconnect();
        realAudioInput.disconnect();
        audioInput = realAudioInput;
    } else {
        realAudioInput.disconnect();
        audioInput = convertToMono( realAudioInput );
    }

    audioInput.connect(inputPoint);
}

function gotStream(stream) {
    inputPoint = audioContext.createGain();

    // Create an AudioNode from the stream.
    realAudioInput = audioContext.createMediaStreamSource(stream);
    audioInput = realAudioInput;
    audioInput.connect(inputPoint);

//    audioInput = convertToMono( input );

    analyserNode = audioContext.createAnalyser();
    analyserNode.fftSize = 2048;
    inputPoint.connect( analyserNode );
    
    //SessionId = Window.document.getElementById("StreamID").value;       
    //NodePort = Window.document.getElementById("StreamsIP").value;      
    audioRecorder = new Recorder( inputPoint );

    zeroGain = audioContext.createGain();
    zeroGain.gain.value = 0.0;
    inputPoint.connect( zeroGain );
    zeroGain.connect( audioContext.destination );
    updateAnalysers();
}

function initAudio() {
        if (!navigator.getUserMedia)
            navigator.getUserMedia = navigator.webkitGetUserMedia || navigator.mozGetUserMedia;
        if (!navigator.cancelAnimationFrame)
            navigator.cancelAnimationFrame = navigator.webkitCancelAnimationFrame || navigator.mozCancelAnimationFrame;
        if (!navigator.requestAnimationFrame)
            navigator.requestAnimationFrame = navigator.webkitRequestAnimationFrame || navigator.mozRequestAnimationFrame;

    navigator.getUserMedia(
        {
            "audio": {
                "mandatory": {
                    "googEchoCancellation": "false",
                    "googAutoGainControl": "false",
                    "googNoiseSuppression": "false",
                    "googHighpassFilter": "false"
                },
                "optional": []
            },
        }, gotStream, function(e) {
            alert('Error getting audio');
            console.log(e);
        });
}

window.addEventListener('load', initAudio );
