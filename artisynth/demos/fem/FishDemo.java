package artisynth.demos.fem;

import java.awt.Color;
import java.awt.Point;
import java.util.*;
import java.io.*;

import javax.swing.*;

import maspack.geometry.*;
import maspack.matrix.*;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.*;
import artisynth.core.femmodels.FemFactory.FemElementType;
import artisynth.core.gui.*;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import maspack.properties.PropertyDesc;
import maspack.properties.PropertyList;
import maspack.render.*;
import maspack.render.Renderer;
import maspack.spatialmotion.SpatialInertia;
import artisynth.core.driver.*;

import java.awt.*;

public class FishDemo extends RootModel {
   public static boolean debug = false;

   //FemModel3d myFemMod;

   MechModel myMechMod;

   static double myDensity = 50000;

   public static PropertyList myProps =
      new PropertyList (FishDemo.class, RootModel.class);

   static {
      myProps.add ("value * *", "a value", 0, "[-1,3] AE");
   }

   double v = 0;

   public double getValue() {
      return v;
   }

   public void setValue (double newv) {
      v = newv;
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public static String rbpath =
      ArtisynthPath.getHomeRelativePath (
         "src/maspack/geometry/sampleData/", ".");

   public static String fempath =
      ArtisynthPath.getHomeRelativePath (
         "src/artisynth/core/femmodels/meshes/", ".");

   public void build (String[] args) throws IOException {

      myMechMod = new MechModel();

      RigidBody table = RigidBody.createBox("table", 0.005, 0.0075, 0.0008, 0);
      table.setDynamic(false);
      table.setPose(
         new RigidTransform3d(
            new Vector3d(0,0.0015,-0.002), AxisAngle.IDENTITY));
      myMechMod.addRigidBody(table);

      
      PolygonalMesh box2Surface = new PolygonalMesh(rbpath + "box2.obj");
      // Create the box
      
      
      
      // Create FEM beam
      FemModel3d beam = new FemModel3d("beam");
      myMechMod.addModel(beam);
      double[] size = {0.003, 0.0015, 0.0015};         // widths
      int[] res = {4, 2, 2};                           // resolution
      FemFactory.createGrid(beam, FemElementType.Hex, 
         size[0], size[1], size[2], 
         res[0], res[1], res[2]);
      
      // Set properties
      beam.setDensity(1000);
      beam.setMaterial(new LinearMaterial(300, 0.33));


     
      beam.transformGeometry (new RigidTransform3d (new Vector3d (
         0, 0, 0.5), new AxisAngle()));
      beam.setSurfaceRendering (SurfaceRender.Shaded);

      myMechMod.addRigidBody (table);
      myMechMod.setProfiling (false);

      myMechMod.addModel (beam);
      myMechMod.setIntegrator (Integrator.BackwardEuler);

      myMechMod.setCollisionBehavior (table, beam, true, 0.1);
      myMechMod.transformGeometry (new RigidTransform3d (
         0, 0, 0, 0, 0, 1, -Math.PI / 2));

      RenderProps.setPointColor (getOutputProbes(), Color.GREEN);
      RenderProps.setLineColor (getOutputProbes(), Color.GREEN);


      addModel (myMechMod);
   }

   @Override
   public void attach (DriverInterface driver) {
   }

   @Override
   public void detach (DriverInterface driver) {
   }

   /**
    * {@inheritDoc}
    */
   public String getAbout() {
      return "simple demo of a 3d fem";
   }
   
   
   protected void setRenderProps (FemModel3d fem) {    
   fem.setSurfaceRendering (SurfaceRender.Shaded);    
   RenderProps.setLineColor (fem, Color.BLUE);    
   RenderProps.setFaceColor (fem, new Color (0.5f, 0.5f, 1f));    
   }
}
