package pkg;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import edu.wpi.grip.core.sources.IPCameraFrameGrabber;

/**
 * Allows for capturing an image from an external source (e.g. camera feed, single image, image
 * file, etc.) given the path to that source. Also contains logic for switching between sources.
 * 
 * @version 3.15.2017
 * @author DavidRein
 */
public class ImageGrabber {
	
	/**Path for a target source*/
	public static String SOURCE_PATH;
	
	/**Represents currently desired source type (e.g. camera feed, single image)*/
	private static int SOURCE_TYPE;
	
	/**Constant that represents the respectively named external source*/
	public static final int k_CAMERA_FEED = 0 , k_IMAGE_STREAM = 1 , k_IMAGE_FILE = 2;
	
	/**The default image to be displayed if no source is detected*/
	public static BufferedImage DEFAULT_IMAGE;
	
	//private VideoCapture vc;
	private IPCameraFrameGrabber camGrabber;
	//private Mat cameraFrame;
	private ConnectionThread connectionThread;
	private ImageStreamer streamer;
	
	/**Constructor - initializes all static fields in the ImageGrabber class*/
	public ImageGrabber() {
		SOURCE_TYPE = k_CAMERA_FEED;
		//SOURCE_PATH = "1ftH8ftD1Angle0Brightness.jpg";
		SOURCE_PATH = "savedImages-2017-2-13.15-34-1/Original";
		try {DEFAULT_IMAGE = ImageIO.read(new File("placeholder.jpg"));} catch (IOException e) {}
		//vc = new VideoCapture(1);
		try {
			camGrabber = new IPCameraFrameGrabber("http://10.6.7.2:1181/?action=stream", 3000, 2000, TimeUnit.MILLISECONDS);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		connectionThread = new ConnectionThread();
		connectionThread.start();
		//cameraFrame = new Mat();
	}
	/**
	 * Captures and stores an image from an external source (e.g. camera feed, image stream, image file)
	 * @return BufferedImage that was captured
	 */
	public BufferedImage grab() {
		if(SOURCE_TYPE == ImageGrabber.k_CAMERA_FEED){
			if(!connectionThread.isAlive()) {
				try {
					return camGrabber.grabBufferedImage();
				} catch (Exception e) {
					System.out.printf("camGrabber.grab(): %s\n", e.getMessage());
		            try { Thread.sleep(1500); } catch (Exception e1) {}
				}
			} else {}
			
			//vc.read(cameraFrame);
			//return TargetingComputer.matToBuf(cameraFrame);
		}
		if(SOURCE_TYPE == ImageGrabber.k_IMAGE_STREAM){
			if(!ImageStreamer.hasEnded()) {
				if(!ImageStreamer.isInitialized()) streamer = new ImageStreamer(SOURCE_PATH);
				try { return ImageIO.read(new File(streamer.streamFileNames()));} catch (IOException e) {return DEFAULT_IMAGE;}
			}
		}
		if(SOURCE_TYPE == ImageGrabber.k_IMAGE_FILE) {
			try { return ImageIO.read(new File(SOURCE_PATH));} catch (IOException e) {return DEFAULT_IMAGE;}
		}
		
		return DEFAULT_IMAGE;
	}
	
	/**@return path for the target source*/
	public static String getSourcePath(){
		return SOURCE_PATH;
	}
	
	/**Switches the desired source type
	 * @param sourceIndex - Constant representation of sources
	 * @see ImageGrabber(line 21)*/
	public static void setSourceType(int sourceIndex) {
		SOURCE_TYPE = sourceIndex;
	}
	
	private class ConnectionThread extends Thread {
		
		public void run(){
			boolean keepTrying = true;
	        while (keepTrying) {
	            String stepName = "null";
	            try {
	                stepName = "camGrabber.start()";
	                camGrabber.start();
	                stepName = "camGrabber.grab()";
	                camGrabber.grab();
	                keepTrying = false;
	            } catch (Exception e) {
	                System.out.printf("%s: %s ... retrying... \n", stepName, e.getMessage());               
	                try { Thread.sleep(1500); } catch (Exception e1) {}
	            }
	        }
		}
		
	}

}
