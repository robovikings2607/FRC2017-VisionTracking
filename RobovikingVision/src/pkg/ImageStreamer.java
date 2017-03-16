package pkg;

import java.io.File;
/**
 * @version 3.15.2017
 * @author DavidRein
 *
 */
public class ImageStreamer {
	
	//TODO Documentation
	
	private static boolean STREAM_INITIALIZED = false;
	private static boolean STREAM_ENDED = false;
	
	private File[] files;
	private File directory;
	private int i;
	
	public ImageStreamer(String dirPath) {
		directory = new File(dirPath);
		files = directory.listFiles();
		i = 0;
		STREAM_INITIALIZED = true;
		STREAM_ENDED = false;
	}
	
	public String streamFileNames() {
		if(i < files.length) {
			return ImageGrabber.getSourcePath().concat("/").concat(files[i++].getName());
		} else {
			STREAM_INITIALIZED = false;
			STREAM_ENDED = true;
			return "placeholder.jpg";
		}
	}
	
	public static void reset() {
		STREAM_INITIALIZED = false;
		STREAM_ENDED = false;
	}
	
	public static boolean isInitialized() {
		return STREAM_INITIALIZED;
	}
	public static boolean hasEnded() {
		return STREAM_ENDED;
	}
	
}
