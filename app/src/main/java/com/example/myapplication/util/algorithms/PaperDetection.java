package com.example.myapplication.util.algorithms;

import com.example.myapplication.util.MatUtil;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PaperDetection {

    public static Rect getBoundingBox(CameraBridgeViewBase.CvCameraViewFrame inputFrame, double minAreaThreshold){
        Mat rgbaFrame = inputFrame.rgba();
        Mat grayFrame = inputFrame.gray();
        List<MatOfPoint> contours = getPossiblePaperContours(grayFrame, 100);
        MatOfPoint contour = getLargestContour(contours);
        return Imgproc.boundingRect(contour);
    }
    public static Mat findPaperContours(CameraBridgeViewBase.CvCameraViewFrame inputFrame, double minAreaThreshold){
        Mat rgbaFrame = inputFrame.rgba();
        Mat grayFrame = inputFrame.gray();

        List<MatOfPoint> contours = getPossiblePaperContours(grayFrame, 100);
        MatOfPoint contour = getLargestContour(contours);

        if (contour != null && Imgproc.contourArea(contour) > minAreaThreshold) {
            // Draw polygon around contour with color #caf9fa
            //Imgproc.drawContours(rgbaFrame, contours, i, new Scalar(202, 249, 250), -1); // -1 fills the contour
            processContourArea(rgbaFrame, contour);

            Rect boundingRect = Imgproc.boundingRect(contour);
            Imgproc.rectangle(rgbaFrame, boundingRect.tl(), boundingRect.br(), new Scalar(202, 203, 250), 2);
        }
        return rgbaFrame;
    }

    private static MatOfPoint getLargestContour(List<MatOfPoint> contours){
        MatOfPoint largestContour = null;
        double maxArea = 0;
        for (MatOfPoint contour : contours) {
            double contourArea = Imgproc.contourArea(contour);
            if (contourArea > maxArea) {
                maxArea = contourArea;
                largestContour = contour;
            }
        }
        return largestContour;
    }

    private static List<MatOfPoint> getPossiblePaperContours(Mat grayFrame, double thresholdValue) {
        Mat thresholdFrame = new Mat();
        Imgproc.threshold(grayFrame, thresholdFrame, thresholdValue, 255, Imgproc.THRESH_BINARY);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(thresholdFrame, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        return contours;
    }

    private static void processContourArea(Mat rgbaFrame, MatOfPoint contour) {
        // Create a mask for the contour area
        Mat mask = Mat.zeros(rgbaFrame.size(), CvType.CV_8UC1);
        Imgproc.drawContours(mask, Collections.singletonList(contour), 0, new Scalar(255), -1);

        // Convert the mask to a binary mask
        Mat binaryMask = new Mat();
        Core.compare(mask, new Scalar(0), binaryMask, Core.CMP_NE);

        // Copy the contour area from the original image
        Mat contourArea = new Mat();
        rgbaFrame.copyTo(contourArea, binaryMask);

        // Convert the contour area to grayscale
        Imgproc.cvtColor(contourArea, contourArea, Imgproc.COLOR_BGR2GRAY);

        // Apply your processing logic to the contour area here
        // For example, let's just blur the contour area
        Imgproc.GaussianBlur(contourArea, contourArea, new Size(15, 15), 0);

        // Copy the processed contour area back to the original image
        contourArea.copyTo(rgbaFrame, binaryMask);
    }
}
