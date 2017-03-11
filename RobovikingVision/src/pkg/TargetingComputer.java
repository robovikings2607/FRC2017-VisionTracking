package pkg;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
/**
 * Contains all logic and algorithms pertaining to processing a given image and calculating
 * data from the information acquired from processing the image. Additionally, allows for 'filtered'
 * images to be saved or displayed.
 * 
 * @version 1.31.2017
 * @author DavidRein
 *
 */
public class TargetingComputer {

	//TODO Use these variables for...something
	public static String SAVED_FRAME_PATH = "";
	public static boolean SAVE_IMAGES;
	private int savedFrameCount;
	
	/**Represents currently desired image filter (e.g. original, binary, or processed)*/
	public static int IMAGE_TYPE;
	
	/**Constant that represents the respectively named image filter*/
	public static final int k_ORIGINAL_IMAGE = 0, k_BINARY_IMAGE = 1, k_PROCESSED_IMAGE = 2;
	
	/**Mat representation of the original BufferedImage*/
	private Mat imageMat;
	
	/**BufferedImage used to display the appropriately filtered image*/
	private BufferedImage processedImage , binaryImage , originalImage;
	
	private Rect boundingRect;

	private double ratioFTperPX;
	private double distToTargetFT;
	private double angleToTargetDEG;
	
	//Settings for Calibrating Vision
	private final double targetWidthFT = 1.25;
	private final double FOVangle = 61.0;
	
	//TODO add a config file or a settings menu to edit these tolerances
	//Tolerances for filtering the vision targets
	private final double areaToleranceLowerBound = 100.0;
	private final double areaToleranceUpperBound = 1100.0;
	private final float ratioToleranceLowerBound = 1.0F;
	private final float ratioToleranceUpperBound = 10.0F;
	private final int numberOfDesiredTargets = 2;
	private final int xDiffTolerance = 20;
	private final int yDiffTolerance = 40;
	
	/**Constructor - loads the OpenCV library and initializes fields of the TargetingComputer class*/
	public TargetingComputer() {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		IMAGE_TYPE = k_ORIGINAL_IMAGE;
		SAVE_IMAGES = false;
		savedFrameCount = 0;
		boundingRect = new Rect();
	}
	
	/**Most important method of this class - calculates all data and generates the images to display
	 * @param buffImg - original image from external source
	 */
	public void process(BufferedImage buffImg) {
		originalImage = buffImg;
		imageMat = bufToMat(buffImg);
		Mat binMat = binarize(imageMat);
		binaryImage = matToBuf(binMat);
		List<MatOfPoint> contours = filter(getTargets(binMat));
		processedImage = matToBuf(drawTargets(contours));
		if(!contours.isEmpty()){
			distToTargetFT = calculateDistanceToTarget((double) boundingRect.width);
			System.out.print(distToTargetFT + "    ");
			angleToTargetDEG = calculateAngleToTarget(distToTargetFT);
			System.out.println(angleToTargetDEG);
			drawHUD();
		} else {
			distToTargetFT = 0.0;
			angleToTargetDEG = 0.0;
		}
		//if(Display.SAVE_IMAGES) saveImage(processedImage);
		if(SAVE_IMAGES) System.out.println("THIS FEATURE IS NOT READY YET");
	}
	
	/**Convert an original Mat image into a black & white Mat image where any White would be 
	 * potentially desirable vision targets
	 * @param image - image to convert
	 * @return  converted black & white Mat image
	 */
	public Mat binarize(Mat image) {
		
		Mat binaryImage = new Mat(image.size() , CvType.CV_8UC1);
		
		Mat greenChannel = new Mat();
		Mat redChannel = new Mat();
		Mat difference = new Mat();
		Core.extractChannel(image, greenChannel, 1);
		Core.extractChannel(image, redChannel, 2);
		Core.subtract(greenChannel, redChannel, difference);
		Imgproc.threshold(difference , binaryImage , 0 , 255 , Imgproc.THRESH_OTSU);
		
		return binaryImage;
	}
	
	/**Generates a list of desireable targets from a binary Mat image
	 * @param binaryImage - binary Mat image to generate targets from
	 * @return list of potential targets
	 */
	public List<MatOfPoint> getTargets(Mat binaryImage) {
		List<MatOfPoint> contours = new ArrayList<>();
		Imgproc.findContours(binaryImage, contours, new Mat(), Imgproc.RETR_CCOMP , Imgproc.CHAIN_APPROX_SIMPLE);
		return filter(contours);
	}
	
	/**
	 * Filters the targets by removing contours that do not fit the area or ratio (width/height)
	 * of the desired target. If more than two targets remain, filter the list again by proximity
	 * of the targets to one another.
	 * @param contours - initial list of targets to be filtered through
	 * @return list of targets
	 */
	public List<MatOfPoint> filter(List<MatOfPoint> contours) {
		List<MatOfPoint> targets = new ArrayList<>();
		for(int contourIndex = 0 ; contourIndex < contours.size() ; contourIndex++) {
			double area = Imgproc.contourArea(contours.get(contourIndex));
			if(area > areaToleranceLowerBound && area < areaToleranceUpperBound) {
				Rect r = Imgproc.minAreaRect(  new MatOfPoint2f( contours.get(contourIndex).toArray() )  ).boundingRect();
				float ratio = (float) r.width / (float) r.height;
				if(ratio > ratioToleranceLowerBound && ratio < ratioToleranceUpperBound) {
					targets.add(contours.get(contourIndex));
				}
			}
		}
		if(targets.size() > numberOfDesiredTargets) {
			for(int contourIndexA = 0 ; contourIndexA < targets.size() - 1 ; contourIndexA++) {
				for(int contourIndexB = 0 ; contourIndexB < targets.size() ; contourIndexB++) {
					Point ContourA = targets.get(contourIndexA).toArray()[0];
					Point ContourB = targets.get(contourIndexB).toArray()[0];
					if(Math.abs(ContourA.x - ContourB.x) < xDiffTolerance){
						if(Math.abs(ContourA.y - ContourB.y) < yDiffTolerance){
							MatOfPoint matchingContourA = targets.get(contourIndexA);
							MatOfPoint matchingContourB = targets.get(contourIndexB);
							targets.clear();
							targets.add(matchingContourA);
							targets.add(matchingContourB);
							break;
						}
					}
				}
				if(targets.size() <= numberOfDesiredTargets) break;
			}
		}
		return targets;
	}
	
	/**
	 * Draws targets onto a Mat image
	 * @param targets - list of targets after filtering
	 * @return Mat image with detected targets
	 */
	public Mat drawTargets(List<MatOfPoint> targets) {
		Mat processedImage = imageMat.clone();
		Imgproc.drawContours(processedImage, targets, -1, new Scalar(0 , 255 , 0));
		if(targets.size() == numberOfDesiredTargets) {
			boundingRect = Imgproc.boundingRect(mergeContours(targets));
			Imgproc.rectangle(processedImage, boundingRect.tl(), boundingRect.br(), new Scalar(255, 0, 255));
		}
		return processedImage;
	}
	
	public void drawHUD() {
		Graphics g = processedImage.createGraphics();
		g.setColor(new Color(255, 0, 255));
		g.setFont(new Font("TimesRoman", Font.PLAIN, 12));
		g.drawString("DIST: " + distToTargetFT, boundingRect.x + boundingRect.width + 4, boundingRect.y + 7);
		g.drawString("ANGLE: " + angleToTargetDEG, boundingRect.x + boundingRect.width + 4, boundingRect.y + 19);
	}
	
	/**
	 * Adds multiple contours together by converting them into a list of points and merging the two
	 * lists
	 * @param targets - list of contours to merge
	 * @return a single MatOfPoint containing contours that were added together
	 */
	public MatOfPoint mergeContours(List<MatOfPoint> targets) {
		List<Point> pointList = new ArrayList<Point>();
		for(int i = 0 ; i < targets.size() ; i++) 
			pointList.addAll(targets.get(i).toList());
		//System.out.println(pointList);
		MatOfPoint mop = new MatOfPoint() ;
		mop.fromList(pointList);
		return mop;
	}
	
	public double calculateDistanceToTarget( double targetWidthPX) {
		//TODO Compensate for the target's width changing based on rotation of camera (if the target is askew
		//it will have a different width than if it was in the center)
		ratioFTperPX = targetWidthFT / targetWidthPX;
		return (0.5 * ((double) imageMat.width()) * ratioFTperPX) / Math.tan(Math.toRadians(FOVangle/2));
	}
	
	public double calculateAngleToTarget( double distanceToTargetFT ) {
		double offset = ((((double) boundingRect.x) + ((double) boundingRect.width) / 2.0) - (0.5 * ((double) imageMat.width())));
		return Math.toDegrees(Math.atan((offset * ratioFTperPX) / distanceToTargetFT));
	}
	/**
	 * Converts a Mat image into a Buffered image
	 * @param m - Mat image to convert
	 * @return BufferedImage
	 */
	public static BufferedImage matToBuf(Mat m) {
		
		//TODO print dimensions and channels of mat file for diagnostics purposes
        MatOfByte mByte = new MatOfByte();
        Imgcodecs.imencode(".jpg", m, mByte);
        try {
            return ImageIO.read(new ByteArrayInputStream(mByte.toArray()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
	
	/**
	 * Converts a BufferedImage into a Mat image
	 * @param img - BufferedImage to convert
	 * @return Mat image
	 */
	public Mat bufToMat(BufferedImage img) {
		byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
		Mat mat = new Mat(img.getHeight(), img.getWidth(), CvType.CV_8UC3);
		mat.put(0, 0, data);
		return mat;
	}
	
	/**@deprecated NOT FINISHED YET
	 * Saves the image to a directory.
	 * @param bi - BufferedImage to save
	 */
	public void saveImage( BufferedImage bi) {
		new ImageSaver(bi , "TEMP_NAME" , savedFrameCount).start();
		savedFrameCount++;
	}
	
	/**
	 * returns the correctly filtered image upon request
	 * @return desired image
	 */
	public BufferedImage getImage() {
		if(IMAGE_TYPE == k_ORIGINAL_IMAGE){
			return originalImage;
		}
		if(IMAGE_TYPE == k_BINARY_IMAGE){
			return binaryImage;
		}
		if(IMAGE_TYPE == k_PROCESSED_IMAGE){
			return processedImage;
		}
		System.out.println("Should've never reached this line");
		System.out.println("Gonna crash NOW");
		return ImageGrabber.DEFAULT_IMAGE;
	}
	
	/**
	 * Allows switching of image filter that is displayed
	 * @param imageTypeIndex - see TargetingComputer(line 28)
	 */
	public static void setImageType(int imageTypeIndex) {
		IMAGE_TYPE = imageTypeIndex;
	}
	
	//TODO Move this to its own separate class along with all the other image saving things
	private class ImageSaver extends Thread {
		private BufferedImage imageToSave;
		private int imageCount;
		private String imageInfo;
		
		public ImageSaver(BufferedImage image, String imageType, int count) {
			imageToSave = image;
			imageCount = count;
			imageInfo = imageType;
		}
		
		public void run(){
			StringBuffer sb = new StringBuffer(SAVED_FRAME_PATH);
			sb.append(File.separatorChar).append(imageInfo).append(".");
			sb.append(imageCount).append(".jpg");
			try {
				ImageIO.write(imageToSave, "jpg", new File(sb.toString()));
			} catch (IOException e) {}
		}
	}

}
