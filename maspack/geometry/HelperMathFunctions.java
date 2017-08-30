/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.Iterator;

import maspack.interpolation.NumericList;
import maspack.interpolation.NumericListKnot;
import maspack.matrix.MatrixNd;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SVDecomposition;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;

public class HelperMathFunctions {

   public double getSquareDistance(VectorNd v, VectorNd v2) {

      double dx = v.get (0) - v2.get (0);
      double dy = v.get (1) - v2.get (1);
      double dz = v.get (2) - v2.get (2);

      return dx * dx + dy * dy + dz * dz;
  }
   
   public double getSquareDistance(Point3d v, Point3d v2) {
      return getSquareDistance(new VectorNd(v), new VectorNd(v2));
  }

   public void planeFit(Vector3d[] points, Vector3d center, Vector3d normal) {
      //Points: Input. Array of Point3d objects specifying points to be regressed
      //Center: Output: point on plane
      //Normal: Output. Normal of plane
      double sumX = 0;
      double sumY = 0;
      double sumZ = 0;
      
      int n = points.length;
      Vector3d[] copyOfPoints = new Point3d[n];
      for (int i =0; i < n; i++) {
         sumX += points[i].get (0);
         sumY += points[i].get (1);
         sumZ += points[i].get (2);
         copyOfPoints[i] = new Vector3d(points[i].copy ());
      }
      
      center = new Point3d(sumX/n, sumY/n, sumZ/n);
      
      MatrixNd mat = new MatrixNd(n, 3);
      
      
      for (int i=0; i <n; i++) {
         
         copyOfPoints[i].sub(center);
         mat.setRow (i, points[i]);
      }      
      
      SVDecomposition svd = new SVDecomposition(mat);
      //MatrixNd V = svd.getV();
      //MatrixNd U = svd.getU ();
      //VectorNd S = svd.getS ();
      svd.getV ().getColumn (2, normal);  
      
   }
 
   double pointPlaneDistance (Vector3d point, double[] planeValues) {
      // returns the distance from a point to a plane defined as Ax + By + Cz + D = 0
      double numerator = Math.abs(planeValues[0] * point.get (0) + planeValues[1] * point.get (1) + planeValues[2] * point.get (2) + planeValues[3]);
      double denominator = Math.sqrt (Math.pow(planeValues[0], 2) + Math.pow(planeValues[1], 2) + Math.pow(planeValues[2], 2));
      return numerator/denominator;
   }

   double[] planeEquationFromNormalAndPoint(Vector3d normal, Vector3d point) {
      // Ax + By + Cz + D = 0
      double[] planeValues = new double[4];
      for (int j = 0; j < 3; j++) {
         planeValues[j] = normal.get (j);
      }
      planeValues[3] = -normal.get(0) * point.get (0) - normal.get(1) * point.get (1)  - normal.get(2) * point.get (2); 
      // returns array of 4: A, B, C, D
      return planeValues;
   }
 
   public NumericListKnot closestNumericListKnotToPlane(Vector3d normal, Vector3d point, RigidTransform3d pose, NumericList numericList) {
      // Returns the closest knot in a numericList to a plane defined by a normal and a point, and pose.
      Vector3d worldNormal = new Vector3d(normal);
      worldNormal.transform (pose);
      System.out.println (worldNormal);
      Point3d worldPoint = new Point3d(point);
      worldPoint.transform (pose);
      double[] planeValues = planeEquationFromNormalAndPoint(worldNormal, worldPoint);
      
      double distance = 1000;
      Point3d tempPoint;
      NumericListKnot closestKnot = numericList.getLast ();
      Iterator<NumericListKnot> itr = numericList.iterator ();
      
      while (itr.hasNext ()) {
         tempPoint  = new Point3d (itr.next ().v);
         double currentDistance = pointPlaneDistance(tempPoint, planeValues);
         if (currentDistance < distance) {
            distance = currentDistance;
            closestKnot = itr.next ();
         }
      }
      
      return closestKnot;
   }
}
