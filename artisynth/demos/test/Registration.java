package artisynth.demos.test;

import java.io.*;
import java.awt.Color;
import java.util.ArrayList;

import artisynth.core.modelbase.*;
import artisynth.core.renderables.EditablePolygonalMeshComp;
import artisynth.core.mechmodels.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import artisynth.core.util.*;
import maspack.geometry.*;
import maspack.geometry.MeshICP.AlignmentType;
import maspack.geometry.io.GenericMeshReader;
import maspack.geometry.io.StlReader;
import maspack.geometry.io.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.collision.*;
import maspack.properties.*;


public class Registration extends MeshTestBase {

   public static String rbpath =
      ArtisynthPath.getHomeRelativePath (
         "src/maspack/geometry/sampleData/", ".");

   public static PropertyList myProps =
      new PropertyList (PolygonalMeshTest.class, MeshTestBase.class);

   @Override
   public void setHasNormals (boolean enabled) {
      if (enabled != myMesh.hasNormals()) {
         if (enabled) {
            myMesh.clearNormals();
         }
         else {
            myMesh.setNormals (null, null);
         }
      }
   }

   void addControlPanel (MeshComponent meshBody) {
      ControlPanel panel = createControlPanel (meshBody);
      panel.addWidget (meshBody, "renderProps.faceStyle");
      panel.addWidget (meshBody, "renderProps.faceColor");
      panel.addWidget (meshBody, "renderProps.backColor");
      panel.addWidget (meshBody, "renderProps.drawEdges");
      panel.addWidget (meshBody, "renderProps.edgeColor");
      panel.addWidget (meshBody, "renderProps.edgeWidth");
      panel.addWidget (meshBody, "renderProps.lineColor");
      panel.addWidget (meshBody, "renderProps.lineWidth");
      addControlPanel (panel);
   }



   PolygonalMesh readBoxMesh(String name) {
      PolygonalMesh mesh = null;
      try {
         //mesh = (PolygonalMesh)GenericMeshReader.readMesh(rbpath + "MarzMandible.stl");
         mesh = StlReader.read(rbpath + name);
         System.out.println ("Read Mesh" + name);
      }
      catch (Exception e) {
         System.out.println ("Unable to read mesh: " + name);
         e.printStackTrace(); 
         System.exit(1); 
      }
      return mesh;
   }

   public void build (String[] args) {

      MechModel msmod = new MechModel ("msmod");
      PolygonalMesh mesh1 = null;
      PolygonalMesh mesh2 = null;
      PolygonalMesh mandible = null;
      PolygonalMesh masterMandible = null;
      PolygonalMesh masterInnerGuide = null;
      PolygonalMesh meshPlane = null;
      PolygonalMesh meshPlaneTransformed = null;

      //mesh = MeshFactory.createSphere (/*radius=*/2.0, /*nsegs=*/24);
      
      //mesh1 = readBoxMesh("Inside.stl");
      //mesh2 = readBoxMesh("Inner.stl");
      mandible = readBoxMesh("Mandible1.stl");
      masterMandible = readBoxMesh("MasterMandible.stl");
      masterInnerGuide = readBoxMesh("MasterInnerGuide.stl");
      
   
      
      //Creating and translating Plane
      meshPlane = MeshFactory.createPlane (999, 999);
      Vector3d targetNormal = new Vector3d(0, 1, 0);
      Point3d targetPoint = new Point3d(0, 289, -76);
      
      Vector3d currentNormal = new Vector3d(0, 0, 1);
      Point3d currentpoint = new Point3d(0, 0, 0);
      
      double dotProduct = currentNormal.dot (targetNormal);
      Vector3d crossProduct = currentNormal.cross (targetNormal);
      double angle = Math.acos (currentNormal.dot(targetNormal));
      maspack.matrix.RigidTransform3d rotmat = new maspack.matrix.RigidTransform3d();
      AxisAngle axisAngle = new AxisAngle();
      axisAngle.set (crossProduct, angle);
      rotmat.setRotation(axisAngle);
      rotmat.setTranslation (targetPoint.sub (currentpoint));

      
      meshPlane.transform(rotmat);
      
    
      
      
      
      //System.out.println (masterMandible.getMeshToWorld());
      //System.out.println (masterInnerGuide.getMeshToWorld());
      //System.out.println (meshPlane.getMeshToWorld());
      
      //maspack.matrix.AffineTransform3d transformICP = null;
      //maspack.geometry.MeshICP icp = new maspack.geometry.MeshICP ();
      //transformICP = MeshICP.align(mandible, masterMandible, AlignmentType.RIGID);
      //masterMandible.transform (transformICP);
      //masterInnerGuide.transform (transformICP);
      
      long startTime = System.nanoTime();
      //maspack.geometry.CPD cpd = new maspack.geometry.CPD ();     
      //maspack.matrix.AffineTransform3d transform = null;     
      //transform = CPD.affine(mandible, masterMandible, 0, 0.01, 500);
      long endTime = System.nanoTime();
      long duration = (endTime - startTime) / 1000000;  //divide by 1000000 to get milliseconds.
      
      System.out.println ("Duration:" + duration);
      //masterMandible.transform (transform);      
      //masterInnerGuide.transform (transform);
      masterInnerGuide.autoGenerateNormals();
            

      
      maspack.geometry.io.StlWriter writer = null;
      try {
         writer = new maspack.geometry.io.StlWriter(rbpath + "output.stl");
         writer.writeMesh(masterInnerGuide);
         writer = new maspack.geometry.io.StlWriter(rbpath + "output2.stl");
         writer.writeMesh(masterMandible);
      }
      catch (Exception e) {
         System.out.println ("Unaable to save" );
      }
      

      myMesh = mandible;
      maspack.geometry.MeshBase myMesh2 = mandible;
      FixedMeshBody meshBody = new FixedMeshBody (masterMandible);
      FixedMeshBody meshBody2 = new FixedMeshBody (mandible);
      FixedMeshBody meshBody3 = new FixedMeshBody (masterInnerGuide);
      FixedMeshBody meshBody4 = new FixedMeshBody (meshPlane);
      msmod.addMeshBody (meshBody);
      msmod.addMeshBody (meshBody2);
      msmod.addMeshBody (meshBody3);
      msmod.addMeshBody (meshBody4);
      addModel (msmod);
      addControlPanel (meshBody);
   }
}
