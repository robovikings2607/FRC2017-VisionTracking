package pkg;

import java.io.File;
import java.io.PrintWriter;
import java.util.Calendar;

/**
 * @version 3.15.2017
 * @author DavidRein
 *
 */
public class DataLogger {
	
	private static String DIRECTORY_NAME;
	private String FILE_NAME;
	
	private PrintWriter writer;
	
	public DataLogger(String header) {
		makeDirectory();
		Calendar cal = Calendar.getInstance();
		FILE_NAME = "logFile-" + cal.get(Calendar.HOUR_OF_DAY)
				+ "-" + cal.get(Calendar.MINUTE) + "-" + cal.get(Calendar.SECOND);
		try {
			writer = new PrintWriter(new File(DIRECTORY_NAME.concat("/").concat(FILE_NAME).concat(".txt")));
			writer.println(cal.get(Calendar.MONTH) + "/" + cal.get(Calendar.DAY_OF_MONTH) + "/" + cal.get(Calendar.YEAR) + "    " 
					+ cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND));
			writer.println(header);
		} catch(Exception e) {}
	}
	
	public static void makeDirectory() {
		Calendar cal = Calendar.getInstance();
		DIRECTORY_NAME = "logs-" + cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) 
				+ "-" + cal.get(Calendar.DAY_OF_MONTH));
		
		File dir = new File(DIRECTORY_NAME);
		if(!dir.exists()) dir.mkdir();
	}
	
	public void log(String input) {
		writer.println(input);
	}
	
	public void close() {
		writer.flush();
		writer.close();
	}
}
