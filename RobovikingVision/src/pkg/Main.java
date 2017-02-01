package pkg;
/**
 * Main class of this program
 * 
 * @version 1.31.2017
 * @author DavidRein
 *
 */
public class Main {

	public static void main(String[] args) {
		ImageGrabber ig = new ImageGrabber();
		TargetingComputer tc = new TargetingComputer();
		ImageDisplay disp = new ImageDisplay();
		while(true) {
			tc.process(ig.grab());
			disp.setImage(tc.getImage());
		}
	}

}
