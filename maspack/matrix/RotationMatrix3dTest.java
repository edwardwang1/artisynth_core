/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.util.RandomGenerator;

class RotationMatrix3dTest extends MatrixTest {
   void mul (Matrix MR, Matrix M1) {
      ((RotationMatrix3d)MR).mul ((RotationMatrix3d)M1);
   }

   void mul (Matrix MR, Matrix M1, Matrix M2) {
      ((RotationMatrix3d)MR).mul ((RotationMatrix3d)M1, (RotationMatrix3d)M2);
   }

   void mulInverse (Matrix MR, Matrix M1) {
      ((RotationMatrix3d)MR).mulInverse ((RotationMatrix3d)M1);
   }

   void mulInverseRight (Matrix MR, Matrix M1, Matrix M2) {
      ((RotationMatrix3d)MR).mulInverseRight (
         (RotationMatrix3d)M1, (RotationMatrix3d)M2);
   }

   void mulInverseLeft (Matrix MR, Matrix M1, Matrix M2) {
      ((RotationMatrix3d)MR).mulInverseLeft (
         (RotationMatrix3d)M1, (RotationMatrix3d)M2);
   }

   void mulInverseBoth (Matrix MR, Matrix M1, Matrix M2) {
      ((RotationMatrix3d)MR).mulInverseBoth (
         (RotationMatrix3d)M1, (RotationMatrix3d)M2);
   }

   void invert (Matrix MR) {
      ((RotationMatrix3d)MR).invert();
   }

   void invert (Matrix MR, Matrix M1) {
      ((RotationMatrix3d)MR).invert ((RotationMatrix3d)M1);
   }

   void transpose (Matrix MR) {
      ((RotationMatrix3d)MR).transpose();
   }

   void transpose (Matrix MR, Matrix M1) {
      ((RotationMatrix3d)MR).transpose ((RotationMatrix3d)M1);
   }

   void negate (Matrix MR) {
      ((RotationMatrix3d)MR).negate();
   }

   void negate (Matrix MR, Matrix M1) {
      ((RotationMatrix3d)MR).negate ((RotationMatrix3d)M1);
   }

   void set (Matrix MR, Matrix M1) {
      ((RotationMatrix3d)MR).set ((RotationMatrix3d)M1);
   }

   void testSetRotations (RotationMatrix3d RR, RotationMatrix3d R1) {
      AxisAngle axisAng = new AxisAngle();
      Quaternion quat = new Quaternion();
      double[] angs = new double[3];

      saveResult (RR);
      MX.set (R1);
      R1.getRpy (angs);
      RR.setRpy (angs);
      checkAndRestoreResult (RR, EPSILON);

      saveResult (RR);
      MX.set (R1);
      R1.getEuler (angs);
      RR.setEuler (angs);
      checkAndRestoreResult (RR, EPSILON);

      saveResult (RR);
      MX.set (R1);
      R1.getAxisAngle (axisAng);
      RR.setAxisAngle (axisAng);
      checkAndRestoreResult (RR, EPSILON);

      saveResult (RR);
      MX.set (R1);
      R1.getAxisAngle (axisAng);
      quat.set (R1);
      axisAng.set (quat);
      quat.setAxisAngle (axisAng);
      RR.set (quat);
      RR.getAxisAngle (axisAng);
      checkAndRestoreResult (RR, EPSILON);
   }

   void setZDirectionCheck (RotationMatrix3d RR, Vector3d zdir) {
      Vector3d xcol = new Vector3d();
      Vector3d ycol = new Vector3d();
      Vector3d zcol = new Vector3d();

      zcol.normalize (zdir);

      double axis_x = -zcol.y;
      double axis_y = zcol.x;
      double len = Math.sqrt (axis_x * axis_x + axis_y * axis_y);
      double ang = Math.atan2 (len, zcol.z);
      if (len != 0) {
         RR.setAxisAngle (axis_x / len, axis_y / len, 0, ang);
         RR.getColumn (0, xcol);
      }
      else {
         xcol.set (1, 0, 0);
      }
      ycol.cross (zcol, xcol);
      RR.setColumn (0, xcol);
      RR.setColumn (1, ycol);
      RR.setColumn (2, zcol);
   }

   void testSetZDirection (RotationMatrix3d RR, Vector3d zdir) {
      Vector3d colz = new Vector3d();
      saveResult (RR);
      setZDirectionCheck (RR, zdir);
      MX.set (RR);
      RR.setZDirection (zdir);
      RR.getColumn (2, colz);
      checkAndRestoreResult (RR, EPSILON);
   }

   void testMulRot (RotationMatrix3d RR) {
      RotationMatrix3d R0 = new RotationMatrix3d(RR);
      RotationMatrix3d RX = new RotationMatrix3d();
      saveResult (RR);

      RX.set (R0);
      RX.mulRotX (Math.PI/2);
      MX.set (RX);
      RR.mulRotX90();
      checkAndRestoreResult (RR, EPSILON);

      RX.set (R0);
      RX.mulRotX (Math.PI);
      MX.set (RX);
      RR.mulRotX180();
      checkAndRestoreResult (RR, EPSILON);

      RX.set (R0);
      RX.mulRotX (3*Math.PI/2);
      MX.set (RX);
      RR.mulRotX270();
      checkAndRestoreResult (RR, EPSILON);

      RX.set (R0);
      RX.mulRotY (Math.PI/2);
      MX.set (RX);
      RR.mulRotY90();
      checkAndRestoreResult (RR, EPSILON);

      RX.set (R0);
      RX.mulRotY (Math.PI);
      MX.set (RX);
      RR.mulRotY180();
      checkAndRestoreResult (RR, EPSILON);

      RX.set (R0);
      RX.mulRotY (3*Math.PI/2);
      MX.set (RX);
      RR.mulRotY270();
      checkAndRestoreResult (RR, EPSILON);

      RX.set (R0);
      RX.mulRotZ (Math.PI/2);
      MX.set (RX);
      RR.mulRotZ90();
      checkAndRestoreResult (RR, EPSILON);

      RX.set (R0);
      RX.mulRotZ (Math.PI);
      MX.set (RX);
      RR.mulRotZ180();
      checkAndRestoreResult (RR, EPSILON);

      RX.set (R0);
      RX.mulRotZ (3*Math.PI/2);
      MX.set (RX);
      RR.mulRotZ270();
      checkAndRestoreResult (RR, EPSILON);
   }

   void testNormalize (RotationMatrix3d RR) {
      saveResult (RR);
      MX.set (RR);
      RR.normalize();

      checkAndRestoreResult (RR, EPSILON);
   }

   

   void testPreciseAxisAngleSet () {
      AxisAngle axisAng = new AxisAngle();
      RotationMatrix3d R = new RotationMatrix3d();
      RotationMatrix3d RX = new RotationMatrix3d();

      axisAng.set (1, 0, 0, 0);
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      checkResult (R, RX, "setAxisAnglePrecise");
      
      axisAng.set (1, 0, 0, Math.toRadians(90));
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      RX.mulRotX90();
      checkResult (R, RX, "setAxisAnglePrecise");
      
      axisAng.set (1, 0, 0, Math.toRadians(180));
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      RX.mulRotX180();
      checkResult (R, RX, "setAxisAnglePrecise");
      
      axisAng.set (1, 0, 0, Math.toRadians(270));
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      RX.mulRotX270();
      checkResult (R, RX, "setAxisAnglePrecise");
      
      axisAng.set (1, 0, 0, Math.toRadians(-90));
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      RX.mulRotX270();
      checkResult (R, RX, "setAxisAnglePrecise");
      
      axisAng.set (-1, 0, 0, Math.toRadians(90));
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      RX.mulRotX270();
      checkResult (R, RX, "setAxisAnglePrecise");
      

      axisAng.set (0, 1, 0, Math.toRadians(90));
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      RX.mulRotY90();
      checkResult (R, RX, "setAxisAnglePrecise");
      
      axisAng.set (0, 1, 0, Math.toRadians(180));
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      RX.mulRotY180();
      checkResult (R, RX, "setAxisAnglePrecise");
      
      axisAng.set (0, 1, 0, Math.toRadians(270));
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      RX.mulRotY270();
      checkResult (R, RX, "setAxisAnglePrecise");
      
      axisAng.set (0, 1, 0, Math.toRadians(-90));
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      RX.mulRotY270();
      checkResult (R, RX, "setAxisAnglePrecise");
      
      axisAng.set (0, 0, 1, Math.toRadians(90));
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      RX.mulRotZ90();
      checkResult (R, RX, "setAxisAnglePrecise");
      
      axisAng.set (0, 0, 1, Math.toRadians(180));
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      RX.mulRotZ180();
      checkResult (R, RX, "setAxisAnglePrecise");
      
      axisAng.set (0, 0, 1, Math.toRadians(270));
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      RX.mulRotZ270();
      checkResult (R, RX, "setAxisAnglePrecise");
      
      axisAng.set (0, 0, 1, Math.toRadians(-90));
      R.setAxisAnglePrecise (axisAng);
      RX.setIdentity();
      RX.mulRotZ270();
      checkResult (R, RX, "setAxisAnglePrecise");
   }

   public void execute() {
      RotationMatrix3d RR = new RotationMatrix3d();
      RotationMatrix3d R1 = new RotationMatrix3d();


      RotationMatrix3d R2 = new RotationMatrix3d();

      RandomGenerator.setSeed (0x1234);

      testGeneric (R1);

      for (int i = 0; i < 100; i++) {
         R1.setRandom();
         R2.setRandom();
         RR.setRandom();

         testMul (RR, R1, R2);
         testMul (RR, RR, RR);

         testMulInverse (RR, R1, R2);
         testMulInverse (RR, RR, RR);

         testNegate (RR, R1);
         testNegate (RR, RR);

         testSet (RR, R1);
         testSet (RR, RR);

         testTranspose (RR, R1);
         testTranspose (RR, RR);

         testInvert (RR, R1);
         testInvert (RR, RR);

         testNorms (R1);

         testSetRotations (RR, R1);
         testNormalize (RR);

         testMulRot (RR);
      }

      for (int i = 0; i < 100; i++) {
         Vector3d zdir = new Vector3d();
         zdir.setRandom();
         zdir.scale (10);
         testSetZDirection (RR, zdir);
         double ex = EPSILON * RandomGenerator.get().nextDouble();
         double ey = EPSILON * RandomGenerator.get().nextDouble();
         testSetZDirection (RR, new Vector3d (ex, ey, 1));
         testSetZDirection (RR, new Vector3d (ex, ey, -1));
      }
      testSetZDirection (RR, new Vector3d (0, 0, 1));
      testSetZDirection (RR, new Vector3d (0, 0, -1));
      testPreciseAxisAngleSet ();
   }

//   private void RPYtest() {
//      double [] rpy = {10, 90-1e-16, -72};
//      
//      RotationMatrix3d R = new RotationMatrix3d();
//      double rpyRad[] = new double[3];
//      double rpyOrigRad[] = new double[3];
//
//      for (int i=0; i<3; i++) {
//         rpyOrigRad[i] = Math.toRadians(rpy[i]);
//      }
//      
//      R.setRpy(rpyOrigRad);
//      R.getRpy(rpyRad);
//      
//      EulerFilter.filter(rpyOrigRad, rpyRad, 1e-8, rpyRad);
//      
//      for (int i=0; i<3; i++) {
//         rpyRad[i] = Math.toDegrees(rpyRad[i]);
//      }
//      
//      System.out.printf("Original degrees: %f, %f, %f\n", rpy[0], rpy[1], rpy[2]);
//      System.out.printf("Recovered degrees: %f, %f, %f\n", rpyRad[0], rpyRad[1], rpyRad[2]);
//      
//   }
   
   public static void main (String[] args) {
      RotationMatrix3dTest test = new RotationMatrix3dTest();

      try {
         test.execute();
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }

      System.out.println ("\nPassed\n");
   }
}
