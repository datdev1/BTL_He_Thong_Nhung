#include "BluetoothSerial.h"
BluetoothSerial SerialBT;

// Travel_distance
float travel_distance = 0;
unsigned long dem2 = 0;

//Encoder
int enco = 2;
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
int speed = 200;
char dieu_khien;


// Cảm biến siêu âm
#define LEFT_TRIG 13  //trang
#define LEFT_ECHO 12  //nau

#define RIGHT_TRIG 32 // Xanh duong
#define RIGHT_ECHO 35 // tim

#define FRONT_TRIG  4// nau 
#define FRONT_ECHO  2// trang 



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
  travel_distance = (((float)dem2 / 20.0) * (6.5 * 3.14));
    if (thoigian - hientai >= timecho) {
        hientai = thoigian;

        rpm = ((float)dem / 20.0) * 60.0;
        tocdo = ((float)dem / 20.0) * (0.066 * 3.14); // Tốc độ (m/s) d*3,14 = cv

        dem = 0;

        Serial.print("RPM: ");
        Serial.println(rpm);
        Serial.print("M/S: ");
        Serial.println(tocdo);

    }
  if (SerialBT.available()) {
    dieu_khien = SerialBT.read();
    // Serial.println(dieu_khien);
    SerialBT.printf("Speed: %f; Travel distance: %f\n", tocdo, travel_distance);
    switch (dieu_khien) 
    {
      case 'F':
        if (distance > 1 && distance < 30) Stop();
        else{
              tien();
              // SerialBT.println(distance);
        } 
        break;
      case 'B':
        // SerialBT.println(distance);
        lui();
        break;
      case 'L':
        trai();
        // SerialBT.println(distance);
        break;
      case 'R':
        phai();
        // SerialBT.println(distance);
        break;
      case 'I':
        tien_phai();
        // SerialBT.println(distance);
        break;
      case 'G':
        tien_trai();
        // SerialBT.println(distance);
        break;
      case 'J':
        lui_phai();
        // SerialBT.println(distance);
        break;
      case 'H':
        lui_trai();
        // SerialBT.println(distance);
        break;
      case 'S':
        Stop();
        // SerialBT.println(distance);
        break;
      case 'D':
        distance = 0;
        dem2 = 0;
        Serial.println("Reset!!!");
      case '0':
        // SerialBT.print("RPM: "); SerialBT.println(rpm);
        // SerialBT.print("Speed (m/s): "); SerialBT.println(tocdo);
        // SerialBT.println(distance);
        Serial.print("Distance send to server");
        Serial.println(distance);
        break;
    }
  }
}

// Hàm đo khoảng cách từ cảm biến siêu âm
void measureDistanceTask(void * parameter) {
  for (;;) {
    int leftDistance = getDistance(LEFT_TRIG, LEFT_ECHO);
    int rightDistance = getDistance(RIGHT_TRIG, RIGHT_ECHO);
    int frontDistance = getDistance(FRONT_TRIG, FRONT_ECHO);

    distance = frontDistance;  // Mặc định sử dụng cảm biến phía trước

    // Kiểm tra nếu có vật cản phía trước
    if (frontDistance > 1 && frontDistance < 30) {
      Stop();
    }

    vTaskDelay(100 / portTICK_PERIOD_MS); // Kiểm tra mỗi 100ms
  }
}

// Hàm đo khoảng cách từ cảm biến siêu âm (không dùng blocking)
int getDistance(int TRIG_PIN, int ECHO_PIN) {
  digitalWrite(TRIG_PIN, LOW);
  delayMicroseconds(2);
  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);
  long duration = pulseIn(ECHO_PIN, HIGH);
  int distance = duration * 0.0343 / 2; // Tính khoảng cách (cm)
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

