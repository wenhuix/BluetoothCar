#include <Servo.h> 
#include <Wire.h>

#define TURN_LEFT_MAX  117
#define TURN_RIGHT_MIN 60
#define INI_SPEED 1465
#define INI_ANGLE 1500

Servo angle;//Rotation angle servo
Servo drive;//Driver motor

boolean flag = false;
int lastSpeed = 0;
char command[2] = {0};  // command[0] = speed; command[1] = angle

//20 triggers per cycle, the car can march about 37cm
volatile int triggers = 0;
//volatile int speed;

void setup() 
{ 
  Serial.begin(9600);//Serial port
  attachInterrupt(0, extInterrupt0, CHANGE);
  initializeServo();
}

void loop() {
  
  controlCar();
} 

//interrupt function
void extInterrupt0(){
  triggers++;
}

void serialEvent(){
  if(Serial.available()>=3)
  {
    char temp[3] = {0};
    Serial.readBytes(temp, 3);
    Serial.print(" command0: ");
    Serial.print(temp[0], DEC);
    Serial.print(" command1: ");
    Serial.println(temp[1], DEC);
    if(temp[0] == 111){
      command[0] = temp[1];
      command[1] = temp[2];
    }
    while(Serial.read());
  }
}

void initializeServo(){
  angle.attach(9);
  drive.attach(10);
  lastSpeed = INI_SPEED;
  //Initialize the drive motor
  drive.writeMicroseconds(INI_SPEED);     // set servo to mid-point
  delay(500);
  //Initialize the direction servo
  angle.writeMicroseconds(INI_ANGLE);  // set servo to mid-point
  delay(500);
}

char convertByteToChar(byte n) {
  if (n < 128) return n;
  return n - 256;
}

void setAngle(int val)
{
  if(val>TURN_LEFT_MAX){
    val = TURN_LEFT_MAX;
  }else if(val<TURN_RIGHT_MIN){
    val = TURN_RIGHT_MIN;
  }
  angle.write(val);
}

void setDrive(int val)
{
  int residual = lastSpeed - val;
  if(residual > 0){
    for(int i=0; i<residual; i++)
    {
      drive.writeMicroseconds(lastSpeed - i);
      delay(1);
    }
  }
  if(residual < 0){ 
    for(int i=0; i<-residual; i++)
    {
      drive.writeMicroseconds(lastSpeed + i);
      delay(1);
    }
  }
  lastSpeed = val;
}

void controlCar()
{
  int drive = INI_SPEED + 2 * command[0];
  int angle = 90 + command[1]/2;

  //printInfo(speed, direction);
  setDrive(drive);
  setAngle(angle);
}

void printInfo(int speed, int direction){
  Serial.print("Speed: ");
  Serial.print(speed);
  Serial.print("Direction: ");
  Serial.print(direction);
  Serial.print("SpeedCount:");
  Serial.println(triggers);
}
