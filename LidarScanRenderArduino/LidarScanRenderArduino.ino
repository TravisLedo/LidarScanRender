/*
   Program: Lidar Render Bot
   By: Travis Ledo
   Date: Jan 12th 2020
   Notes: This uses an Arduino Mega with multiple serial ports to avoid the SoftwareSerial and Servo conflicts.
*/




#include <Servo.h>
#include "CustomTFMini.h" //Needed to modify somecode in the original TFMini library

Servo tiltServo;
Servo panServo;

TFMini tfmini;

String incomingByte; // for incoming serial data

int minPanPos = 0;
int maxPanPos = 180;
int minTiltPos = 0;
int maxTiltPos = 90;

int panPos = minPanPos;    // variable to store the servo position
int tiltPos = minTiltPos;    // variable to store the servo position


bool keepScanning = true;
bool panForward = true;


void printPoint(int pan, int tilt, int dis) { //Java software will read these printed texts

  Serial.print(addLeadingZeros(pan));
  Serial.print(pan);
  Serial.print(",");
  Serial.print(addLeadingZeros(tilt));
  Serial.print(tilt);
  Serial.print(",");
  Serial.print(addLeadingZeros(dis));
  Serial.print(dis);
  Serial.println();

}




String addLeadingZeros(int number) //Add leading zeros so every number sent will always be 3 digits long for consistency
{
  String leadingZeros = "";

  if (number < 10)
  {
    leadingZeros = "00";
  }
  else if (number >= 10 && number < 100)
  {
    leadingZeros = "0";
  }
  else
  {
    leadingZeros = "";

  }

  return leadingZeros;
}




void updatePos() //move servos to next position
{
  if (panPos == maxPanPos)
  {
    panPos = 0;
    tiltPos++;
    panServo.write(panPos);
    tiltServo.write(tiltPos);
    delay(1000);
    keepScanning = true;
  }
  else if (panPos < maxPanPos)
  {
    panPos ++;
    panServo.write(panPos);
    delay(15);

    keepScanning = true;
  }

  if (tiltPos > maxTiltPos)
  {
    keepScanning = false;
  }
}




void takeMeasurement(int panPos, int tiltPos) //get a reading from the TFMini
{

  uint16_t dist1 = tfmini.getDistance();

  while (dist1 < 0 || dist1 > 1500)
  {
    dist1 = tfmini.getDistance();
  }


  uint16_t dist2 = tfmini.getDistance();

  while (dist2 < 0 || dist2 > 1500)
  {
    dist2 = tfmini.getDistance();
  }

  uint16_t dist3 = tfmini.getDistance();

  while (dist3 < 0 || dist3 > 1500)
  {
    dist3 = tfmini.getDistance();
  }



  uint16_t dist = dist1 + dist2 + dist3;

  uint16_t distAv = dist / 3;



  // Display the measurement
  printPoint(panPos, tiltPos, distAv);
  updatePos();
}





void setup() {

  panServo.attach(8);  // attaches the servo on pin
  tiltServo.attach(9);  // attaches the servo on pin

  // Step 1: Initialize hardware serial port 
  Serial.begin(115200); //For usb/bt data?

  // Step 2: Initialize the data rate for TFMini
  Serial1.begin(TFMINI_BAUDRATE);

  // Step 3: Initialize the TF Mini sensor
  tfmini.begin(&Serial1);

  panServo.attach(8);  // attaches the servo on pin
  tiltServo.attach(9);  // attaches the servo on pin
  tiltServo.write(tiltPos);
  panServo.write(panPos);
  delay(2000);


  //start first measurement
  takeMeasurement(panPos, tiltPos);

}



void loop() {

  if (tiltPos > maxTiltPos) //end program succesfully
  {
    keepScanning = false;
  }

  // send data only when you receive data:
  if (Serial.available() > 0 && keepScanning) {
    keepScanning = false; //need this to make the loop not go so fast
    takeMeasurement(panPos, tiltPos);
  }
}
