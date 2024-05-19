package com.example.myapplication.util.figures.Filters;

import com.example.myapplication.util.figures.Interfaces.ARFilter;

public class NoneARFilter extends NoneFilter implements ARFilter {
        @Override
        public float[] getGLPose() {
            return null;
        }
}
