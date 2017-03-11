package pkg;

import org.opencv.core.Core;

/**
 * Main class of this program
 * 
 * @version 3.11.2017
 * @author DavidRein
 *
 */
public class Main {

	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		ImageGrabber ig = new ImageGrabber();
		TargetingComputer tc = new TargetingComputer();
		ImageDisplay disp = new ImageDisplay();
		while(true) {
			tc.process(ig.grab());
			disp.setImage(tc.getImage());
		}
	}

}
