#include "BluetoothSerial.h"
#include "MPU6050_6Axis_MotionApps20.h"
BluetoothSerial SerialBT;

  bool isMove = false;
  bool isRoll = false;
  bool isRollRight = false;
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
  
  VectorInt16 aaReal;     // [x, y, z]            Gravity-free accel sensor measurements
  float ypr[3];           // [yaw, pitch, roll]   Yaw/Pitch/Roll container and gravity vector


float alphaEn = 1;
float travel_distance = 0;
unsigned long dem2 = 0;
float duong_kinh_banh_xe = 6.6;

// Encoder
int dem = 0;
float rpm = 0;
float tocdo = 0;
int timecho = 1000;
unsigned long thoigian = 0, hientai = 0;


int speed = 120;
int delta_speed = -14;
String dieu_khien;
TaskHandle_t flowTaskHandle;
// Cảm biến siêu âm

long leftDistance = 0;
long rightDistance = 0;
long frontDistance = 0;

void setup()
{
  Serial.begin(115200);
  SerialBT.begin("ESP_TEST");
  Serial.println("Bluetooth is ready. Pair with ESP32_BT to start!");
  xTaskCreatePinnedToCore(
    flowTask, // Hàm thực hiện đo khoảng cách
    "Flow Task",      // Tên task
    5000,                 // Kích thước stack
    NULL,                 // Tham số cho task (không cần)
    1,                    // Mức ưu tiên
    &flowTaskHandle,  // Task handle
    0                     // Chạy trên lõi 0
  );
}

void loop()
{
  thoigian = millis() - hientai;
  travel_distance = (((float)dem2 / 20.0) * (duong_kinh_banh_xe * 3.14)) * alphaEn;

  
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
      // tien_phai();
      phai();
    }
    else if (dieu_khien == "FL")
    {
      // tien_trai();
      trai();
    }
    else if (dieu_khien == "BL")
    {
      // lui_phai();
      phai();
    }
    else if (dieu_khien == "BR")
    {
      // lui_trai();
      trai();
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
    }
    else if (dieu_khien.indexOf("Delta_speed") != -1)
    {
      String set_delta_speed = dieu_khien.substring(12, dieu_khien.length());
      delta_speed = set_delta_speed.toInt();
    }
    else if (dieu_khien.indexOf("alphaEn") != -1)
    {
      String set_alpha_encoder = dieu_khien.substring(8, dieu_khien.length());
      alphaEn = set_alpha_encoder.toFloat();
    }
    else if (dieu_khien == "music"){
      
    }
    else if (dieu_khien == "calculatingCalibration")
    {
      isCalibration = true;
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
    message += "TravelDis: " + String(travel_distance, 2) + "\n";
    message += "SpeedMotor: " + String(speed) + "; ";
    message += "Delta_speed: " + String(delta_speed) + "; ";
    message += "Vong: " + String((float)dem2 / 20.0, 2) + "\n";
    // Dữ liệu cảm biến siêu âm
    message += "Sonic: [L: " + String(leftDistance) + "; R: " + String(rightDistance) + "; F: " + String(frontDistance) + "]\n";

    //Gia tốc bỏ qua gia tốc trọng trường
    message += "Accel: [X: " + String(aaReal.x) + "; Y: " + String(aaReal.y) + "; Z: " + String(aaReal.z) + "]\n";

    // Dữ liệu con quay hồi chuyển
    message += "Gyro: [X: " + String(gyroX) + "; Y: " + String(gyroY) + "; Z: " + String(gyroZ) + "]\n";
    
    message += "YPR: [Y: " + String(ypr[0], 2) + "; P: " + String(0.12, 2) + "; R: " + String(0.28, 2) + "]\n";

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
    leftDistance = random(1, 50);
    rightDistance = random(1, 50);
    frontDistance = random(1, 50);
    gy87Task();
    dem_xung();
    vTaskDelay(100 / portTICK_PERIOD_MS); // Kiểm tra mỗi 100ms
  };
}

void dem_xung()
{
  if(isMove)
  {
    dem += (int)speed/12;
    dem2 += (int)speed/12;
  }
}

void gy87Task()
{
    if(isMove)
    {
      aaReal.x = (int)random(0, 1000)/100;
      aaReal.y = (int)(random(0, 1000)/100 + speed/10);
      aaReal.z = (int)random(0, 1000)/100;
    }
    // message += "Temp: " + String(24) + "'C; Pres: " + String(1010.20) + "Pa; Alti" + String(3.26, 2) + "m\n";
    temp = (float)random(2400, 2500) / 100;
    pressure = (float)random(101000, 101999);
    if(isRoll)
    {
      compassX = random(-1000, 1000);
      compassY = random(-1000, 1000);
      compassZ = random(-1000, 1000);
      compassHeading = random(-179, 179);
    }
    calAngle();
};


void tien() { 
  isMove = true;
  isRoll = false;
}
void lui() {
  isMove = true;
  isRoll = false;
}
void trai() {
  isMove = true;
  isRoll = true;
  isRollRight = false;
}
void phai() {
  isMove = true;
  isRoll = true;
  isRollRight = true;
}

void Stop() { 
  isMove = false;
  isRoll = false;
 }



void calculatingCalibration()
{
  if(isCalibration)
  {
    compassX = random(-1000, 1000);
    compassY = random(-1000, 1000);
    compassZ = random(-1000, 1000);

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
  isCalibration = false;
}

void calAngle()
{
  if(isMove)
  {
    aaReal.x = (int)random(0, 1000)/100;
    aaReal.y = (int)(random(0, 1000)/100 + speed/10);
    aaReal.z = (int)random(0, 1000)/100;
  }
  if(isRoll)
  {
    if(isRollRight)
    {
      ypr[0] += 0.01*speed/100;
      ypr[1] = 0.12;
      ypr[2] = 0/36;
    }
    else
    {
      ypr[0] -= 0.01*speed/100;
      ypr[1] = -0.12;
      ypr[2] = -0/36;
    }
  }
}

float calculateAltitude(float pressure, float temperature) {
  const float P0 = 1013.25; // Áp suất chuẩn ở mực nước biển (hPa)
  return (temperature + 273.15) / 0.0065 * (1 - pow(pressure / P0, 1.0 / 5.257));
}