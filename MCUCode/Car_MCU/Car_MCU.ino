#include <Servo.h> 
#include <Wire.h>

#define TURN_LEFT_MAX  115
#define TURN_RIGHT_MIN 65
#define MID_DRIVE 1500
#define MID_ANGLE 1500

Servo angle;//Rotation angle servo
Servo drive;//Driver motor

boolean flag = false;
int lastDrive = MID_DRIVE;
char command[2] = {0};  // command[0] = speed; command[1] = angle

//20 triggers per cycle, the car can march about 37cm
volatile int triggers = 0;
int speed;

void setup() 
{ 
  Serial.begin(9600);//Serial port

  angle.attach(9);
  drive.attach(10);
  initializeServo();
  
  attachInterrupt(0, extInterrupt0, CHANGE);
}

void loop() {
  int drive = MID_DRIVE + 2 * command[0];
  int angle = 90 + command[1]/2;
  controlCar(drive, angle);
} 

///////////////////////////////////////
// send rotation angle and drive value
void controlCar(int drive, int angle)
{
  //printInfo(speed, direction);
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
  if(Serial.available()>=3)
  {
    char temp[3] = {0};
    if(temp[0] == 111){
      command[0] = temp[1];
      command[1] = temp[2];
    }
    //discard the rest of the data
    while(Serial.read());
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
  else(isreverse && val < MID_DREIVE)     mode = 4; //from forward to backword
  
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



void printInfo(int speed, int direction){
  Serial.print("Speed: ");
  Serial.print(speed);
  Serial.print("Direction: ");
  Serial.print(direction);
  Serial.print("SpeedCount:");
  Serial.println(triggers);
}
