/*
 * Renders 3D points from Arduino Data through Serial Port
 */

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Point3D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

/**
 * @author travis
 */
public class MainLogic extends Application {

    private static double WIDTH = 800; //default sizes
    private static double HEIGHT = 600;

    FPSCamera fpsCam;
    Stage exitBoxStage;
    Group root2D;
    Group subRoot3D;

    Box xBox; //the xAxis line 
    Box yBox; //the yAxis line 
    Box zBox; //the zAxis line 
    Cylinder laser = new Cylinder(0, 0); //laser for cooler rendering look

    //setup drop down menu for list of ports
    ObservableList<String> serialItems;
    SerialPort[] availableSerialPorts;
    SerialPort selectedPort;
    ComboBox serialComboBox;

    // will use to concat incomin data later
    String mergedText = "";

    boolean xAxisShowing;
    boolean yAxisShowing;
    boolean zAxisShowing;

    FileChooser fileChooser;
    boolean canBrowse = true; // allows user to select file from start

    ArrayList<Point> allPoints = new ArrayList<>(); //arraylist of points
    ArrayList<Point> newPoints = new ArrayList<>(); //temp array to add new points

    //Do not run this main, use the one in the App class to start the app
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        //3D scene setup
        fpsCam = new FPSCamera();
        subRoot3D = new Group(fpsCam, laser);

        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds(); //use to get the users resolution to match the window size

        //set Stage boundaries to visible bounds of the main screen
        WIDTH = primaryScreenBounds.getWidth();
        HEIGHT = primaryScreenBounds.getHeight();

        primaryStage.setX(primaryScreenBounds.getMinX());
        primaryStage.setY(primaryScreenBounds.getMinY());
        primaryStage.setWidth(WIDTH);
        primaryStage.setHeight(HEIGHT);

        SubScene subScene = new SubScene(subRoot3D, WIDTH, HEIGHT * 0.9, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.BLACK);
        subScene.setCamera(fpsCam.getCamera());

        // 2D interface
        BorderPane pane = new BorderPane();
        pane.setCenter(subScene);
        Button clearButton = new Button("Clear");
        clearButton.setOnAction((ActionEvent e) -> { //Clear all points
            ClearPoints();
            System.out.println("Clear");

        });

        Button browseButton = new Button("Open");
        browseButton.setOnAction((ActionEvent e) -> { //button lets user browse for file
            selectFile(e);

        });
        CheckBox xAxisCheck = new CheckBox("X-Axis"); //checkbox for toggling axis line
        xAxisCheck.setTextFill(Color.RED);
        xAxisCheck.setSelected(true);
        xAxisCheck.setOnAction(e -> {
            toggleAxis(xAxisCheck.isSelected(), "x");
        });

        CheckBox yAxisCheck = new CheckBox("Y-Axis"); //checkbox for toggling axis line
        yAxisCheck.setTextFill(Color.GREEN);
        yAxisCheck.setSelected(true);
        yAxisCheck.setOnAction(e -> {
            toggleAxis(yAxisCheck.isSelected(), "y");
        });

        CheckBox zAxisCheck = new CheckBox("Z-Axis"); //checkbox for toggling axis line
        zAxisCheck.setTextFill(Color.BLUE);
        zAxisCheck.setSelected(true);
        zAxisCheck.setOnAction(e -> {
            toggleAxis(zAxisCheck.isSelected(), "z");
        });

        serialItems = FXCollections.observableArrayList(); //items for dropdown of serial ports
        serialComboBox = new ComboBox(serialItems); //drop down menu for serial ports
        serialComboBox.setPromptText("Select Serial Port");

        Button scanButton = new Button("Scan");
        scanButton.setOnAction((ActionEvent e) -> { //bScan real time points
            Scan(serialComboBox.getSelectionModel().getSelectedIndex());
        });

        Button saveScanButton = new Button("Save");
        saveScanButton.setOnAction((ActionEvent e) -> {

            savePointsToText(primaryStage);

        });

        VBox vbox = new VBox();
        HBox hbox1 = new HBox(xAxisCheck, yAxisCheck, zAxisCheck);
        HBox hbox2 = new HBox(serialComboBox, scanButton);
        HBox hbox3 = new HBox(clearButton, browseButton, saveScanButton);

        vbox.getChildren().add(hbox1);
        vbox.getChildren().add(hbox2);
        vbox.getChildren().add(hbox3);

        pane.setBottom(vbox);
        //  pane.setPrefSize(300,300);

        Scene scene = new Scene(pane);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Lidar Render Bot");
        primaryStage.show();

        fpsCam.loadControlsForSubScene(subScene); //load controls for camera from other class

        //  DrawAxisLines();
        //moving view to a better starting position. (cant use transform because not using coords for camera)
        for (int i = 0; i < 15; i++) {
            fpsCam.moveBack();
        }
        for (int i = 0; i < 1; i++) {
            fpsCam.moveUp();
        }
        for (int i = 0; i < 1; i++) {
            fpsCam.strafeRight();
        }

        loadSerialPorts();
        scene.setOnKeyPressed(e -> { //excape key will close the program
            if (e.getCode() == KeyCode.ESCAPE) {
            }
        });
        toggleAxis(true, "x");
        toggleAxis(true, "y");
        toggleAxis(true, "z");

    } //end of start method

    private void selectFile(ActionEvent event) {

        if (canBrowse) {
            canBrowse = false;
            fileChooser = new FileChooser();
            fileChooser.setTitle("Select File");
            FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("SELECT A FILE (TXT)", "*.txt"); //extensions allowed
            fileChooser.getExtensionFilters().add(filter);
            File selectedFile = fileChooser.showOpenDialog(null); //selected file chosen and clicked ok
            ReadFile(selectedFile);
            canBrowse = true;

        } else {
            canBrowse = true;
        }

    }

    private void ReadFile(File file) {

        //Creating Scanner instnace to read File in Java
        Scanner scnr;
        try {
            scnr = new Scanner(file);

            //Reading each line of file using Scanner class
            int lineNumber = 1;
            while (scnr.hasNextLine()) {
                String line = scnr.nextLine();

                double x;
                double y;
                double z;

                if (line.trim().length() == 0) {
                    //ignore empty lines
                } else {

                    String[] values = line.split(",");

                    x = Double.parseDouble(values[0]);
                    y = Double.parseDouble(values[1]);
                    z = Double.parseDouble(values[2]);

                    //   AddPoint(x, y, z);
                    Point newPoint = new Point(x, y, z);
                    newPoints.add(newPoint);

                    lineNumber++;
                }
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(MainLogic.class.getName()).log(Level.SEVERE, null, ex);
        }

        RenderPoints();

    }

    public void toggleAxis(boolean checked, String axis) { //hide or show the axis lines

        if (checked && axis.equals("x")) {
            xBox = new Box(0.1, 0.1, 5000);
            xBox.setMaterial(new PhongMaterial(Color.RED));
            subRoot3D.getChildren().add(xBox);
            xAxisShowing = true;
        } else if (!checked && axis.equals("x")) {
            subRoot3D.getChildren().remove(xBox);
            xAxisShowing = false;
        } else if (checked && axis.equals("y")) {
            yBox = new Box(5000, 0.1, 0.1);
            yBox.setMaterial(new PhongMaterial(Color.GREEN));
            subRoot3D.getChildren().add(yBox);
            yAxisShowing = true;
        } else if (!checked && axis.equals("y")) {
            subRoot3D.getChildren().remove(yBox);
            yAxisShowing = false;

        } else if (checked && axis.equals("z")) {
            zBox = new Box(0.1, 5000, 0.1);
            zBox.setMaterial(new PhongMaterial(Color.BLUE));
            subRoot3D.getChildren().add(zBox);
            zAxisShowing = true;
        } else if (!checked && axis.equals("z")) {
            subRoot3D.getChildren().remove(zBox);
            zAxisShowing = false;

        }

    }

    public void RenderAPoint(Point pointToRender) //zRender 1 Point, used with real time mapping
    {

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                subRoot3D.getChildren().remove(laser); //remove laser 
                pointToRender.setMaterial(new PhongMaterial(Color.WHITE));
                subRoot3D.getChildren().add(pointToRender);
                allPoints.add(pointToRender);
                //add rendering laser
                Point3D vectorOfPoint = new Point3D(pointToRender.getX(), -pointToRender.getY(), -pointToRender.getZ());
                laser = drawLaser(Point3D.ZERO, vectorOfPoint);
                subRoot3D.getChildren().add(laser);
            }
        });

        pointToRender.setTranslateX(pointToRender.getX());
        pointToRender.setTranslateY(-pointToRender.getY()); //make negative because computer screen point system is different
        pointToRender.setTranslateZ(-pointToRender.getZ());
        //show point values of point mouse enters
        pointToRender.setOnMouseEntered((event) -> {
            DecimalFormat df = new DecimalFormat("####.##");
            Tooltip.install(
                    pointToRender,
                    new Tooltip("" + df.format(pointToRender.getZ()) + ", " + df.format(pointToRender.getX()) + ", " + df.format(pointToRender.getY()))
            );

        });

    }

    public void RenderPoints() { //when rending a file, can render all at once with this 

        for (int i = 0; i < newPoints.size(); i++) {
            Point pointToRender = newPoints.get(i);
            subRoot3D.getChildren().add(pointToRender);
            allPoints.add(newPoints.get(i));

            pointToRender.setTranslateX(pointToRender.getX());
            pointToRender.setTranslateY(-pointToRender.getY());
            pointToRender.setTranslateZ(-pointToRender.getZ());
            //show point values of point mouse enters
            pointToRender.setOnMouseEntered((event) -> {

                DecimalFormat df = new DecimalFormat("####.##");

                Tooltip.install(
                        pointToRender,
                        new Tooltip("" + df.format(pointToRender.getZ()) + ", " + df.format(pointToRender.getX()) + ", " + df.format(pointToRender.getY()))
                );

            });

        }

        newPoints.clear(); //clear the temp points 

    }

    public Point3D ConvertToPoint(double panAngle, double tiltAngle, double distance) //method to convert distance and angle data and convert it to a x, y, z point 
    {

        double panAngleToRadian = Math.toRadians(panAngle); //need to convert to radians 
        double tiltAngleToRadian = Math.toRadians(tiltAngle - 90);
        Point3D resultPoint = new Point3D(distance * sin(tiltAngleToRadian) * sin(panAngleToRadian), distance * cos(tiltAngleToRadian), distance * sin(tiltAngleToRadian) * cos(panAngleToRadian));
        return resultPoint;

    }

    public void loadSerialPorts() { //run at startup and also have a button to refresh the ports with this method too
        availableSerialPorts = SerialPort.getCommPorts(); //Get array of all ports on system. 
        for (SerialPort availableSerialPort : availableSerialPorts) {
            //  System.out.println(availableSerialPorts[i].getSystemPortName());
            serialItems.add(availableSerialPort.getSystemPortName());
        }
    }

    public void Scan(int index) {

        if (index == -1) {
            // user did not select port
        } else {

            selectedPort = availableSerialPorts[index];
            selectedPort.openPort();
            selectedPort.setBaudRate(115200);

            selectedPort.addDataListener(new SerialPortDataListener() {
                @Override
                public int getListeningEvents() {
                    return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
                }

                @Override
                public void serialEvent(SerialPortEvent event) {

                    if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                        return;
                    }

                    byte[] newData = new byte[selectedPort.bytesAvailable()];
                    int byteSize = selectedPort.readBytes(newData, newData.length);
                    String text = new String(newData);

                    if (text.contains(".*[a-z].*")) {
                        text = "";
                    }

                    //will print only once we get entire line of text 
                    if (mergedText.length() < 13) {
                        mergedText = mergedText + text;

                        if (mergedText.length() >= 13) {
                            System.out.print(mergedText);
                            String[] values = mergedText.split(",");

                            double panAngle = Integer.parseInt(values[0].replaceAll("[^0-9]", "").trim());
                            double titltAngle = Integer.parseInt(values[1].replaceAll("[^0-9]", "").trim());
                            double distance = Integer.parseInt(values[2].replaceAll("[^0-9]", "").trim());

                            Point3D vector = ConvertToPoint(panAngle, titltAngle, distance);
                            Point newPoint = new Point(vector.getX(), vector.getY(), vector.getZ());
                            newPoints.add(newPoint);

                            // RenderPoints();
                            RenderAPoint(newPoint);

                            String signalString = "1";
                            byte[] signalByte = signalString.getBytes();
                            selectedPort.writeBytes(signalByte, signalByte.length);
                            mergedText = "";
                        }

                    } else {
                        System.out.print(mergedText);
                        String[] values = mergedText.split(",");

                        int angle1 = Integer.parseInt(values[0].replaceAll("[^0-9]", ""));
                        int angle2 = Integer.parseInt(values[1].replaceAll("[^0-9]", ""));
                        int distance = Integer.parseInt(values[2].replaceAll("[^0-9]", ""));

                        Point3D vector = ConvertToPoint(angle1, angle2, distance);
                        Point newPoint = new Point(vector.getX(), vector.getY(), vector.getZ());
                        newPoints.add(newPoint);
                        RenderAPoint(newPoint);

                        String signalString = "0";
                        byte[] signalByte = signalString.getBytes();
                        selectedPort.writeBytes(signalByte, signalByte.length);
                        mergedText = "";
                        //  mergedByteSize = 0;
                    }
                }
            });
        }
    }

    public void ClearPoints() {
        newPoints.clear();
        subRoot3D.getChildren().removeAll(allPoints);
        allPoints.clear();

    }

    public Cylinder drawLaser(Point3D origin, Point3D target) { //draws a laser for visual rendering points
        Point3D yAxis = new Point3D(0, 1, 0);
        Point3D diff = target.subtract(origin);
        double height = diff.magnitude();

        Point3D mid = target.midpoint(origin);
        Translate moveToMidpoint = new Translate(mid.getX(), mid.getY(), mid.getZ());

        Point3D axisOfRotation = diff.crossProduct(yAxis);
        double angle = Math.acos(diff.normalize().dotProduct(yAxis));
        Rotate rotateAroundCenter = new Rotate(-Math.toDegrees(angle), axisOfRotation);

        Cylinder line = new Cylinder(.5, height);
        line.setMaterial(new PhongMaterial(Color.web("#ff000060")));

        line.getTransforms().addAll(moveToMidpoint, rotateAroundCenter);

        return line;
    }

    public void savePointsToText(Stage primaryStage) //createes text file with points
    {
        final String testString = generateText();
        fileChooser = new FileChooser();

        //Set extension filter for text files
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
        fileChooser.getExtensionFilters().add(extFilter);

        //Show save file dialog
        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                PrintWriter writer;
                writer = new PrintWriter(file);
                writer.println(testString);
                writer.close();
            } catch (IOException ex) {
                Logger.getLogger(MainLogic.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    public String generateText() //creates text for file to save
    {
        String finalText = "";
        for (int i = 0; i < allPoints.size(); i++) {
            finalText = finalText + allPoints.get(i).getX() + "," + allPoints.get(i).getY() + "," + allPoints.get(i).getZ() + "\n";
        }
        return finalText;
    }
}
