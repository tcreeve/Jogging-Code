import java.net.*; 
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

class Server { 
	
	private static List<DataPoint> dataList = new ArrayList();
	private static List<IDIPMatch> IdIPList = new ArrayList();
	final static int THIS_RECIEVE_PORT = 8093;
	//	static String android_ip = "127.0.0.1";
	final static String IP_PC_2 = "192.168.3.103";
	final static int SEND_PORT_2 = 8092;
	final static int ANDROID_SEND_PORT = 8083;
	static int identification = 0;
	
	public static void main(String args[]) throws Exception { 
		
		DatagramSocket serverSocket = new DatagramSocket(THIS_RECIEVE_PORT); 
//		DatagramSocket clientSocketAndroid = new DatagramSocket(ANDROID_SEND_PORT);
//		DatagramSocket clientSocketPC2 = new DatagramSocket(SEND_PORT_2);
		
		DatagramSocket clientSocketAndroid = new DatagramSocket();
		DatagramSocket clientSocketPC2 = new DatagramSocket();
		
		byte[] receiveData = new byte[1024]; 
		byte[] sendData  = new byte[1024]; 
		
		//int identification = 0;
		
		while(true) {
			for(int i=0; i<receiveData.length; i++){
				receiveData[i]=' ';
			}
			for(int i=0; i<sendData.length; i++){
				sendData[i]=' ';
			}
			DatagramPacket receivePacket = 
					new DatagramPacket(receiveData, receiveData.length); 
			serverSocket.receive(receivePacket); 
			String sentence = new String(receivePacket.getData()); 
			System.out.println(sentence);
			
			InetAddress IPAddress = receivePacket.getAddress(); 	
			int port = receivePacket.getPort(); 
		   
			System.out.println(createString(IPAddress.toString(), sentence));
			
			if(sentence.contains("A:getID")){
				identification++;
				String send = "P: ID ["+identification+"]";
				System.out.println(send);
				sendData = send.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, ANDROID_SEND_PORT);
				
				clientSocketAndroid.send(sendPacket);
				System.out.println("Sent to android ("+IPAddress+":"+port+")");
				
				sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(IP_PC_2), SEND_PORT_2);
				clientSocketPC2.send(sendPacket);
				System.out.println("Sent to PC2 ("+IP_PC_2+":"+SEND_PORT_2+")");
			} else if(sentence.contains("A:")){
				if(sentence.contains(":DataPoint")){
					int start = sentence.indexOf("[");
					int end = sentence.indexOf("]");
					String msg = sentence.substring(start+1, end);
					int idNum = addToDataList(msg);	
					if(idNum > identification){
						identification = idNum;
						String send = "P: ID ["+identification+"]";
						System.out.println(send);
						sendData = send.getBytes();
						new DatagramPacket(sendData, sendData.length, IPAddress, ANDROID_SEND_PORT);
						
						new DatagramPacket(sendData, sendData.length, InetAddress.getByName(IP_PC_2), SEND_PORT_2);
					}
				} else if(sentence.contains("Send comparisons")){
					String msg = "P:newDataPoints {"+getAndroidIDFromString(sentence)+
							"}  ("+IPAddress.toString() +") ["+getNumberOfNewDataPoints()+"]";
					sendData = msg.getBytes();
					System.out.println(msg+SEND_PORT_2);
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(IP_PC_2), SEND_PORT_2);
					clientSocketPC2.send(sendPacket);
					System.out.println("Message sent");
				}
			} else if(sentence.contains("P:")){
				if(sentence.contains("newDataPoints")){
					int num = getNumberOfNewDataPoints();
					int start = sentence.indexOf("[");
					int end = sentence.indexOf("]");
					int otherNum = Integer.parseInt(sentence.substring(start+1, end));
					System.out.println("PC1:"+num+"\nPC2:"+otherNum);
					String msg = "";
					if(num<=otherNum){
						for(DataPoint data : dataList){
							if(!data.getSeenPC2()){
								String dp = "P: DataPoint ("+getIPFromPacket(sentence)+") ["+data.getLongitude()+"|"+data.getLatitude()+"|"+data.getAltitude()+"|"
										+data.getTime()+"|"+data.getID()+"|"+true+"]";
								System.out.println(dp);
								byte[] send = dp.getBytes();
								DatagramPacket sendPacket = new DatagramPacket(send, send.length, 
										InetAddress.getByName(IP_PC_2), SEND_PORT_2);
								clientSocketAndroid.send(sendPacket);
								data.setSeenPC2(true);
							}
						}
						String dp = "P: LastDataPointSent  {"+getAndroidIDFromString(sentence)+"} ("+getIPFromPacket(sentence)+")";
						byte[] send = dp.getBytes();
						DatagramPacket sendPacket = new DatagramPacket(send, send.length,
								InetAddress.getByName(IP_PC_2), SEND_PORT_2);
						clientSocketAndroid.send(sendPacket);
					} else{
						msg = "P:SendDataToPC1  {"+getAndroidIDFromString(sentence)+"} ("+getIPFromPacket(sentence)+")";
						System.out.println(msg);
						byte[] send = msg.getBytes();
						DatagramPacket sendMSG = new DatagramPacket(send, send.length, 
								InetAddress.getByName(IP_PC_2), SEND_PORT_2);
						clientSocketPC2.send(sendMSG);
					}
					System.out.println("Sent "+msg);
				} else if(sentence.contains("LastDataPointSent")){
					int start = sentence.indexOf("(");
					int end = sentence.indexOf(")");
					String androidIP = sentence.substring(start+2, end);
					InetAddress address = InetAddress.getByName(androidIP);
					long d1 =  Calendar.getInstance().getTimeInMillis();
					address.isReachable(10000);
					long d2 = Calendar.getInstance().getTimeInMillis();
					long time_diff = d2 - d1;
					String comparison = compareData(getAndroidIDFromString(sentence));
					String time = "P:ReachAndroidSpeed  {"+getAndroidIDFromString(sentence)+"} ("+androidIP+") ["+time_diff+"] <"+comparison+">";
					sendData = time.getBytes();
					DatagramPacket sendMSG = new DatagramPacket(sendData, sendData.length, 
							InetAddress.getByName(IP_PC_2),SEND_PORT_2);
					clientSocketPC2.send(sendMSG);	
					System.out.println(time);
				} else if(sentence.contains("ReachAndroidSpeed")){
					int start = sentence.indexOf("(");
					int end = sentence.indexOf(")");
					String androidIP = sentence.substring(start+1, end);
					InetAddress address = InetAddress.getByName(androidIP);
					
					start = sentence.indexOf("[");
					end = sentence.indexOf("]");
					String timePC2String = sentence.substring(start+1, end);
					long timePC2 = Long.parseLong(timePC2String);
					
					long d1 =  Calendar.getInstance().getTimeInMillis();
					address.isReachable(10000);
					long d2 = Calendar.getInstance().getTimeInMillis();
					long time_diff = d2 - d1;
					
					System.out.println("Time differences: \n\tPC1:"+timePC2String+"\n\tPC2:"+time_diff);
					System.out.println(sentence);
					String time;
					if(time_diff>timePC2){
						time = "P:PC2Closer {"+getAndroidIDFromString(sentence)+"}  ("+androidIP+") <"+getComparisonString(sentence)+">";
					} else{
						time = "P:PC1Closer  {"+getAndroidIDFromString(sentence)+"} ("+androidIP+") <"+getComparisonString(sentence)+">";
						
						//String ip_android = sentence.substring(start+1, end);
						InetAddress send_ip = InetAddress.getByName(androidIP);
						String comparison = "P:Comparison ["+getComparisonString(sentence)+"]";
						System.out.println(androidIP+":"+ANDROID_SEND_PORT+" = "+comparison);
						byte [] send = comparison.getBytes();
						DatagramPacket sendComparison = new DatagramPacket(send, send.length, send_ip, ANDROID_SEND_PORT);
						clientSocketAndroid.send(sendComparison);
					}
					sendData = time.getBytes();
					DatagramPacket sendMSG = new DatagramPacket(sendData, sendData.length, 
							InetAddress.getByName(IP_PC_2), SEND_PORT_2);
					clientSocketPC2.send(sendMSG);	
				} else if(sentence.contains("SendDataToPC2")){
					System.out.println("Recieved -"+sentence);
					for(DataPoint data : dataList){
						System.out.println(data.getID()+" - "+data.getTime());
						if(!data.getSeenPC2()){
							String dp = "P: DataPoint ("+getIPFromPacket(sentence)+") ["+data.getLongitude()+"|"+data.getLatitude()+"|"+data.getAltitude()+"|"
									+data.getTime()+"|"+data.getID()+"|"+true+"]";
							System.out.println("SENT - "+dp);
							byte[] send = dp.getBytes();
							DatagramPacket sendPacket = new DatagramPacket(send, send.length, InetAddress.getByName(IP_PC_2), SEND_PORT_2);
							clientSocketAndroid.send(sendPacket);
							data.setSeenPC2(true);
						}
					}
					String dp = "P: LastDataPointSent  {"+getAndroidIDFromString(sentence)+"} ("+getIPFromPacket(sentence)+")";
					byte[] send = dp.getBytes();
					DatagramPacket sendPacket = new DatagramPacket(send, send.length, InetAddress.getByName(IP_PC_2), SEND_PORT_2);
					clientSocketAndroid.send(sendPacket);
				} else if(sentence.contains("P:PC1Closer")){
					int start = sentence.indexOf("(");
					int end = sentence.indexOf(")");
					String ip_android = sentence.substring(start+1, end);
					InetAddress send_ip = InetAddress.getByName(ip_android);
					String comparison = "P:Comparison ["+compareData(getAndroidIDFromString(sentence))+"]";
					byte [] send = comparison.getBytes();
					DatagramPacket sendComparison = new DatagramPacket(send, send.length, send_ip, ANDROID_SEND_PORT);
					clientSocketAndroid.send(sendComparison);
				} else if(sentence.contains(": ID")){
					int start = sentence.indexOf("[");
					int end = sentence.indexOf("]");
					int IDnum = Integer.parseInt(sentence.substring(start+1, end));
					identification = IDnum;
					System.out.println("Recieved ID number from PC ("+identification+")");
				}
			}
		} 
	} 
	
	private static String getAndroidIDFromString(String s){
		return s.substring(s.indexOf("{")+1, s.indexOf("}"));
	}
	
	private static String getComparisonString(String s){
		return s.substring(s.indexOf("<")+1, s.indexOf(">"));
	}
	
	private static String getIPFromPacket(String s){
		int start = s.indexOf("(");
		int end = s.indexOf(")");
		return s.substring(start+1, end);
	}
	
	private static int addToDataList(String data){
		String[] dataSection = data.split(";");
		
		double longitude = Double.parseDouble(dataSection[0].trim());
		double latitude = Double.parseDouble(dataSection[1].trim());
		double altitude = Double.parseDouble(dataSection[2].trim());
		long time = Long.parseLong(dataSection[3].trim());
		String id = dataSection[4].trim();
		boolean seenpc2 = Boolean.parseBoolean(dataSection[5].trim());
		dataList.add(new DataPoint(longitude,latitude,altitude,time,id,seenpc2));
		
		System.out.println(id+" "+time);
		
		return (Integer.parseInt(id));
	}
	
	private static String compareData(String id){	
		long[] timeID = getTimeByID();
		double[] distanceID = getDistanceByID();
		double[] velocityID = getVelocityByID(timeID, distanceID);
		String[] timeRankings = getTimeRankingsOfData(timeID);
		String[] distanceRankings = getDistanceRankingsOfData(distanceID);
		String[] velocityRankings = getVelocityRankingsOfData(velocityID);
		
		int timeRank = 0;
		int disRank = 0;
		int velRank = 0;
		for(int i=0; i<identification; i++){
			if(timeRankings[i].equals(id)){
				timeRank = (i+1);
			}	
			if(distanceRankings[i].equals(id)){
				disRank = (i+1);
			}
			if(velocityRankings[i].equals(id)){
				velRank = (i+1);
			}
		}
	
		return timeRank+";"+disRank+";"+velRank+";"+identification;
	}

	private static double[] getDistanceByID(){
		double[] distance = new double[identification];
		for(int i=0; i<distance.length; i++){
			distance[i]=0;
		}	
		DataPoint[] timeList = orderDataListByTime();
		double lat1 = 3000;
		double lat2 = 3000;
		double alt1 = 0;
		double long1 = 3000;
		double long2 = 3000;
		double alt2 = 0;
		for(int i=1; i<=identification; i++){
			for(int j=0; j<timeList.length; j++){
				if(timeList[j].getID().equals(i+"")){
					if(lat1 == 3000){
						lat1 = timeList[j].getLatitude();
						long1 = timeList[j].getLongitude();
						alt1 = timeList[j].getAltitude();
					} else if(lat2 == 3000){
						long2 = timeList[j].getLongitude();
						lat2 = timeList[j].getLatitude();
						alt2 = timeList[j].getAltitude();
					
						distance[i-1]+=Math.sqrt(((lat1-lat2)*(lat1-lat2))+((long1-long2)*(long1-long2))+((alt1-alt2)*(alt1-alt2)));
						
						lat1 = lat2;
						long1 = long2;
						
						long2 = 3000;
						lat2 = 3000;
					}	
				}
			}
		}
		return distance;
	}
	private static String[] getDistanceRankingsOfData(double[] distance){
		int[] rankingOrder = new int[identification];
		for(int i=1; i<=identification; i++){
			rankingOrder[i-1]=i;
		}	
		for(int k=0; k<identification; k++){
			for(int j=k+1; j<identification;j++){
				double tempDis;
				int tempID;
				if(j<identification){
					if(distance[k]<distance[j]){
						tempDis=distance[k];
						distance[k]=distance[j];
						distance[j]=tempDis;
						
						tempID=rankingOrder[k];
						rankingOrder[k]=rankingOrder[j];
						rankingOrder[j]=tempID;
					}
				}
			}
		}
		String [] IDOrder = new String[identification];
		for(int i=0; i<identification; i++){
			IDOrder[i]=rankingOrder[i]+"";
		}
		return IDOrder;
	}

	private static double[] getVelocityByID(long[] time, double[] dis){
		double[] velID = new double[identification];
		for(int i=0; i<identification; i++){
			velID[i] = (double)(dis[i]/time[i]);
		}
		return velID;
	}
	private static String[] getVelocityRankingsOfData(double[] vel){
		int[] rankingOrder = new int[identification];
		for(int i=1; i<=identification; i++){
			rankingOrder[i-1]=i;
		}	
		for(int k=0; k<identification; k++){
			for(int j=k+1; j<identification;j++){
				double tempVel;
				int tempID;
				if(j<identification){
					if(vel[k]>vel[j]){
						tempVel=vel[k];
						vel[k]=vel[j];
						vel[j]=tempVel;
						
						tempID=rankingOrder[k];
						rankingOrder[k]=rankingOrder[j];
						rankingOrder[j]=tempID;
					}
				}
			}
		}
		String [] IDOrder = new String[identification];
		for(int i=0; i<identification; i++){
			IDOrder[i]=rankingOrder[i]+"";
		}
		return IDOrder;
	}
	
	private static DataPoint[] orderDataListByTime(){
		int num = 0;
		for(DataPoint dp : dataList){
			num++;
		}
		DataPoint[] tempArr = new DataPoint[num];
		num = 0;
		for(DataPoint dp : dataList){
			tempArr[num] = dp;
			num++;
		}
		for(int i=0; i<tempArr.length; i++){
			for(int j=i+1; j<tempArr.length; j++){
				if(tempArr[i].getTime()>tempArr[j].getTime()){
					DataPoint temp = tempArr[i];
					tempArr[j] = temp;
					temp = tempArr[j];
				}
			}
		}
		return tempArr;
	}
	
	private static long[] getTimeByID(){
		long[] time = new long[identification];
		long[] beginTime = new long[identification];
		long[] endTime = new long[identification];
		for(int i=0; i<identification; i++){
			beginTime[i]=-1;
			endTime[i]=-1;
			for(DataPoint data : dataList){
				if(data.getID().equals(i+"")){
					if(beginTime[i]<0){
						beginTime[i]=data.getTime();
					} else if(beginTime[i]>data.getTime()){
						beginTime[i]=data.getTime();
					}
					if(endTime[i]<0){
						endTime[i]=data.getTime();
					} else if(endTime[i]<data.getTime()){
						endTime[i]=data.getTime();
					}
				}
			}
			time[i]=(endTime[i]-beginTime[i]);
		}
		return time;
	}
	private static String[] getTimeRankingsOfData(long[] time){
		int[] rankingOrder = new int[identification];
		for(int i=1; i<=identification; i++){
			rankingOrder[i-1]=i;
		}
		for(int k=0; k<identification; k++){
			for(int j=k+1; j<identification;j++){
				long tempTime;
				int tempID;
				if(k==j){
					j++;
				}
				if(j<identification){
					if(time[k]<time[j]){
						tempTime=time[k];
						time[k]=time[j];
						time[j]=tempTime;
						
						tempID=rankingOrder[k];
						rankingOrder[k]=rankingOrder[j];
						rankingOrder[j]=tempID;
					}
				}
			}
		}
		String [] IDOrder = new String[identification];
		for(int i=0; i<identification; i++){
			IDOrder[i]=rankingOrder[i]+"";
		}
		return IDOrder;
	}
	
	private static int getNumberOfNewDataPoints(){
		int numNew = 0;
		for(DataPoint data : dataList){
			if(!data.getSeenPC2())
				numNew++;
		}
		return numNew;
	}
  
	private static String createString(String ip, String sentence){
		String str = "";
		str += "IP Address: "+ip+"\n";
  		str += "Sentence: "+sentence+"\n";
  		return str;
  	}
 }  