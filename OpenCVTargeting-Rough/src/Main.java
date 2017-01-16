import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class Main {

	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		Main m = new Main();
	}

	public Main() {
		processImage("1ftH5ftD1Angle0Brightness.jpg");
		processImage("1ftH10ftD1Angle0Brightness.jpg");
		processImage("1ftH10ftD2Angle0Brightness.jpg");
		processImage("1ftH10ftD3Angle0Brightness.jpg");
		processImage("1ftH11ftD2Angle0Brightness.jpg");
		processImage("1ftH11ftD3Angle0Brightness.jpg");
		processImage("1ftH12ftD2Angle0Brightness.jpg");
		processImage("1ftH12ftD3Angle0Brightness.jpg");
		processImage("1ftH13ftD3Angle0Brightness.jpg");
		processImage("1ftH14ftD3Angle0Brightness.jpg");
		processImage("1ftH150ftD0Angle0Brightness.jpg");
		processImage("1ftH2ftD0Angle0Brightness.jpg");
		processImage("1ftH3ftD0Angle0Brightness.jpg");
		processImage("1ftH3ftD2Angle0Brightness.jpg");
		processImage("1ftH3ftD3Angle0Brightness.jpg");
		processImage("1ftH4ftD0Angle0Brightness.jpg");
		processImage("1ftH4ftD1Angle0Brightness.jpg");
		processImage("1ftH4ftD2Angle0Brightness.jpg");
		processImage("1ftH4ftD3Angle0Brightness.jpg");
		processImage("1ftH5ftD1Angle0Brightness.jpg");
		processImage("1ftH5ftD2Angle0Brightness.jpg");
		processImage("1ftH5ftD3Angle0Brightness.jpg");
		processImage("1ftH6ftD1Angle0Brightness.jpg");
		processImage("1ftH6ftD3Angle0Brightness.jpg");
		processImage("1ftH7ftD1Angle0Brightness.jpg");
		processImage("1ftH7ftD2Angle0Brightness.jpg");
		processImage("1ftH7ftD3Angle0Brightness.jpg");
		processImage("1ftH8ftD1Angle0Brightness.jpg");
		processImage("1ftH8ftD2Angle0Brightness.jpg");
		processImage("1ftH8ftD3Angle0Brightness.jpg");
		processImage("1ftH9ftD1Angle0Brightness.jpg");
		processImage("1ftH9ftD2Angle0Brightness.jpg");
		processImage("1ftH9ftD3Angle0Brightness.jpg");
	}
	
	int runCount = 1;
	public void processImage(String filename) {
		
		//Acquire the image and convert it into a Mat
//		String filepath = "C:/Users/david/git/FRC2017-VisionTracking/sampleImages/LED Boiler/" + filename;
		String filepath = filename;
		//System.out.println(filepath);
		Mat Image = new Mat(); 
		Image = Imgcodecs.imread(filename);
		
		//Create a Mat with the same size as the original image
		Mat BinImg = new Mat(Image.size() , CvType.CV_8UC1);
		
		//Create empty Mats
		Mat G = new Mat();
		Mat R = new Mat();
		Mat Diff = new Mat();
		
		//Generate Mat of green values of the image and Mat of red values of the image
		Core.extractChannel(Image, G, 1);
		Core.extractChannel(Image, R, 2);
		
		//Generate a Mat of Green values of the image without any red value
		Core.subtract(G, R, Diff);
		
		//Generate a Binary Mat
		Imgproc.threshold(Diff, BinImg, 0, 255, Imgproc.THRESH_OTSU);
		
		
//		Imgcodecs.imwrite("C:/Users/david/Documents/eclipseworkspace/OpenCVPractice/BinaryImage" + runCount + ".jpg", BinImg);
		Imgcodecs.imwrite("BinaryImage" + runCount + ".jpg", BinImg);
		
		List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();
		Imgproc.findContours(BinImg, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
		
		//Filter Contours
		List<MatOfPoint> targetContours = new ArrayList<>();
		for(int contID = 0 ; contID <= contours.size() - 1 ; contID++) {
			RotatedRect rr = Imgproc.minAreaRect((new MatOfPoint2f(contours.get(contID).toArray())));
			Rect r = rr.boundingRect();
			float ratio = (float) r.width/ (float) r.height;
			double area = Imgproc.contourArea(contours.get(contID));
			if(area > 100.0 && area < 1100.0) {
				if(ratio < 10.0 && ratio > 1.0){
					targetContours.add(contours.get(contID));
	
					System.out.print(filename + " " + runCount);
					System.out.print(" " + Imgproc.contourArea(contours.get(contID)));
					System.out.print(" " + contours.get(contID).toArray()[0].x + " " + contours.get(contID).toArray()[0].y);
					System.out.println(" " + ratio);
				}
			}
		}
		
		boolean broken = false;
		List<MatOfPoint> targetContoursLVL2 = new ArrayList<>();
		if(targetContours.size() > 2) {
			for(int i = 0 ; i < targetContours.size() - 1 ; i++) {
				for(int j = i + 1 ; j < targetContours.size() ; j++) {
					if(Math.abs(targetContours.get(i).toArray()[0].x - targetContours.get(j).toArray()[0].x) < 20 ) {
						if(Math.abs(targetContours.get(i).toArray()[0].y - targetContours.get(j).toArray()[0].y) < 40){
							targetContoursLVL2.add(targetContours.get(i));
							targetContoursLVL2.add(targetContours.get(j));
							broken = true;
							break;
						}
					}
				}
				if(broken) break;
			}
		}
		
		//Draw contours
		Mat procImage = Image;
		if(targetContours.size() > 2)
			Imgproc.drawContours(procImage, targetContoursLVL2, -1, new Scalar(0 , 0 , 255), 2);
		else Imgproc.drawContours(procImage, targetContours, -1, new Scalar(0 , 0 , 255), 2);
//		Imgcodecs.imwrite("C:/Users/david/Documents/eclipseworkspace/OpenCVPractice/ProcessedImage" + runCount + ".jpg", procImage);
		Imgcodecs.imwrite("ProcessedImage" + runCount + ".jpg", procImage);
//		System.out.println(contours.get(contID).toArray()[0].x + " , " + contours.get(contID).toArray()[0].y);
		runCount++;
	}
}
