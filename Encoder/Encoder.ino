int enco = 2;  //D-CAM  A-VÀNG

int dem = 0;
float rpm = 0;
float tocdo = 0;

int timecho = 1000; // Khoảng thời gian cập nhật (1 giây)
unsigned long thoigian;
unsigned long hientai = 0;

void dem_xung() {
    dem++;
}

void setup() {
    Serial.begin(9600);

    pinMode(enco, INPUT_PULLUP);
    attachInterrupt(digitalPinToInterrupt(enco), dem_xung, RISING);
}

void loop() {
    thoigian = millis();
    if (thoigian - hientai >= timecho) {
        hientai = thoigian;

        rpm = ((float)dem / 20.0) * 60.0;
        tocdo = ((float)dem / 20.0) * (0.025 * 3.14); // Tốc độ (m/s)

        dem = 0;

        Serial.print("RPM: ");
        Serial.println(rpm);
        Serial.print("M/S: ");
        Serial.println(tocdo);
    }
}
