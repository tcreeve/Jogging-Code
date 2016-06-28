package com.example.jogging;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Send {
	
	final String IP_PC = "152.7.22.56 ";
	final int PORT = 8002;
	
	 public void sendUDPMessage(String data) throws java.io.IOException {
	        DatagramSocket socket = new DatagramSocket();
	        InetAddress serverIP = InetAddress.getByName(IP_PC);
	        byte[] outData = data.getBytes();
	        DatagramPacket out = new DatagramPacket(outData,outData.length, serverIP,PORT);
	        socket.send(out);
	        socket.close();
	    }
}
