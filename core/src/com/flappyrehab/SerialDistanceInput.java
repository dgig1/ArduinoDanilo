package com.flappyrehab;

import com.fazecast.jSerialComm.SerialPort;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Reads one distance in centimeters per line from Arduino over USB serial.
 * Accepted formats: "23.4", "DIST:23.4", or "distance=23.4".
 */
public class SerialDistanceInput {
    private final String portName;
    private final AtomicReference<Double> distanceCm = new AtomicReference<>(Double.NaN);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile SerialPort port;
    private volatile boolean connected;
    private volatile String status = "Arduino desconectado";

    public SerialDistanceInput(String portName) {
        this.portName = portName;
    }

    public void start() {
        Thread thread = new Thread(this::readLoop, "arduino-serial-reader");
        thread.setDaemon(true);
        thread.start();
    }

    private void readLoop() {
        running.set(true);
        try {
            port = SerialPort.getCommPort(portName);
            port.setBaudRate(9600);
            port.setNumDataBits(8);
            port.setNumStopBits(SerialPort.ONE_STOP_BIT);
            port.setParity(SerialPort.NO_PARITY);
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 500, 0);
            if (!port.openPort()) {
                status = "Nao foi possivel abrir " + portName + " (feche o Monitor Serial)";
                return;
            }
            connected = true;
            status = "Arduino conectado: " + portName;
            StringBuilder line = new StringBuilder();
            byte[] buffer = new byte[1];
            while (running.get()) {
                int n = port.readBytes(buffer, 1);
                if (n <= 0) continue;
                char c = (char) (buffer[0] & 0xff);
                if (c == '\n' || c == '\r') {
                    String value = line.toString().trim();
                    line.setLength(0);
                    if (!value.isEmpty()) parseDistance(value);
                } else if (line.length() < 80) {
                    line.append(c);
                } else {
                    line.setLength(0);
                }
            }
        } catch (Exception e) {
            status = "Erro serial: " + e.getMessage();
        } finally {
            connected = false;
            if (port != null && port.isOpen()) port.closePort();
        }
    }

    private void parseDistance(String line) {
        String value = line.replace(',', '.').replaceAll("(?i)^(dist|distance)\\s*[:=]\\s*", "").trim();
        try {
            double d = Double.parseDouble(value);
            if (d >= 2 && d <= 400) distanceCm.set(d);
        } catch (NumberFormatException ignored) {
            // Ignore non-distance lines.
        }
    }

    public double getDistanceCm() { return distanceCm.get(); }
    public boolean isConnected() { return connected; }
    public String getStatus() { return status; }
    public String getPortName() { return portName; }

    public void close() {
        running.set(false);
        if (port != null && port.isOpen()) port.closePort();
    }
}
