/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.mcdinversom;
import java.util.Scanner;
import java.util.ArrayList;
/**
 *
 * @author ricar
 */
public class MCDInversoM {
public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Tus 3 variables base
        System.out.print("Ingresa alfa: ");
        int alfa = sc.nextInt();

        System.out.print("Ingresa Beta: ");
        int Beta = sc.nextInt();
        
        System.out.print("Ingresa n: ");
        int m = sc.nextInt();

        // Estructuras dinámicas para TODO el rastro
        ArrayList<Integer> cocientes = new ArrayList<>();
        ArrayList<Integer> residuos = new ArrayList<>();
        ArrayList<Integer> resultados = new ArrayList<>();

        int rPrev = m;    // r_{-1}
        int rCurr = alfa; // r_{0}

        // Agregamos los valores iniciales a la secuencia de resultados
        resultados.add(rPrev);
        resultados.add(rCurr);

        System.out.println("\n--- Trazado de Euclides ---");
        
        while (rCurr > 0) {
            int q = rPrev / rCurr;
            int rNext = rPrev % rCurr;

            // Guardamos absolutamente todo
            cocientes.add(q);
            residuos.add(rNext);
            
            // Solo guardamos en resultados si no es el cero final (para tu salida específica)
            if (rNext > 0) {
                resultados.add(rNext);
            }

            System.out.println(rPrev + " = (" + q + " * " + rCurr + ") + " + rNext);

            // Desplazamiento
            rPrev = rCurr;
            rCurr = rNext;
        }

        // --- SALIDA FINAL SOLICITADA ---
        System.out.println("\nResultados (Secuencia de residuos):");
        for (int res : resultados) {
            System.out.print(res + " ");
        }
        
        System.out.println("\n\nCocientes (q):");
        System.out.println(cocientes);

        System.out.println("\nResiduos finales (r):");
        System.out.println(residuos);

        sc.close();
    }
}
