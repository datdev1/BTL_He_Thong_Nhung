#include "BluetoothSerial.h"

BluetoothSerial SerialBT; // Khởi tạo đối tượng Bluetooth Serial

// Cảm biến siêu âm
#define LEFT_TRIG 13  //white
#define LEFT_ECHO 12  //brown

#define RIGHT_TRIG 32 // Xanh duong
#define RIGHT_ECHO 35 // Tims

#define FRONT_TRIG  4// brown 
#define FRONT_ECHO  2// white 

// Định nghĩa chân điều khiển động cơ
const int enA = 19;    // xanh la cay
const int in1 = 27;    // nau
const int in2 = 26;     // do
const int in3 = 25;     // cam
const int in4 = 33;     // vang
const int enB = 21;    // xanh duong

int speed = 200;
volatile int distance = 0; // Biến đo khoảng cách
char dieu_khien;

// Task handle cho task đo khoảng cách
TaskHandle_t distanceTaskHandle;

void setup() {
  Serial.begin(115200);  
  SerialBT.begin("ESP32_BT");
  Serial.println("Bluetooth is ready. Pair with ESP32_BT to start!");
  pinMode(enA, OUTPUT);
  pinMode(in1, OUTPUT);
  pinMode(in2, OUTPUT);
  
  pinMode(enB, OUTPUT);
  pinMode(in3, OUTPUT);
  pinMode(in4, OUTPUT);
  
  pinMode(LEFT_TRIG, OUTPUT);
  pinMode(LEFT_ECHO, INPUT);
  pinMode(RIGHT_TRIG, OUTPUT);
  pinMode(RIGHT_ECHO, INPUT);
  pinMode(FRONT_TRIG, OUTPUT);
  pinMode(FRONT_ECHO, INPUT);

  digitalWrite(in1, LOW);
  digitalWrite(in2, LOW);
  digitalWrite(in3, LOW);
  digitalWrite(in4, LOW);
  analogWrite(enA, speed);
  analogWrite(enB, speed);

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
   if (distance > 1 && distance < 30) {
    if (digitalRead(in1) == LOW && digitalRead(in2) == HIGH && digitalRead(in3) == LOW && digitalRead(in4) == HIGH) {
      
    }
    else{
      Stop();
    }
  }
  if (SerialBT.available()) {
    dieu_khien = SerialBT.read();
    Serial.println(dieu_khien);
    switch (dieu_khien) 
    {
      case 'F':
        if (distance > 1 && distance < 30) Stop();
        else{
              tien();
              SerialBT.println(distance);
        } 
        break;
      case 'B':
        SerialBT.println(distance);
        lui();
        break;
      case 'L':
        trai();
        SerialBT.println(distance);
        break;
      case 'l':
        trai_1();
        SerialBT.println(distance);
        break;
      case 'R':
        phai();
        SerialBT.println(distance);
        break;
      case 'r':
        phai_1();
        SerialBT.println(distance);
        break;
      case 'I':
        tien_phai();
        SerialBT.println(distance);
        break;
      case 'G':
        tien_trai();
        SerialBT.println(distance);
        break;
      case 'J':
        lui_phai();
        SerialBT.println(distance);
        break;
      case 'H':
        lui_trai();
        SerialBT.println(distance);
        break;
      case 'S':
        Stop();
        SerialBT.println(distance);
        break;
      case '0':
        SerialBT.println(distance);
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

// Các hàm điều khiển động cơ
void dieuKhienDongCo(bool in1_val, bool in2_val, bool in3_val, bool in4_val) {
  digitalWrite(in1, in1_val);
  digitalWrite(in2, in2_val);
  digitalWrite(in3, in3_val);
  digitalWrite(in4, in4_val);
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
void phai_1() { dieuKhienDongCo(HIGH, LOW, HIGH, LOW); delay(500); tien();}
void trai_1() { dieuKhienDongCo(LOW, HIGH, LOW, HIGH); delay(500); tien();}

// void tien() { dieuKhienDongCo(HIGH, LOW, HIGH, LOW); }
// void lui() { dieuKhienDongCo(LOW, HIGH, LOW, HIGH); }
// void phai() { dieuKhienDongCo(HIGH, LOW, LOW, HIGH); }
// void trai() { dieuKhienDongCo(LOW, HIGH, HIGH, LOW); }
// void Stop() { dieuKhienDongCo(LOW, LOW, LOW, LOW); }
// void tien_trai() { dieuKhienDongCo(LOW, LOW, HIGH, LOW);}
// void tien_phai() { dieuKhienDongCo(HIGH, LOW, LOW, LOW); }
// void lui_phai() { dieuKhienDongCo(LOW, HIGH, LOW, LOW);}
// void lui_trai() { dieuKhienDongCo(LOW, LOW, LOW, HIGH);}
// void phai_1() { dieuKhienDongCo(HIGH, LOW, LOW, HIGH); delay(500); tien();}
// void trai_1() { dieuKhienDongCo(LOW, HIGH, HIGH, LOW); delay(500); tien();}

