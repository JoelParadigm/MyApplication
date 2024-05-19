package com.example.myapplication.util;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class MatUtil {
    public static Mat rotateMat90CounterClockwise(Mat src) {
        Mat dst = new Mat();
        double angle=-90;
        Point rotPoint=new Point(src.cols()/2.0, src.rows()/2.0);
        Mat rotMat = Imgproc.getRotationMatrix2D( rotPoint, angle, 1);
        Imgproc.warpAffine(src, dst, rotMat, src.size());

        return dst;
    }

    public static Mat drawPerspectiveGrid(Mat image, Point[] planePoints) {
        // Check that we have exactly four points
        if (planePoints.length != 4) {
            return image;
        }

        // Define the destination points for perspective transform
        Point[] dstPoints = new Point[] {
                new Point(0, 0),
                new Point(image.cols(), 0),
                new Point(image.cols(), image.rows()),
                new Point(0, image.rows())
        };

        // Convert points to Mat
        Mat srcMat = new Mat(4, 1, CvType.CV_32FC2);
        Mat dstMat = new Mat(4, 1, CvType.CV_32FC2);
        for (int i = 0; i < 4; i++) {
            srcMat.put(i, 0, planePoints[i].x, planePoints[i].y);
            dstMat.put(i, 0, dstPoints[i].x, dstPoints[i].y);
        }

        // Get the perspective transform matrix
        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(srcMat, dstMat);

        // Draw the perspective grid
        int numLines = 10; // Number of lines in the grid
        for (int i = 0; i <= numLines; i++) {
            double alpha = (double) i / numLines;
            // Interpolate points on the plane edges
            Point p1 = new Point((1 - alpha) * planePoints[0].x + alpha * planePoints[1].x,
                    (1 - alpha) * planePoints[0].y + alpha * planePoints[1].y);
            Point p2 = new Point((1 - alpha) * planePoints[3].x + alpha * planePoints[2].x,
                    (1 - alpha) * planePoints[3].y + alpha * planePoints[2].y);
            Point p3 = new Point((1 - alpha) * planePoints[0].x + alpha * planePoints[3].x,
                    (1 - alpha) * planePoints[0].y + alpha * planePoints[3].y);
            Point p4 = new Point((1 - alpha) * planePoints[1].x + alpha * planePoints[2].x,
                    (1 - alpha) * planePoints[1].y + alpha * planePoints[2].y);

            // Transform the points to the destination perspective
            Mat p1Mat = new Mat(1, 1, CvType.CV_32FC2);
            Mat p2Mat = new Mat(1, 1, CvType.CV_32FC2);
            Mat p3Mat = new Mat(1, 1, CvType.CV_32FC2);
            Mat p4Mat = new Mat(1, 1, CvType.CV_32FC2);
            p1Mat.put(0, 0, p1.x, p1.y);
            p2Mat.put(0, 0, p2.x, p2.y);
            p3Mat.put(0, 0, p3.x, p3.y);
            p4Mat.put(0, 0, p4.x, p4.y);

            Mat p1Trans = new Mat();
            Mat p2Trans = new Mat();
            Mat p3Trans = new Mat();
            Mat p4Trans = new Mat();
            Core.perspectiveTransform(p1Mat, p1Trans, perspectiveTransform);
            Core.perspectiveTransform(p2Mat, p2Trans, perspectiveTransform);
            Core.perspectiveTransform(p3Mat, p3Trans, perspectiveTransform);
            Core.perspectiveTransform(p4Mat, p4Trans, perspectiveTransform);

            Point p1TransPoint = new Point(p1Trans.get(0, 0));
            Point p2TransPoint = new Point(p2Trans.get(0, 0));
            Point p3TransPoint = new Point(p3Trans.get(0, 0));
            Point p4TransPoint = new Point(p4Trans.get(0, 0));

            // Draw horizontal lines
            Imgproc.line(image, p1TransPoint, p2TransPoint, new Scalar(0, 255, 0), 1);
            // Draw vertical lines
            Imgproc.line(image, p3TransPoint, p4TransPoint, new Scalar(0, 255, 0), 1);
        }

        // Draw the horizon line (approximated as a horizontal line at the top of the image)
        Imgproc.line(image, new Point(0, 0), new Point(image.cols(), 0), new Scalar(255, 0, 0), 2);

        return image;
    }

    public static Mat drawPerpendicularLine(Mat image, Point[] planePoints) {
        if (planePoints.length != 4) {
            return image;
        }

        // Calculate midpoints of opposite sides
        Point midpoint1 = getMidpoint(planePoints[0], planePoints[1]);
        Point midpoint2 = getMidpoint(planePoints[2], planePoints[3]);

        // Draw the perpendicular line (yellow color)
        Imgproc.line(image, midpoint1, midpoint2, new Scalar(0, 255, 255), 2);

        return image;
    }

    private static Point getMidpoint(Point p1, Point p2) {
        return new Point((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
    }

    public static Mat warpImage(Mat image, Point p1, Point p2, Point p3, Point p4) {
        // Define the destination points (the corners of the output image)
        Point[] destPoints = new Point[]{
                new Point(0, 0),                      // Top-left corner
                new Point(image.cols() - 1, 0),       // Top-right corner
                new Point(image.cols() - 1, image.rows() - 1), // Bottom-right corner
                new Point(0, image.rows() - 1)        // Bottom-left corner
        };

        // Define the source points (the points to be warped to the corners)
        Point[] srcPoints = new Point[]{ p1, p2, p3, p4 };

        // Convert points to MatOfPoint2f
        MatOfPoint2f srcMat = new MatOfPoint2f(srcPoints);
        MatOfPoint2f destMat = new MatOfPoint2f(destPoints);

        // Get the perspective transformation matrix
        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(srcMat, destMat);

        // Apply the perspective transformation
        Mat warpedImage = new Mat();
        Imgproc.warpPerspective(image, warpedImage, perspectiveTransform, new Size(image.cols(), image.rows()));

        return warpedImage;
    }

    public static Point[] sortPoints(Point[] points) {
        if (points.length != 4) {
            throw new IllegalArgumentException("The input array must contain exactly 4 points.");
        }

        // Calculate the centroid of the points
        Point centroid = new Point(0, 0);
        for (Point point : points) {
            centroid.x += point.x;
            centroid.y += point.y;
        }
        centroid.x /= 4;
        centroid.y /= 4;

        // Sort points based on their relative positions to the centroid
        Arrays.sort(points, new Comparator<Point>() {
            @Override
            public int compare(Point p1, Point p2) {
                double angle1 = Math.atan2(p1.y - centroid.y, p1.x - centroid.x);
                double angle2 = Math.atan2(p2.y - centroid.y, p2.x - centroid.x);
                return Double.compare(angle1, angle2);
            }
        });

        // Ensure points are in the correct order
        Point topLeft = points[0];
        for (Point point : points) {
            if (point.x < topLeft.x || (point.x == topLeft.x && point.y < topLeft.y)) {
                topLeft = point;
            }
        }

        // Rotate points array so that the top-left point is first
        while (points[0] != topLeft) {
            Point first = points[0];
            System.arraycopy(points, 1, points, 0, points.length - 1);
            points[points.length - 1] = first;
        }

        // Swap last two points if necessary to ensure correct order
        if (points[1].x > points[2].x) {
            Point temp = points[1];
            points[1] = points[3];
            points[3] = points[2];
            points[2] = temp;
        }

        return points;
    }

    public static Mat findAndDrawBoundingBoxes(Mat src) {
        // Convert the image to grayscale
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);

        // Apply a binary threshold to the grayscale image
        Mat binary = new Mat();
        Imgproc.threshold(gray, binary, 100, 255, Imgproc.THRESH_BINARY);

        // Find contours
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        // Draw bounding boxes around detected contours
        for (MatOfPoint contour : contours) {
            Rect boundingRect = Imgproc.boundingRect(contour);
            Imgproc.rectangle(src, boundingRect.tl(), boundingRect.br(), new Scalar(0, 255, 0), 2);
        }

        return src;
    }

    public static Mat increaseContrast(Mat inputImage, double alpha) {
        try {
            // Convert the image to HSV color space
            Mat hsvImage = new Mat();
            Imgproc.cvtColor(inputImage, hsvImage, Imgproc.COLOR_BGR2HSV);

            // Split the HSV image into channels
            List<Mat> channels = new ArrayList<>();
            Core.split(hsvImage, channels);

            // Increase the saturation channel
            Mat saturationChannel = channels.get(1); // Index 1 is for the saturation channel
            Core.add(saturationChannel, new Scalar(alpha), saturationChannel);

            // Merge the channels back into an HSV image
            Core.merge(channels, hsvImage);

            // Convert the HSV image back to BGR
            Mat resultImage = new Mat();
            Imgproc.cvtColor(hsvImage, resultImage, Imgproc.COLOR_HSV2BGR);

            return resultImage;
        } catch (Exception e) {
            System.err.println("An error occurred in increaseSaturation method:");
            e.printStackTrace();
            return null;
        }
    }

    public static List<MatOfPoint> findContours(Mat inputImage) {
        try {
            // Convert the image to grayscale
            Mat grayImage = new Mat();
            Imgproc.cvtColor(inputImage, grayImage, Imgproc.COLOR_BGR2GRAY);

            // Threshold the grayscale image
            Mat binaryImage = new Mat();
            Imgproc.threshold(grayImage, binaryImage, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

            // Find contours in the binary image
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(binaryImage, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            return contours;
        } catch (Exception e) {
            System.err.println("An error occurred in findContours method:");
            e.printStackTrace();
            return null;
        }
    }

    public static void drawContours(Mat inputImage, List<MatOfPoint> contours) {
        try {
            // Draw contours on the input image
            Imgproc.drawContours(inputImage, contours, -1, new Scalar(0, 255, 0), 2);
        } catch (Exception e) {
            System.err.println("An error occurred in drawContours method:");
            e.printStackTrace();
        }
    }

    public static Mat reduceColors(Mat inputImage, int k) {
        try {
            // Reshape the image to a 1D array of pixels
            Mat reshapedImage = inputImage.reshape(1, inputImage.cols() * inputImage.rows());

            // Convert the reshaped image to float
            reshapedImage.convertTo(reshapedImage, CvType.CV_32F);

            // Define criteria and apply k-means clustering
            TermCriteria criteria = new TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 10, 1.0);
            Core.kmeans(reshapedImage, k, new Mat(), criteria, 3, Core.KMEANS_RANDOM_CENTERS);

            // Convert back to 8-bit unsigned integer
            reshapedImage.convertTo(reshapedImage, CvType.CV_8U);

            // Reshape the result back to the original image size and return
            return reshapedImage.reshape(3, inputImage.rows());
        } catch (Exception e) {
            System.err.println("An error occurred in reduceColors method:");
            e.printStackTrace();
            return null;
        }
    }

    public static List<Rect> drawPinkContours(Mat inputImage) {
        List<Rect> boundingBoxes = new ArrayList<>();
        try {
            // Convert the image to HSV color space
            Mat hsvImage = new Mat();
            Imgproc.cvtColor(inputImage, hsvImage, Imgproc.COLOR_BGR2HSV);

            // Define the lower and upper bounds for pink hues
            Scalar lowerPink = new Scalar(160, 100, 100); // Lower HSV range for pink
            Scalar upperPink = new Scalar(180, 255, 255); // Upper HSV range for pink

            // Create a binary mask for pink regions
            Mat mask = new Mat();
            Core.inRange(hsvImage, lowerPink, upperPink, mask);

            // Find contours in the binary mask
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            // Filter contours based on area
            List<MatOfPoint> largeContours = new ArrayList<>();
            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);
                if (area > 1000) { // Adjust the minimum area threshold as needed
                    largeContours.add(contour);
                }
            }

            // Draw filled red contours on the original image and collect bounding boxes
            for (MatOfPoint contour : largeContours) {
                Rect boundingRect = Imgproc.boundingRect(contour);
                Imgproc.rectangle(inputImage, boundingRect.tl(), boundingRect.br(), new Scalar(0, 255, 0), 2);
                boundingBoxes.add(boundingRect);
            }
        } catch (Exception e) {
            System.err.println("An error occurred in drawPinkContours method:");
            e.printStackTrace();
        }
        return boundingBoxes;
    }

    public static Mat reduceNoise(Mat inputImage) {
        Mat outputImage = new Mat();
        try {
            // Apply Gaussian blur to reduce noise
            Imgproc.GaussianBlur(inputImage, outputImage, new org.opencv.core.Size(5, 5), 0);

            // Apply morphological operations for further noise reduction
            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new org.opencv.core.Size(3, 3));
            Imgproc.morphologyEx(outputImage, outputImage, Imgproc.MORPH_OPEN, kernel);
        } catch (Exception e) {
            System.err.println("An error occurred in reduceNoise method:");
            e.printStackTrace();
        }
        return outputImage;
    }


    public static List<Rect> mergeIntersectingRects(List<Rect> rects) {
        List<Rect> mergedRects = new ArrayList<>();

        // Iterate through the list of rects
        for (Rect rect : rects) {
            boolean merged = false;

            // Iterate through the existing merged rects to check for intersections
            for (Rect mergedRect : mergedRects) {
                // Check if the current rect intersects with the merged rect
                if (rectOverlap(rect, mergedRect)) {
                    // Merge the rects
                    mergedRect = mergeRects(rect, mergedRect);
                    merged = true;
                    break;
                }
            }

            // If the rect didn't intersect with any merged rects, add it as a new merged rect
            if (!merged) {
                mergedRects.add(rect);
            }
        }

        return mergedRects;
    }

    // Helper method to check if two rects overlap
    private static boolean rectOverlap(Rect rect1, Rect rect2) {
        return rect1.x < rect2.x + rect2.width &&
                rect1.x + rect1.width > rect2.x &&
                rect1.y < rect2.y + rect2.height &&
                rect1.y + rect1.height > rect2.y;
    }

    // Helper method to merge two rects into a larger one
    private static Rect mergeRects(Rect rect1, Rect rect2) {
        int x = Math.min(rect1.x, rect2.x);
        int y = Math.min(rect1.y, rect2.y);
        int width = Math.max(rect1.x + rect1.width, rect2.x + rect2.width) - x;
        int height = Math.max(rect1.y + rect1.height, rect2.y + rect2.height) - y;
        return new Rect(x, y, width, height);
    }

    public static List<Rect> drawYellowContours(Mat inputImage) {
        List<Rect> boundingBoxes = new ArrayList<>();
        try {
            // Convert the image to HSV color space
            Mat hsvImage = new Mat();
            Imgproc.cvtColor(inputImage, hsvImage, Imgproc.COLOR_BGR2HSV);

            // Define the lower and upper bounds for yellow hues based on specified colors
            Scalar lowerYellow = new Scalar(50, 50, 80);  // Adjusted lower HSV range for yellow
            Scalar upperYellow = new Scalar(65, 255, 255);

            // Create binary mask for yellow regions
            Mat mask = new Mat();
            Core.inRange(hsvImage, lowerYellow, upperYellow, mask);

            // Reduce noise in the mask
            Mat noiseReducedMask = reduceNoise(mask);

            // Find contours in the noise-reduced mask
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(noiseReducedMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            // Filter contours based on area
            List<MatOfPoint> largeContours = new ArrayList<>();
            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);
                if (area > 1000) { // Adjust the minimum area threshold as needed
                    largeContours.add(contour);
                }
            }

            // Draw filled yellow contours on the original image and collect bounding boxes
            for (MatOfPoint contour : largeContours) {
                Rect boundingRect = Imgproc.boundingRect(contour);
                Imgproc.rectangle(inputImage, boundingRect.tl(), boundingRect.br(), new Scalar(0, 255, 255), 2);
                boundingBoxes.add(boundingRect);
            }
        } catch (Exception e) {
            System.err.println("An error occurred in drawYellowContours method:");
            e.printStackTrace();
        }
        return boundingBoxes;
    }

    public static List<Rect> drawBlueContours(Mat inputImage) {
        List<Rect> boundingBoxes = new ArrayList<>();
        try {
            // Convert the image to HSV color space
            Mat hsvImage = new Mat();
            Imgproc.cvtColor(inputImage, hsvImage, Imgproc.COLOR_BGR2HSV);

            // Define the lower and upper bounds for blue hues based on specified colors
            Scalar lowerBlue = new Scalar(180, 50, 90);  // Adjusted lower HSV range for blue
            Scalar upperBlue = new Scalar(200, 255, 255);

            // Create binary mask for blue regions
            Mat mask = new Mat();
            Core.inRange(hsvImage, lowerBlue, upperBlue, mask);

            // Reduce noise in the mask
            Mat noiseReducedMask = reduceNoise(mask);

            // Find contours in the noise-reduced mask
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(noiseReducedMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            // Filter contours based on area
            List<MatOfPoint> largeContours = new ArrayList<>();
            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);
                if (area > 1000) { // Adjust the minimum area threshold as needed
                    largeContours.add(contour);
                }
            }

            // Draw filled blue contours on the original image and collect bounding boxes
            for (MatOfPoint contour : largeContours) {
                Rect boundingRect = Imgproc.boundingRect(contour);
                Imgproc.rectangle(inputImage, boundingRect.tl(), boundingRect.br(), new Scalar(255, 0, 0), 2);
                boundingBoxes.add(boundingRect);
            }
        } catch (Exception e) {
            System.err.println("An error occurred in drawBlueContours method:");
            e.printStackTrace();
        }
        return boundingBoxes;
    }

    public static Mat displayBlueChannel(Mat inputImage, int chanelNr) {
        // Split the channels
        List<Mat> channels = new ArrayList<>();
        Core.split(inputImage, channels);

        // Get the blue channel (channels.get(0) is the blue channel in BGR)
        Mat blueChannel = channels.get(chanelNr);

        // Create an empty matrix with the same size and type as the input image
        Mat resultImage = Mat.zeros(inputImage.size(), inputImage.type());

        // Check if the image has an alpha channel
        if (channels.size() == 4) {
            // Image has an alpha channel
            // Create the list of channels for the new image (blue channel, zero, zero, alpha)
            List<Mat> newChannels = Arrays.asList(
                    blueChannel,
                    Mat.zeros(blueChannel.size(), CvType.CV_8UC1),
                    Mat.zeros(blueChannel.size(), CvType.CV_8UC1),
                    channels.get(3) // Alpha channel
            );
            // Merge the channels back into a single image
            Core.merge(newChannels, resultImage);
        } else {
            // Image does not have an alpha channel
            // Create the list of channels for the new image (blue channel, zero, zero)
            List<Mat> newChannels = Arrays.asList(
                    blueChannel,
                    Mat.zeros(blueChannel.size(), CvType.CV_8UC1),
                    Mat.zeros(blueChannel.size(), CvType.CV_8UC1)
            );
            // Merge the channels back into a single image
            Core.merge(newChannels, resultImage);
        }

        return resultImage;
    }

    public static Mat makeObjectWhiteAndBackgroundBlack(Mat inputImage, int thresholdValue) {

        // Apply binary thresholding
        Mat binaryImage = new Mat();
        Imgproc.threshold(inputImage, binaryImage, thresholdValue, 255, Imgproc.THRESH_BINARY);

        return binaryImage;
    }

    public static List<Rect> findObjectBoundingBoxes(Mat inputImage, double minContourArea) {
        List<Rect> boundingBoxes = new ArrayList<>();

        // Find contours
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(inputImage, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Compute bounding boxes for each contour
        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            if (area > minContourArea) {
                Rect boundingRect = Imgproc.boundingRect(contour);
                boundingBoxes.add(boundingRect);
            }
        }

        return boundingBoxes;
    }

    public static List<String> identifyObjects(List<Rect> boundingBoxes, Mat inputImage) {
        List<String> results = new ArrayList<>();

        // Iterate through each bounding box
        for (int i = 0; i < boundingBoxes.size(); i++) {
            Rect boundingBox = boundingBoxes.get(i);

            // Extract region of interest (ROI) from the input image
            Mat roi = new Mat(inputImage, boundingBox);

            // Determine the color of the figure inside the bounding box
            String color = detectColor(roi);

            // Display the color information and unique ID
            String result = color + " " + (i + 1) + " " +(boundingBox.x + (boundingBox.width / 2)) + " " + (boundingBox.y + (boundingBox.height / 2));
            results.add(result);

            // Draw the color label and center circle
            drawLabelAndCircle(inputImage, boundingBox, color, (i + 1));
        }

        return results;
    }

    private static String detectColor(Mat roi) {
        // Calculate the center pixel coordinates
        int centerX = roi.cols() / 2;
        int centerY = roi.rows() / 2;

        // Get the BGR values of the center pixel
        double[] centerPixel = roi.get(centerY, centerX);

        // Extract the B, G, and R values
        double blue = centerPixel[0];
        double green = centerPixel[1];
        double red = centerPixel[2];

        // Check if it's blue or yellow based on the criteria
        if (blue >= 220 && red < 200) {
            return "yellow";
        } else if (blue < 200 && red >= 220) {
            return "blue";
        } else {
            // For other cases, return "unknown" or handle them according to your requirements
            return "yellow";
        }
    }




    private static void drawLabelAndCircle(Mat inputImage, Rect boundingBox, String color, int id) {
        // Draw the color label within the bounding box
        Point labelPosition = new Point(boundingBox.x, boundingBox.y - 10);
        Imgproc.putText(inputImage, "color: " + color + ", id: " + id, labelPosition, 3, 0.5, new Scalar(0, 0, 0), 2);

        // Draw a circle in the center of the bounding box with the corresponding color
        Point center = new Point(boundingBox.x + boundingBox.width / 2, boundingBox.y + boundingBox.height / 2);
        Scalar circleColor = color.equals("blue") ? new Scalar(255, 0, 0) : new Scalar(0, 0, 255); // Blue or Yellow
        Imgproc.circle(inputImage, center, 8, circleColor, -1);
    }
}


