package com.example.myapplication.ui.notifications;

import static com.example.myapplication.util.MatUtil.drawPinkContours;
import static com.example.myapplication.util.MatUtil.drawYellowContours;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.databinding.FragmentNotificationsBinding;
import com.example.myapplication.game.Action;
import com.example.myapplication.game.City;
import com.example.myapplication.util.MatUtil;
import com.example.myapplication.util.algorithms.CornerFind;
import com.example.myapplication.util.algorithms.MotionTracking;
import com.example.myapplication.util.algorithms.PaperDetection;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private CameraBridgeViewBase cameraView;
    private boolean startSimulation = false;
    List<City> cities = new ArrayList<>();
    List<Action> actions = new ArrayList<>();

    public enum MyEnum {
        MotionTracking, SegmentPaper, CannyEdge, Threshhold, RenderBlock, RenderGrid, PerspectiveWarp, DetectYellow, ObjectDetect, VALUE3
    }
    private ArrayAdapter<MyEnum> adapter;
    private MyEnum selectedValue;
    private boolean firstFrame = true;
    private int cameraViewWidth;
    private int cameraViewHeight;
    private Button action;

    private int turn = 0;



    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        NotificationsViewModel notificationsViewModel =
                new ViewModelProvider(this).get(NotificationsViewModel.class);

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textNotifications;
        notificationsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        Log.d("HomeActivity", "Home created");
        InitNotificationPage();
        cameraView.enableView();
        return root;
    }

    private void InitNotificationPage() {
        cameraView = binding.cameraView;
        action = binding.action;
        action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set the boolean variable to true when the button is clicked
                startSimulation = true;
            }
        });

        Spinner spinner = binding.spinnerNotifications;
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, MyEnum.values());
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Retrieve the selected item from the spinner
                selectedValue = (MyEnum) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle case when nothing is selected
            }
        });

        cameraView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                cameraView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                cameraViewWidth = cameraView.getWidth();
                cameraViewHeight = cameraView.getHeight();
                Log.d("CameraGlobal", "width: "+cameraViewWidth+" height:"+cameraViewHeight);
            }
        });

        cameraView.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {
                Log.d("CameraStart", "width: "+width+" height:"+height);
            }

            @Override
            public void onCameraViewStopped() {}

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                Mat result = new Mat();
                switch(selectedValue){
                    case MotionTracking:
                        result = MotionTracking.getMotionTracking(inputFrame, new Scalar(255,0,0), 4,  true);
                        break;
                    case SegmentPaper:
                        result = PaperDetection.findPaperContours(inputFrame, 150000);
                        break;
                    case CannyEdge:
                        Rect boundingRect = PaperDetection.getBoundingBox(inputFrame, 150000);
                        result = applyCannyEdgeDetection(inputFrame);
                        result = CornerFind.findAndDrawCorners(boundingRect, result);
                        Imgproc.rectangle(result, boundingRect.tl(), boundingRect.br(), new Scalar(255,0,0), 4);
                        break;
                    case Threshhold:
                        Imgproc.threshold(inputFrame.gray(), result, 100, 255, Imgproc.THRESH_BINARY);
                        break;
                    case RenderBlock:
                        result= renderCube(inputFrame);
                        break;
                    case RenderGrid:
                        Point[] points = getPoints(inputFrame);
                        result = inputFrame.rgba();
                        for (Point point : points) {
                            Imgproc.circle(result, point, 10, new Scalar(0, 255, 0), 4);
                        }
//                        MatUtil.drawPerspectiveGrid(result, points);
                        drawPaperEdges(result, points);

                        break;
                    case PerspectiveWarp:
                        points = getPoints(inputFrame);
                        if(points.length>=4){
                            result = MatUtil.warpImage(inputFrame.rgba(), points[1], points[0],points[3],points[2]);
                            Mat startingImage = result.clone();
                            // print out the chareteristics of result to this point before proceeding furether.
                            result = MatUtil.increaseContrast(result, 100);
                            result = MatUtil.reduceNoise(result);


                            result = MatUtil.displayBlueChannel(result, 1);
                            Imgproc.cvtColor(result, result, Imgproc.COLOR_BGR2GRAY);
                            result = MatUtil.makeObjectWhiteAndBackgroundBlack(result, 22);
                            List<Rect> redRect = MatUtil.findObjectBoundingBoxes(result, 100);
                            for(Rect rect : redRect){
                                Imgproc.rectangle(startingImage, rect.tl(), rect.br(), new Scalar(0, 255, 0), 2);
                            }
                            result = startingImage;
                            List<String> playerStates = MatUtil.identifyObjects(redRect, result);
                            for(String s : playerStates){
//                                System.out.println(s);
                            }

                            if(startSimulation){
                                System.out.println("startSimulation "+startSimulation);
                                createCities(playerStates);
                                startSimulation = false;
                            }
                            if (!cities.isEmpty()){
                                System.out.println("number of cities "+cities.size());
                                updateLocations(playerStates);
                                System.out.println("number of actions, pre "+actions.size());
                                List<Action> actionsToRemove = new ArrayList<>();
                                for(Action action : actions){
                                    action.proceed();
                                    City attacker = getCity(action.from);
                                    City defender = getCity(action.to);
                                    Random random = new Random();
                                    if(action.progress >= 300 + random.nextInt(100)){
                                        if(attacker.color.equals(defender.color)){
                                            defender.population += action.army;
                                        } else {
                                            defender.population -= action.army;
                                            if(defender.population <= 0){
                                                attacker.population++;
                                            }
                                        }
                                        actionsToRemove.add(action);
                                    }
                                    Point startPoint = new Point(attacker.x, attacker.y);
                                    Point endPoint = new Point(defender.x, defender.y);
                                    Scalar color = (attacker.color.equals(defender.color))? new Scalar(255, 0, 0) : new Scalar(0, 255, 255);
                                    int thickness = 8; // Line thickness
                                    Imgproc.line(result, startPoint, endPoint, color, thickness);

                                    Point textPoint = new Point((startPoint.x + endPoint.x)/2, (startPoint.y + endPoint.y)/2);
                                    Imgproc.putText(result, ""+action.army, textPoint, 3, 2, new Scalar(255, 255, 255), 2);


                                }
                                for(Action a: actionsToRemove){
                                    actions.remove(a);
                                }
                                // add new actions
                                for(City city : cities){
                                    String actionString = city.move();
                                    System.out.println("City "+city.id+"chose:"+actionString);
                                    if(!actionString.equals("rest")){
                                        Action newAction = new Action(actionString);
                                        System.out.println("action new action added");
                                        actions.add(newAction);
                                    }
                                    Imgproc.putText(result, ""+city.population, new Point(city.x, city.y), 3, 4, new Scalar(0, 0, 255), 4);
                                }
                                System.out.println("number of actions, post "+actions.size());
                            }
                        } else
                            result = inputFrame.rgba();
                        break;
                    case DetectYellow:
                        points = getPoints(inputFrame);
                        if(points.length>=4){
                            result = MatUtil.warpImage(inputFrame.rgba(), points[1], points[0],points[3],points[2]);
                            Mat startingImage = result.clone();
                            // print out the chareteristics of result to this point before proceeding furether.
                            result = MatUtil.increaseContrast(result, 100);
                            result = MatUtil.reduceNoise(result);

                            result = MatUtil.displayBlueChannel(result, 0);
                            List<Rect> redRect = drawYellowContours(result);
                            List<Rect> finalRedRects = MatUtil.mergeIntersectingRects(redRect);
                            for(Rect rect : finalRedRects){
                                Imgproc.rectangle(result, rect.tl(), rect.br(), new Scalar(0, 255, 0), 2);
                            }
//                            result = startingImage;
                        } else
                            result = inputFrame.rgba();
                        break;
                    case ObjectDetect:
                        points = getPoints(inputFrame);
                        if(points.length>=4){
                            result = MatUtil.findAndDrawBoundingBoxes(inputFrame.rgba());
                        } else
                            result = inputFrame.rgba();
                        break;
                    default:
                        result = inputFrame.rgba();
                        break;
                }
                result = MatUtil.rotateMat90CounterClockwise(result);
                return result;
            }
        });
    }

    private void updateLocations(List<String> playerStates) {
        for(String s : playerStates){
            String[] parts = s.split("\\s+");
//            System.out.println(s+ "-"+parts[0]+" "+parts[1]+" "+parts[2]+" "+parts[3]+" ");

            String color = parts[0];
            int id = Integer.parseInt(parts[1]);
            int x = Integer.parseInt(parts[2]);
            int y = Integer.parseInt(parts[3]);
            City currentCity = getCity(id);
            if(currentCity != null){
                currentCity.x = x;
                currentCity.y = y;
                currentCity.enemies = getEnemyIds(currentCity);
                currentCity.friends = getFriendIds(currentCity);
            }
        }
    }

    public List<Integer> getFriendIds(City city){
        List<Integer> ids = new ArrayList<>();
        for(City c: cities){
            if(city.color.equals(c.color) && city.id != c.id){
                ids.add(new Integer(c.id));
            }
        }
        return ids;
    }
    public List<Integer> getEnemyIds(City city){
        List<Integer> ids = new ArrayList<>();
        for(City c: cities){
            if(!city.color.equals(c.color) && city.id != c.id && c.population > 0){
                ids.add(new Integer(c.id));
            }
        }
        return ids;
    }

    private City getCity(int id) {
        for(City city : cities){
            if(city.id == id)
                return city;
        }
        return null;
    }

    private void createCities(List<String> playerStates) {
        cities = new ArrayList<>();
        for(String s : playerStates){
            String[] parts = s.split("\\s+");

            String color = parts[0];
            int id = Integer.parseInt(parts[1]);
            int x = Integer.parseInt(parts[2]);
            int y = Integer.parseInt(parts[3]);
//            System.out.println(s);

            City city = new City(color, id,x,y);
            cities.add(city);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private Point[] getPoints(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        Rect rect = PaperDetection.getBoundingBox(inputFrame, 150000);
        Mat temp = applyCannyEdgeDetection(inputFrame);
        Point[] points = CornerFind.getCorners(rect, temp);
        return points;
    }

    private Mat applyCannyEdgeDetection(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat gray = inputFrame.gray();
        Mat edges = new Mat();

        Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 1.5, 1.5);
        Imgproc.Canny(gray, edges, 100, 200);
        return edges;
    }

    private Mat renderCube(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgbaMat = inputFrame.rgba();

        int cubeSize = 100;
        int centerX = rgbaMat.cols() / 2;
        int centerY = rgbaMat.rows() / 2;

        Point[] frontFace = new Point[]{
                new Point(centerX - cubeSize, centerY - cubeSize),
                new Point(centerX + cubeSize, centerY - cubeSize),
                new Point(centerX + cubeSize, centerY + cubeSize),
                new Point(centerX - cubeSize, centerY + cubeSize)
        };
        Point[] backFace = new Point[]{
                new Point(centerX - cubeSize + 50, centerY - cubeSize - 50),
                new Point(centerX + cubeSize + 50, centerY - cubeSize - 50),
                new Point(centerX + cubeSize + 50, centerY + cubeSize - 50),
                new Point(centerX - cubeSize + 50, centerY + cubeSize - 50)
        };

        Scalar[] colors = new Scalar[]{
                new Scalar(255, 0, 0),   // Red
                new Scalar(0, 255, 0),   // Green
                new Scalar(0, 0, 255),   // Blue
                new Scalar(255, 255, 0)  // Yellow
        };

        // Draw front face
        for (int i = 0; i < 4; i++) {
            Imgproc.line(rgbaMat, frontFace[i], frontFace[(i + 1) % 4], colors[i], 2);
        }

        // Draw back face
        for (int i = 0; i < 4; i++) {
            Imgproc.line(rgbaMat, backFace[i], backFace[(i + 1) % 4], colors[(i + 1) % 4], 2);
        }

        // Draw connecting lines between front and back faces
        for (int i = 0; i < 4; i++) {
            Imgproc.line(rgbaMat, frontFace[i], backFace[i], new Scalar(0, 255, 0), 2);
        }

        return rgbaMat;
    }

    private void drawPaperEdges(Mat image, Point[] planePoints){
        if(planePoints.length<4){
            return;
        }
        Imgproc.line(image, planePoints[0], planePoints[1], new Scalar(0, 0, 255), 2);
        Imgproc.line(image, planePoints[1], planePoints[2], new Scalar(0, 0, 255), 2);
        Imgproc.line(image, planePoints[2], planePoints[3], new Scalar(0, 0, 255), 2);
        Imgproc.line(image, planePoints[3], planePoints[0], new Scalar(0, 0, 255), 2);
    }
}