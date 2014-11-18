/*
* Used to control a bluetooth car
* 
* For more information and the latest version please refer to
*    https://github.com/wenhuix/BluetoothCar
* Author: wenhuix
* Email: littlewest@foxmail.com
* Log:
*  2014-03-12 modified
*  2014-04-14 modified
*  2014-11-18 remove useless code
*/

#include <Servo.h> 
#include <Wire.h>

#define TURN_LEFT_MAX  130
#define TURN_RIGHT_MIN 50
#define MAX_DRIVE 1750  //maximun drive value
#define MIN_DRIVE 1250  //minimun drive value

#define MID_DRIVE 1500
#define MID_ANGLE 1500

Servo angleServo;//Rotation angle servo
Servo driveMotor;//Driver motor

/* used to store command from serial port
* the range of command value from Android phone:
* speed = -100~100 scale:  1 = 0.01m/s  
% angle = -60 ~ 60
*/
char speedVolume = 0;
char angleVolume = 0;

/*
* 100 triggers per cycle, the car can march about 37cm
*/ 
volatile int triggers = 0;
volatile float v = 0;  //Velocity
float vBuffer[4] = {0};
int lastDrive = MID_DRIVE;
int lastT = 0;  //last time
float e[3] = {0}; //error e(t-2),e(t-1),e(t)

// PID controller parameters
#define KP 195
#define KI 5
#define KD 1.25

float speed = 0;//desired speed
float angle = 0;//desired angle
int driveV = 0; //DC motor input signal
int u = 0;      //Output of PID controller

void setup() 
{ 
  Serial.begin(9600);//Serial port

  angleServo.attach(9);
  driveMotor.attach(10);
  initializeServo();
  
  attachInterrupt(0, extInterrupt0, CHANGE);
}
//////////////////////////////////////
// main()

void loop() {
  
  calcuVelocity();
  speed = getSpeedValue();
  angle = getAngleValue();
  
  if(speed==0){
    driveV = MID_DRIVE;
    u = 0;
  } else {
    u = u + (int)speedPidController(speed);
    
    if(u>0){
      driveV = MID_DRIVE + 50 + u;
      driveV = min(driveV, MAX_DRIVE);
    }else if(u<0){
      driveV = MID_DRIVE - 50 + u;
      driveV = max(driveV, MIN_DRIVE);
    }
  }
  controlCar(driveV, angle);
} 

//////////////////////////////////////
// convert command value to control value
float getSpeedValue(){
  return speedVolume * 0.01;
}

int getAngleValue(){
  return 90 + angleVolume;
}

///////////////////////////////////////
// PID speed controller
// 增量式PID控制算法
float speedPidController(float speed){
  
  //e(t-2),e(t-1),e(t)
  e[0] = e[1];
  e[1] = e[2];
  if (speed<0)
    e[2] = speed + v;
  else if(speed>0)
    e[2] = speed - v;

  //Delta {u_t} = {K_P}({e_t} - {e_{t - 1}}) + {K_I}{e_t} + {K_D}({e_t} - 2{e_{t - 1}} + {e_{t - 2}})
  // = ({K_p} + {K_I} + {K_D}){e_t} - ({K_p} + 2{K_D}){e_{t - 1}} + {K_D}{e_{t - 2}}
  return (KP + KI + KD) * e[2] - (KP + 2 * KD) * e[1] + KD * e[0];
}

///////////////////////////////////////
// calculate car speed/Velocity
// return: velocity (m/s)
// v = (tirggers * 0.37 * 1000) / (t*20*5)
void calcuVelocity(){
  int t = millis() - lastT;
  while(t < 50) {
    delay(1);
    t = t + 1;
  }
  //Add filter
  vBuffer[0] = vBuffer[1];
  vBuffer[1] = vBuffer[2];
  vBuffer[2] = vBuffer[3];  
  vBuffer[3] = triggers * 6.1 / t;
  //v = (vBuffer[0] + vBuffer[1] + vBuffer[2])/3 * 0.8 + vBuffer[3] * 0.2;
  //v = (vBuffer[0] + vBuffer[1] + vBuffer[2] + vBuffer[3])/4;
  v = (vBuffer[1] + vBuffer[2] + vBuffer[3])/3;
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
  while(Serial.available()>=3)
  {
    if(Serial.read() == 111)
    {
      speedVolume = Serial.read();
      angleVolume = Serial.read();
    }
  }
}

////////////////////////////////////////
// Initialize the two servo
void initializeServo(){
  driveMotor.writeMicroseconds(MID_DRIVE);
  delay(500); //DON'T REMOVE IT!!!!
  angleServo.writeMicroseconds(MID_ANGLE);
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
  angleServo.write(val);
}

/////////////////////////////////////////
// Set drive value
/* On standard servos 1000us is fully counter-clockwise, 
* 2000us is fully clockwise, and 1500us is in the middle.
*
* In this DC motor controller, value < 1500 is backward
* value > 1500 is forward. Actually, the car will move until
* value > 1600 or value < 1400
* the power in proportion to |value-1500|
*
* I don't want to add the following comment, but there is no choice.
* It's depends on DC motor comtorller.
* 1) if the direction is reversed, it should be
*    wait for a second (depends on DC motor controller)
*/
void setDrive(int val)
{
  //Serial.println(val);
  boolean isreverse = false;
  //long Data type is very important! Don't touch it!
  if((long)(lastDrive - MID_DRIVE) * (long)(val - MID_DRIVE) < 0){
    isreverse = true;
  }
  if(isreverse){
    driveMotor.writeMicroseconds(MID_DRIVE);
    delay(5);
  }
  driveMotor.writeMicroseconds(val);
  //keep the last value
  lastDrive = val;
}
