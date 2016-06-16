import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;

public class Temperature {

	// If this is set to false the SERVLET_URL_local will be used send the values to.
	// If this is set to true the SERVLET_URL_cloud will be used send the values to.
	public static boolean RUNINCLOUD = true;
	// Set this to true if you are working behind a proxy server.
	// If you are working from home you should set this most probable to false
	public static boolean USEPROXY = false;
	// Assign the right IP Address of the machine where you are running a local instance
	// of the SAP HCP app. Can be ignored if you are just testing in the cloud (RUNINCLOUD = true)
	public static String SERVLET_URL_local = "http://192.168.178.23:8080/iotscenario";
	// Assign the right link of your app in your account. Just copy from the account cockpit of your account
	// under the Java app that you've deployed in the second blog post
	// (under "Applications URLs" in the Java Application Dashboard of your Java app)
	public static String SERVLET_URL_cloud = "https://iotscenariop1941968690trial.hanatrial.ondemand.com/iotscenario/";
	// This id needs to be adapted to the sensor ID of the digital sensor.
	// To do that checkout the folder /sys/bus/w1/devices/ and search for a
	// folder
	// called 28-xxxxxxxxxxx. That folder name is the id of your sensor
	public static String SENSOR_HARDWARE_ID = "28-0115c1b180ff";

	// You need to assign the hardware sensors to the sensor id
	// in your database
	// Check your output by simply calling the URL defined under
	// SERVLET_URL_cloud or SERVLET_URL_local
	// /
	// Assign ID to the sensor that should be assigned to the CPU temperature
	// here:
	public static String SENSOR_DB_ID_CPU_TEMP = "1";
	//
	// Assign ID to the sensor that should be assigned to the external
	// temperature sensor here:
	public static String SENSOR_DB_ID_TEMP_SENSOR = "2";

	public static void main(String args[]) {
		String repeatString = null;

		if (args.length > 0) {
			repeatString = args[0];
		} else {
			repeatString = "1000";
		}

		// Helps simulating a "freezy" environment for the external temperature
		// sensor
		String calibrationValue = "0";

		int repeats = Integer.parseInt(repeatString);

		for (int i = 0; i < repeats; i++) {
			System.out.println("Loop " + (i + 1));
			String infoOnboardSensor = getOnboardTempSensor(SENSOR_DB_ID_CPU_TEMP, "Celsius", "1", "0");
			if (infoOnboardSensor != null) {
				sendToCloud(infoOnboardSensor);
			}

			String infoSensor2 = getSensorInfo(SENSOR_DB_ID_TEMP_SENSOR, "Celsius", "0.001", calibrationValue);
			if (infoSensor2 != null) {
				sendToCloud(infoSensor2);
			}
		}
	}

	private static String getSensorInfo(String sensorId, String valueUnit, String sensorValueMultiplier, String sensorValueCalibration) {

		String result = null;
		String filename = "/sys/bus/w1/devices/" + SENSOR_HARDWARE_ID + "/w1_slave";

		String fileContent = null;
		fileContent = readFile(filename);
		String sensorValue = null;

		if (fileContent != null && fileContent.length() > 0) {
			String[] temp = fileContent.split("t=");
			sensorValue = temp[1].trim();
			System.out.println(" - Measured temperature on sensor >" + SENSOR_HARDWARE_ID + "< is " + sensorValue);
			result = buildUrlParameters(sensorId, sensorValue, valueUnit, sensorValueMultiplier, sensorValueCalibration);
		}

		return result;
	}

	private static String getOnboardTempSensor(String sensorId, String valueUnit, String sensorValueMultiplier, String sensorValueCalibration) {

		String result = null;
		String command = "/opt/vc/bin/vcgencmd measure_temp";

		String fileContent = getCommandOutput(command);
		String sensorValue = null;

		if (fileContent != null && fileContent.length() > 0) {
			String[] temp = fileContent.split("=");
			sensorValue = temp[1].trim();
			sensorValue = sensorValue.substring(0, sensorValue.length() - 2);
			System.out.println(" - Measured CPU sensor temperature is " + sensorValue);
			result = buildUrlParameters(sensorId, sensorValue, valueUnit, sensorValueMultiplier, sensorValueCalibration);

		}
		return result;
	}

	private static String getCommandOutput(String command) {

		String result = "";
		String s;
		Process p;

		try {
			p = Runtime.getRuntime().exec(command);
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ((s = br.readLine()) != null)
				result += s;
			p.waitFor();
			p.destroy();
		} catch (Exception e) {
		}
		return result;
	}

	private static String readFile(String filename) {
		String result = null;
		try {
			List<String> lines = Files.readAllLines(Paths.get(filename), Charset.defaultCharset());
			result = lines.get(1).toString();
		} catch (IOException e) {
			return null;
		}
		return result;
	}

	private static void setProxy(boolean needsProxy) {

		Properties systemSettings = System.getProperties();
		if (needsProxy == true) {
			systemSettings.put("https.proxyHost", "proxy");
			systemSettings.put("https.proxyPort", "8080");
			systemSettings.put("http.proxyHost", "proxy");
			systemSettings.put("http.proxyPort", "8080");

		} else {
			systemSettings.put("http.proxySet", "false");
			systemSettings.put("https.proxySet", "false");
		}
	}

	private static String buildUrlParameters(String sensorId, String sensorValue, String valueUnit, String sensorValueMultiplier, String sensorValueCalibration) {
		String urlParameters = "&sensorvalue=" + urlEncodeParameter(sensorValue);
		urlParameters += "&sensorid=" + urlEncodeParameter(sensorId);
		urlParameters += "&unit=" + urlEncodeParameter(valueUnit);
		urlParameters += "&sensorvaluemultiplier=" + urlEncodeParameter(sensorValueMultiplier);
		urlParameters += "&sensorvaluecalibration=" + urlEncodeParameter(sensorValueCalibration);
		return "/?action=addsensorvalue" + urlParameters;
	}

	private static void sendToCloud(String urlParameters) {

		long sensorTimestamp = System.currentTimeMillis();

		String url = null;

		if (RUNINCLOUD == true) {
			url = SERVLET_URL_cloud;
		} else {
			url = SERVLET_URL_local;
		}
		setProxy(USEPROXY);

		url += urlParameters;

		try {
			URL obj = new URL(url);
			String responseMessage = "<NONE>";
			// System.out.println(" Calling url " + url);

			if (RUNINCLOUD == true) {
				HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
				// add request header
				con.setRequestMethod("POST");
				con.setRequestProperty("User-Agent", "Mozilla/5.0");
				con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

				// Send post request
				con.setDoOutput(true);
				DataOutputStream wr = new DataOutputStream(con.getOutputStream());
				wr.flush();
				wr.close();

				responseMessage = con.getResponseMessage();
			} else {
				HttpURLConnection con = (HttpURLConnection) obj.openConnection();
				// add request header
				con.setRequestMethod("POST");
				con.setRequestProperty("User-Agent", "Mozilla/5.0");
				con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

				// Send post request
				con.setDoOutput(true);
				DataOutputStream wr = new DataOutputStream(con.getOutputStream());
				wr.flush();
				wr.close();

				responseMessage = con.getResponseMessage();

				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();

				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();

				// print result
				System.out.println(response.toString());

			}
			System.out.println("  - Sent value to cloud. Response: " + responseMessage);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static String urlEncodeParameter(String parameter) {
		String result = "CouldNotEncodeParameter";

		try {
			result = URLEncoder.encode(parameter, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return result;
	}

}