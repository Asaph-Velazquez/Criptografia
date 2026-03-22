package com.mycompany.mcdinversom;

import java.util.ArrayList;
import java.util.Scanner;

public class MCDInversoM {
  
    public static class RastroEuclides {
        public ArrayList<Integer> dividendos = new ArrayList<>();
        public ArrayList<Integer> divisores = new ArrayList<>();
        public ArrayList<Integer> cocientes = new ArrayList<>();
        public ArrayList<Integer> residuos = new ArrayList<>();
        public StringBuilder log = new StringBuilder(); // NECESARIO para la GUI xd
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Ingresa alfa: ");
        int valorAlfa = sc.nextInt();
        System.out.print("Ingresa n (módulo): ");
        int moduloN = sc.nextInt();

        RastroEuclides rastro = new RastroEuclides();
        int mcdIndex = ejecutarEuclides(valorAlfa, moduloN, rastro);
        if (mcdIndex >= 0 && rastro.residuos.get(mcdIndex) == 1) {
            System.out.println(rastro.log.toString());
            int inv = ejecutarSustitucion(valorAlfa, moduloN, rastro, mcdIndex);
            System.out.println(rastro.log.toString());
        }
        sc.close();
    }

    public static int ejecutarEuclides(int valorAlfa, int moduloN, RastroEuclides rastro) {
        int residuoPrevio = moduloN;  
        int residuoActual = valorAlfa; 
        char letra = 'a';
        
        rastro.log.append("--- Algoritmo de Euclides ---\n");
        while (residuoActual > 0) {
            int cocienteTemporal = residuoPrevio / residuoActual;
            int residuoSiguiente = residuoPrevio % residuoActual;
            
            rastro.dividendos.add(residuoPrevio);
            rastro.divisores.add(residuoActual);
            rastro.cocientes.add(cocienteTemporal);
            rastro.residuos.add(residuoSiguiente);

            if (residuoSiguiente != 0) {
                rastro.log.append(String.format("%d = %d(%d) + %d \t -> %d = %d - %d(%d) \t %c\n", 
                residuoPrevio, residuoActual, cocienteTemporal, residuoSiguiente,
                residuoSiguiente, residuoPrevio, residuoActual, cocienteTemporal, letra++));
            } else {
                rastro.log.append(String.format("%d = %d(%d) + %d\n", residuoPrevio, residuoActual, cocienteTemporal, residuoSiguiente));
            }
            residuoPrevio = residuoActual;
            residuoActual = residuoSiguiente;
        }
        return rastro.residuos.size() - 2; 
    }

    public static int ejecutarSustitucion(int valorAlfa, int moduloN, RastroEuclides rastro, int mcdIndex) {
        rastro.log.setLength(0); 
        rastro.log.append("\nSustitución y Factorización\n");
        
        int base1 = rastro.dividendos.get(mcdIndex);
        int coef1 = 1;
        int base2 = rastro.divisores.get(mcdIndex);
        int coef2 = -rastro.cocientes.get(mcdIndex);

        rastro.log.append(String.format("Ec. Base: 1 = %d(%d) + %d(%d)\n", base1, coef1, base2, coef2));

        for (int i = mcdIndex - 1; i >= 0; i--) {
            char letraSust = (char)('a' + i);
            if (i == mcdIndex - 1) rastro.log.append("\nSust ").append(letraSust).append(" en ").append((char)('a' + mcdIndex)).append("\n");
            else rastro.log.append("\nSust ").append(letraSust).append("\n");

            int dividendoHist = rastro.dividendos.get(i);
            int divisorHist = rastro.divisores.get(i);
            int cocienteHist = rastro.cocientes.get(i);
            int residuoA_Sustituir = rastro.residuos.get(i);

            if (base2 == residuoA_Sustituir) {
                String t1 = base1 + "(" + coef1 + ")";
                rastro.log.append(String.format("1 = %s + [%d - %d(%d)](%d)\n", t1, dividendoHist, divisorHist, cocienteHist, coef2));
                
                int dist1 = coef2; 
                int dist2 = -cocienteHist * coef2; 
                
                String strDist1 = "+ " + dividendoHist + "(" + dist1 + ")";
                String strDist2 = "+ " + divisorHist + "(" + dist2 + ")";
                rastro.log.append(String.format("1 = %s %s %s\n", t1, strDist1, strDist2));
                
                coef1 = coef1 + dist2; 
                base2 = dividendoHist; 
                coef2 = dist1;
                
                rastro.log.append(String.format("1 = %d(%d) + %d(%d)\n", base1, coef1, base2, coef2));
                
            } else if (base1 == residuoA_Sustituir) {
                String t2 = "+ " + base2 + "(" + coef2 + ")";
                rastro.log.append(String.format("1 = [%d - %d(%d)](%d) %s\n", dividendoHist, divisorHist, cocienteHist, coef1, t2));
                
                int dist1 = coef1; 
                int dist2 = -cocienteHist * coef1; 
                
                String strDist1 = dividendoHist + "(" + dist1 + ")"; 
                String strDist2 = "+ " + divisorHist + "(" + dist2 + ")";
                rastro.log.append(String.format("1 = %s %s %s\n", strDist1, strDist2, t2));
                
                coef2 = coef2 + dist2; 
                base1 = dividendoHist;
                coef1 = dist1;
                
                rastro.log.append(String.format("1 = %d(%d) + %d(%d)\n", base1, coef1, base2, coef2));
            }
            
            int val1 = base1 * coef1;
            int val2 = base2 * coef2;
            rastro.log.append(String.format("1 = %d %s %d  v\n", val1, (val2 < 0 ? "-" : "+"), Math.abs(val2)));
        }

        // --- CÁLCULO FINAL Y EXPLICACIÓN DEL MÓDULO xd ---
        int inversoCrudo = (base1 == valorAlfa) ? coef1 : coef2;
        int alfaInverso = (inversoCrudo % moduloN + moduloN) % moduloN;
        
        rastro.log.append("\n-------------------------------------------------\n");
        if (inversoCrudo < 0) {
            rastro.log.append(String.format("Inverso preliminar = %d\n", inversoCrudo));
            rastro.log.append(String.format("Ajuste a positivo: %d mod %d = (%d + %d) mod %d = %d\n", 
                inversoCrudo, moduloN, inversoCrudo, moduloN, moduloN, alfaInverso));
        } else {
            rastro.log.append(String.format("Inverso preliminar = %d\n", inversoCrudo));
            rastro.log.append(String.format("El inverso ya es positivo en Z_%d.\n", moduloN));
        }
        
        rastro.log.append(String.format("=> Inverso Multiplicativo Final (α^-1) = %d\n", alfaInverso));
        
        return alfaInverso;
    }
}