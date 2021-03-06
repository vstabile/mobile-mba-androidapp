package com.samknows.measurement.storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.samknows.libcore.SKLogger;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.environment.CellTowersData;
import com.samknows.measurement.environment.DCSData;
import com.samknows.measurement.environment.LocationData;
import com.samknows.measurement.environment.NetworkData;
import com.samknows.measurement.environment.PhoneIdentityData;
import com.samknows.measurement.environment.TrafficData;
import com.samknows.measurement.schedule.condition.ConditionResult;
import com.samknows.measurement.schedule.condition.CpuActivityCondition;
import com.samknows.measurement.schedule.condition.DatacapCondition;
import com.samknows.measurement.schedule.condition.NetActivityCondition;
import com.samknows.tests.ClosestTarget;
import com.samknows.tests.HttpTest;
import com.samknows.tests.LatencyTest;
import com.samknows.tests.Test;


public class ExportFile {
	//csv file exports
	private static final int ZIP_BUFFER_SIZE = 2048;
	public static final String ZIP_FILE = "extract.zip";
	public static final String FILE_ENCODING = "UTF-8";
	public static final String FIELD_DELIMITER= "\"";
	public static final String FIELD_SEPARATOR= ",";
	public static final String RECORD_SEPARATOR= "\r\n";
	public static final String EMPTY_FIELD = "";
	public static final String TEST_RESULTS_JSON_FILE = "results.json";
	public static final String MAINSECTION_FILENAME = "app_metadata";
	public static final String CSV_FILEEXTENSION = ".csv";
	public static final long FILES_MAX_SIZE = 1024 * 1024;
	
	private static File storage;
	//main section data
	public static final String[] MAIN_FIELDS = {SK2AppSettings.JSON_UNIT_ID, SK2AppSettings.JSON_APP_VERSION_CODE, SK2AppSettings.JSON_APP_VERSION_NAME, SK2AppSettings.JSON_SCHEDULE_CONFIG_VERSION, SK2AppSettings.JSON_TIMEZONE, SK2AppSettings.JSON_TIMESTAMP, SK2AppSettings.JSON_DATETIME, SK2AppSettings.JSON_ENTERPRISE_ID, SK2AppSettings.JSON_SIMOPERATORCODE, SK2AppSettings.JSON_USER_SELF_ID};
	
	//tests data
	public static final String[] HTTP_FIELDS = {Test.JSON_TYPE, Test.JSON_TIMESTAMP, Test.JSON_DATETIME, Test.JSON_TARGET, Test.JSON_TARGET_IPADDRESS, Test.JSON_SUCCESS, 
		HttpTest.JSON_TRANFERTIME, HttpTest.JSON_TRANFERBYTES, HttpTest.JSON_BYTES_SEC, HttpTest.JSON_WARMUPTIME, HttpTest.JSON_WARMUPBYTES, HttpTest.JSON_NUMBER_OF_THREADS};
	public static final String[] LATENCY_FIELDS = {Test.JSON_TYPE, Test.JSON_TIMESTAMP, Test.JSON_DATETIME, Test.JSON_TARGET, Test.JSON_TARGET_IPADDRESS, Test.JSON_SUCCESS,
		LatencyTest.JSON_RTT_AVG, LatencyTest.JSON_RTT_MIN, LatencyTest.JSON_RTT_MAX, LatencyTest.JSON_RTT_STDDEV, LatencyTest.JSON_RECEIVED_PACKETS, LatencyTest.JSON_LOST_PACKETS};
	public static final String[] CLOSESTTARGET_FIELDS = {Test.JSON_TYPE, Test.JSON_TIMESTAMP, Test.JSON_DATETIME, Test.SUCCESS, ClosestTarget.JSON_CLOSETTARGET, ClosestTarget.JSON_IPCLOSESTTARGET};
	
	//metrics data
	public static final String[] LOCATION_FIELDS = {DCSData.JSON_TYPE, DCSData.JSON_TIMESTAMP, DCSData.JSON_DATETIME, LocationData.JSON_LOCATION_TYPE, LocationData.JSON_LATITUDE, LocationData.JSON_LONGITUDE, LocationData.JSON_ACCURACY};
	public static final String[] NETWORKDATA_FIELDS = {DCSData.JSON_TYPE, DCSData.JSON_TIMESTAMP, DCSData.JSON_DATETIME, NetworkData.JSON_TYPE_VALUE, NetworkData.JSON_PHONE_TYPE, NetworkData.JSON_PHONE_TYPE_CODE, NetworkData.JSON_NETWORK_TYPE, NetworkData.JSON_NETWORK_TYPE_CODE, NetworkData.JSON_ACTIVE_NETWORK_TYPE, NetworkData.JSON_ACTIVE_NETWORK_TYPE_CODE, NetworkData.JSON_CONNECTED, NetworkData.JSON_ROAMING, NetworkData.JSON_NETWORK_OPERATOR_CODE, NetworkData.JSON_NETWORK_OPERATOR_NAME, NetworkData.JSON_SIM_OPERATOR_CODE, NetworkData.JSON_SIM_OPERATOR_NAME};
	public static final String[] PHONEIDENTITY_FIELDS = {DCSData.JSON_TYPE, DCSData.JSON_TIMESTAMP, DCSData.JSON_DATETIME, PhoneIdentityData.JSON_IMEI, PhoneIdentityData.JSON_IMSI, PhoneIdentityData.JSON_MANUFACTURER, PhoneIdentityData.JSON_MODEL,	PhoneIdentityData.JSON_OSTYPE, PhoneIdentityData.JSON_OSVERSION};
	public static final String[] TRAFFIC_FIELDS = {DCSData.JSON_TYPE, DCSData.JSON_TIMESTAMP, DCSData.JSON_DATETIME, TrafficData.JSON_MOBILERXBYTES, TrafficData.JSON_MOBILETXBYTES, TrafficData.JSON_TOTALRXBYTES, TrafficData.JSON_TOTALTXBYTES, TrafficData.JSON_APPRXBYTES, TrafficData.JSON_APPTXBYTES, TrafficData.JSON_DURATION};
	public static final String[] GSMCELL_FIELDS= {DCSData.JSON_TYPE, DCSData.JSON_TIMESTAMP, DCSData.JSON_DATETIME, CellTowersData.JSON_CELL_TOWER_ID, CellTowersData.JSON_LOCATION_AREA_CODE, CellTowersData.JSON_UMTS_PSC, CellTowersData.JSON_SIGNAL_STRENGTH};
	public static final String[] CDMACELL_FIELDS = {DCSData.JSON_TYPE, DCSData.JSON_TIMESTAMP, DCSData.JSON_DATETIME, CellTowersData.JSON_BASE_STATION_ID, CellTowersData.JSON_BASE_STATION_LATITUDE, CellTowersData.JSON_BASE_STATION_LONGITUDE, CellTowersData.JSON_SYSTEM_ID, CellTowersData.JSON_NETWORK_ID, CellTowersData.JSON_DBM, CellTowersData.JSON_ECIO};
	public static final String[] NEIGHBOUR_FIELDS = {DCSData.JSON_TYPE, DCSData.JSON_TIMESTAMP, DCSData.JSON_DATETIME, CellTowersData.JSON_NETWORK_TYPE_CODE, CellTowersData.JSON_NETWORK_TYPE, CellTowersData.JSON_RSSI, CellTowersData.JSON_UMTS_PSC, CellTowersData.JSON_CELL_TOWER_ID, CellTowersData.JSON_LOCATION_AREA_CODE};
	
	//condition data
	public static final String[] NETACTIVITYCONDITION_FIELDS = {ConditionResult.JSON_TYPE, ConditionResult.JSON_TIMESTAMP, ConditionResult.JSON_DATETIME, ConditionResult.JSON_SUCCESS, NetActivityCondition.JSON_MAXBYTESIN, NetActivityCondition.JSON_MAXBYTESOUT, NetActivityCondition.JSON_BYTESIN, NetActivityCondition.JSON_BYTESOUT};
	public static final String[] CPUACTIVITYCONDITION_FIELDS = {ConditionResult.JSON_TYPE, ConditionResult.JSON_TIMESTAMP, ConditionResult.JSON_DATETIME, ConditionResult.JSON_SUCCESS, CpuActivityCondition.JSON_MAX_AVG, CpuActivityCondition.JSON_READ_AVG };
	public static final String[] DATACAPCONDITION_FIELDS = {ConditionResult.JSON_TYPE, ConditionResult.JSON_TIMESTAMP, ConditionResult.JSON_DATETIME, ConditionResult.JSON_SUCCESS};
	
	private static final Map<String, String[]> convertor;
	static{
		Map <String, String[]> aMap = new HashMap<String, String[]>();
		aMap.put(HttpTest.DOWNSTREAMMULTI, HTTP_FIELDS);
		aMap.put(HttpTest.DOWNSTREAMSINGLE, HTTP_FIELDS);
		aMap.put(HttpTest.UPSTREAMMULTI, HTTP_FIELDS);
		aMap.put(HttpTest.UPSTREAMSINGLE, HTTP_FIELDS);
		aMap.put(LatencyTest.STRING_ID, LATENCY_FIELDS);
		aMap.put(ClosestTarget.TESTSTRING, CLOSESTTARGET_FIELDS);
		aMap.put(LocationData.JSON_LOCATION, LOCATION_FIELDS);
		aMap.put(NetworkData.JSON_TYPE_VALUE, NETWORKDATA_FIELDS);
		aMap.put(PhoneIdentityData.JSON_TYPE_PHONE_IDENTITY, PHONEIDENTITY_FIELDS);
		aMap.put(TrafficData.JSON_TYPE_NETUSAGE, TRAFFIC_FIELDS);
		aMap.put(CellTowersData.JSON_TYPE_GSM_CELL_LOCATION, GSMCELL_FIELDS);
		aMap.put(CellTowersData.JSON_TYPE_CDMA_CELL_LOCATION, CDMACELL_FIELDS);
		aMap.put(CellTowersData.JSON_TYPE_CELL_TOWER_NEIGHBOUR, NEIGHBOUR_FIELDS);
		aMap.put(NetActivityCondition.TYPE_VALUE,NETACTIVITYCONDITION_FIELDS);
		aMap.put(CpuActivityCondition.TYPE_VALUE,CPUACTIVITYCONDITION_FIELDS);
		aMap.put(DatacapCondition.JSON_DATACAP, DATACAPCONDITION_FIELDS);
		convertor = Collections.unmodifiableMap(aMap);
	}
	
	public static void setStorage(File storage){
		ExportFile.storage = storage;
	}
	
	public static void saveResults(JSONObject resultToSave ){
		checkFileSize();
		appendJSON(resultToSave);
		appendToCSVFiles(resultToSave);
	}
	
	private static void arrayToCSV(String type_field, JSONArray ja){
		for(int i = 0; i < ja.length(); i++){
			JSONObject curr = ja.optJSONObject(i);
			String test_type = curr.optString(type_field);
			String[] fields =  convertor.get(test_type);
			if(fields != null){
				appendToCSV(test_type.toLowerCase() + CSV_FILEEXTENSION, fields, extractValues(fields,curr));
			}
		}
	}
	
	private static void appendToCSVFiles(JSONObject resultToSave){
		String[] main = extractValues(MAIN_FIELDS, resultToSave);
		appendToCSV(MAINSECTION_FILENAME, MAIN_FIELDS, main);
		JSONArray tests = resultToSave.optJSONArray(ResultsContainer.JSON_TESTS);
		if(tests != null){
			arrayToCSV(Test.JSON_TYPE, tests);
		}
		JSONArray metrics = resultToSave.optJSONArray(ResultsContainer.JSON_METRICS);
		if(metrics != null){
			arrayToCSV(DCSData.JSON_TYPE, metrics);
		}
		JSONArray conditions = resultToSave.optJSONArray(ResultsContainer.JSON_CONDITIONS);
		if(conditions != null){
			arrayToCSV(ConditionResult.JSON_TYPE, conditions);
		}
	}
	
	private static String[] extractValues(String[] fields, JSONObject o){
		List<String> ret = new ArrayList<String>();
		for(String k: fields){
			ret.add(o.optString(k,EMPTY_FIELD));
		}
		return ret.toArray(new String[0]);
	}
	
	private static byte[] csvLine(String[] values){
		StringBuilder sb = new StringBuilder();
		byte[] ret;
		for(int i=0; i < values.length ; i++){
			sb.append(FIELD_DELIMITER);
			sb.append(values[i]);
			sb.append(FIELD_DELIMITER);
			if(i < values.length - 1){
				sb.append(FIELD_SEPARATOR);
			}
		}
		sb.append(RECORD_SEPARATOR);
		try{
			ret = sb.toString().getBytes(FILE_ENCODING);
		}catch(UnsupportedEncodingException e){
			ret = new byte[0];
		}
		return ret;
	}
	
	public static void appendToCSV(String fileName, String[] fields, String[] values){
		File csv = new File(storage, fileName);
		boolean addHeader = !csv.exists();
		try{
			FileOutputStream os = new FileOutputStream(csv,true);
			if(addHeader){
				os.write(csvLine(fields));
			}
			os.write(csvLine(values));
			os.close();
		}catch(IOException eio){
			SKLogger.e(ExportFile.class, "Failed to write " + fileName, eio);
		}
		
	}
	
	private static void appendJSON(JSONObject resultToSave){
		File resultsJSONFile = new File(storage, TEST_RESULTS_JSON_FILE);
		JSONArray results = new JSONArray();
		if(!resultsJSONFile.exists()){
			try{
				resultsJSONFile.createNewFile();
			}catch(IOException e){
				SKLogger.e(ExportFile.class, "Failed to create "+ TEST_RESULTS_JSON_FILE + " to save results.", e);
				return;
			}
		}else{
			//read content from the file, it is a json array, each entry is a json object submitted
			try{
				FileInputStream is = new FileInputStream(resultsJSONFile);
				int size = is.available();
				if(size != 0){
					byte[] buffer = new byte [size];
					is.read(buffer);
					is.close();
					results = new JSONArray(new String(buffer, FILE_ENCODING));
				}
			}catch(IOException e){
				SKLogger.e(ExportFile.class, "Cannot read from: "+ TEST_RESULTS_JSON_FILE + " to save results.", e);
				return;
			}catch(JSONException je){
				SKLogger.e(ExportFile.class, "Cannot parse the content of "+ TEST_RESULTS_JSON_FILE + " to a json array", je);
				results = new JSONArray();
			}
		}
		results.put(resultToSave);
		//save the update json
		try{
			FileOutputStream os = new FileOutputStream(resultsJSONFile, false);
			String toFile = results.toString();
			os.write(toFile.getBytes(FILE_ENCODING));
			os.close();
		}catch(IOException e){
			SKLogger.e(ExportFile.class, "Unable to save json array to file" + TEST_RESULTS_JSON_FILE, e);
			return;
		}
	}
	
	public static File getZipFile(){
		zipFiles();
		return new File(storage, ZIP_FILE);
	}
	
	public static File[] getAllFiles(){
		List<File> ret = new ArrayList<File>();
		ret.add(new File(storage, TEST_RESULTS_JSON_FILE));
		ret.add(new File(storage, MAINSECTION_FILENAME + CSV_FILEEXTENSION));
		for(String file: convertor.keySet()){
			ret.add(new File(storage,file.toLowerCase() + CSV_FILEEXTENSION));
		}
		return ret.toArray(new File[0]);
	}
 	
	private static void checkFileSize(){
		long totalSize = 0;
		File[] files = getAllFiles();
		for(File currFile: files){
			totalSize += currFile.length();
		}
		if(totalSize > FILES_MAX_SIZE){
			for(File currFile: files){
				currFile.delete();
			}
		}
	}
	
	private static void zipFiles(){
		BufferedInputStream bis = null;
		try{
			FileOutputStream dest = new FileOutputStream(new File(storage, ZIP_FILE));
			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
			byte[] data = new byte[ZIP_BUFFER_SIZE];
			for(File currFile: getAllFiles()){
				String currFileName = currFile.getName();
				if(!currFile.exists()) continue;
				FileInputStream fis = new FileInputStream(currFile);
				bis = new BufferedInputStream(fis, ZIP_BUFFER_SIZE);
				ZipEntry entry = new ZipEntry(currFileName);
				out.putNextEntry(entry);
				int count;
				while((count = bis.read(data,0, ZIP_BUFFER_SIZE)) != -1 ){
					out.write(data, 0, count);
				}
				bis.close();
			}
			out.close();
		}catch(IOException e){
			SKLogger.e(ExportFile.class, "Error in creating the zip file for the export", e);
		}
	}
	
}

