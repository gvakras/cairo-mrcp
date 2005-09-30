package com.onomatopia.cairo.server.recorder;

import com.onomatopia.cairo.server.MrcpGenericChannel;
import com.onomatopia.cairo.server.rtp.RTPStreamReplicator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.mrcp4j.MrcpRequestState;
import org.mrcp4j.message.MrcpResponse;
import org.mrcp4j.message.request.RecordRequest;
import org.mrcp4j.message.request.StartInputTimersRequest;
import org.mrcp4j.message.request.StopRequest;
import org.mrcp4j.server.MrcpServerSocket;
import org.mrcp4j.server.MrcpSession;
import org.mrcp4j.server.provider.RecorderRequestHandler;

public class MrcpRecorderChannel extends MrcpGenericChannel implements RecorderRequestHandler {

    private RTPRecorderChannel _recorderChannel;
    private boolean _recording = false;

    public MrcpRecorderChannel(RTPRecorderChannel recorderChannel) {
        _recorderChannel = recorderChannel;
    }

    public synchronized MrcpResponse record(RecordRequest request, MrcpSession session) {
        MrcpRequestState requestState = MrcpRequestState.COMPLETE;
        short statusCode = -1;
        if (_recording) {
            statusCode = MrcpResponse.STATUS_METHOD_NOT_VALID_IN_STATE;
        } else {
            try {
                _recorderChannel.startRecording(true);
                statusCode = MrcpResponse.STATUS_SUCCESS;
                requestState = MrcpRequestState.IN_PROGRESS;
                _recording = true;
            } catch (IllegalStateException e){
                e.printStackTrace();
                statusCode = MrcpResponse.STATUS_METHOD_NOT_VALID_IN_STATE;
            } catch (IOException e){
                e.printStackTrace();
                statusCode = MrcpResponse.STATUS_SERVER_INTERNAL_ERROR;
            }
        }
        // TODO: cache event acceptor if request is not complete
        return session.createResponse(statusCode, requestState);
    }

    public synchronized MrcpResponse stop(StopRequest request, MrcpSession session) {
        MrcpRequestState requestState = MrcpRequestState.COMPLETE;
        short statusCode = -1;
        if (_recording) {
            try {
                _recorderChannel.stopRecording();
                statusCode = MrcpResponse.STATUS_SUCCESS;
                //requestState = MrcpRequestState.IN_PROGRESS;
                _recording = false;
            } catch (IllegalStateException e){
                statusCode = MrcpResponse.STATUS_METHOD_NOT_VALID_IN_STATE;
            }
        } else {
            statusCode = MrcpResponse.STATUS_METHOD_NOT_VALID_IN_STATE;
        }
        // TODO: release event acceptor if request is complete
        return session.createResponse(statusCode, requestState);
    }

    public synchronized MrcpResponse startInputTimers(StartInputTimersRequest request, MrcpSession session) {
        return session.createResponse(MrcpResponse.STATUS_SERVER_INTERNAL_ERROR, MrcpRequestState.COMPLETE);
    }

    public static void main(String[] args) throws Exception {
        // We need three parameters to receive and record RTP transmissions
        // For example,
        //   java MrcpRecorderChannel "C:\\work\\cvs\\onomatopia\\cairo\\output\\prompts" 32416 42050

        String channelID = "32AECB23433801@recorder";
        
        if (args.length < 3) {
            printUsage();
        }

        int mrcpPort = -1;
        try {
            mrcpPort = Integer.parseInt(args[1]);
        } catch (Exception e){
            e.printStackTrace();
        }
        if (mrcpPort < 0) {
            printUsage();
        }

        int rtpPort = -1;
        try {
            rtpPort = Integer.parseInt(args[2]);
        } catch (Exception e){
            e.printStackTrace();
        }
        if (rtpPort < 0) {
            printUsage();
        }

        File dir = new File(args[0]);

        System.out.println("Starting up RTPStreamReplicator...");
        RTPStreamReplicator replicator = new RTPStreamReplicator(rtpPort);

        System.out.println("Starting up MrcpServerSocket...");
        MrcpServerSocket serverSocket = new MrcpServerSocket(mrcpPort);
        RTPRecorderChannel recorder = new RTPRecorderChannel(dir, replicator);
        serverSocket.openChannel(channelID, new MrcpRecorderChannel(recorder));

        System.out.println("MRCP recorder resource listening on port " + mrcpPort);

        System.out.println("Hit <enter> to shutdown...");
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        String cmd = consoleReader.readLine();
        Thread.sleep(90000);
        System.out.println("Shutting down...");
        replicator.shutdown();
    }

    static void printUsage() {
        System.err.println("Usage: MrcpRecorderChannel <recordDir> <mrcpPort> <rtpPort>");
        System.err.println("     <recordDir>: directory to place recordings of RTP transmissions");
        System.err.println("     <mrcpPort>: port to listen for MRCP messages");
        System.err.println("     <rtpPort>: port to listen for RTP transmissions");
        System.exit(0);
    }

}