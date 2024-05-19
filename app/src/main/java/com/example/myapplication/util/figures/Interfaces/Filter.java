package com.example.myapplication.util.figures.Interfaces;

import org.opencv.core.Mat;

public interface Filter {
    public abstract void apply(final Mat src, final Mat dst);
}
