package com.unblock.vpn.websites.thread;

import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.unblock.vpn.websites.util.ByteBufferPool;
import com.unblock.vpn.websites.util.Packet;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import static com.unblock.vpn.websites.util.Packet.decrypt;

public class VPNRunnable implements Runnable {

    private static final String TAG = VPNRunnable.class.getSimpleName();
    ParcelFileDescriptor vpnInterface;
    private boolean isStop;

    public VPNRunnable(ParcelFileDescriptor vpnInterface){
        isStop = false;
        this.vpnInterface = vpnInterface;
    }

    public void stop(){
        isStop = true;
    }

    @Override
    public void run() {
        FileChannel vpnInput = new FileInputStream(vpnInterface.getFileDescriptor()).getChannel();
        FileChannel vpnOutput = new FileOutputStream(vpnInterface.getFileDescriptor()).getChannel();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] b = baos.toByteArray();

        byte[] keyStart = "this is a key".getBytes();
        KeyGenerator kgen = null;
        try {
            kgen = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        SecureRandom sr = null;
        try {
            sr = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        sr.setSeed(keyStart);
        kgen.init(128, sr); // 192 and 256 bits may not be available
        SecretKey skey = kgen.generateKey();
        byte[] key = skey.getEncoded();

// encrypt
        byte[] encryptedData = new byte[0];
        try {
            encryptedData = (Packet.encrypt(key,b));
        } catch (Exception e) {
            e.printStackTrace();
        }
// decrypt

        try {
            byte[] decryptedData = decrypt(key,encryptedData);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ByteBuffer bufferToNetwork = null;
        while (true)
        {
            if(isStop)
            {
                vpnInterface = null;

                //AES

                break;
            }

            if (bufferToNetwork != null)
            {
                bufferToNetwork.clear();
            }
            else
            {
                bufferToNetwork = ByteBufferPool.acquire();
            }

            int readBytes = 0;
            try {
                readBytes = vpnInput.read(bufferToNetwork);
            } catch (IOException e) {
                e.printStackTrace();
            }


            if (readBytes > 0)
            {
                bufferToNetwork.flip();
                Packet packet = null;
                try
                {
                    packet = new Packet(bufferToNetwork, true);
                }
                catch (UnknownHostException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                //
                String sIp = null;
                if(packet.ip4Header.destinationAddress !=null)
                {
                    sIp = packet.ip4Header.destinationAddress.getHostAddress();

                }


                if (packet.isUDP())
                {
                    Log.i(TAG,"udp address:" + packet.ip4Header.sourceAddress.getHostAddress() + " udp port:"
                            + packet.udpHeader.sourcePort + " des:" + sIp + " des port:" + packet.udpHeader.destinationPort);

                }
                else if (packet.isTCP())
                {

                    Log.i(TAG,"tcp address:" + packet.ip4Header.sourceAddress.getHostAddress() + "tcp port:"
                            + packet.tcpHeader.sourcePort + " des:" + sIp + " des port:" + packet.tcpHeader.destinationPort);

                }
                else if (packet.isPing())
                {
                    Log.w(TAG, packet.ip4Header.toString());
                }
                else
                {
                    Log.w(TAG, "Unknown packet type");
                    Log.w(TAG, packet.ip4Header.toString());
                }
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
