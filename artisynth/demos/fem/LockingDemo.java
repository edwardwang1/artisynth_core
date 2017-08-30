package artisynth.demos.fem;

import java.awt.Point;
import java.util.*;
import java.io.*;

import javax.swing.*;

import java.awt.event.*;

import javax.swing.event.*;

import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.properties.PropertyList;
import maspack.render.*;
import maspack.render.Renderer;
import maspack.util.*;
import maspack.widgets.DoubleFieldSlider;
import maspack.interpolation.Interpolation;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.*;
import artisynth.core.materials.AxialMuscleMaterial;
import artisynth.core.materials.MooneyRivlinMaterial;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.gui.*;
import artisynth.core.gui.selectionManager.SelectionEvent;
import artisynth.core.gui.selectionManager.SelectionListener;
import artisynth.core.modelbase.*;
import artisynth.core.probes.*;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;
import maspack.render.*;
import artisynth.core.driver.*;

import java.awt.*;
import java.util.*;

public class LockingDemo extends RootModel {

   static double LENGTH = 1.0; // 1.0;
   static double WIDTH = 1.0; // .20;
   static double DENSITY = 1000;
   static double EPS = 1e-8;

   static int NX = 5;
   static int NY = 5;
   static int NZ = 5;

   FemModel3d myHexMod;   
   FemModel3d myTetMod;   
   MechModel myMechMod;   

   ControlPanel myControlPanel;

   private void setModelProperties (FemModel3d mod) {
      mod.setDensity (DENSITY);
      setRenderProperties (mod, LENGTH);

      mod.setMaterial (
         new MooneyRivlinMaterial (2000, 0, 0, 0, 0, 5000000));
      for (FemNode3d n : mod.getNodes()) {
         if (Math.abs(n.getPosition().z-LENGTH/2) < EPS) {
            n.setDynamic(false);
         }
      }
   }

   public void build (String[] args) {

      myTetMod = new FemModel3d ("tet");
      FemFactory.createHexGrid (
         myTetMod, WIDTH, WIDTH, LENGTH, NX, NY, NZ);

      myTetMod.transformGeometry (new RigidTransform3d (-3*WIDTH/4, 0, 0));
      setModelProperties (myTetMod);

      myHexMod = new FemModel3d ("hex");
      FemFactory.createHexGrid (
         myHexMod, WIDTH, WIDTH, LENGTH, NX, NY, NZ);
      setModelProperties (myHexMod);

      myHexMod.transformGeometry (new RigidTransform3d (3*WIDTH/4, 0, 0));

      myMechMod = new MechModel ("mech");
      myMechMod.addModel (myTetMod);
      myMechMod.addModel (myHexMod);

      addModel (myMechMod);

      myMechMod.getSolver().profileKKTSolveTime = true;

      addControlPanel();
   }


   public void setRenderProperties (FemModel3d mod, double length) {
      
      mod.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setShading (mod, Renderer.Shading.FLAT);
      RenderProps.setFaceColor (mod, new Color (0.7f, 0.7f, 0.9f));
      RenderProps.setLineWidth (mod.getElements(), 3);
      RenderProps.setLineColor (mod.getElements(), Color.blue);
      RenderProps.setPointRadius (mod, 0.02*length);
      RenderProps.setPointStyle (mod, Renderer.PointStyle.SPHERE);
      RenderProps.setPointColor (mod.getNodes(), Color.GREEN);
   }

   public void addControlPanel () {

      myControlPanel = new ControlPanel ("options", "LiveUpdate");

      myControlPanel.addWidget (
         "tetVolume", myTetMod, "volume");
      myControlPanel.addWidget (
         "hexVolume", myHexMod, "volume");
      myControlPanel.addWidget (
         "tetIncompressible", myTetMod, "incompressible");
      myControlPanel.addWidget (
         "hexIncompressible", myHexMod, "incompressible"); 
      myControlPanel.addWidget (
         "tetIncompCompliance", myTetMod, "incompCompliance");
      myControlPanel.addWidget (
         "hexIncompCompliance", myHexMod, "incompCompliance"); 
      myControlPanel.addWidget (
         "tetSoftIncomp", myTetMod, "softIncompMethod");
      myControlPanel.addWidget (
         "hexSoftIncomp", myHexMod, "softIncompMethod");
      myControlPanel.addWidget (
         "tetMaterial", myTetMod, "material");
      myControlPanel.addWidget (
         "hexMaterial", myHexMod, "material");

      addControlPanel (myControlPanel);
      Main.getMain().arrangeControlPanels(this);
   }

}
