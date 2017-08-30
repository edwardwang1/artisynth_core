/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.ArrayList;
import java.util.List;

import maspack.geometry.io.StlReader;
import maspack.matrix.AxisAngle;
import maspack.matrix.MatrixNd;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SVDecomposition;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;

public class HelperMeshFunctions {
   
   HelperMathFunctions mathHelper = new HelperMathFunctions();

   public void setPlaneOrigin(PolygonalMesh plane, Vector3d origin) {
      RigidTransform3d translationMat = new RigidTransform3d();
      Vector3d centroid = new Vector3d();
      plane.computeCentroid (centroid);
      RigidTransform3d transMat = new RigidTransform3d();
      transMat.setTranslation (origin.sub (centroid));
      plane.transform (transMat);
   }
   
   public PolygonalMesh createPlane(Vector3d targetNormal, Point3d targetOrigin, double width, double length) {
      
      PolygonalMesh meshPlane = MeshFactory.createPlane (width, length);

      Vector3d currentNormal = new Vector3d(0, 0, 1);
      Point3d currentpoint = new Point3d(0, 0, 0);
      
      Vector3d crossProduct = new Vector3d(currentNormal);
      crossProduct.cross (targetNormal);
      double angle = -Math.acos (currentNormal.dot(targetNormal));
      RigidTransform3d rotmat = new RigidTransform3d();
      AxisAngle axisAngle = new AxisAngle();
      axisAngle.set (crossProduct, angle);
      rotmat.setRotation(axisAngle);
      meshPlane.transform(rotmat);
      
      setPlaneOrigin(meshPlane, targetOrigin);
      
      return meshPlane;
   }
   
   public Vector3d pointsForTransform(PolygonalMesh model, VectorNd point1, VectorNd point2){
      Vector3d point3 = null;
      Vector3d center = new Vector3d((point1.get (0) + point2.get (0))/2,(point1.get (1) + point2.get (1))/2, (point1.get (2) + point2.get (2))/2); 
      
      List <Vertex3d> vertices = new ArrayList<Vertex3d>();
      vertices = model.getVertices();
      Point3d pos = null;
      double mindistance = 1000000;
      double distance = 0;
      int idx1 = 0;
      int idx2 = 0;

      //Searching the Index of the Vertex closest to point1
      for (int i=0; i < vertices.size(); i++){
          pos = vertices.get(i).getPosition();
          distance = Math.sqrt(mathHelper.getSquareDistance(point1, point2));
          if (distance < mindistance){
              mindistance = distance;
              idx1 = i;
          }    
      }

      //Searching the Index of the Vertex closest to point2
      for (int i=0; i < vertices.size(); i++){
          pos = vertices.get(i).getPosition();
          distance = Math.sqrt(mathHelper.getSquareDistance(point1, point2));
          if (distance < mindistance){
              mindistance = distance;
              idx2 = i;
          }    
      }

      //Swapping idxs if idx1 > idx2 only --> for looping through the normals later on
      if (idx1 > idx2){
          int temp = idx1;
          idx1 = idx2;
          idx2 = temp;
      }
      
      //Calculating average normal
      Vector3d normal1 = model.getNormal (idx1);
      Vector3d normal2 = model.getNormal (idx2);

      Vector3d averageNormal = new Vector3d((normal1.get (0) + normal2.get (0))/2,(normal1.get (1) + normal2.get (1))/2, (normal1.get (2) + normal2.get (2))/2); 
      averageNormal.normalize ();
      
      point3 = new Vector3d(center.add (averageNormal.scale (0.01)));
      return point3;
  }

   public PolygonalMesh readMesh (String path, String name) {
      PolygonalMesh mesh = null;
      try {
         // mesh = (PolygonalMesh)GenericMeshReader.readMesh(rbpath +
         // "MarzMandible.stl");
         System.out.println ("Opening: " + name);
         mesh = StlReader.read (path);
         System.out.println ("Opened: " + name);
      }
      catch (Exception e) {
         System.out.println ("Unable to read mesh: " + name);
         e.printStackTrace ();
         System.exit (1);
      }
      return mesh;
   }
   
}
