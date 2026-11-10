
const int trigPin = 9;
const int echoPin = 10;

const int NUM_LEITURAS = 5;

float ultimaDistancia = 25.0;

unsigned long ultimaLeituraValida = 0;

const unsigned long TEMPO_TOLERANCIA = 350;

float medirDistancia() {

    digitalWrite(trigPin, LOW);
    delayMicroseconds(2);

    digitalWrite(trigPin, HIGH);
    delayMicroseconds(10);

    digitalWrite(trigPin, LOW);

    long duracao = pulseIn(echoPin, HIGH, 12000);

    if (duracao == 0) {
        return -1;
    }

    float distancia = duracao * 0.0343 / 2.0;

    if (distancia < 3 || distancia > 60) {
        return -1;
    }

    return distancia;
}

// Calcula a mediana das leituras válidas
float calcularMediana(float valores[], int quantidade) {

    for (int i = 0; i < quantidade - 1; i++) {

        for (int j = i + 1; j < quantidade; j++) {

            if (valores[i] > valores[j]) {

                float temp = valores[i];
                valores[i] = valores[j];
                valores[j] = temp;
            }
        }
    }

    return valores[quantidade / 2];
}

void setup() {

    Serial.begin(9600);

    pinMode(trigPin, OUTPUT);
    pinMode(echoPin, INPUT);

    digitalWrite(trigPin, LOW);
}

void loop() {

    float leituras[NUM_LEITURAS];

    int quantidadeValidas = 0;

    for (int i = 0; i < NUM_LEITURAS; i++) {

        float distancia = medirDistancia();

        if (distancia > 0) {

            leituras[quantidadeValidas] = distancia;
            quantidadeValidas++;
        }

        delay(12);
    }

    // Precisamos de pelo menos 3 leituras válidas
    if (quantidadeValidas >= 3) {

        float mediana = calcularMediana(
            leituras,
            quantidadeValidas
        );

        ultimaDistancia = mediana;
        ultimaLeituraValida = millis();

        Serial.println(ultimaDistancia);

    } else {

        // Mantém a última leitura por um curto período
        if (millis() - ultimaLeituraValida <
                TEMPO_TOLERANCIA) {

            Serial.println(ultimaDistancia);

        } else {

            // 200 significa que a mão não foi detectada
            Serial.println(200);
        }
    }

    delay(30);
}
