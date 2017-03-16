package pkg;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import javax.imageio.ImageIO;

/**
 * @version 3.15.2017
 * @author DavidRein
 *
 */
public class ImageSaver extends Thread {
	
	private static String DIRECTORY = "";
	
	private BufferedImage imageToSave;
	private int imageCount;
	private String imageInfo;
	
	public ImageSaver(BufferedImage image, String imageType, int counter) {
		imageInfo = imageType;
		imageToSave = image;
		imageCount = counter;
	}
	
	public static void makeDirectory() {
		Calendar cal = Calendar.getInstance();
		DIRECTORY = "savedImages-" + cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) 
				+ "-" + cal.get(Calendar.DAY_OF_MONTH) + "." + cal.get(Calendar.HOUR_OF_DAY)
				+ "-" + cal.get(Calendar.MINUTE) + "-" + cal.get(Calendar.SECOND));
		new File(DIRECTORY).mkdir();
		new File(DIRECTORY.concat("/").concat("Binary")).mkdir();
		new File(DIRECTORY.concat("/").concat("Original")).mkdir();
		new File(DIRECTORY.concat("/").concat("Processed")).mkdir();
		
	}
	
	
	@Override
	public void run(){
		StringBuffer sb = new StringBuffer(DIRECTORY.concat("/").concat(imageInfo));
		sb.append(File.separatorChar).append(imageInfo).append("-");
		if(imageCount < 10) sb.append("000");
		else if(imageCount < 100) sb.append("00");
		else if(imageCount < 1000) sb.append("0");
		sb.append(imageCount).append(".jpg");
		try {
			ImageIO.write(imageToSave, "jpg", new File(sb.toString()));
		} catch (IOException e) {}
	}
}
