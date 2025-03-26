# Lidar Scan & Render Bot

![Results](/screenshots/Final.gif?raw=true "")

## Video Link

* [YouTube](https://youtu.be/6YTCKRSWroE) - Video Demo


## How It Works
This project consists of two parts. The Arduino side handles all the servo movements and gets measurement readings from the Lidar sensor. The Java side does the spherical to cartesian points conversion and renders them in 3D. They communicate with each other over the serial port. The user can move around the scene with game-like fps controls for a better experience.

## The Math
There are two servos in the project. One will be used as the Pan (Phi) angle and the other as the Tilt (Theta) angle. The reading we get from the Lidar sensor will become the Radius. Which means that device itself would be at point (0,0,0). Each data set sent from the Arduino to the Java program consists of these three numbers (radius, phi, theta) with leading zeroes for consistency.

From here we have to convert these Spherical points to Cartesian(x, y, z) so we can properly plot them in our Java program.

We can achieve these conversions using these formulas.

Note: If you look inside the Java code and notice that some of the values of x, y, and z are swapped, some times even set to negative, that's because the computer coordinate system is different as well. For example, in computer coordinates, the x goes horizontal not y. Y positive goes down, not up. Things like that. These modifications made to the formula were only to get Java to render the points in the correct place.

## Serial Communication
I started this project with an Arduino Uno but ran into problems with the serial port conflicting with the servos. They cannot work at the same time. When data was being sent back and forth, the servos would jitter constantly. From research I learned that this was a common problem using SoftwareSerial. I found some workarounds for this problem but they were such a hassle and didn't work for me. I decided to switch to an Arduino Mega and it solved the jittering problem. This is because the Mega has multiple serial ports.

The steps of communicating

-Java opens port and listens for data
-Arduino takes first measurement, moves the servos to the next position and wait for an "Okay" signal from Java
 Arduino sends the data to Java in this format 000,000,000
-Java waits for all the fragments of data to reach a certain amount that is enough to convert
-Java converts those numbers, plots the point, and send the Arduino a single char as the "Okay" signal
-Once the Arduino receives the signal, it repeats what it did before at the new angle positions

The reason Java has to wait is because even though you tell the Arduino to send 9 digits and 2 commas, sending data over serial does not work that way. It will send fragments of whatever data it wants whenever it can. So we want Java to until all the data received looks like something it can recognize.

I used a USB cable as the serial connection but I am sure one could use a bluetooth module as well if wireless was needed.

## Powering The Project
I used a 5v wall brick to run the servos because I just hate dealing with batteries and avoid them when possible. The Lidar sensor was powered through the Arduino 5V. And of course the Arduino was powered with the same USB cable that was used as a serial port.


### Materials
* [Arduino Mega 2560](https://amzn.to/4c5rFt3)
* [Lidar Sensor](https://amzn.to/3XwOXC2)
* [Servo Motors](https://amzn.to/42jcc5k)
* [Brackets](https://amzn.to/3FSMJGZ)


### Prerequisites

* [Java](https://www.java.com) - Java Runtime Environment
* [Arduino](https://www.arduino.cc/) - Arduino Mega Microcontroller
* [TFMini(UART)](https://www.sparkfun.com/products/14588) - TFMini Lidar sensor

* [MG996R Servo x 2](https://www.towerpro.com.tw/product/mg996r/) - 180 degrees standard servos


## Programmed In

* [IntelliJ](https://www.jetbrains.com/idea/download) - Java IDE
* [Arduino](https://https://www.arduino.cc/) - Arduino IDE


## Schematic

![Schematic](/screenshots/schematic.png?raw=true "")

## Screenshots

![The Device](/screenshots/ss5.png?raw=true "")
![The Software](/screenshots/ss4.png?raw=true "")
![Running The Program](/screenshots/ss3.png?raw=true "")
![Results](/screenshots/ss2.png?raw=true "")


## Authors

* **[Travis Ledo](https://travisledo.github.io)** - *Initial work* - [LidarScanRender](https://github.com/TravisLedo)
