package com.example.myapplication.util.algorithms;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class CornerFind {
    public static Point[] getCorners(Rect boundingRect, Mat edges){
        Mat result = edges.clone(); // Create a copy of the input edges matrix to avoid modifying the original
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(result, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.cvtColor(result, result, Imgproc.COLOR_GRAY2RGBA);
        double maxArea = 0;
        MatOfPoint largestContour = new MatOfPoint();
        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            if (area > maxArea) {
                maxArea = area;
                largestContour = contour;
            }
        }
        MatOfPoint2f contour2f = new MatOfPoint2f(largestContour.toArray());
        double epsilon = 0.02 * Imgproc.arcLength(contour2f, true);
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        Imgproc.approxPolyDP(contour2f, approxCurve, epsilon, true);
        Point[] points = approxCurve.toArray();
        return points;
    }
    public static Mat findAndDrawCorners(Rect boundingRect, Mat edges) {
        Mat result = edges.clone(); // Create a copy of the input edges matrix to avoid modifying the original

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(result, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.cvtColor(result, result, Imgproc.COLOR_GRAY2RGBA);
        // Find the largest contour
        double maxArea = 0;
        MatOfPoint largestContour = new MatOfPoint();
        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            if (area > maxArea) {
                maxArea = area;
                largestContour = contour;
            }
        }

        // If no contours are found, return the original edges matrix
        if (largestContour.empty()) {
            Log.e("CornerFind", "No contours found.");
            return result;
        }

        // Approximate the contour to a polygon
        MatOfPoint2f contour2f = new MatOfPoint2f(largestContour.toArray());
        double epsilon = 0.02 * Imgproc.arcLength(contour2f, true);
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        Imgproc.approxPolyDP(contour2f, approxCurve, epsilon, true);

        // Draw the corners
        Log.d("PointLog", " DrawPoint");
        Point[] points = approxCurve.toArray();
        for (Point point : points) {
            // Adjust point location to the original frame

//            Point adjustedPoint = new Point(point.x + boundingRect.x, point.y + boundingRect.y);
            Point adjustedPoint = new Point(point.x, point.y);
            Log.d("Point", "(x: "+adjustedPoint.x+", y: "+adjustedPoint.y+");");
            Imgproc.circle(result, adjustedPoint, 10, new Scalar(0, 255, 0), 4);
        }

        return result;
    }
}
