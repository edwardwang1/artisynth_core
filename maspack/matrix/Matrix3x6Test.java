/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.RandomGenerator;
import maspack.util.TestException;
import maspack.util.FunctionTimer;

class Matrix3x6Test extends MatrixTest {
   void add (Matrix MR, Matrix M1) {
      ((Matrix3x6)MR).add ((Matrix3x6)M1);
   }

   void add (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix3x6)MR).add ((Matrix3x6)M1, (Matrix3x6)M2);
   }

   void sub (Matrix MR, Matrix M1) {
      ((Matrix3x6)MR).sub ((Matrix3x6)M1);
   }

   void sub (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix3x6)MR).sub ((Matrix3x6)M1, (Matrix3x6)M2);
   }

   void scale (Matrix MR, double s, Matrix M1) {
      ((Matrix3x6)MR).scale (s, (Matrix3x6)M1);
   }

   void scale (Matrix MR, double s) {
      ((Matrix3x6)MR).scale (s);
   }

   void scaledAdd (Matrix MR, double s, Matrix M1) {
      ((Matrix3x6)MR).scaledAdd (s, (Matrix3x6)M1);
   }

   void scaledAdd (Matrix MR, double s, Matrix M1, Matrix M2) {
      ((Matrix3x6)MR).scaledAdd (s, (Matrix3x6)M1, (Matrix3x6)M2);
   }

   void setZero (Matrix MR) {
      ((Matrix3x6)MR).setZero();
   }

   void set (Matrix MR, Matrix M1) {
      ((Matrix3x6)MR).set ((Matrix3x6)M1);
   }

   void mulAdd (Matrix MR, Matrix M1, Matrix M2) {
      ((Matrix3x6)MR).mulAdd (M1, M2);
   }

   public void execute() {
      Matrix3x6 MR = new Matrix3x6();
      Matrix3x6 M1 = new Matrix3x6();
      Matrix3x6 M2 = new Matrix3x6();

      RandomGenerator.setSeed (0x1234);

      testGeneric (M1);
      testSetZero (MR);

      for (int i = 0; i < 10; i++) {
         M1.setRandom();
         M2.setRandom();
         MR.setRandom();

         testAdd (MR, M1, M2);
         testAdd (MR, MR, MR);

         testSub (MR, M1, M2);
         testSub (MR, MR, MR);

         testScale (MR, 1.23, M1);
         testScale (MR, 1.23, MR);

         testScaledAdd (MR, 1.23, M1, M2);
         testScaledAdd (MR, 1.23, MR, MR);

         testSet (MR, M1);
         testSet (MR, MR);

         testNorms (M1);

         testMulAdd (MR);
      }
   }

   public static void main (String[] args) {
      Matrix3x6Test test = new Matrix3x6Test();

      try {
         test.execute();
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }

      System.out.println ("\nPassed\n");

      if (false) {
         FunctionTimer timer = new FunctionTimer();
         MatrixNd M1_3x3N = new MatrixNd (3, 3);
         MatrixNd M2_3x6N = new MatrixNd (3, 6);

         Matrix3d M1_3x3 = new Matrix3d();
         Matrix3x6 M2_3x6 = new Matrix3x6();

         M1_3x3N.setRandom();
         M2_3x6N.setRandom();
         M1_3x3.setRandom();
         M2_3x6.setRandom();
         Matrix3x6 MR = new Matrix3x6();

         int cnt = 5000000;
         for (int i=0; i<cnt; i++) {
            MR.setZero();
            MR.mulAdd (M1_3x3, M2_3x6);
            MR.mulAdd (M1_3x3N, M2_3x6N);
         }

         timer.start();
         for (int i=0; i<cnt; i++) {
            MR.setZero();
            MR.mulAdd (M1_3x3, M2_3x6);
         }
         timer.stop();
         System.out.println ("fixed matrices: " + timer.result(cnt));

         timer.start();
         for (int i=0; i<cnt; i++) {
            MR.setZero();
            MR.mulAdd (M1_3x3N, M2_3x6N);
         }
         timer.stop();
         System.out.println ("general matrices: " + timer.result(cnt));
      }

   }
}
