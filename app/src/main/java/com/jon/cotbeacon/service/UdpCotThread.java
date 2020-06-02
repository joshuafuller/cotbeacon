package com.jon.cotbeacon.service;

import android.content.SharedPreferences;

import com.jon.cotbeacon.cot.CursorOnTarget;
import com.jon.cotbeacon.utils.Key;
import com.jon.cotbeacon.utils.PrefUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

import timber.log.Timber;

class UdpCotThread extends CotThread {
    private DatagramSocket socket;

    UdpCotThread(SharedPreferences sharedPreferences) {
        super(sharedPreferences);
    }
    UdpCotThread(SharedPreferences prefs, CotGenerator generator) {
        super(prefs, generator);
    }

    @Override
    void shutdown() {
        super.shutdown();
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

    @Override
    public void run() {
        super.run();
        initialiseDestAddress();
        openSocket();
        int bufferTimeMs = periodMilliseconds() / cotIcons.size();

        while (isRunning) {
            for (CursorOnTarget cot : cotIcons) {
                sendToDestination(cot);
                bufferSleep(bufferTimeMs);
            }
            cotIcons = cotGenerator.generate();
        }
        shutdown();
    }

    protected void initialiseDestAddress() {
        try {
            destIp = InetAddress.getByName(PrefUtils.getString(prefs, Key.DEST_ADDRESS));
        } catch (UnknownHostException e) {
            Timber.e("Error parsing destination address: %s", PrefUtils.getString(prefs, Key.DEST_ADDRESS));
            shutdown();
        }
        destPort = PrefUtils.parseInt(prefs, Key.DEST_PORT);
    }

    protected void openSocket() {
        try {
            if (destIp.isMulticastAddress()) {
                socket = new MulticastSocket();
                ((MulticastSocket)socket).setLoopbackMode(false);
            } else {
                socket = new DatagramSocket();
            }
        } catch (IOException e) {
            Timber.e("Error when building transmit UDP socket");
            shutdown();
        }
    }

    @Override
    protected void sendToDestination(CursorOnTarget cot) {
        try {
            final byte[] buf = cot.toBytes();
            socket.send(new DatagramPacket(buf, buf.length, destIp, destPort));
        } catch (IOException e) {
            e.printStackTrace();
            Timber.e(e);
            shutdown();
        } catch (NullPointerException e) {
            shutdown();
        }
    }

}
