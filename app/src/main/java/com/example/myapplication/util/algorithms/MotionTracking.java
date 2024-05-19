package com.example.myapplication.util.algorithms;

import com.example.myapplication.util.MatUtil;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class MotionTracking {
    private static Mat prev_gray;
    private static boolean firstFrame = true;

    public static Mat getMotionTracking(CameraBridgeViewBase.CvCameraViewFrame inputFrame,
                                        Scalar scalar,
                                        int thickness,
                                        boolean aSimple){
        if(firstFrame){
            prev_gray = inputFrame.gray();
            firstFrame = false;
            return MatUtil.rotateMat90CounterClockwise(inputFrame.rgba()); // Clone the rgb matrix
        }

        Mat diff = new Mat();
        Mat current_gray = inputFrame.gray();
        Core.absdiff(current_gray, prev_gray, diff);
        Imgproc.threshold(diff, diff, 40, 255, Imgproc.THRESH_BINARY);

        List<MatOfPoint> contours = new ArrayList<>();
        if(aSimple)
            Imgproc.findContours(diff, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        else
            Imgproc.findContours(diff, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);

        Mat rgb = inputFrame.rgba();
        Imgproc.drawContours(rgb, contours, -1,scalar, thickness);

        prev_gray = current_gray.clone();
        return rgb;
    }
}
