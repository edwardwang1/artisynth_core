/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import maspack.geometry.GeometryTransformer;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Vector3i;
import maspack.geometry.SignedDistanceGrid;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Matrix;
import maspack.matrix.Matrix6d;
import maspack.matrix.Point3d;
import maspack.matrix.Quaternion;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.PropertyList;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.spatialmotion.SpatialInertia;
import maspack.spatialmotion.Twist;
import maspack.spatialmotion.Wrench;
import maspack.util.DoubleInterval;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.Range;
import maspack.util.RangeBase;
import maspack.util.ReaderTokenizer;
import maspack.util.StringHolder;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.util.ScanToken;

public class RigidBody extends Frame 
   implements CollidableBody, HasSurfaceMesh, HasDistanceGrid, ConnectableBody {
   
   protected SpatialInertia mySpatialInertia;
   protected SpatialInertia myEffectiveInertia;
   protected GeometryTransformer.Constrainer myTransformConstrainer = null;
   
   MeshInfo myMeshInfo = new MeshInfo();
   protected ArrayList<BodyConnector> myConnectors;

   // pre-allocated temporary storage variables
   protected Wrench myCoriolisForce = new Wrench();
   protected Twist myBodyVel = new Twist();
   protected Twist myBodyAcc = new Twist();

   protected double myDensity = DEFAULT_DENSITY;

   static final boolean useExternalAttachments = false;

   private static final Vector3d zeroVect = new Vector3d();

   /* Indicates how inertia is determined for a RigidBody */
   public enum InertiaMethod {
      /**
       * Inertia is determined implicitly from the surface mesh and a specified 
       * density.
       */
      Density, 
      
      /**
       * Inertia is determined implicitly from the surface mesh and a specified 
       * mass (which is divided by the mesh volume to determine a density).
       */     
      Mass, 
      
      /** 
       * Inertia is explicitly specified.
       */
      Explicit
   };
   
   protected InertiaMethod myInertiaMethod = DEFAULT_INERTIA_METHOD;
   
   public static PropertyList myProps =
      new PropertyList (RigidBody.class, Frame.class);

   private static InertiaMethod DEFAULT_INERTIA_METHOD =
      InertiaMethod.Density;
   private static SymmetricMatrix3d DEFAULT_INERTIA =      
      new SymmetricMatrix3d (1, 1, 1, 0, 0, 0);
   private static double DEFAULT_MASS = 1.0;
   private static Point3d DEFAULT_CENTER_OF_MASS = new Point3d (0, 0, 0);
   private static double DEFAULT_DENSITY = 1.0;

   protected static final Collidability DEFAULT_COLLIDABILITY =
      Collidability.ALL;   
   protected Collidability myCollidability = DEFAULT_COLLIDABILITY;
   protected int myCollidableIndex;

   static int DEFAULT_DISTANCE_GRID_MAX_DIVS = 20;
   static Vector3i DEFAULT_DISTANCE_GRID_DIVS = new Vector3i(0, 0, 0);
   static boolean DEFAULT_RENDER_DISTANCE_GRID = false;
   static boolean DEFAULT_RENDER_DISTANCE_SURFACE = false;
   static String DEFAULT_DISTANCE_GRID_RENDER_RANGES = "* * *";
   SignedDistanceGrid mySDGrid = null;
   PolygonalMesh mySDSurface = null;
   boolean mySDGridValid = false;
   Vector3i myDistanceGridRes = new Vector3i(DEFAULT_DISTANCE_GRID_DIVS);
   int myDistanceGridMaxRes = DEFAULT_DISTANCE_GRID_MAX_DIVS;
   boolean myRenderDistanceGrid = DEFAULT_RENDER_DISTANCE_GRID;
   boolean myRenderDistanceSurface = DEFAULT_RENDER_DISTANCE_SURFACE;
   String myDistanceGridRenderRanges = DEFAULT_DISTANCE_GRID_RENDER_RANGES;
   double myGridMargin = 0.1;

    static {
      myProps.remove ("renderProps");
      myProps.add ("renderProps * *", "render properties", null);
      myProps.add (
         "inertiaMethod", "means by which inertia is determined",
         DEFAULT_INERTIA_METHOD);
      myProps.add (
         "density", "average density of this body", DEFAULT_DENSITY, "NW");
      myProps.add ("mass", "mass of this body", DEFAULT_MASS, "1E NW");
      myProps.add (
         "inertia getRotationalInertia setRotationalInertia",
         "rotational inertia of this body", DEFAULT_INERTIA, "1E NW");
      myProps.add (
         "centerOfMass", "center of mass of this body", DEFAULT_CENTER_OF_MASS,
         "1E NW");
      myProps.add (
         "dynamic isDynamic", "true if component is dynamic (non-parametric)",
         true);
      myProps.add (
         "collidable", 
         "sets the collidability of this body", DEFAULT_COLLIDABILITY);
      myProps.add (
         "distanceGridRes", 
         "divisions for signed distance grid along x, y, and z",
         DEFAULT_DISTANCE_GRID_DIVS);
      myProps.add (
         "distanceGridMaxRes", 
         "max divisions for signed distance grid",
         DEFAULT_DISTANCE_GRID_MAX_DIVS);
      myProps.add (
         "renderDistanceGrid", 
         "render the distance grid in the viewer",
         DEFAULT_RENDER_DISTANCE_GRID);
      myProps.add (
         "renderDistanceSurface", 
         "render the iso-surface of the distance grid in the viewer",
         DEFAULT_RENDER_DISTANCE_SURFACE);
      myProps.add (
         "distanceGridRenderRanges",
         "which part of the distance grid to render", 
         DEFAULT_DISTANCE_GRID_RENDER_RANGES);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public boolean isMassConstant() {
      return !dynamicVelInWorldCoords;
   }

   public double getMass (double t) {
      return getMass();
   }

   public void getMass (Matrix M, double t) {
      if (dynamicVelInWorldCoords) {
         if (M instanceof Matrix6d) {
            mySpatialInertia.getRotated ((Matrix6d)M, getPose().R);
         }
         else {
            throw new IllegalArgumentException (
               "Matrix not instance of Matrix6d");
         }
      }
      else {
         doGetInertia (M, mySpatialInertia);
      }
   }

   public SpatialInertia getEffectiveInertia() {
      if (myEffectiveInertia == null) {
         return mySpatialInertia;
      }
      else {
         return myEffectiveInertia;
      }
   }
   
   public void getEffectiveMass (Matrix M, double t) {
      SpatialInertia S = getEffectiveInertia();
      if (dynamicVelInWorldCoords) {
         if (M instanceof Matrix6d) {
            S.getRotated ((Matrix6d)M, getPose().R);
         }
         else {
            throw new IllegalArgumentException (
               "Matrix not instance of Matrix6d");
         }
      }
      else {
         doGetInertia (M, S);
      }
   }
   
   public int mulInverseEffectiveMass (
      Matrix M, double[] a, double[] f, int idx) {
      SpatialInertia S = getEffectiveInertia();

      return mulInverseEffectiveMass (getEffectiveInertia(), a, f, idx);
   }
   
   public static int mulInverseEffectiveMass (
      SpatialInertia S, double[] a, double[] f, int idx) {
      Twist tw = new Twist();
      Wrench wr = new Wrench();
      wr.f.x = f[idx];
      wr.f.y = f[idx+1];
      wr.f.z = f[idx+2];
      wr.m.x = f[idx+3];
      wr.m.y = f[idx+4];
      wr.m.z = f[idx+5];
      S.mulInverse (tw, wr);
      a[idx++] = tw.v.x;
      a[idx++] = tw.v.y;
      a[idx++] = tw.v.z;
      a[idx++] = tw.w.x;
      a[idx++] = tw.w.y;
      a[idx++] = tw.w.z;
      return idx;     
   }
   
   public static int getEffectiveMassForces (
      VectorNd f, double t, FrameState state, 
      SpatialInertia effectiveInertia, int idx) {
      
      Wrench coriolisForce = new Wrench();
      Twist bodyVel = new Twist();
      bodyVel.inverseTransform (state.XFrameToWorld.R, state.vel);
      SpatialInertia S = effectiveInertia;
      S.coriolisForce (coriolisForce, bodyVel);
      if (dynamicVelInWorldCoords) {
         coriolisForce.transform (state.XFrameToWorld.R);
      }
      double[] buf = f.getBuffer();
      buf[idx++] = -coriolisForce.f.x;
      buf[idx++] = -coriolisForce.f.y;
      buf[idx++] = -coriolisForce.f.z;
      buf[idx++] = -coriolisForce.m.x;
      buf[idx++] = -coriolisForce.m.y;
      buf[idx++] = -coriolisForce.m.z;
      return idx;     
   }
   
   public int getEffectiveMassForces (VectorNd f, double t, int idx) {
      return getEffectiveMassForces (
         f, t, myState, getEffectiveInertia(), idx);
   }

   public void resetEffectiveMass() {
      myEffectiveInertia = null;
   }

   /**
    * {@inheritDoc}
    */
   public void addEffectivePointMass (double m, Vector3d loc) {
      if (myEffectiveInertia == null) {
         myEffectiveInertia = new SpatialInertia (mySpatialInertia);
      }
      SpatialInertia.addPointMass (myEffectiveInertia, m, loc);
   }

   public void addEffectiveInertia (SpatialInertia M) {
      if (myEffectiveInertia == null) {
         myEffectiveInertia = new SpatialInertia (mySpatialInertia);
      }
      myEffectiveInertia.add (M);
   }

   public void getInverseMass (Matrix Minv, Matrix M) {
      if (!(Minv instanceof Matrix6d)) {
         throw new IllegalArgumentException ("Minv not instance of Matrix6d");
      }
      if (!(M instanceof Matrix6d)) {
         throw new IllegalArgumentException ("M not instance of Matrix6d");
      }
      SpatialInertia.invert ((Matrix6d)Minv, (Matrix6d)M);
   }

   // *************** Methods for setting the inertia ***********************

   /*
    * Returns the {@link #InertiaMethod InertiaMethod} method used to determine 
    * the inertia for this RigidBody.
    * 
    * @return inertia method for this RigidBody
    * @see #setInertia
    * @see #setInertiaFromDensity
    * @see #setInertiaFromMass
    */
   public InertiaMethod getInertiaMethod() {
      return myInertiaMethod;
   }
   
   /**
    * Sets the {@link RigidBody.InertiaMethod InertiaMethod} method used to
    * determine the inertia for this RigidBody.
    * 
    * @param method inertia method for this RigidBody
    * @see #setInertia
    * @see #setInertiaFromDensity
    * @see #setInertiaFromMass
    */
   public void setInertiaMethod (InertiaMethod method) {
      PolygonalMesh mesh = getMesh();
      if (method != myInertiaMethod) {
         switch (method) {
            case Density: {
               if (mesh != null) {
                  double mass = mesh.computeVolume()*myDensity;
                  setInertiaFromMesh (myDensity);
                  mySpatialInertia.setMass (mass);
               }
               break;
            }
            case Mass: {
               if (mesh != null) {
                  double density = getMass()/mesh.computeVolume();
                  setInertiaFromMesh (density);
                  myDensity = density;
               }
               break;
            }
            case Explicit: {
               break;
            }
            default: {
               throw new InternalErrorException (
                  "Unimplemented inertia method " + method);
            }
         }
         myInertiaMethod = method;
      }
   }

   protected SpatialInertia getInertia() {
      return mySpatialInertia;
   }

   public void getInertia (SpatialInertia M) {
      M.set (mySpatialInertia);
   }

   public void setCenterOfMass (Point3d com) {
      mySpatialInertia.setCenterOfMass (com);
      myInertiaMethod = InertiaMethod.Explicit;
   }

   public void getCenterOfMass (Point3d com) {
      mySpatialInertia.getCenterOfMass (com);
   }

   public Point3d getCenterOfMass() {
      return mySpatialInertia.getCenterOfMass();
   }

   /**
    * Adjusts the pose so that it reflects the rigid body's center of mass
    */
   public void centerPoseOnCenterOfMass() {
      Point3d com = mySpatialInertia.getCenterOfMass();
      mySpatialInertia.setCenterOfMass(0,0,0);
      myState.pos.add(com);
   }

   /** 
    * Causes the inertia to be automatically computed from the mesh volume
    * and a given density. If the mesh is currently <code>null</code> then
    * the inertia remains unchanged. Subsequent (non-<code>null</code>) changes
    * to the mesh will cause the inertia to be recomputed.
    * The inertia method is set to
    * {@link RigidBody.InertiaMethod#Density Density}. 
    * 
    * @param density desired uniform density
    */
   public void setInertiaFromDensity (double density) {
      if (density < 0) {
         throw new IllegalArgumentException ("density must be non-negative");
      }
      if (getMesh() != null) {
         setInertiaFromMesh (density);
      }
      myDensity = density;
      myInertiaMethod = InertiaMethod.Density;
   }

   /** 
    * Causes the inertia to be automatically computed from the mesh volume
    * and a given mass (with the density computed by dividing the mass
    * by the mesh volume). If the mesh is currently <code>null</code> the mass 
    * of the inertia is updated but the otherwise the inertia and density
    * are left unchanged. Subsequent (non-<code>null</code>) changes
    * to the mesh will cause the inertia to be recomputed.
    * The inertia method is set to {@link RigidBody.InertiaMethod#Mass Mass}. 
    * 
    * @param mass desired body mass
    */
   public void setInertiaFromMass (double mass) {
      if (mass < 0) {
         throw new IllegalArgumentException ("mass must be non-negative");
      }
      if (getMesh() != null) {
         myDensity = mass/getMesh().computeVolume();
         setInertiaFromMesh (myDensity);
      }
      mySpatialInertia.setMass (mass);
      myInertiaMethod = InertiaMethod.Mass;      
   }
   
   public double computeVolume() {
      PolygonalMesh mesh = getMesh();
      if (mesh != null) {
         return mesh.computeVolume();
      }
      return 0;
   }

   /**
    * Sets the density for the mesh, which is defined at the mass divided
    * by the mesh volume. If the mesh is currently non-null, the mass
    * will be updated accordingly. If the current InertiaMethod
    * is either {@link RigidBody.InertiaMethod#Density Density} or 
    * {@link RigidBody.InertiaMethod#Mass Mass}, the other components of
    * the spatial inertia will also be updated.
    * 
    * @param density
    * new density value
    */
   public void setDensity (double density) {
      if (density < 0) {
         throw new IllegalArgumentException ("density must be non-negative");
      }
      if (getMesh() != null) {
         double mass = density*computeVolume();
         if (myInertiaMethod == InertiaMethod.Mass ||
             myInertiaMethod == InertiaMethod.Density) {
            setInertiaFromMesh (density);
         }
         mySpatialInertia.setMass (mass);
      }
      myDensity = density;
   }

   public Range getDensityRange () {
      return DoubleInterval.NonNegative;
   }

   /**
    * Returns the density of this body. This is either the value set explictly 
    * by {@link #setInertiaFromDensity setInertiaFromDensity}, or is the
    * mass/volume ratio for the most recently defined mesh.
    * 
    * @return density for this body.
    */
   public double getDensity() {
      return myDensity;
   }

   /**
    * Sets the mass for the mesh. If the mesh is currently non-null, then the
    * density (defined as the mass divided by the mesh volume) will be updated
    * accordingly. If the current InertiaMethod is either {@link
    * InertiaMethod#Density Density} or
    * {@link RigidBody.InertiaMethod#Mass Mass}, the
    * other components of the spatial inertia will also be updated.
    * 
    * @param mass
    * new mass value
    */
   public void setMass (double mass) {
      if (mass < 0) {
         throw new IllegalArgumentException ("Mass must be non-negative");
      }
      if (getMesh() != null) {
         double density = mass/getMesh().computeVolume();
         if (myInertiaMethod == InertiaMethod.Mass ||
             myInertiaMethod == InertiaMethod.Density) {
            setInertiaFromMesh (myDensity);
         }
         myDensity = density;
      }    
      mySpatialInertia.setMass (mass);
   }

   /**
    * Returns the mass of this body.
    */
   public double getMass() {
      return mySpatialInertia.getMass();
   }

   public Range getMassRange () {
      return DoubleInterval.NonNegative;
   }

   /**
    * Explicitly sets the rotational inertia of this body. Also sets the uniform
    * density (as returned by {@link #getDensity getDensity}) to be undefined).
    */
   public void setRotationalInertia (SymmetricMatrix3d J) {
      mySpatialInertia.setRotationalInertia (J);
      myInertiaMethod = InertiaMethod.Explicit;
   }

   public void getRotationalInertia (SymmetricMatrix3d J) {
      mySpatialInertia.getRotationalInertia (J);
   }

   /**
    * Returns the rotational inertia of this body.
    * 
    * @return rotational inertia (should not be modified).
    */
   public SymmetricMatrix3d getRotationalInertia() {
      return mySpatialInertia.getRotationalInertia();
   }

   private void doSetInertia (SpatialInertia M) {
      mySpatialInertia.set (M);
      if (getMesh() != null) {
         myDensity = M.getMass()/getMesh().computeVolume();
      } 
      myInertiaMethod = InertiaMethod.Explicit;
   }

   /**
    * Explicitly sets the spatial inertia of this body. Also sets the uniform
    * density (as returned by {@link #getDensity getDensity}) to be undefined).
    */
   public void setInertia (SpatialInertia M) {
      doSetInertia (M);
   }

   private void setInertiaFromMesh (double density) {
      PolygonalMesh mesh = null;
      if ((mesh = getMesh()) == null) {
         throw new IllegalStateException ("Mesh has not been set");
      }
      SpatialInertia M = mesh.createInertia (density);
      mySpatialInertia.set (M);
   }

   /**
    * Explicitly sets the mass and rotational inertia of this body. Also sets
    * the uniform density (as returned by {@link #getDensity getDensity}) to be
    * undefined).
    */
   public void setInertia (double m, SymmetricMatrix3d J) {
      SpatialInertia M = new SpatialInertia();
      M.set (m, J);
      doSetInertia (M);
   }

   /**
    * Explicitly sets the mass, rotational inertia, and center of mass of this
    * body. Also sets the uniform density (as returned by {@link #getDensity
    * getDensity}) to be undefined).
    */
   public void setInertia (double m, SymmetricMatrix3d J, Point3d com) {
      SpatialInertia M = new SpatialInertia();
      M.set (m, J, com);
      doSetInertia (M);
   }

   /**
    * Explicitly sets the mass and rotational inertia of this body. Also sets
    * the uniform density (as returned by {@link #getDensity getDensity}) to be
    * undefined).
    */
   public void setInertia (double m, double Jxx, double Jyy, double Jzz) {
      doSetInertia (
         new SpatialInertia (m, Jxx, Jyy, Jzz));
   }

   //**************************************************************************

   public PolygonalMesh getMesh() {
      return getSurfaceMesh();
   }
   
   public AffineTransform3d getFileTransform() {
      return new AffineTransform3d(myMeshInfo.myFileTransform);
   }

   public boolean isFileTransformRigid() {
      return myMeshInfo.myFileTransformRigidP;
   }

   public boolean isMeshModfied() {
      return myMeshInfo.myMeshModifiedP;
   }

   public PolygonalMesh getSurfaceMesh() {
      return (PolygonalMesh)myMeshInfo.myMesh;
   }
   
   public int numSurfaceMeshes() {
      return getSurfaceMesh() != null ? 1 : 0;
   }
   
   public PolygonalMesh[] getSurfaceMeshes() {
      return MeshComponent.createSurfaceMeshArray (getSurfaceMesh());
   }
   
   //   public MeshBase getMeshBase() {
   //      return myMeshInfo.myMesh;
   //   }
   
   public String getMeshFileName() {
      return myMeshInfo.getFileName();
   }

   public void setMeshFileName (String filename) {
      myMeshInfo.setFileName (filename);
   }

   /**
    * Returns the file transform associated with this rigid body's mesh.
    * 
    * @return mesh file transform (should not be modified)
    * @see #setMeshFileTransform
    */
   public AffineTransform3dBase getMeshFileTransform() {
      return myMeshInfo.getFileTransform();
   }

//   /**
//    * Sets the transform used to modify a mesh originally read from a file. It
//    * is only meaningful if there is a also mesh file name.
//    * 
//    * @param X
//    * new mesh file transform, or <code>null</code>
//    */
//   public void setMeshFileTransform (AffineTransform3dBase X) {
//      myMeshInfo.setFileTransform (X);
//   }

   public void setMesh (PolygonalMesh mesh) {
      setSurfaceMesh (mesh, null, null);
   }
   
   public void setMesh (PolygonalMesh mesh, String fileName) {
      setSurfaceMesh (mesh, fileName, null);
   }
   
   public void setSurfaceMesh (PolygonalMesh mesh, String fileName) {
      setSurfaceMesh (mesh, fileName, null);
   }
   
   public void scaleMesh (double sx, double sy, double sz) {
      myMeshInfo.scale (sx, sy, sz);
      if (myInertiaMethod == InertiaMethod.Density) {
         setInertiaFromMesh (myDensity);
      }     
   }
   
   public void scaleMesh (double s) {
      scaleMesh (s, s, s);
   }

   protected void setMeshFromInfo () {
      PolygonalMesh mesh = getMesh();
      if (mesh != null) {
         mesh.setFixed (true);
         mesh.setMeshToWorld (myState.XFrameToWorld);
         if (myInertiaMethod == InertiaMethod.Density) {
            setInertiaFromMesh (myDensity);
         }
         else {
            myDensity = mySpatialInertia.getMass()/mesh.computeVolume();
            if (myInertiaMethod == InertiaMethod.Mass) {
               setInertiaFromMesh (myDensity);            
            }
         }
      }
   }

   /**
    * Sets a mesh for this body. If the body has a uniform density (i.e.,
    * {@link #getDensity getDensity} returns a non-negative value), then the
    * spatial inertia is automatically calculated from the mesh and the uniform
    * density.
    */
   public void setMesh (
      PolygonalMesh mesh, String fileName, AffineTransform3dBase X) {
      setSurfaceMesh(mesh, fileName, X);
   }
   
   /**
    * Sets a mesh for this body. If the body has a uniform density (i.e.,
    * {@link #getDensity getDensity} returns a non-negative value), then the
    * spatial inertia is automatically calculated from the mesh and the uniform
    * density.
    */
   public void setSurfaceMesh (
      PolygonalMesh mesh, String fileName, AffineTransform3dBase X) {

      myMeshInfo.set (mesh, fileName, X);
      setMeshFromInfo();
   }

   @Override
   protected void updatePosState() {
      updateAttachmentPosStates();
      PolygonalMesh mesh = getMesh();
      if (mesh != null) {
         mesh.setMeshToWorld (myState.XFrameToWorld);
         if (myRenderDistanceSurface && getDistanceSurface() != null) {
            getDistanceSurface().setMeshToWorld (myState.XFrameToWorld);
         }
      }
   }

   protected void updateVelState() {
   }

//   public void setPose (RigidTransform3d XBodyToWorld) {
//      super.setPose (XBodyToWorld);
//      updatePosState();
//   }

   public void setPose (double x, double y, double z,
                        double roll, double pitch, double yaw) {
      RigidTransform3d X = new RigidTransform3d();
      X.p.set (x, y, z);
      X.R.setRpy (Math.toRadians(roll),
                  Math.toRadians(pitch),
                  Math.toRadians (yaw));
      setPose (X);
   }

//   public void setPosition (Point3d pos) {
//      super.setPosition (pos);
//      updatePosState();
//   }   
//
//   public void setRotation (Quaternion q) {
//      super.setRotation (q);
//      updatePosState();
//   }   

   public void extrapolatePose (Twist vel, double h) {
      myState.adjustPose (vel, h);
      updatePosState();
   }

//   public void setVelocity (Twist v) {
//      super.setVelocity (v);
//      updateVelState();
//   }
//
//   public void setBodyVelocity (Twist v) {
//      myState.setBodyVelocity (v);
//      updateVelState();
//   }

//   public void addVelocity (Twist v) {
//      myState.addVelocity (v);
//      updateVelState();
//   }
//
//   public void addScaledVelocity (double s, Twist v) {
//      myState.addScaledVelocity (s, v);
//      updateVelState();
//   }

//   public void setState (Frame frame) {
//      super.setState (frame);
//      updatePosState();
//      updateVelState();
//   }

//   public int setState (VectorNd x, int idx) {
//      idx = super.setState (x, idx);
//      updatePosState();
//      updateVelState();
//      return idx;
//   }

//   public void setState (ComponentState state) {
//      super.setState (state);
//      updatePosState();
//      updateVelState();
//   }

//   public int setPosState (double[] buf, int idx) {
//      idx = myState.setPos (buf, idx);
//      updatePosState();
//      return idx;
//   }
//
//   public int setVelState (double[] buf, int idx) {
//      idx = super.setVelState (buf, idx);
//      updateVelState();
//      return idx;
//   }

   public RigidBody() {
      super();
      // Give the default inertia unit mass and rotational inertia,
      // so that even if the body is inactives, we don't need to
      // handle zero inertia in the numerics code.
      mySpatialInertia = new SpatialInertia (1, 1, 1, 1);
      // myEffectiveInertia = new SpatialInertia (1, 1, 1, 1);
      setDynamic (true);
      setRenderProps (createRenderProps());
   }

   public RigidBody (String name) {
      this();
      setName (name);
   }

   public RigidBody (RigidTransform3d XBodyToWorld, SpatialInertia M,
   PolygonalMesh mesh, String meshFileName) {
      this();
      myState.setPose (XBodyToWorld);
      setInertia (M);
      setMesh (mesh, meshFileName);
   }

//   public void updatePose() {
//      super.updatePose();
//      updatePosState();
//   }

   public void applyGravity (Vector3d gacc) {
      // apply a force of -mass gacc at the bodies's center of mass
      Point3d com = new Point3d();
      Vector3d fgrav = new Vector3d();
      //      SpatialInertia inertia = MechModel.getEffectiveInertia(this);
      SpatialInertia inertia = mySpatialInertia;
      inertia.getCenterOfMass (com);
      com.transform (myState.XFrameToWorld.R);
      fgrav.scale (inertia.getMass(), gacc);
      myForce.f.add (fgrav, myForce.f);
      myForce.m.crossAdd (com, fgrav, myForce.m);
   }

//   public int applyPosImpulse (double[] delx, int idx) {
//      Twist tw = new Twist();
//      tw.v.x = delx[idx++];
//      tw.v.y = delx[idx++];
//      tw.v.z = delx[idx++];
//      tw.w.x = delx[idx++];
//      tw.w.y = delx[idx++];
//      tw.w.z = delx[idx++];
//      //myState.adjustPose (tw);
//      myState.adjustPose (tw, 1);
//      updatePosState();
//      return idx;
//   }

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      myState.vel.setZero();
      super.scan (rtok, ref);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "mesh")) {
         myMeshInfo.scan (rtok);  
         setMeshFromInfo();
         return true;
      }
      else if (scanAttributeName (rtok, "pose")) {
         RigidTransform3d X = new RigidTransform3d();
         X.scan (rtok);
         myState.setPose (X);
         return true;
      }
      else if (scanAttributeName (rtok, "position")) {
         Point3d pos = new Point3d();
         pos.scan (rtok);
         setPosition (pos);
         return true;
      }
      else if (scanAttributeName (rtok, "rotation")) {
         Quaternion q = new Quaternion();
         q.scan (rtok);
         setRotation (q);
         return true;
      }
      else if (scanAttributeName (rtok, "vel")) {
         rtok.scanToken ('[');
         myState.vel.scan (rtok);
         rtok.scanToken (']');
         return true;
      }
      else if (scanAttributeName (rtok, "inertia")) {
         SpatialInertia M = new SpatialInertia();
         M.scan (rtok);
         setInertia (M);           
         return true;
      }
      else if (scanAttributeName (rtok, "mass")) {
         double mass = rtok.scanNumber();
         setMass (mass);
         return true;
      }
      else if (scanAttributeName (rtok, "density")) {
         double density = rtok.scanNumber();
         setDensity (density);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt);
      pw.println ("position=[ " + myState.getPosition().toString (fmt) + "]");
      pw.println ("rotation=[ " + myState.getRotation().toString (fmt) + "]");
      // pw.println ("pose=" + myState.XFrameToWorld.toString (
      //    fmt, RigidTransform3d.AXIS_ANGLE_STRING));
      if (!myState.vel.v.equals (zeroVect) || !myState.vel.w.equals (zeroVect)) {
         pw.print ("vel=[ ");
         pw.print (myState.vel.toString (fmt));
         pw.println (" ]");
      }
      switch (myInertiaMethod) {
         case Explicit: {
            pw.println (
               "inertia=" + mySpatialInertia.toString (
                  fmt, SpatialInertia.MASS_INERTIA_STRING));
            break;
         }
         case Mass: {
            pw.println ("mass=" + fmt.format (getMass()));
            break;
         }
         case Density: {
            pw.println ("density=" + fmt.format (getDensity()));
            break;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented inertia method " + myInertiaMethod);
         }
      }
      myMeshInfo.write (pw, fmt);      
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      dowrite (pw, fmt, ref);
   }


   /* ======== Renderable implementation ======= */

   public RenderProps createRenderProps() {
      return RenderProps.createRenderProps (this);
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      PolygonalMesh mesh = getMesh();
      if (mesh != null) {
         mesh.updateBounds (pmin, pmax);
      }
      else {
         myState.pos.updateBounds (pmin, pmax);
      }
   }

   public void render (Renderer renderer, int flags) {
      if (myAxisLength > 0) {
         int lineWidth = myRenderProps.getLineWidth();
         renderer.drawAxes (
            myRenderFrame, myAxisLength, lineWidth, isSelected());
      }
      if (isSelected()) {
         flags |= Renderer.HIGHLIGHT;
      }
      if (myRenderDistanceSurface && getDistanceSurface() != null) {
         PolygonalMesh surf = getDistanceSurface();
         surf.render (renderer, myRenderProps, flags);
      }
      else {
         myMeshInfo.render (renderer, myRenderProps, flags);
      }
      if (myRenderDistanceGrid && getDistanceGrid() != null) {
         getDistanceGrid().render (renderer, myRenderProps, flags);
      }
   }

   public void prerender (RenderList list) {
      myRenderFrame.set (myState.XFrameToWorld);
      // list.addIfVisible (myMarkers);
      if (myRenderDistanceSurface && getDistanceSurface() != null) {
         PolygonalMesh surf = getDistanceSurface();
         surf.prerender (myRenderProps); 
      }
      else {
         myMeshInfo.prerender (myRenderProps);      
      }
      if (myRenderDistanceGrid && getDistanceGrid() != null) {
         getDistanceGrid().prerender (list);
      }
   }
   
   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      
      super.transformGeometry (gtr, context, flags);
      PolygonalMesh mesh = getMesh();
      if (mesh != null) {
         // for now, only transform the mesh if we are simulating. Otherwise,
         // just update the mesh's transform.
         if ((flags & TransformableGeometry.TG_SIMULATING) == 0) {
            if (myMeshInfo.transformGeometryAndPose (
                  gtr, myTransformConstrainer)) {
               if (myInertiaMethod == InertiaMethod.Density) {
                  setInertiaFromMesh (myDensity);
               }
            }
         }
         else {
            mesh.setMeshToWorld (myState.XFrameToWorld);
         }
         mySDGridValid = false;
         mySDSurface = null;
      }
   }   

   public void scaleDistance (double s) {
      super.scaleDistance (s);
      this.myAxisLength *= s;
      mySpatialInertia.scaleDistance (s);
      // probably don't need this, since effectiveInertia will be recalculated:
      // myEffectiveInertia.scaleDistance (s); 
      PolygonalMesh mesh = getMesh();
      if (mesh != null) {
         mesh.scale (s);
         mesh.setMeshToWorld (myState.XFrameToWorld);
         mySDGridValid = false;
         mySDSurface = null;
      }
      
      myDensity /= (s * s * s);
      // Should we send a GEOMETRY_CHANGED event? Don't for now ...
   }

   public void scaleMass (double s) {
      mySpatialInertia.scaleMass (s);
      // probably don't need this, since effectiveIneria will be recalculated:
      // myEffectiveInertia.scaleMass (s);
      myDensity *= s;
      myFrameDamping *= s;
      myRotaryDamping *= s;
   }

   public void setDynamic (boolean dynamic) {
      super.setDynamic (dynamic);
   }

   // protected void notifyStructureChanged (Object comp) {
   //    if (comp instanceof CompositeComponent) {
   //       notifyParentOfChange (new StructureChangeEvent (
   //          (CompositeComponent)comp));
   //    }
   //    else {
   //       notifyParentOfChange (StructureChangeEvent.defaultEvent);
   //    }
   // }

   /**
    * {@inheritDoc}
    */
   public boolean isDuplicatable() {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public boolean getCopyReferences (
      List<ModelComponent> refs, ModelComponent ancestor) {
      return true;
   }

   @Override
   public RigidBody copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      RigidBody comp = (RigidBody)super.copy (flags, copyMap);

      // comp.myAttachments = new ArrayList<PointFrameAttachment>();
      // comp.myComponents = new ArrayList<ModelComponent>();
      // comp.indicesValidP = false;
      comp.setDynamic (true);

      comp.mySpatialInertia = new SpatialInertia (mySpatialInertia);
      // comp.myEffectiveInertia = new SpatialInertia (mySpatialInertia);
      PolygonalMesh mesh = getMesh();
      comp.myMeshInfo = new MeshInfo();
      if (mesh != null) {
         PolygonalMesh meshCopy = mesh.copy();
         comp.setMesh (meshCopy, getMeshFileName(), getMeshFileTransform());
         comp.myMeshInfo.myFlippedP = myMeshInfo.myFlippedP;
      }
      else {
         comp.setMesh (null, null, null);
      }
      // comp.myMarkers =
      // new PointList<FrameMarker> (
      // FrameMarker.class, "markers", "k");
      // comp.add (comp.myMarkers);

      comp.myBodyForce = new Wrench();
      comp.myCoriolisForce = new Wrench();
      comp.myBodyVel = new Twist();
      comp.myBodyAcc = new Twist();
      comp.myQvel = new Quaternion();
      comp.myTmpPos = new Point3d();
      comp.myConnectors = null;

      return comp;
   }

   /**
    * Returns an array of all FrameMarkers currently associated with this rigid
    * body.
    * 
    * @return list of all frame markers
    */
   public FrameMarker[] getFrameMarkers() {
      LinkedList<FrameMarker> list = new LinkedList<FrameMarker>();
      if (myMasterAttachments != null) {
         for (DynamicAttachment a : myMasterAttachments) {
            if (a.getSlave() instanceof FrameMarker) {
               list.add ((FrameMarker)a.getSlave());
            }
         }
      }
      return list.toArray (new FrameMarker[0]);
   }

   public void addConnector (BodyConnector c) {
      if (myConnectors == null) {
         myConnectors = new ArrayList<BodyConnector>();
      }
      myConnectors.add (c);
   }

   public void removeConnector (BodyConnector c) {
      if (myConnectors == null || !myConnectors.remove (c)) {
         throw new InternalErrorException ("connector not found");
      }
      if (myConnectors.size() == 0) {
         myConnectors = null;
      }
   }
   
   public List<BodyConnector> getConnectors() {
      return myConnectors;
   }
   
   public boolean isFreeBody() {
      return !isParametric();
   }

   public void updateAttachmentPosStates() {
      if (myMasterAttachments != null) {
         for (DynamicAttachment a : myMasterAttachments) {
            a.updatePosStates();
         }
      }
   }

   /** 
    * Creates an spherical RigidBody with a prescribed uniform density.
    * The sphere is centered on the origin.
    * 
    * @param bodyName name of the RigidBody
    * @param r radius of the sphere
    * @param density density of the body
    * @param nslices number of slices used in creating the mesh
    * @return spherical rigid body
    */
   public static RigidBody createSphere (
      String bodyName, double r, double density, int nslices) {

      RigidBody body = new RigidBody (bodyName);
      PolygonalMesh mesh = MeshFactory.createSphere (r, nslices);
      body.setMesh (mesh, null);
      double mass = 4/3.0*Math.PI*r*r*r*density;
      body.setInertia (SpatialInertia.createSphereInertia (mass, r));
      return body;
   }

   /** 
    * Creates an icosahedrally spherical RigidBody with a prescribed uniform
    * density.  The sphere is centered on the origin.
    * 
    * @param bodyName name of the RigidBody
    * @param r radius of the sphere
    * @param density density of the body
    * @param ndivisions number of divisions used in creating the mesh
    * @return spherical rigid body
    */
   public static RigidBody createIcosahedralSphere (
      String bodyName, double r, double density, int ndivisions) {

      RigidBody body = new RigidBody (bodyName);
      PolygonalMesh mesh = MeshFactory.createIcosahedralSphere (
         r, Point3d.ZERO, ndivisions);
      body.setMesh (mesh, null);
      double mass = 4/3.0*Math.PI*r*r*r*density;
      body.setInertia (SpatialInertia.createSphereInertia (mass, r));
      return body;
   }

   /** 
    * Creates an ellipsoidal RigidBody with a prescribed uniform density.
    * The ellipsoid is centered on the origin.
    * 
    * @param bodyName name of the RigidBody
    * @param a semi-axis length in the x direction
    * @param b semi-axis length in the y direction
    * @param c semi-axis length in the z direction
    * @param density density of the body
    * @param nslices number of slices used in creating the mesh
    * @return ellipsoidal rigid body
    */
   public static RigidBody createEllipsoid (
      String bodyName, double a, double b, double c,
      double density, int nslices) {

      RigidBody body = new RigidBody (bodyName);
      PolygonalMesh mesh = MeshFactory.createSphere (1.0, nslices);
      AffineTransform3d XScale = new AffineTransform3d();
      XScale.applyScaling (a, b, c);
      mesh.transform (XScale);
      body.setMesh (mesh, null);
      double mass = 4/3.0*Math.PI*a*b*c*density;
      body.setInertia (
         SpatialInertia.createEllipsoidInertia (mass, a, b, c));
      return body;
   }

   /** 
    * Creates a box-shaped RigidBody with a prescribed uniform density.
    * The box is centered on the origin.
    * 
    * @param bodyName name of the RigidBody
    * @param wx width of the box in the x direction
    * @param wy width of the box in the y direction
    * @param wz width of the box in the z direction
    * @param density density of the body
    * @return box-shaped rigid body
    */
   public static RigidBody createBox (
      String bodyName, double wx, double wy, double wz, double density, boolean addNormals) {

      RigidBody body = new RigidBody (bodyName);
      PolygonalMesh mesh = MeshFactory.createBox (wx, wy, wz, addNormals);
      body.setInertiaFromDensity (density);
      body.setMesh (mesh, null);
      return body;
   }
   
   /** 
    * Creates a box-shaped RigidBody with a prescribed uniform density.
    * The box is centered on the origin.
    * 
    * @param bodyName name of the RigidBody
    * @param wx width of the box in the x direction
    * @param wy width of the box in the y direction
    * @param wz width of the box in the z direction
    * @param density density of the body
    * @return box-shaped rigid body
    */
   public static RigidBody createBox (
      String bodyName, double wx, double wy, double wz, double density) {

      RigidBody body = new RigidBody (bodyName);
      PolygonalMesh mesh = MeshFactory.createBox (wx, wy, wz);
      body.setInertiaFromDensity (density);
      body.setMesh (mesh, null);
      return body;
   }

   /** 
    * Creates a cylindrical RigidBody with a prescribed uniform density.  The
    * cylinder is centered on the origin and is parallel to the z axis.
    * 
    * @param bodyName name of the RigidBody
    * @param r cylinder radius
    * @param h cylinder height
    * @param density density of the body
    * @param nsides number of sides used in creating the cylinder mesh
    * @return cylindrical rigid body
    */
   public static RigidBody createCylinder (
      String bodyName, double r, double h,
      double density, int nsides) {

      RigidBody body = new RigidBody (bodyName);
      PolygonalMesh mesh =
         MeshFactory.createCylinder (r, h, nsides);
      body.setMesh (mesh, null);
      // body.setDensity (density);
      double mass = Math.PI*r*r*h*density;
      body.setInertia (
         SpatialInertia.createCylinderInertia (mass, r, h));
      return body;
   }

   public static RigidBody createFromMesh (
      String bodyName, PolygonalMesh mesh, double density, double scale) {
      
      return createFromMesh (bodyName, mesh, null, density, scale);
   }

   public static RigidBody createFromMesh (
      String bodyName, PolygonalMesh mesh, String meshFilePath,
      double density, double scale) {

      RigidBody body = new RigidBody (bodyName);
      mesh.triangulate(); // just to be sure ...
      mesh.scale (scale);
      AffineTransform3d X = null;
      if (scale != 1) {
         X = AffineTransform3d.createScaling (scale);
      }
      body.setDensity (density);
      body.setMesh (mesh, meshFilePath, X);
      return body;
   }
      
   public static RigidBody createFromMesh (
      String bodyName, String meshPath, double density, double scale) {

      PolygonalMesh mesh = null;
      try {
         mesh = new PolygonalMesh (new File (meshPath));
      }
      catch (IOException e) {
         System.out.println ("Can't create mesh from "+meshPath);
         e.printStackTrace();
         return null;
      }
      return createFromMesh (bodyName, mesh, meshPath, density, scale);
   }

   public static RigidBody createFromMesh (
      String bodyName, Object obj, String relPath, double density, double scale){

      String path = ArtisynthPath.getSrcRelativePath (obj, relPath);
      return createFromMesh (bodyName, path, density, scale);
   }
    

   //   private void recursivelyFindFreeAttachedBodies (
   //      LinkedHashSet<RigidBody> bodies, RigidBody top) {
   //
   //      if (myConnectors != null) {
   //         for (BodyConnector c : myConnectors) {
   //         }
   //      }
   //   }
  
//   private static RigidBody getOtherBody (
//      BodyConnector c, RigidBody body) {
//      
//      if (c.myBodyA == body) {
//         return c.myBodyB;
//      }
//      else if (c.myBodyB == body) {
//         return c.myBodyA;
//      }
//      else {
//         throw new InternalErrorException (
//            "Connector does not reference body " +
//            ComponentUtils.getPathName(body));
//      }
//   }
//
//   private static boolean recursivelyFindFreeAttachedBodies (
//      RigidBody body, LinkedList<RigidBody> list, boolean rejectSelected) {
//
//      if (body == null) {
//         return false;
//      }
//      boolean isFree = true;     
//      if (!body.isMarked()) {
//         body.setMarked (true);
//         list.add (body);
//         if (body.isParametric()) {
//            isFree = false;
//         }
//         if (rejectSelected && body.isSelected()) {
//            isFree = false;
//         }
//         if (body.myConnectors != null) {
//            for (BodyConnector c : body.myConnectors) {
//               RigidBody obody = getOtherBody (c, body);
//               if (!recursivelyFindFreeAttachedBodies (
//                      obody, list, rejectSelected)) {
//                  isFree = false;
//               }
//            }
//         }
//      }
//      return isFree;
//   }
//  
//   public boolean findFreeAttachedBodies (
//      List<RigidBody> freeBodies, boolean rejectSelected) {
//      LinkedList<RigidBody> nonfreeBodies = new LinkedList<RigidBody>();
//      LinkedList<RigidBody> list = new LinkedList<RigidBody>();
//      boolean allFree = true;
//      setMarked (true);
//      if (isParametric() || (rejectSelected && isSelected())) {
//         allFree = false;
//      }
//      if (myConnectors != null) {
//         for (BodyConnector c : myConnectors) {
//            RigidBody body = getOtherBody (c, this);
//            list.clear();
//            if (recursivelyFindFreeAttachedBodies (body, list, rejectSelected)) {
//               freeBodies.addAll (list);
//            }
//            else {
//               nonfreeBodies.addAll (list);
//               allFree = false;
//            }
//         }
//      }
//      for (RigidBody b : freeBodies) {
//         b.setMarked (false);
//      }
//      for (RigidBody b : nonfreeBodies) {
//         b.setMarked (false);
//      }
//      setMarked (false);
//      return allFree;
//   }

   // public ArrayList<RigidTransform3d> getRelativePoses (
   //    List<RigidBody> bodies) {
   //    ArrayList<RigidTransform3d> poses =
   //       new ArrayList<RigidTransform3d>(bodies.size());

   //    for (RigidBody bod : bodies) {
   //       // X is the transform from bod to this body
   //       RigidTransform3d X = new RigidTransform3d();
   //       X.mulInverseLeft (getPose(), bod.getPose());
   //       poses.add (X);
   //    }
   //    return poses;
   // }

   // public void setRelativePoses (
   //    List<RigidBody> bodies, ArrayList<RigidTransform3d> poses) {
      
   //    if (bodies.size() != poses.size()) {
   //       throw new IllegalArgumentException (
   //          "Error: poses and bodies have inconsistent sizes");
   //    }
   //    int i = 0;
   //    RigidTransform3d X = new RigidTransform3d();
   //    for (RigidBody bod : bodies) {
   //       X.mul (getPose(), poses.get(i));
   //       bod.setPose (X);
   //       i++;
   //    }
   // }
   
   // begin Collidable interface

   @Override
   public PolygonalMesh getCollisionMesh() {
      return getMesh();
   }

   /**
    * Returns <code>true</code> if this RigidBody supports a signed
    * distance grid that can be used with a SignedDistanceCollider.
    * A grid is available if either the property
    * <code>distanceGridMaxRes</code> is positive, or
    * the property <code>distanceGridRes</code> contains all positive values.
    * 
    * @return <code>true</code> if a signed distance grid is available
    * for this RigidBody
    * @see #getDistanceGridMaxRes
    * @see #getDistanceGridRes
    */
   public boolean hasDistanceGrid() {
      return (myDistanceGridMaxRes > 0 ||
              !myDistanceGridRes.equals (Vector3i.ZERO));
   }

   /**
    * Returns a signed distance grid that can be used with a
    * SignedDistanceCollider, or <code>null</code> if a grid is not available
    * (i.e., if {@link #hasDistanceGrid} returns <code>false</code>).
    * The number of divisons in the grid is controlled explicitly by the
    * property <code>distanceGridRes</code>, or, that is 0, by the property
    * <code>distanceGridMaxRes</code>. If both properties are 0, no grid is
    * available and <code>null</code> will be returned.
    *
    * @return signed distance grid, or <code>null</code> if a grid is
    * not available this RigidBody
    * @see #getDistanceGridMaxRes
    * @see #getDistanceGridRes
    */
   public SignedDistanceGrid getDistanceGrid() {
      if (mySDGrid == null || !mySDGridValid) {
         if (!myDistanceGridRes.equals (Vector3i.ZERO)) {
            mySDGrid = new SignedDistanceGrid (
               getMesh(), myGridMargin, myDistanceGridRes);
         }
         else if (myDistanceGridMaxRes > 0) {
             mySDGrid = new SignedDistanceGrid (
               getMesh(), myGridMargin, myDistanceGridMaxRes);           
         }
         mySDGridValid = true;
      }
      return mySDGrid;
   }

   public PolygonalMesh getDistanceSurface() {
      if (mySDSurface == null) {
         if (hasDistanceGrid()) {
            SignedDistanceGrid grid = getDistanceGrid();
            mySDSurface = grid.computeDistanceSurface();
            mySDSurface.setMeshToWorld (myState.XFrameToWorld);
         }
      }
      return mySDSurface;
   }

   @Override
   public Collidability getCollidable () {
      getSurfaceMesh(); // build surface mesh if necessary
      return myCollidability;
   }

   @Override
   public Collidable getCollidableAncestor() {
      return null;
   }

   @Override
   public boolean isCompound() {
      return false;
   }

   public void setCollidable (Collidability c) {
      if (myCollidability != c) {
         myCollidability = c;
         notifyParentOfChange (new StructureChangeEvent (this));
      }
   }

   @Override
   public boolean isDeformable () {
      return false;
   }

   public void getVertexMasters (List<ContactMaster> mlist, Vertex3d vtx) {
      mlist.add (new ContactMaster (this, 1));
   }
   
   public boolean containsContactMaster (CollidableDynamicComponent comp) {
      return comp == this;
   }  
   
   public boolean allowCollision (
      ContactPoint cpnt, Collidable other, Set<Vertex3d> attachedVertices) {
      return true;
   }

   public int getCollidableIndex() {
      return myCollidableIndex;
   }
   
   public void setCollidableIndex (int idx) {
      myCollidableIndex = idx;
   }
   
   // end Collidable interface

   // begin HasDistanceGrid

   /**
    * {@inheritDoc}
    */
   public void setDistanceGridMaxRes (int max) {
      if (myDistanceGridMaxRes != max) {
         if (myDistanceGridRes.equals (Vector3i.ZERO)) {
            // will need to rebuild grid
            mySDGridValid = false;
            mySDSurface = null;
         }
         if (max < 0) {
            // MaxDivs == 0 and Divs == (0,0,0) disables grid
            max = 0;
         }
         myDistanceGridMaxRes = max;
         myDistanceGridRenderRanges = DEFAULT_DISTANCE_GRID_RENDER_RANGES;
      }
   }

   /**
    * {@inheritDoc}
    */
   public int getDistanceGridMaxRes () {
      return myDistanceGridMaxRes;
   }

   /**
    * {@inheritDoc}
    */
   public void setDistanceGridRes (Vector3i res) {
      if (!myDistanceGridRes.equals (res)) {
         if (res.x <= 0 || res.y <= 0 || res.z <= 0) {
            // MaxDivs == 0 and Divs == (0,0,0) disables grid
            myDistanceGridRes.setZero();
         }
         else {
            myDistanceGridRes.set (res);
         }
         mySDGridValid = false; // will need to rebuild grid
         mySDSurface = null;
         myDistanceGridRenderRanges = DEFAULT_DISTANCE_GRID_RENDER_RANGES;
      }
   }

   /**
    * {@inheritDoc}
    */
   public Vector3i getDistanceGridRes () {
      return myDistanceGridRes;
   }

   /**
    * {@inheritDoc}
    */
   public boolean getRenderDistanceGrid() {
      return myRenderDistanceGrid;
   }

   /**
    * {@inheritDoc}
    */
   public void setRenderDistanceGrid(boolean enable) {
      myRenderDistanceGrid = enable;
   }

   /**
    * Queries whether or not rendering of the distance surface is
    * enabled.
    *
    * @return <code>true</code> if rendering of the distance surface is enabled
    * @see #setRenderDistanceSurface
    */
   public boolean getRenderDistanceSurface() {
      return myRenderDistanceSurface;
   }

   /**
    * Enables or disables rendering of the distance surface. If enabled, then
    * an approximation of the isosurface for the signed distance grid is
    * rendered <i>instead</i> of the surface mesh. This can give the user a
    * better understanding of how the distance grid approximates the associated
    * mesh.
    *
    * @param enable if <code>true</code>, enables rendering of the distance
    * surface
    * @see #getRenderDistanceSurface
    */
   public void setRenderDistanceSurface(boolean enable) {
      myRenderDistanceSurface = enable;
   }

   /**
    * {@inheritDoc}
    */
   public String getDistanceGridRenderRanges() {
      return myDistanceGridRenderRanges;
   }
   
   /**
    * {@inheritDoc}
    */
   public void setDistanceGridRenderRanges (String ranges) {
      if (ranges.matches ("\\s*\\*\\s*") ||
          ranges.matches ("\\s*\\*\\s+\\*\\s+\\*\\s*")) {
         myDistanceGridRenderRanges = "* * *";
         if (mySDGrid != null) {
            mySDGrid.setRenderRanges (ranges);
         }
      }
      else {
         SignedDistanceGrid grid = getDistanceGrid();
         StringHolder errMsg = new StringHolder();
         if (grid.parseRenderRanges (ranges, errMsg) == null) {
            throw new IllegalArgumentException (
               "Illegal range spec: " + errMsg.value);
         }
         else {
            grid.setRenderRanges (ranges);
            myDistanceGridRenderRanges = grid.getRenderRanges();
         }
      }
   }
   
   protected class DistanceGridRenderRangesRange extends RangeBase {

      @Override
      public boolean isValid (Object obj, StringHolder errMsg) {
         if (!(obj instanceof String)) {
            errMsg.value = "Object is not a string";
            return false;
         }
         String ranges = (String)obj;
         if (ranges.matches ("\\s*\\*\\s*") ||
             ranges.matches ("\\s*\\*\\s+\\*\\s+\\*\\s*")) {
            return true;
         }
         else if (mySDGrid != null) {
            mySDGrid.parseRenderRanges (ranges, errMsg);
            return errMsg.value == null;
         }
         else {
            errMsg.value = 
               "Ranges must be '* * *' when distance grid uninitialized";
            return false;
         }
      }
   }

   public Range getDistanceGridRenderRangesRange() {
      return new DistanceGridRenderRangesRange();
   }

}
