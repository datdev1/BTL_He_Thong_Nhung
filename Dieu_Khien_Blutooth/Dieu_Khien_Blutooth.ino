#include "BluetoothSerial.h"
#include <Wire.h>
#include <Adafruit_BMP085.h>
// #include <MPU6050.h>
#include "MPU6050_6Axis_MotionApps20.h"
#include <QMC5883LCompass.h>
#include <PlayNote.h>

// playNote
PlayNote playnote;
TaskHandle_t playNoteTaskHandle;
bool isPlayNote = false;

BluetoothSerial SerialBT;

// Cam bien GY-87
Adafruit_BMP085 bmp;
MPU6050 mpu;
QMC5883LCompass compass;
  // Cảm biến gia tốc
  int16_t accelX = 0, accelY = 0, accelZ = 0;
  // Con quay hồi chuyển
  int16_t gyroX = 0, gyroY = 0, gyroZ = 0;
  // La bàn
  int compassX = 0, compassY = 0, compassZ = 0, compassHeading = 0;
  // Cảm biến môi trường
  float temp = 0.0;
  int32_t pressure = 0;
  // SDA Đỏ - 21
  // SCL Nâu - 22
  int xMinCalibra = 99999, xMaxCalibra = -99999, yMinCalibra = 99999, yMaxCalibra = -99999, zMinCalibra = 99999, zMaxCalibra = -99999;
  bool isCalibration = false;

  /*---MPU6050 Control/Status Variables---*/
  bool DMPReady = false;  // Set true if DMP init was successful
  uint8_t devStatus;      // Return status after each device operation (0 = success, !0 = error)
  uint8_t FIFOBuffer[64]; // FIFO storage buffer

  /*---Orientation/Motion Variables---*/ 
  Quaternion q;           // [w, x, y, z]         Quaternion container
  VectorFloat gravity;    // [x, y, z]            Gravity vector
  VectorInt16 aa;         // [x, y, z]            Accel sensor measurements
  VectorInt16 aaReal;     // [x, y, z]            Gravity-free accel sensor measurements
  float ypr[3];           // [yaw, pitch, roll]   Yaw/Pitch/Roll container and gravity vector


// Travel_distance
float alphaEn = 1;
float travel_distance = 0;
unsigned long dem2 = 0;
float duong_kinh_banh_xe = 6.6;

// Encoder
int enco = 13; // D-CAM  A-VÀNG
int dem = 0;
float rpm = 0;
float tocdo = 0;
int timecho = 1000;
unsigned long thoigian = 0, hientai = 0;
void dem_xung()
{
  dem++;
  dem2++;
}

// Định nghĩa chân điều khiển động cơ l298n   den-gnd    trang-vin
const int enA = 2;  // xanh la cay 19 old
const int in1 = 27; // nau
const int in2 = 26; // do
const int in3 = 25; // cam
const int in4 = 33; // vang
const int enB = 4;  // xanh duong 21 old
int speed = 120;
int delta_speed = -14;
String dieu_khien;

// Cảm biến siêu âm
#define LEFT_TRIG 5  // nau
#define LEFT_ECHO 34 // trang

#define RIGHT_TRIG 23 // Vang
#define RIGHT_ECHO 32 // Xanh la

#define FRONT_TRIG 18 // nau
#define FRONT_ECHO 35 // trang

long leftDistance = 0;
long rightDistance = 0;
long frontDistance = 0;

// Task handle cho task đo khoảng cách
// volatile int distance = 0;
TaskHandle_t flowTaskHandle;

void setup()
{
  Serial.begin(115200);
  SerialBT.begin("ESP_TEST");
  Serial.println("Bluetooth is ready. Pair with ESP32_BT to start!");

  //GY-87
  Wire.begin(21, 22); // SDA = 21, SCL = 22 (ESP32 default I2C pins)
  Wire.setClock(400000);

  // BMP180
  if (!bmp.begin()) {
    Serial.println("Could not find BMP180 sensor!");
    while (1);
  }

  // MPU6050
  mpu.initialize();
  if (!mpu.testConnection()) {
    Serial.println("MPU6050 not connected!");
    while (1);
  }

  devStatus = mpu.dmpInitialize();

  getCalibrationMPU();
  /* Making sure it worked (returns 0 if so) */ 
  if (devStatus == 0) {
    mpu.CalibrateAccel(6);  // Calibration Time: generate offsets and calibrate our MPU6050
    mpu.CalibrateGyro(6);
    Serial.println("These are the Active offsets: ");
    mpu.PrintActiveOffsets();
    Serial.println(F("Enabling DMP..."));   //Turning ON DMP
    mpu.setDMPEnabled(true);


    /* Set the DMP Ready flag so the main loop() function knows it is okay to use it */
    Serial.println(F("DMP ready! Waiting for first interrupt..."));
    DMPReady = true;
  } 
  else {
    Serial.print(F("DMP Initialization failed (code ")); //Print the error code
    Serial.print(devStatus);
    Serial.println(F(")"));
    // 1 = initial memory load failed
    // 2 = DMP configuration updates failed
  }

  mpu.setI2CBypassEnabled(true);  

  // QMC5883L (magnetometer)
  compass.init();
  compass.setCalibration(-3620, -580, -1958, 926, -2641, 395);

  Serial.println("GY-87 Initialized with Adafruit Libraries");

  // car
  pinMode(enA, OUTPUT);
  pinMode(in1, OUTPUT);
  pinMode(in2, OUTPUT);
  pinMode(enB, OUTPUT);
  pinMode(in3, OUTPUT);
  pinMode(in4, OUTPUT);
  digitalWrite(in1, LOW);
  digitalWrite(in2, LOW);
  digitalWrite(in3, LOW);
  digitalWrite(in4, LOW);
  analogWrite(enA, speed);
  analogWrite(enB, speed);

  // hc-sr
  pinMode(LEFT_TRIG, OUTPUT);
  pinMode(LEFT_ECHO, INPUT);
  pinMode(RIGHT_TRIG, OUTPUT);
  pinMode(RIGHT_ECHO, INPUT);
  pinMode(FRONT_TRIG, OUTPUT);
  pinMode(FRONT_ECHO, INPUT);

  // encoder
  pinMode(enco, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(enco), dem_xung, RISING);

  // Tạo task đo khoảng cách trên lõi 0
  xTaskCreatePinnedToCore(
    flowTask, // Hàm thực hiện đo khoảng cách
    "Flow Task",      // Tên task
    5000,                 // Kích thước stack
    NULL,                 // Tham số cho task (không cần)
    1,                    // Mức ưu tiên
    &flowTaskHandle,  // Task handle
    0                     // Chạy trên lõi 0
  );

  //playNote
  playnote.setBuzzerPin(15);
  playnote.play(784); // play a exactly Hz
  playnote.play(880);
  playnote.play(988);
  playnote.play(1047);

  // xTaskCreatePinnedToCore(
  //   gy87Task,             // Task function
  //   "GY-87 Task",         // Name
  //   4000,                 // Stack size (tùy RAM, bạn có thể tăng nếu lỗi)
  //   NULL,                 // Parameter
  //   1,                    // Priority
  //   &gy87TaskHandle,      // Task handle
  //   0                     // Core 0
  // );
  // xTaskCreatePinnedToCore(
  //   playMusic,             // Task function
  //   "Play Note",         // Name
  //   1000,                 // Stack size (tùy RAM, bạn có thể tăng nếu lỗi)
  //   NULL,                 // Parameter
  //   2,                    // Priority
  //   &playNoteTaskHandle,      // Task handle
  //   0                     // Core 0
  // );
}

void loop()
{
  thoigian = millis();

  travel_distance = (((float)dem2 / 20.0) * (duong_kinh_banh_xe * 3.14)) * alphaEn;

  if (thoigian - hientai >= timecho)
  {
    hientai = thoigian;

    rpm = ((float)dem / 20.0) * 60.0;
    tocdo = ((float)dem / 20.0) * (duong_kinh_banh_xe / 100 * 3.14); // Tốc độ (m/s) d*3,14 = cv

    dem = 0;

    Serial.print("RPM: ");
    Serial.println(rpm);
    Serial.print("M/S: ");
    Serial.println(tocdo);
  }
  // SerialBT.printf("Speed: %f; Travel distance: %f\n", tocdo, travel_distance);
  if (SerialBT.available())
  {
    dieu_khien = SerialBT.readStringUntil('\n');

    // Serial.println(dieu_khien);
    if (dieu_khien == "F")
    {
      tien();
    }
    else if (dieu_khien == "B")
    {
      lui();
    }
    else if (dieu_khien == "L")
    {
      trai();
    }
    else if (dieu_khien == "R")
    {
      phai();
    }
    else if (dieu_khien == "FR")
    {
      tien_phai();
    }
    else if (dieu_khien == "FL")
    {
      tien_trai();
    }
    else if (dieu_khien == "BL")
    {
      lui_phai();
    }
    else if (dieu_khien == "BR")
    {
      lui_trai();
    }
    else if (dieu_khien == "S")
    {
      Stop();
    }
    else if (dieu_khien == "D")
    {
      travel_distance = 0;
      dem2 = 0;
      Serial.println("Reset!!!");
    }
    else if (dieu_khien.indexOf("Speed") != -1)
    {
      String set_speed = dieu_khien.substring(6, dieu_khien.length());
      speed = set_speed.toInt();
      analogWrite(enA, speed);
      analogWrite(enB, speed + delta_speed);
    }
    else if (dieu_khien.indexOf("Delta_speed") != -1)
    {
      String set_delta_speed = dieu_khien.substring(12, dieu_khien.length());
      delta_speed = set_delta_speed.toInt();
      analogWrite(enA, speed);
      analogWrite(enB, speed + delta_speed);
    }
    else if (dieu_khien.indexOf("alphaEn") != -1)
    {
      String set_alpha_encoder = dieu_khien.substring(8, dieu_khien.length());
      alphaEn = set_alpha_encoder.toFloat();
    }
    else if (dieu_khien == "music"){
      isPlayNote = true;
    }
    else if (dieu_khien == "calculatingCalibration")
    {
      isCalibration = true;
      xMinCalibra = 99999; 
      xMaxCalibra = -99999; 
      yMinCalibra = 99999; 
      yMaxCalibra = -99999; 
      zMinCalibra = 99999; 
      zMaxCalibra = -99999;
      compass.clearCalibration();
    }
    else if (dieu_khien == "resetCalibration")
    {
      resetCalibration();
    }
    calculatingCalibration();
    sendAllInformation();
  }
}

void sendAllInformation()
{
  String message = "";

  // Nối từng phần tùy nhu cầu — có thể bật/tắt bằng cách comment
  if(!isCalibration )
  {
    message += "Speed: " + String(tocdo, 2) + "; ";
    message += "TravelDis: " + String(travel_distance, 2) + "; " + "dieu_khien:" + dieu_khien  + "\n";
    message += "SpeedMotor: " + String(speed) + "; ";
    message += "Delta_speed: " + String(delta_speed) + "; ";
    message += "Vong: " + String((float)dem2 / 20.0, 2) + "\n";
    // Dữ liệu cảm biến siêu âm
    message += "Sonic: [L: " + String(leftDistance) + "; R: " + String(rightDistance) + "; F: " + String(frontDistance) + "]\n";

    // Dữ liệu gia tốc raw
    // message += "Accel: [X: " + String(accelX) + "; Y: " + String(accelY) + "; Z: " + String(accelZ) + "]\n";

    //Gia tốc bỏ qua gia tốc trọng trường
    message += "Accel: [X: " + String(aaReal.x) + "; Y: " + String(aaReal.y) + "; Z: " + String(aaReal.z) + "]\n";

    // Dữ liệu con quay hồi chuyển
    message += "Gyro: [X: " + String(gyroX) + "; Y: " + String(gyroY) + "; Z: " + String(gyroZ) + "]\n";
    
    message += "YPR: [Y: " + String(ypr[0] * 180/M_PI, 2) + "; P: " + String(ypr[1] * 180/M_PI, 2) + "; R: " + String(ypr[2] * 180/M_PI, 2) + "]\n";

    float altitude = calculateAltitude(float(pressure)/100, temp);
    message += "Temp: " + String(temp) + "'C; Pres: " + String(float(pressure)/100) + "Pa; Alti" + String(altitude, 2) + "m\n";
  }
  

  // Dữ liệu la bàn
  message += "Compass: [X: " + String(compassX)
           + "; Y: " + String(compassY)
           + "; Z: " + String(compassZ)
           + "; Heading: " + String(compassHeading) + "]";

  if (isCalibration)
  {
    message += "\nCalculating Calibration:\n";
    message += "X: [" + String(xMinCalibra) + "; " + String(xMaxCalibra) + "]\n";
    message += "Y: [" + String(yMinCalibra) + "; " + String(yMaxCalibra) + "]\n";
    message += "Z: [" + String(zMinCalibra) + "; " + String(zMaxCalibra) + "]";
  }
  message += "\r\n";
  // Gửi qua Bluetooth
  SerialBT.print(message);
  SerialBT.flush();
}

void flowTask(void * parameter) {
  for (;;) {
  // Hàm đo khoảng cách từ cảm biến siêu âm
    leftDistance = getDistance(LEFT_TRIG, LEFT_ECHO);
    rightDistance = getDistance(RIGHT_TRIG, RIGHT_ECHO);
    frontDistance = getDistance(FRONT_TRIG, FRONT_ECHO);
    gy87Task();
    playMusic();
    vTaskDelay(100 / portTICK_PERIOD_MS); // Kiểm tra mỗi 100ms
  };
}

void gy87Task()
{
  // GY-87
    mpu.getMotion6(&accelX, &accelY, &accelZ, &gyroX, &gyroY, &gyroZ);
    temp = bmp.readTemperature();
    pressure = bmp.readPressure();
    compass.read();
    compassX = compass.getX();
    compassY = compass.getY();
    compassZ = compass.getZ();
    compassHeading = compass.getAzimuth();
    calAngle();
};

long getDistance(int TRIG_PIN, int ECHO_PIN) {
  digitalWrite(TRIG_PIN, LOW);
  delayMicroseconds(2);
  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(5);
  digitalWrite(TRIG_PIN, LOW);
  long duration = pulseIn(ECHO_PIN, HIGH);
  long distance = int(duration / 2 / 29.412); // Tính khoảng cách (cm)
  return distance;
}

void tien() { dieuKhienDongCo(HIGH, LOW, LOW, HIGH); }
void lui() { dieuKhienDongCo(LOW, HIGH, HIGH, LOW); }
void trai() { dieuKhienDongCo(HIGH, LOW, HIGH, LOW); }
void phai() { dieuKhienDongCo(LOW, HIGH, LOW, HIGH); }
void Stop() { dieuKhienDongCo(LOW, LOW, LOW, LOW); }
void tien_phai() { dieuKhienDongCo(LOW, LOW, LOW, HIGH); }
void tien_trai() { dieuKhienDongCo(HIGH, LOW, LOW, LOW); }
void lui_trai() { dieuKhienDongCo(LOW, HIGH, LOW, LOW); }
void lui_phai() { dieuKhienDongCo(LOW, LOW, HIGH, LOW); }

void dieuKhienDongCo(bool in1_val, bool in2_val, bool in3_val, bool in4_val)
{
  digitalWrite(in1, in1_val);
  digitalWrite(in2, in2_val);
  digitalWrite(in3, in3_val);
  digitalWrite(in4, in4_val);
}

void calculatingCalibration()
{
  if(isCalibration)
  {
    compass.read();
  
    compassX = compass.getX();
    compassY = compass.getY();
    compassZ = compass.getZ();
    compassHeading = compass.getAzimuth();

    if (xMinCalibra > compassX) xMinCalibra = compassX;
    if (yMinCalibra > compassY) yMinCalibra = compassY;
    if (zMinCalibra > compassZ) zMinCalibra = compassZ;
    
    if (xMaxCalibra < compassX) xMaxCalibra = compassX;
    if (yMaxCalibra < compassY) yMaxCalibra = compassY;
    if (zMaxCalibra < compassZ) zMaxCalibra = compassZ;
  };

}

void resetCalibration()
{
  
  compass.setCalibration(xMinCalibra, xMaxCalibra, yMinCalibra, yMaxCalibra, zMinCalibra, zMaxCalibra);
  isCalibration = false;
}

void calAngle()
{
  if (!DMPReady) return; // Stop the program if DMP programming fails.
  /* Read a packet from FIFO */
  if (mpu.dmpGetCurrentFIFOPacket(FIFOBuffer)) { // Get the Latest packet 
      // OUTPUT_READABLE_YAWPITCHROLL
      /* Display Euler angles in degrees */
      mpu.dmpGetQuaternion(&q, FIFOBuffer);
      mpu.dmpGetGravity(&gravity, &q);
      mpu.dmpGetYawPitchRoll(ypr, &q, &gravity);
      // Serial.print("ypr\t");
      // Serial.print(ypr[0] * 180/M_PI);
      // Serial.print("\t");
      // Serial.print(ypr[1] * 180/M_PI);
      // Serial.print("\t");
      // Serial.println(ypr[2] * 180/M_PI);

      mpu.dmpGetAccel(&aa, FIFOBuffer);
      mpu.dmpGetLinearAccel(&aaReal, &aa, &gravity);
      // Serial.print("areal\t");
      // Serial.print(aaReal.x);
      // Serial.print("\t");
      // Serial.print(aaReal.y);
      // Serial.print("\t");
      // Serial.println(aaReal.z);
  }
}


void getCalibrationMPU() {
  /* Supply your gyro offsets here, scaled for min sensitivity */
  mpu.setXGyroOffset(0);
  mpu.setYGyroOffset(0);
  mpu.setZGyroOffset(0);
  mpu.setXAccelOffset(0);
  mpu.setYAccelOffset(0);
  mpu.setZAccelOffset(0);
}

int countMusic = 0;

struct PlayNote::notes song1[] = {
	{"sol5", 0.5}, {"la5", 0.5}, {"sol5", 1}, {"0", 1},
};
int lenSong1 = sizeof(song1)/sizeof(PlayNote::notes);

struct PlayNote::notes song2[] = {
	{"do5", 0.5}, {"mi5", 0.5}, {"sol5", 0.5}, {"0", 1},
};
int lenSong2 = sizeof(song2)/sizeof(PlayNote::notes);

struct PlayNote::notes song3[] = {
	{"3'", 0.5}, {"6'", 0.5}, {"5'", 0.5}, {"0", 1},
};
int lenSong3 = sizeof(song3)/sizeof(PlayNote::notes);


void playMusic()
{
  if(isPlayNote)
  {
    // long randNumber = random(1, 3);
    if(countMusic == 0) playnote.playSong(song1, lenSong1);
    else if(countMusic == 1) playnote.playSong(song2, lenSong2);
    else if(countMusic == 2) playnote.playSong(song3, lenSong3);
    
    countMusic = (countMusic++)%3;
    isPlayNote = false;
  }
}

float calculateAltitude(float pressure, float temperature) {
  const float P0 = 1013.25; // Áp suất chuẩn ở mực nước biển (hPa)
  return (temperature + 273.15) / 0.0065 * (1 - pow(pressure / P0, 1.0 / 5.257));
}