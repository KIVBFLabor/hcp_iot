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
import java.text.DecimalFormat;
import java.text.Format;
import java.util.List;
import java.util.Properties;
import javax.net.ssl.HttpsURLConnection;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalMultipurpose;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

public class Distance {
	public static boolean USEPROXY = false;
	public static String SERVLET_URL_cloud = "https://distmeasurep1941968690trial.hanatrial.ondemand.com/distancemeasurement/";
	public static String SENSOR_DB_ID_DIST_SENSOR = "1";

	private final static Format DF22 = new DecimalFormat("#0.00");
	private final static Format DF_N = new DecimalFormat("#.##########################");

	private final static double SOUND_SPEED = 34300;
	private final static double DIST_FACT   = SOUND_SPEED / 2;

	private static boolean verbose = false;
	private final static long BILLION      = (long)10E9;
	private final static int TEN_MICRO_SEC = 10 * 1000;

	public static void main(String args[]) {
		String repeatString = null;

		if (args.length > 0) {
			repeatString = args[0];
		} else {
			repeatString = "1000";
		}

		String calibrationValue = "0";

		int repeats = Integer.parseInt(repeatString);

		
			String infoSensor1 = getSensorInfo(SENSOR_DB_ID_DIST_SENSOR, "cm", "1", calibrationValue);
			if (infoSensor1 != null) {
				sendToCloud(infoSensor1);
			}
	
	}
	
	private static String getSensorInfo(String sensorId, String valueUnit, String sensorValueMultiplier, String sensorValueCalibration) {

		verbose = "true".equals(System.getProperty("verbose", "false"));

		final GpioController gpio = GpioFactory.getInstance();

		final GpioPinDigitalOutput trigPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "Trig", PinState.LOW);
		final GpioPinDigitalInput  echoPin = gpio.provisionDigitalInputPin(RaspiPin.GPIO_01,  "Echo");

		String result = null;
		String sensorValue = null;

		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			public void run()
			{
				System.out.println("Oops!");
				gpio.shutdown();
				System.out.println("Exiting nicely.");
			}       
		});
    
		System.out.println(">>> Waiting for the sensor to be ready (2s)...");
		try { Thread.sleep(2000); } catch (Exception ex) { ex.printStackTrace(); }

		trigPin.low();
		try { Thread.sleep(500); } catch (Exception ex) { ex.printStackTrace(); } 
      
    
		if (echoPin.isHigh())
			System.out.println(">>> !! Before sending signal, echo PIN is " + (echoPin.isHigh() ? "High" : "Low"));
        
		trigPin.high();
		try { Thread.sleep(0, TEN_MICRO_SEC); } catch (Exception ex) { ex.printStackTrace(); } 
		trigPin.low();
		while (echoPin.isLow());
			long start = System.nanoTime();
		while (echoPin.isHigh());
			long end   = System.nanoTime();

		System.out.println(">>> TOP: start=" + start + ", end=" + end);
      
		if (end > start)
		{
			double pulseDuration = (double)(end - start) / (double)BILLION;
			double distance = pulseDuration * DIST_FACT;
			System.out.println("Distance: " + DF22.format(distance) + " cm.");
		}
		else
		{
			if (verbose)
				System.out.println("Hiccup! start:" + start + ", end:" + end);
			try { Thread.sleep(500L); } catch (Exception ex) {}
		}
		System.out.println("Done.");
		trigPin.low();

		gpio.shutdown();
		sensorValue = DF22.format(distance);
		result = buildUrlParameters(sensorId, sensorValue, valueUnit, sensorValueMultiplier, sensorValueCalibration)

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

		// long sensorTimestamp = System.currentTimeMillis();

		String url = null;

		url = SERVLET_URL_cloud;
		setProxy(USEPROXY);

		url += urlParameters;

		try {
			URL obj = new URL(url);
			String responseMessage = "<NONE>";
			// System.out.println(" Calling url " + url);

			HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.flush();
			wr.close();

			responseMessage = con.getResponseMessage();

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