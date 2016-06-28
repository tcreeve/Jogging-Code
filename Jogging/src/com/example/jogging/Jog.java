package com.example.jogging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.Provider;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.app.Activity;
import android.content.Context;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

public class Jog extends Activity implements LocationListener {
	
	final static private String TAG = "Jog";
	Button start;
	Button stop;
	TextView rankingText;
	DatagramPacket pack;
	
	private Handler updateHandler;
	private Handler startHandler;
	
	private final int SLEEP_TIME = 1000;
	
	private volatile boolean keepgoing = false;
	boolean stoppressed = false;
	
//	final String IP_PC_1 = "192.168.3.103";
//	final String IP_PC_2 = "192.168.3.103";
	final String IP_PC_1 = "192.168.16.1";
	final String IP_PC_2 = "192.168.16.1";
//	final String IP_PC_1 = "192.168.138.152";
//	final String IP_PC_2 = "192.168.138.152";
	final int SEND_PORT_1 = 8093;
	final int RECIEVE_PORT = 8083;
	final int SEND_PORT_2 = 8092;
	private String id_num;

	
	 public void sendUDPMessage(String data) {
		 Log.d(TAG, "sendUDPMessage begin - "+data);
		 try{
			 InetAddress address1 = InetAddress.getByName(IP_PC_1);
			 long d1 =  Calendar.getInstance().getTimeInMillis();
			 boolean reach = address1.isReachable(10000);
			 long d2 = Calendar.getInstance().getTimeInMillis();
			 long time_diff1 = d2 - d1;
			 
			 InetAddress address2 = InetAddress.getByName(IP_PC_2);
			 d1 =  Calendar.getInstance().getTimeInMillis();
			 reach = address2.isReachable(10000);
			 d2 = Calendar.getInstance().getTimeInMillis();
			 long time_diff2 = d2 - d1;
			 
			 InetAddress IPAddress = InetAddress.getByName(IP_PC_1);
			 DatagramSocket clientSocket = new DatagramSocket();
			 
			 int port = SEND_PORT_1;
			 Log.d(TAG, "PC1: "+time_diff1+"  PC2: "+time_diff2);
			 long short_diff = time_diff1;
			 if(time_diff1>time_diff2){
				 port = SEND_PORT_2;
				 IPAddress = InetAddress.getByName(IP_PC_2);
				 short_diff = time_diff2;
			 }
			 
			 Log.d(TAG, "Created clientSocket");
			 
			 
			// String sentence = data;
			 byte[] sendData = data.getBytes();     
			 Log.d(TAG, "Converted String to byte array");
			 DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress,port); 
			 Log.d(TAG, "Created sendPacket ADDRESS:"+IPAddress.toString()+":"+port);
			 
			 DatagramSocket serverSocket = new DatagramSocket(RECIEVE_PORT); 
			 clientSocket.send(sendPacket);
			 
			 Log.d(TAG, "sent packet: "+sendPacket.getAddress().toString()+":"+sendPacket.getPort()+ " ("+data+")");
			 clientSocket.close(); 
			
			 if(data.contains("getID")){
				 byte[] recieveData = new byte[1024]; 
				 DatagramPacket recievePacket = new DatagramPacket(recieveData, recieveData.length);
				 Log.d(TAG, "Created serverSocket");
				 serverSocket.setSoTimeout((int)short_diff*2);
				 serverSocket.receive(recievePacket);
				 Log.d(TAG, "Recieved id packet");
				 id_num = getIDFromPacket(recievePacket); 
			 } else if(data.contains("Send comparisons")){
				 byte[] recieveData = new byte[1024];
				 DatagramPacket recievePacket = new DatagramPacket(recieveData, recieveData.length);
				 serverSocket.setSoTimeout((int)short_diff*2);
				 serverSocket.receive(recievePacket);
				 Log.d(TAG, "Recieved comparisons");
				 pack = recievePacket;
				 updateHandler = new Handler();
//				 displayPacketInfo(recievePacket);
				 updateHandler.post(updateRankings);
				 Log.d(TAG, "Updated ranking list");
			 }	
		 serverSocket.close();
		 }catch(IOException e){
			 Log.e(TAG, "Input/Output Exception in Jog.sendUDPMessage "+e.getLocalizedMessage());
		 }
	 }
	 
	 private String getIDFromPacket(DatagramPacket p){
		 String sen = new String(p.getData());
		 int start = sen.indexOf("[");
		 int end = sen.indexOf("]");
		 return sen.substring(start+1, end);
	 }
	 
	 private Runnable updateRankings = new Runnable(){
		 public void run(){
			 String completeMsg = new String(pack.getData());
			 int start = completeMsg.indexOf("[");
			 int end = completeMsg.indexOf("]");
			 String values = completeMsg.substring(start+1, end);
			 
			 String[] rankString = values.split(";");
			 
			 String txt = "Ranks:\n"+"Time rank: "+rankString[0]+" out of "+rankString[rankString.length-1]+"\n"+
					 "Distance Rank: "+rankString[1]+" out of "+rankString[rankString.length-1]+"\n"+
					 "Velocity Rank: "+rankString[2]+" out of "+rankString[rankString.length-1];
			 
			 rankingText.setText(txt);
		 }
	 };
	 
	 /*
	 private void displayPacketInfo(DatagramPacket p){
		 //code here
		 String completeMsg = new String(p.getData());
		 int start = completeMsg.indexOf("[");
		 int end = completeMsg.indexOf("]");
		 String values = completeMsg.substring(start+1, end);
		 
		 String[] rankString = values.split(";");
		 
		 rankingText.setText("Ranks:\n"+"Time rank: "+rankString[0]+" out of "+rankString[rankString.length-1]);
	 }
	 */
	 
	 private Runnable startButtonClicked = new Runnable(){
		public void run(){
			sendLocation();
			sendUDPMessage("A:{"+id_num+"} Send comparisons ");
			stop.setEnabled(true);
			if(keepgoing)
				startHandler.postDelayed(startButtonClicked, SLEEP_TIME);
		}
	 };
	
	 private void sendLocation(){
		 Log.d(TAG, "Beginning collection");
    	
		 Context context = getApplicationContext();
		 
		 LocationManager mloc = (LocationManager)getSystemService(context.LOCATION_SERVICE);
		 Criteria crit = new Criteria();
		 String provider = mloc.getBestProvider(crit, false);
		 Log.d(TAG, "Got context ("+context.toString()+")");
		 Location loc = getLocation(context);
		 if(loc == null)
			 loc = mloc.getLastKnownLocation(provider);
		 Log.d(TAG, "Got location");
		 
		 double longi;
		 double lati;
		 double alti;
		 if(loc == null){
			 longi = 100;
			 lati = 100;
			 alti = 10;
			 Log.e(TAG, "Location is null");
		 } else{
			 longi = loc.getLongitude();
			 lati = loc.getLatitude();
			 alti = loc.getAltitude();
		 }

    	
		 long milTime = Calendar.getInstance().getTimeInMillis();
    	
		 Log.d(TAG, milTime+"");
		 
		 sendUDPMessage("A:DataPoint ["+longi+";"+lati+";"+alti+";"+milTime+";"+id_num+";false]");
    	
		 Log.d(TAG, "Ending collection");		
	 }
	 
	 /** Called when the activity is first created. 
	 * @return */
	 
	 private OnClickListener start_listener = new OnClickListener() {
		 public void onClick(View v) {
			 stop.setEnabled(false);
			 start.setEnabled(false);
			 keepgoing = true;
			 String tempNum = null;
			 if(id_num!=null)
				 tempNum = id_num;
			 while((id_num == null) || (id_num.equals(tempNum))){
				 sendUDPMessage("A:getID");
			 }
			 startHandler = new Handler();
			 startHandler.postDelayed(startButtonClicked, SLEEP_TIME);
		 }
	 };
	 private OnClickListener stop_listener = new OnClickListener(){
		 public void onClick(View v){
			 keepgoing = false;
			 start.setEnabled(true);
			 startHandler.removeCallbacks(startButtonClicked);
		 }
	 };
	 

/**
 * This is a fast code to get the last known location of the phone. If there
 * is no exact gps-information it falls back to the network-based location
 * info. This code is using LocationManager. Code from:
 * http://www.androidsnippets.org/snippets/21/
 * 
 * @param ctx
 * @return
 */
	 public static Location getLocation(Context ctx) {
		 //	List<String> providers = lm.getProviders(true);

		 /*
		  * Loop over the array backwards, and if you get an accurate location,
		  * then break out the loop
		  */
		 /*  	Location l = null;
	
    	for (int i = providers.size() - 1; i >= 0; i--) {
    		l = lm.getLastKnownLocation(providers.get(i));
    		if (l != null)
    			break;
    	}
    	return l;*/
		 LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
    	Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    	return location;
	 }



	  @Override
	  public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_jog);
	    
	    start = (Button)findViewById(R.id.start);
	    stop = (Button)findViewById(R.id.stop);
	    rankingText = (TextView)findViewById(R.id.RankingsList);
	    
	    stop.setOnClickListener(stop_listener);  
	    start.setOnClickListener(start_listener);  
	  }
	 

	  public void onLocationChanged(Location arg0) {
		  // TODO Auto-generated method stub
		
	  }



	  public void onProviderDisabled(String arg0) {
		  // TODO Auto-generated method stub
		
	  }



	  public void onProviderEnabled(String arg0) {
		  // TODO Auto-generated method stub
		
	  }



	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub
		
	}
	
	
	
}
