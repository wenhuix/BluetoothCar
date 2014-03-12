/*
* Used to control a bluetooth car
* 
* For more information and the latest version please refer to
*    https://github.com/wenhuix/BTCar/
* Author: wenhuix
* Email: xiangwenhui@gamil.com
* Log:
*  2014-3-12 modified
*/

#include <Servo.h> 
#include <Wire.h>

#define TURN_LEFT_MAX  115
#define TURN_RIGHT_MIN 65
#define MID_DRIVE 1500
#define MID_ANGLE 1465

Servo angle;//Rotation angle servo
Servo drive;//Driver motor

/* used to store command from serial port
* command[0] = speed; command[1] = angle
* the range of command value from Android phone:
* speed = -100~100  angle = -60 ~ 60
*/
char command[2] = {0};  

/*
* 20 triggers per cycle, the car can march about 37cm
*/ 
volatile int triggers = 0;
volatile float v = 0;  //Velocity
int lastDrive = MID_DRIVE;
int lastT = 0;  //last time
int totalR = 0; //total residual

void setup() 
{ 
  Serial.begin(9600);//Serial port

  angle.attach(9);
  drive.attach(10);
  initializeServo();
  
  attachInterrupt(0, extInterrupt0, CHANGE);
}
//////////////////////////////////////
// main()
void loop() {

  printInfo( );
  calcuVelocity();
  //controlCar(getDrive(), getAngle());
  int driveV = MID_DRIVE + (int)calcuDrive(2);
  Serial.println(driveV);
  controlCar(driveV, 90);
  delay(50);
} 

//////////////////////////////////////
// convert command value to control value
int getDrive(){
  return MID_DRIVE + command[0];
}

int getAngle(){
  return 90 + command[1]/2;
}

///////////////////////////////////////
// PID controller
float calcuDrive(float speed){
  float r = speed - v;
  totalR += r;
  return 20 * r + 0.5 * totalR;
}

///////////////////////////////////////
// calculate car speed/Velocity
// return: velocity (m/s)
void calcuVelocity(){
  int t = millis() - lastT;
  //if(t < 50) return;
  
  v = triggers * 0.37 * 50 / t;
  lastT = millis();
  triggers = 0;
}

///////////////////////////////////////
// send rotation angle and drive value
void controlCar(int drive, int angle)
{
  setDrive(drive);
  setAngle(angle);
}

////////////////////////////////////////
//interrupt function
//used to calculate speed
void extInterrupt0(){
  triggers++;
}

////////////////////////////////////////
//receive serial prot data
void serialEvent(){
  if(Serial.available()>=3){
    if(Serial.read() == 111);
      Serial.readBytes(command, 2);
    //discard the rest of the data
    //while(Serial.read());
  }
}

////////////////////////////////////////
// Initialize the two servo
void initializeServo(){
  drive.writeMicroseconds(MID_DRIVE);
  delay(500); //DON'T REMOVE IT!!!!
  angle.writeMicroseconds(MID_ANGLE);
  delay(500); //DON'T REMOVE IT!!!!
}

/////////////////////////////////////////
// set rotation angle
// between 0 and 180 degrees
void setAngle(int val)
{
  if(val>TURN_LEFT_MAX){
    val = TURN_LEFT_MAX;
  }else if(val<TURN_RIGHT_MIN){
    val = TURN_RIGHT_MIN;
  }
  angle.write(val);
}

/////////////////////////////////////////
// Set drive value
/* On standard servos 1000us is fully counter-clockwise, 
* 2000us is fully clockwise, and 1500us is in the middle.
*
* In this DC motor controller, value < 1500 is backward
* value > 1500 is forward. 
* the power in proportion to |value-1500|
*
* I don't want to add the following comment, but there is no choice.
* It's depends on DC motor comtorller.
* 1) the value should add slowly; 
* 2) if lastDrive < 1500 and val > 1500 this time, it should be
*    wait for a second (depends on DC motor controller)
*/
void setDrive(int val)
{
  boolean isreverse = false;
  int mode;
  int r = val - lastDrive;
  if((lastDrive - MID_DRIVE) * (val - MID_DRIVE) < 0)
    isreverse = true;
  
  if(!isreverse && r >= 0)                mode = 1; //speed up(forward); slow down(backward)
  else if(!isreverse && r < 0)            mode = 2; //slow down(forward); speed up(backward)
  else if(isreverse && val >= MID_DRIVE)  mode = 3; //from backword to forward
  else                                    mode = 4; //from forward to backword
  
  switch(mode){
    case 1:
      for(int i=0; i<r; i++){  
        drive.writeMicroseconds(lastDrive + i);
        delay(1); 
      }
      break;
    case 2:
      for(int i=0; i<-r; i++){  
        drive.writeMicroseconds(lastDrive - i);
        delay(1); 
      }
      break;
    case 3:
      for(int i=0; i<val - MID_DRIVE; i++){
        drive.writeMicroseconds(MID_DRIVE + i);
        delay(1); 
      }
      break;
    case 4:
      for(int i=0; i<MID_DRIVE - val; i++){
        drive.writeMicroseconds(MID_DRIVE - i);
        delay(1); 
      }
  }
  //keep the last value
  lastDrive = val;
}

//////////////////////////////////////
// print some information
void printInfo(){
  Serial.print("com:");
  Serial.print(command[0], DEC);
  Serial.print(" ");
  Serial.print(command[1], DEC);
  Serial.print(" der:");
  Serial.print(getDrive());
  Serial.print(" ang:");
  Serial.print(getAngle());
  Serial.print(" trig:");
  Serial.print(triggers);
  Serial.print(" V:");
  Serial.println(v);
}
