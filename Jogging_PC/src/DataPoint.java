import java.util.ArrayList;
import java.util.List;


public class DataPoint {
	private double longitude;
	private double latitude;
	private double altitude;
	private long time;
	private String id;
	private boolean seenPC1 = true;
	private boolean seenPC2;
	
	public DataPoint(double lon, double lat, double alt, long t, String identification, boolean pc2){
		longitude = lon;
		latitude = lat;
		altitude = alt;
		time = t;
		id = identification;
		seenPC2 = pc2;
	}
	
	public double getLongitude(){
		return longitude;
	}
	
	public void setLongitude(double l){
		longitude = l;
	}
	
	public double getLatitude(){
		return latitude;
	}
	
	public void setLatitude(double l){
		latitude = l;
	}
	
	public double getAltitude(){
		return altitude;
	}
	
	public void setAltitude(double a){
		altitude = a;
	}
	
	public long getTime(){
		return time;
	}
	
	public void setTime(long t){
		time = t;
	}
	
	public String getID(){
		return id;
	}
	
	public void setID(String i){
		id = i;
	}
	
	public boolean getSeenPC1(){
		return seenPC1;
	}
	
	public void setSeenPC1(boolean pc1){
		seenPC1 = pc1;
	}
	
	public boolean getSeenPC2(){
		return seenPC2;
	}
	
	public void setSeenPC2(boolean pc2){
		seenPC2 = pc2;
	}
}
