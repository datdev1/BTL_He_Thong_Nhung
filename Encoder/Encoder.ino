#define BUTTON_Start 32
int enable_measure_distance = 0;
float distance = 0;
unsigned long dem2 = 0;

int enco = 2;  //D-CAM  A-VÀNG

int dem = 0;
float rpm = 0;
float tocdo = 0;

int timecho = 1000; // Khoảng thời gian cập nhật (1 giây)
unsigned long thoigian;
unsigned long hientai = 0;

void dem_xung() {
    dem++;
    dem2++;
}

void setup() {
    Serial.begin(9600);
    pinMode(BUTTON_Start, INPUT_PULLUP);
    pinMode(enco, INPUT_PULLUP);
    attachInterrupt(digitalPinToInterrupt(enco), dem_xung, RISING);
}

void loop() {
    thoigian = millis();
    distance = (((float)dem2 / 20.0) * (6.5 * 3.14));
    if (thoigian - hientai >= timecho) {
        
        rpm = ((float)dem / 20.0) * (float)(thoigian - hientai);
        tocdo = (((float)dem / 20.0) * (0.066 * 3.14)) / (float)(thoigian - hientai) *1000 ; // Tốc độ (m/s)

        Serial.printf("\nDiff time: %f\n", (float)(thoigian - hientai));
        Serial.print("RPM: ");
        Serial.println(rpm);
        Serial.print("M/S: ");
        Serial.println(tocdo);
        Serial.printf("\nDistance: %f\n", distance);

        dem = 0;
        hientai = thoigian;
    }

    if (digitalRead(BUTTON_Start) == LOW)
    {
        distance = 0;
        dem2 = 0;
        Serial.println("Reset!!!");
    }
}
