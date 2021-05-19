package com.example.tfg_teleasistencia_2021;

import java.util.Collections;
import java.util.LinkedList;

public class Window {

    protected final int TAMANHO_DEFAULT = 10;
    protected final double UMBRAL = 8;

    protected final int TAMANHO;

    protected LinkedList<Double> valores;

    public Window() {
        TAMANHO = TAMANHO_DEFAULT;
        valores = new LinkedList<>();
    }

    public Window(int size) {
        TAMANHO = size;
        valores = new LinkedList<>();
    }

    public void add(double value) {
        if(!isFull()) {
            valores.add(new Double(value));
        } else {
            valores.removeFirst();
            valores.add(new Double(value));
        }
    }

    public void clear() {
        valores.clear();
    }

    public Boolean isFull() {
        return (valores.size() > TAMANHO);
    }

    public Boolean isFallDetected() {
        double max = Collections.max(valores);
        double min = Collections.min(valores);
        double diff = Math.abs(max - min);

        // Comprueba si el valor minimo se detecto primero que el maximo
        Boolean isFall = (valores.indexOf(max) > valores.indexOf(min));

        //Si primero se detecto el valor mayor(caída) que el menor(valor normal) y la diferencia de aceleración entre el más alto y el más bajo es mayor que el umbral, hay caida
        return (diff > UMBRAL && isFall);
    }
}
