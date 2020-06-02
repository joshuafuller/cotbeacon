package com.jon.cotbeacon.service;

import android.content.SharedPreferences;

import com.jon.cotbeacon.cot.CursorOnTarget;
import com.jon.cotbeacon.utils.Key;
import com.jon.cotbeacon.utils.NetworkHelper;
import com.jon.cotbeacon.utils.PrefUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

class UdpCotThread extends CotThread {
    private List<DatagramSocket> sockets = new ArrayList<>();

    UdpCotThread(SharedPreferences sharedPreferences) {
        super(sharedPreferences);
    }
    UdpCotThread(SharedPreferences prefs, CotGenerator generator) {
        super(prefs, generator);
    }

    @Override
    void shutdown() {
        super.shutdown();
        if (sockets != null) {
            for (DatagramSocket socket : sockets) {
                socket.close();
            }
            sockets.clear();
        }
    }

    @Override
    public void run() {
        super.run();
        initialiseDestAddress();
        openSockets();
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

    protected void openSockets() {
        try {
            if (destIp.isMulticastAddress()) {
                final List<NetworkInterface> interfaces = NetworkHelper.getValidInterfaces();
                for (NetworkInterface ni : interfaces) {
                    MulticastSocket socket = new MulticastSocket();
                    socket.setNetworkInterface(ni);
                    socket.setLoopbackMode(false);
                    sockets.add(socket);
                }
            } else {
                sockets.add(new DatagramSocket());
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
            for (DatagramSocket socket : sockets) {
                socket.send(new DatagramPacket(buf, buf.length, destIp, destPort));
                Timber.i("Sent cot: %s", cot.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
            Timber.e(e);
            shutdown();
        } catch (NullPointerException e) {
            shutdown();
        }
    }

}
