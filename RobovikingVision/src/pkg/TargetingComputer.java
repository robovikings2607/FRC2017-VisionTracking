package pkg;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import edu.wpi.first.smartdashboard.robot.Robot;
/**
 * Contains all logic and algorithms pertaining to processing a given image and calculating
 * data from the information acquired from processing the image. Additionally, allows for 'filtered'
 * images to be saved or displayed.
 * 
 * @version 3.15.2017
 * @author DavidRein
 *
 */
public class TargetingComputer {

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
	
	/**Rectangle that wraps the target contours, allowing for easy estimation of length and width of the target*/
	private Rect boundingRect;

	/**Ratio used to convert Pixels to Feet (Feet per Pixel ratio)*/
	private double ratioFTperPX;
	
	/**Stores the Distance from the camera to the target*/
	private double distToTargetFT , distToTargetPX;
	
	private double targetOffsetX , targetOffsetY;
	private double targetWidthPX , targetHeightPX;
	
	/**Stores the angle that the robot must turn to line up with the target in degrees*/
	private double angleToTargetDEG;
	
	//TODO add a config file or a settings menu to edit these tolerances
	//Tolerances for filtering the vision targets
	private final double areaToleranceLowerBound = 400.0;
	private final double areaToleranceUpperBound = 4000.0;
	private final float ratioToleranceLowerBound = 0.0F;
	private final float ratioToleranceUpperBound = 100.0F;
	private final int numberOfDesiredTargets = 2;
	private final int xDiffTolerance = 1000;
	private final int yDiffTolerance = 100;
	
	//Settings for Calibrating Vision
	private final double targetWidthFT = 1.25;
	private final double FOVangle = 61.0;
		
	/**Constructor - loads the OpenCV library and initializes fields of the TargetingComputer class*/
	public TargetingComputer() {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		IMAGE_TYPE = k_ORIGINAL_IMAGE;
		SAVE_IMAGES = false;
		savedFrameCount = 0;
		boundingRect = new Rect();
		Robot.setTeam(607);
	}
	
	boolean firstRun = true;
	DataLogger logger;
	/**Most important method of this class - calculates all data and generates the images to display
	 * @param buffImg - original image from external source
	 */
	public void process(BufferedImage buffImg) {
		originalImage = buffImg;
		imageMat = bufToMat(buffImg);
		Mat binMat = binarize(imageMat);
		binaryImage = matToBuf(binMat);
		List<MatOfPoint> contours = getTargets(binMat);
		processedImage = matToBuf(drawTargets(contours));
		if(!contours.isEmpty()){
			/*
			targetWidthPX = (double) boundingRect.width;
			targetHeightPX = (double) boundingRect.height;
			distToTargetFT = calculateDistanceToTarget(targetWidthPX);
			System.out.print(distToTargetFT + "    ");
			angleToTargetDEG = calculateAngleToTarget(distToTargetFT);
			System.out.println(angleToTargetDEG);
			*/
			angleToTargetDEG = calculateAngleToTurn();
			drawHUD();
		} else {
			distToTargetFT = 0.0;
			angleToTargetDEG = 0.0;
		}
		
		Robot.getTable().putNumber("degToRotate", angleToTargetDEG);
		
		if(SAVE_IMAGES) {
			switch(savedFrameCount) {
			case 0:
				ImageSaver.makeDirectory();
			default:
				new ImageSaver(originalImage, "Original", savedFrameCount).start();
				new ImageSaver(binaryImage, "Binary", savedFrameCount).start();
				new ImageSaver(processedImage, "Processed", savedFrameCount).start();
				savedFrameCount++;
			}
		} else {
			savedFrameCount = 0;
		}
		
		if(SAVE_IMAGES) {
			if(firstRun){
				logger = new DataLogger("Distance[ft] , Distance[px] , "
						+ "TargetWidth[px] , TargetHeight[px] , "
						+ "TargetOffsetX[px] , TargetOffsetY[px] , "
						+ "Angle[deg] , ratioFTperPX[ft/px]");
				firstRun = false;
			}
			logger.log(this.distToTargetFT + " , " + this.distToTargetPX + " , " 
					+ this.targetWidthPX + " , " + this.targetHeightPX + " , "
					+ this.targetOffsetX + " , " + this.targetOffsetY + " , "
					+ this.angleToTargetDEG + " , " + this.ratioFTperPX + " , ");
		} else if(!SAVE_IMAGES && !firstRun) {
			logger.close();
			firstRun = true;
		}
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
	
	/**Generates a list of desirable targets from a binary Mat image
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
	
	/**
	 * Draws graphics onto the processed image
	 */
	public void drawHUD() {
		Graphics g = processedImage.createGraphics();
		g.setColor(new Color(255, 0, 255));
		g.setFont(new Font("TimesRoman", Font.PLAIN, 12));
		g.drawString("DIST: " + distToTargetFT, boundingRect.x + boundingRect.width + 4, boundingRect.y + 7);
		g.drawString("ANGLE: " + angleToTargetDEG, boundingRect.x + boundingRect.width + 4, boundingRect.y + 19);
		g.setColor(new Color(255, 255, 255));
		g.drawLine(processedImage.getWidth() / 2, processedImage.getHeight() / 2, boundingRect.x + (boundingRect.width / 2), 
				processedImage.getHeight() / 2);
		g.drawLine(processedImage.getWidth() / 2, processedImage.getHeight() / 2, processedImage.getWidth() / 2, 
				boundingRect.y + (boundingRect.height / 2));
		g.setColor(new Color(255, 255, 0));
		g.drawLine(processedImage.getWidth() / 2, boundingRect.y + (boundingRect.height / 2), 
				boundingRect.x + (boundingRect.width / 2), boundingRect.y + (boundingRect.height / 2));
		g.drawLine(boundingRect.x + (boundingRect.width / 2), processedImage.getHeight() / 2, 
				boundingRect.x + (boundingRect.width / 2), boundingRect.y + (boundingRect.height / 2));
		g.drawLine(processedImage.getWidth() / 2, processedImage.getHeight() / 2, 
				boundingRect.x + (boundingRect.width / 2), boundingRect.y + (boundingRect.height / 2));
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
	
	/**
	 * Uses the width of the target in pixels to calculate the distance from the target to the camera in feet. Also
	 * sets the Feet per pixel Ratio.
	 * @param targetWidthPX - The width of the target in pixels
	 * @return double representing the distance between the target and camera
	 */
	public double calculateDistanceToTarget( double targetWidthPX) {
		//TODO Compensate for the target's width changing based on rotation of camera (if the target is askew
		//it will have a different width than if it was in the center)
		ratioFTperPX = targetWidthFT / targetWidthPX;
		distToTargetPX = (0.5 * (double) imageMat.width()) / Math.tan(Math.toRadians(FOVangle/2));
		return (0.5 * ((double) imageMat.width()) * ratioFTperPX) / Math.tan(Math.toRadians(FOVangle/2));
	}
	
	
	/**
	 * Uses the distance to the target to calculate the angle that the camera must rotate in order to line up with the target
	 * @param distanceToTargetFT - distance to target in feet
	 * @return double representing the targetOffsetX angle in degrees
	 */
	public double calculateAngleToTarget( double distanceToTargetFT ) {
		targetOffsetX = ((((double) boundingRect.x) + (targetWidthPX / 2.0)) - (0.5 * ((double) imageMat.width())));
		targetOffsetY = ((((double) boundingRect.y) + (targetHeightPX / 2.0)) - (0.5 * ((double) imageMat.height())));
		return Math.toDegrees(Math.atan((targetOffsetX * ratioFTperPX) / distanceToTargetFT));
	}
	
	public double calculateAngleToTurn() {
		double targetCOM = boundingRect.x + (boundingRect.width / 2);
		double degToTurn = (targetCOM - 240) * 61 / imageMat.width();
		System.out.println(degToTurn);
		return degToTurn;
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

}
