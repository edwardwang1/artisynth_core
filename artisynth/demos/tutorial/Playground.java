package artisynth.demos.tutorial;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.AxisAngle;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.render.GL.GLViewer;
import maspack.render.GL.GLViewer.BlendFactor;
import maspack.render.GL.GL2.GL2Viewer;
import maspack.render.GL.GLGridPlane;
import maspack.matrix.Plane;
import artisynth.core.renderables.GridPlane;

public class Playground extends RootModel {
   
   public void build (String[] args) {
      MechModel mechmodel = new MechModel ("mechmodel");
      
      GridPlane gridPlane = new GridPlane(null, null, null);
           
      
      mechmodel.addRenderable (gridPlane);
      
      
   }
   
 
}
