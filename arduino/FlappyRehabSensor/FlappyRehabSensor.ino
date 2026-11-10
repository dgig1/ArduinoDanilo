// Flappy Rehab - HC-SR04 distance sensor
// Wiring (Arduino UNO):
// HC-SR04 VCC -> 5V
// HC-SR04 GND -> GND
// HC-SR04 TRIG -> D9
// HC-SR04 ECHO -> D10
//
// Sends one distance in centimeters per line at 9600 baud.
// In the game, greater distance = character goes up; smaller distance = down.

const int TRIG_PIN = 9;
const int ECHO_PIN = 10;

void setup() {
  pinMode(TRIG_PIN, OUTPUT);
  pinMode(ECHO_PIN, INPUT);
  Serial.begin(9600);
  digitalWrite(TRIG_PIN, LOW);
}

void loop() {
  digitalWrite(TRIG_PIN, LOW);
  delayMicroseconds(3);
  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);

  unsigned long duration = pulseIn(ECHO_PIN, HIGH, 30000UL);
  if (duration == 0) {
    Serial.println("0");
  } else {
    float distanceCm = duration * 0.0343f / 2.0f;
    Serial.println(distanceCm, 1);
  }
  delay(50);
}
