#include <Servo.h> 
#include <Wire.h>

#define TURN_LEFT_MAX  117
#define TURN_RIGHT_MIN 60

Servo dir;//Direction servo
Servo drive;//Driver motor

int currentSpeed = 0;
boolean flag = false;
int readFromBT[2] = {0};  // readFromBT[0] = speed  readFromBT[1] = direction
int iniSpeed = 1500;      // motor speed = 0
int iniDirection = 1465;  // direction = straight

//20 triggers per cycle, the car can march about 37cm
volatile int speedCount = 0;

void setup() 
{ 
  Serial.begin(9600);//Serial port
  attachInterrupt(0, countSpeed, CHANGE);
  initializeServo();
}

void loop() {
  int dataCount = 0;
  int temp;
  while(Serial.available())
  {
    temp = (int)convertByteToChar(Serial.read());
    if(temp == 111){
      flag = true; //receive right data package
      dataCount = 0;
      continue;
    }
    if(flag){
      readFromBT[dataCount] = temp;
      dataCount = dataCount + 1;
      if(dataCount == 2){
        //Serial.print("readFromBT: ");
        //Serial.print(readFromBT[0]);
        //Serial.println(readFromBT[1]);
        flag = false;
      }    
    } 
  }
  controlCar();
} 

//interrupt function
void countSpeed(){
  speedCount++;
}

void initializeServo(){
  dir.attach(9);
  drive.attach(10);
  //Initialize the drive motor
  dir.writeMicroseconds(iniSpeed);     // set servo to mid-point
  currentSpeed = iniSpeed;
  delay(500);
  //Initialize the direction servo
  dir.writeMicroseconds(iniDirection);  // set servo to mid-point
  delay(500);
}

char convertByteToChar(byte n) {
  if (n < 128) return n;
  return n - 256;
}

void setDirection(int val)
{
  if(val>TURN_LEFT_MAX){
    val = TURN_LEFT_MAX;
  }else if(val<TURN_RIGHT_MIN){
    val = TURN_RIGHT_MIN;
  }
  dir.write(val);
}

void setSpeed(int val)
{
  int speedResidual = currentSpeed - val;
  if(speedResidual > 0){
    for(int i=0; i<speedResidual; i++)
    {
      drive.writeMicroseconds(currentSpeed - i);
      delay(1);
    }
  }
  if(speedResidual < 0){ 
    for(int i=0; i<-speedResidual; i++)
    {
      drive.writeMicroseconds(currentSpeed + i);
      delay(1);
    }
  }
  currentSpeed = val;
}

void controlCar()
{
  int speed = iniSpeed + 2 * readFromBT[0];
  int direction = 90 + readFromBT[1]/2;
  Serial.print("Speed: ");
  Serial.print(speed);
  Serial.print("Direction: ");
  Serial.print(direction);
  Serial.print("SpeedCount:");
  Serial.println(speedCount);
  setDirection(direction);
  setSpeed(speed);
}
