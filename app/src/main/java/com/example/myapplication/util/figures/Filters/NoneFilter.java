package com.example.myapplication.util.figures.Filters;

import com.example.myapplication.util.figures.Interfaces.Filter;
import org.opencv.core.Mat;


public class NoneFilter  implements Filter {
    @Override
    public void apply(final Mat src, final Mat dst) {
        // Do nothing.
    }
}
