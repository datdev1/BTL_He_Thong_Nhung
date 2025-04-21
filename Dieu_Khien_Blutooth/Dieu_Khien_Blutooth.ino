#include "BluetoothSerial.h"
#include <Wire.h>
#include <Adafruit_BMP085.h>
#include <MPU6050.h>
#include <QMC5883LCompass.h>

BluetoothSerial SerialBT;

//Cam bien GY-87
Adafruit_BMP085 bmp;
MPU6050 mpu;
QMC5883LCompass compass;
int16_t accelX, accelY, accelZ, gyroX, gyroY, gyroZ;
int compassX, compassY, compassZ, compassHeading;
float temp;
int32_t pressure;
// SDA Đỏ - 21
//SCL Nâu - 22


// Travel_distance
float travel_distance = 0;
unsigned long dem2 = 0;
float duong_kinh_banh_xe = 6.6;

//Encoder
int enco = 13;//D-CAM  A-VÀNG
int dem = 0;
float rpm = 0;
float tocdo = 0;
int timecho = 1000; 
unsigned long thoigian = 0, hientai = 0;
void dem_xung() {
    dem++;
    dem2++;
}



// Định nghĩa chân điều khiển động cơ l298n   den-gnd    trang-vin
const int enA = 2;    // xanh la cay 19 old
const int in1 = 27;    // nau
const int in2 = 26;     // do
const int in3 = 25;     // cam
const int in4 = 33;     // vang
const int enB = 4;    // xanh duong 21 old
int speed = 120;
int delta_speed = -14;
String dieu_khien;



// Cảm biến siêu âm
#define LEFT_TRIG 5 //nau
#define LEFT_ECHO 34  //trang

#define RIGHT_TRIG 23 // Vang
#define RIGHT_ECHO 32 // Xanh la

#define FRONT_TRIG  18// nau 
#define FRONT_ECHO  35// trang 

long leftDistance = 0;
long rightDistance = 0;
long frontDistance = 0;



// Task handle cho task đo khoảng cách
// volatile int distance = 0;
TaskHandle_t flow2TaskHandle;

void setup() {
  Serial.begin(115200);  
  SerialBT.begin("ESP_TEST");
  Serial.println("Bluetooth is ready. Pair with ESP32_BT to start!");

  //GY-87
  Wire.begin(21, 22); // SDA = 21, SCL = 22 (ESP32 default I2C pins)
  Wire.setClock(100000);

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

  mpu.setI2CBypassEnabled(true);  

  // QMC5883L (magnetometer)
  compass.init();

  Serial.println("GY-87 Initialized with Adafruit Libraries");

  //car
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

  //hc-sr
  pinMode(LEFT_TRIG, OUTPUT);
  pinMode(LEFT_ECHO, INPUT);
  pinMode(RIGHT_TRIG, OUTPUT);
  pinMode(RIGHT_ECHO, INPUT);
  pinMode(FRONT_TRIG, OUTPUT);
  pinMode(FRONT_ECHO, INPUT);

  //encoder
  pinMode(enco, INPUT_PULLUP);
  attachInterrupt(digitalPinToInterrupt(enco), dem_xung, RISING);

  // Tạo task đo khoảng cách trên lõi 0
  xTaskCreatePinnedToCore(
    flow2Task, // Hàm thực hiện đo khoảng cách
    "Distance Task",      // Tên task
    1000,                 // Kích thước stack
    NULL,                 // Tham số cho task (không cần)
    1,                    // Mức ưu tiên
    &flow2TaskHandle,  // Task handle
    0                     // Chạy trên lõi 0
  );
}

void loop() {
  thoigian = millis();
  travel_distance = (((float)dem2 / 20.0) * (duong_kinh_banh_xe * 3.14));
    if (thoigian - hientai >= timecho) {
        hientai = thoigian;

        rpm = ((float)dem / 20.0) * 60.0;
        tocdo = ((float)dem / 20.0) * (duong_kinh_banh_xe/100 * 3.14); // Tốc độ (m/s) d*3,14 = cv

        dem = 0;

        Serial.print("RPM: ");
        Serial.println(rpm);
        Serial.print("M/S: ");
        Serial.println(tocdo);

    }
  // SerialBT.printf("Speed: %f; Travel distance: %f\n", tocdo, travel_distance);
  if (SerialBT.available()) {
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
    sendAllInformation();
  }
}

void sendAllInformation()
{
  String message = "";

  // Nối từng phần tùy nhu cầu — có thể bật/tắt bằng cách comment
  message += "SpeedCar: " + String(tocdo, 2) + "; ";
  message += "TravelDistance: " + String(travel_distance, 2) + "\n";
  message += "SpeedMotor: " + String(speed) + "; ";
  message += "Delta_speed: " + String(delta_speed) + "; ";
  message += "Vong: " + String((float)dem2 / 20.0, 2) + "\n";
  // Dữ liệu cảm biến siêu âm
  message += "Ultrasonic: [Left: " + String(leftDistance)
           + "; Right: " + String(rightDistance)
           + "; Front: " + String(frontDistance) + "]\n";

  // Dữ liệu gia tốc
  message += "Accel: [X: " + String(accelX)
           + "; Y: " + String(accelY)
           + "; Z: " + String(accelZ) + "]\n";

  // Dữ liệu con quay hồi chuyển
  message += "Gyro: [X: " + String(gyroX)
           + "; Y: " + String(gyroY)
           + "; Z: " + String(gyroZ) + "]\n";

  // Dữ liệu la bàn
  message += "Compass: [X: " + String(compassX)
           + "; Y: " + String(compassY)
           + "; Z: " + String(compassZ)
           + "; Heading: " + String(compassHeading) + "]\n";

  // Gửi qua Bluetooth
  SerialBT.print(message);
}

void flow2Task(void * parameter) {
  for (;;) {
  // Hàm đo khoảng cách từ cảm biến siêu âm
    leftDistance = getDistance(LEFT_TRIG, LEFT_ECHO);
    rightDistance = getDistance(RIGHT_TRIG, RIGHT_ECHO);
    frontDistance = getDistance(FRONT_TRIG, FRONT_ECHO);
  // GY-87
    mpu.getMotion6(&accelX, &accelY, &accelZ, &gyroX, &gyroY, &gyroZ);
    temp = bmp.readTemperature();
    pressure = bmp.readPressure();
    compass.read();
    compassX = compass.getX();
    compassY = compass.getY();
    compassZ = compass.getZ();
    compassHeading = compass.getAzimuth();

    vTaskDelay(100 / portTICK_PERIOD_MS); // Kiểm tra mỗi 100ms
  };
}

long getDistance(int TRIG_PIN, int ECHO_PIN) {
  digitalWrite(TRIG_PIN, LOW);
  delayMicroseconds(2);
  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(5);
  digitalWrite(TRIG_PIN, LOW);
  long duration = pulseIn(ECHO_PIN, HIGH);
  long distance = int(duration/2/29.412); // Tính khoảng cách (cm)
  return distance;
}

void tien() { dieuKhienDongCo(HIGH, LOW, LOW, HIGH); }
void lui() { dieuKhienDongCo(LOW, HIGH, HIGH, LOW); }
void trai() { dieuKhienDongCo(HIGH, LOW, HIGH, LOW); }
void phai() { dieuKhienDongCo(LOW, HIGH, LOW, HIGH); }
void Stop() { dieuKhienDongCo(LOW, LOW, LOW, LOW); }
void tien_phai() { dieuKhienDongCo(LOW, LOW, LOW, HIGH);}
void tien_trai() { dieuKhienDongCo(HIGH, LOW, LOW, LOW); }
void lui_trai() { dieuKhienDongCo(LOW, HIGH, LOW, LOW);}
void lui_phai() { dieuKhienDongCo(LOW, LOW, HIGH, LOW);}

void dieuKhienDongCo(bool in1_val, bool in2_val, bool in3_val, bool in4_val) {
  digitalWrite(in1, in1_val);
  digitalWrite(in2, in2_val);
  digitalWrite(in3, in3_val);
  digitalWrite(in4, in4_val);
}

