package artisynth.demos.test;

import java.io.*;
import java.awt.Color;
import java.util.ArrayList;

import artisynth.core.modelbase.*;
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

public class BooleanOperations extends MeshTestBase {

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

      //mesh = MeshFactory.createSphere (/*radius=*/2.0, /*nsegs=*/24);
      
      mesh1 = readBoxMesh("CombinedGuide.stl");
      mesh2 = readBoxMesh("MandibleScaledUp.stl");



      maspack.collision.SurfaceMeshIntersector intersector = new maspack.collision.SurfaceMeshIntersector();
      
      
      long startTime = System.nanoTime();
      //mesh1 = intersector.findDifference10(mesh1, mesh2);
      mesh1 = intersector.findDifference01 (mesh1, mesh2);
      long endTime = System.nanoTime();
      long duration = (endTime - startTime) / 1000000;  //divide by 1000000 to get milliseconds.

      
      

      maspack.geometry.io.StlWriter writer = null;
      try {
         writer = new maspack.geometry.io.StlWriter(rbpath + "output.stl");
         writer.writeMesh(mesh1);
      }
      catch (Exception e) {
         System.out.println ("Unable to save" );
      }
      

      myMesh = mesh1;
      maspack.geometry.MeshBase myMesh2 = mesh2;
      FixedMeshBody meshBody = new FixedMeshBody (mesh1);
      msmod.addMeshBody (meshBody);
      addModel (msmod);
      addControlPanel (meshBody);
      FixedMeshBody meshBody2 = new FixedMeshBody (mesh2);
      msmod.addMeshBody (meshBody2);
   }
}
