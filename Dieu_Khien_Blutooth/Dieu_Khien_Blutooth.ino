#include "BluetoothSerial.h"
BluetoothSerial SerialBT;

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
const int enA = 19;    // xanh la cay
const int in1 = 27;    // nau
const int in2 = 26;     // do
const int in3 = 25;     // cam
const int in4 = 33;     // vang
const int enB = 21;    // xanh duong
int speed = 120;
int delta_speed = -14;
String dieu_khien;



// Cảm biến siêu âm
#define LEFT_TRIG 12 //nau
#define LEFT_ECHO 34  //trang

#define RIGHT_TRIG 12 // Vang
#define RIGHT_ECHO 32 // Xanh la

#define FRONT_TRIG  12// nau 
#define FRONT_ECHO  35// trang 

long leftDistance = 0;
long rightDistance = 0;
long frontDistance = 0;



// Task handle cho task đo khoảng cách
volatile int distance = 0;
TaskHandle_t distanceTaskHandle;

void setup() {
  Serial.begin(115200);  
  SerialBT.begin("ESP_TEST");
  Serial.println("Bluetooth is ready. Pair with ESP32_BT to start!");

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
    measureDistanceTask, // Hàm thực hiện đo khoảng cách
    "Distance Task",      // Tên task
    1000,                 // Kích thước stack
    NULL,                 // Tham số cho task (không cần)
    1,                    // Mức ưu tiên
    &distanceTaskHandle,  // Task handle
    0                     // Chạy trên lõi 0
  );
}

void loop() {
  // Xử lý tín hiệu Bluetooth từ điện thoại
  //  if (distance > 1 && distance < 30) {
  //   if (digitalRead(in1) == LOW && digitalRead(in2) == HIGH && digitalRead(in3) == LOW && digitalRead(in4) == HIGH) {
      
  //   }
  //   else{
  //     Stop();
  //   }
  // }
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
    SerialBT.printf("Speed of car: %.2f; Travel distance: %.2f; speed of motor: %d\n, Delta_speed: %d, Vòng: %.2f\n leftDistance: %d; rightDistance: %d; frontDistance: %d; ", tocdo, travel_distance, speed, delta_speed, (float)dem2 / 20.0, leftDistance, rightDistance, frontDistance);
  }
}

// Hàm đo khoảng cách từ cảm biến siêu âm
void measureDistanceTask(void * parameter) {
  for (;;) {
    // leftDistance = getDistance(LEFT_TRIG, LEFT_ECHO);
    // rightDistance = getDistance(RIGHT_TRIG, RIGHT_ECHO);
    frontDistance = getDistance(FRONT_TRIG, FRONT_ECHO);

    // distance = frontDistance;  // Mặc định sử dụng cảm biến phía trước

    // // Kiểm tra nếu có vật cản phía trước
    // if (frontDistance > 1 && frontDistance < 30) {
    //   Stop();
    // }

    vTaskDelay(100 / portTICK_PERIOD_MS); // Kiểm tra mỗi 100ms
  }
}

// Hàm đo khoảng cách từ cảm biến siêu âm (không dùng blocking)
long getDistance(int TRIG_PIN, int ECHO_PIN) {
  digitalWrite(TRIG_PIN, LOW);
  delayMicroseconds(2);
  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);
  long duration = pulseIn(ECHO_PIN, HIGH)/10000;
  long distance = duration * 343 / 2; // Tính khoảng cách (cm)
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

