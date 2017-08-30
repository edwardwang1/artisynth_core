/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import maspack.geometry.GeometryTransformer;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Matrix;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3x3Block;
import maspack.matrix.Matrix3x3DiagBlock;
import maspack.matrix.MatrixBlock;
import maspack.matrix.Point3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.Renderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.RenderableUtils;
import maspack.util.IndentingPrintWriter;
import maspack.util.NumberFormat;
import artisynth.core.mechmodels.MotionTarget.TargetActivity;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.Traceable;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.util.ScalableUnits;

public class Point extends DynamicComponentBase
   implements TransformableGeometry, ScalableUnits,
              DynamicComponent, Traceable, MotionTargetComponent, 
              CopyableComponent, CollidableDynamicComponent {

   protected PointState myState = new PointState();
   protected PointTarget myTarget = null;
   protected TargetActivity myTargetActivity = TargetActivity.Auto;
   // XXX add in render properties
   protected Vector3d myForce;
   protected Vector3d myExternalForce;
   public float[] myRenderCoords = new float[3];
   // protected Activity myActivity = Activity.Unknown;
   protected boolean mySolveBlockValidP;
   private int mySolveBlockNum = -1;
   protected double myPointDamping;
   private PropertyMode myPointDampingMode = PropertyMode.Inherited;

   protected Frame myPointFrame = null;
   protected PointState myWorldState = null;

   public static PropertyList myProps =
      new PropertyList (Point.class, ModelComponentBase.class);

   static {
      myProps.add ("renderProps * *", "render properties", null);
      myProps.add ("position * *", "position state", Point3d.ZERO, "%.8g");
      myProps.add ("velocity * *", "velocity state", Vector3d.ZERO, "%.8g");
      myProps.add ("targetPosition", "position target", Point3d.ZERO, "%.8g NW");
      myProps.add ("targetVelocity", "velocity target", Vector3d.ZERO, "%.8g NW");
      myProps.add ("targetActivity", "specifies which targets are active",
                   TargetActivity.Auto, "NW");
      myProps.addReadOnly ("force", "total force", "%8.3f");
      myProps.add ("externalForce * *", "external force", null, "%.8g NW");
      myProps.addInheritable (
         "pointDamping:Inherited", "intrinsic damping force", 0.0, "%.8g");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public Point() {
      myState = new PointState();
      myForce = new Vector3d();
      myWorldState = myState;
      myExternalForce = new Vector3d();
      mySolveBlockValidP = false;
   }

   public Point (Point3d pnt) {
      this();
      myState.pos.set (pnt);
   }

//   public PointState getState() {
//      return myState;
//   }

   public void setPointFrame (Frame frame) {
      if (frame != myPointFrame) {
         myPointFrame = frame;
         if (frame == null) {
            // removing the frame, so world state and state will now be the same
            myState.set (myWorldState);
            myWorldState = myState;
         }
         else {
            // adding or replacing frame, so compute new local state to 
            // match current world state
            PointState worldState = myWorldState;
            if (myWorldState == myState) {
               myWorldState = new PointState();
            }
            setPosition (worldState.pos);
            setVelocity (worldState.vel);
         }
         DynamicAttachment sa = getAttachment();
         if (sa != null) {
            sa.invalidateMasters();
         }
         List<DynamicAttachment> masters = getMasterAttachments();
         if (masters != null) {
            for (DynamicAttachment ma : masters) {
               ma.invalidateMasters();
            }
         }
      }
   }

   public Frame getPointFrame() {
      return myPointFrame;
   }

   public Vector3d getForce() {
      return myForce;
   }

   public void setForce (Vector3d f) {
      myForce.set (f);
   }

   public void addForce (Vector3d f) {
      myForce.add (f);
      if (myPointFrame != null) {
         myPointFrame.addPointForce (getLocalPosition(), f);
      }
   }

   public void addScaledForce (double s, Vector3d f) {
      myForce.scaledAdd (s, f, myForce);
      if (myPointFrame != null) {
         Vector3d fscaled = new Vector3d();
         fscaled.scale (s, f);
         myPointFrame.addPointForce (getLocalPosition(), fscaled);
      }
   }

   public void subForce (Vector3d f) {
      myForce.sub (f);
      if (myPointFrame != null) {
         myPointFrame.subPointForce (getLocalPosition(), f);
      }
   }

   public void addLocalForce (Vector3d f) {
      myForce.add (f);
   }

   public void addScaledLocalForce (double s, Vector3d f) {
      myForce.scaledAdd (s, f, myForce);
   }

   public void subLocalForce (Vector3d f) {
      myForce.sub (f);
   }

   public void zeroForces() {
      myForce.setZero();
   }

   // public void setForcesToExternal() {
   //    myForce.set (myExternalForce);
   // }

   public void applyExternalForces() {
      myForce.add (myExternalForce);
      if (myPointFrame != null) {
         myPointFrame.addPointForce (getLocalPosition(), myExternalForce);
      }
   }

   public void addExternalForce(Vector3d f) {
      myExternalForce.add(f);
   }
   
   public void addScaledExternalForce(double s, Vector3d f) {
      myExternalForce.scaledAdd(s, f);
   }
   
   public Vector3d getExternalForce() {
      return myExternalForce;
   }

   public void setExternalForce (Vector3d f) {
      myExternalForce.set (f);
   }

   public void zeroExternalForces() {
      myExternalForce.setZero();
   }

   public void setScaledExternalForce (double s, Vector3d f) {
      myExternalForce.scale (s, f);
   }

   public void applyForces (double t) {
      if (myPointDamping != 0) {
         addScaledForce (-myPointDamping, myWorldState.vel);
      }
   }

   public void addVelJacobian (SparseNumberedBlockMatrix S, double s) {
      if (myPointDamping != 0) {
         addToSolveBlockDiagonal (S, -s * myPointDamping);
      }
   }

   public void addPosJacobian (SparseNumberedBlockMatrix S, double s) {
      // nothing to do
   }

   public void addSolveBlocks (SparseNumberedBlockMatrix S) {
      // nothing to do
   }

   public int getJacobianType() {
      return Matrix.SPD;
   }

   /**
    * Returns the current position of this point, in world coordinates. The
    * returned value should be treated as read-only and not modified.
    *
    * @return current world position (read-only)
    */
   public Point3d getPosition() {
      return myWorldState.pos;
   }

   public void getPosition (Point3d pos) {
      pos.set (myWorldState.pos);
   }

   public void setPosition (Point3d p) {
      myWorldState.pos.set (p);
      if (myState != myWorldState) {
         myPointFrame.computePointLocation (myState.pos, p);
      }
   }

   public void setPosition (double x, double y, double z) {
      setPosition (new Point3d (x, y, z));
   }

   // Sanchez, March 27, 2013
   // Changed to getPosition() instead of myState.pos
   // so I can use correct position in MFreeModel
   public double distance (Point pnt) {
      return getPosition().distance (pnt.getPosition());
   }

   public double distance (Point3d pos) {
      return getPosition().distance (pos);
   }

   public int getPosState (double[] x, int idx) {
      x[idx++] = myState.pos.x;
      x[idx++] = myState.pos.y;
      x[idx++] = myState.pos.z;
      return idx;
   }

   // public void scalePosition (double s) {
   //    myState.pos.scale (s);
   // }

//   public void addPosition (double dx, double dy, double dz) {
//      myState.pos.add (dx, dy, dz);
//   }

//   /**
//    * Adds a scaled vector to this point's position.
//    * 
//    * @param s
//    * scale factor
//    * @param v
//    * vector to scale and add
//    */
//   public void addScaledPosition (double s, Vector3d v) {
//      myState.pos.scaledAdd (s, v);
//   }

   public int setPosState (double[] p, int idx) {
      myState.pos.x = p[idx++];
      myState.pos.y = p[idx++];
      myState.pos.z = p[idx++];
      updatePosState(); // update world state if necessary
      return idx;
   }

   public void addPosImpulse (
      double[] xbuf, int xidx, double h, double[] vbuf, int vidx) {

      xbuf[xidx  ] += h*vbuf[vidx  ];
      xbuf[xidx+1] += h*vbuf[vidx+1];
      xbuf[xidx+2] += h*vbuf[vidx+2];
   }

   public int getPosDerivative (double[] dxdt, int idx) {
      dxdt[idx++] = myState.vel.x;
      dxdt[idx++] = myState.vel.y;
      dxdt[idx++] = myState.vel.z;
      return idx;
   }

   public Vector3d getVelocity() {
      return myWorldState.vel;
   }

   public void getVelocity (Vector3d vel) {
      vel.set (myWorldState.vel);
   }

   public void setVelocity (Vector3d vel) {
      myWorldState.vel.set (vel);
      if (myState != myWorldState) {
         // compute corresponding local velocity
         Vector3d tmp = new Vector3d();
         myPointFrame.computePointVelocity (tmp, myState.pos);
         tmp.sub (vel, tmp);
         tmp.inverseTransform (myPointFrame.getPose().R);
         myState.vel.set (tmp);
      }
   }

   public void setVelocity (double x, double y, double z) {
      setVelocity (new Vector3d (x, y, z));
   }

   public int getVelState (double[] v, int idx) {
      v[idx++] = myState.vel.x;
      v[idx++] = myState.vel.y;
      v[idx++] = myState.vel.z;
      return idx;
   }

//   public void addVelocity (double dx, double dy, double dz) {
//      myState.vel.add (dx, dy, dz);
//   }
//
//   public void addScaledVelocity (double s, Vector3d v) {
//      myState.vel.scaledAdd (s, v, myState.vel);
//   }

   public int setVelState (double[] v, int idx) {
      myState.vel.x = v[idx++];
      myState.vel.y = v[idx++];
      myState.vel.z = v[idx++];
      updateVelState();
      return idx;
   }

   public int setForce (double[] f, int idx) {
      myForce.x = f[idx++];
      myForce.y = f[idx++];
      myForce.z = f[idx++];
      if (myState != myWorldState) {
         myForce.transform (myPointFrame.getPose());
      }
      return idx;
   }
 
   public int getForce (double[] f, int idx) {
      Vector3d force;
      if (myState == myWorldState) {
         force = myForce;
      }
      else {
         force = new Vector3d();
         force.inverseTransform (myPointFrame.getPose(), myForce);
      }
      f[idx++] = force.x;
      f[idx++] = force.y;
      f[idx++] = force.z;
      return idx;
   }
 
   public void resetTargets() {
      if (myTarget != null) {
         myTarget.syncState (TargetActivity.None, myState);
      }      
   }

   public TargetActivity getTargetActivity () {
      if (myTarget == null) {
         return myTargetActivity;
      }
      else {
         return myTarget.getActivity();
      }
   }

   public void setTargetActivity (TargetActivity activity) {
      if (activity == null) {
         throw new IllegalArgumentException ("activity cannot be null");
      }
      if (activity == TargetActivity.None) {
         myTarget = null;
      }
      else {
         TargetActivity prevActivity = TargetActivity.None;
         if (myTarget == null) {
            myTarget = new PointTarget (activity);
         }
         else {
            prevActivity = myTarget.getActivity();
            myTarget.setActivity (activity);
         }
         myTarget.syncState (prevActivity, myState);
      }
      myTargetActivity = activity;
   }

   protected void setDynamic (boolean dynamic) {
      if (myDynamicP && !dynamic) {
         if (myTarget != null) {
            myTarget.syncState (TargetActivity.None, myState);
         }
      }
      super.setDynamic (dynamic);
   }         

   public Point3d getTargetPosition () {
      if (myTarget == null) {
         return myState.pos;
      }
      else {
         return myTarget.getTargetPos (myState);
      }
   }

   public void setTargetPosition (Point3d pos) {
      if (myTarget == null) {
         myTarget = new PointTarget (myTargetActivity);
      }
      myTarget.setTargetPos (pos);
   }

   public Vector3d getTargetVelocity () {
      if (myTarget == null) {
         return myState.vel;
      }
      else {
         return myTarget.getTargetVel (myState);
      }
   }

   public void setTargetVelocity (Vector3d vel) {
      if (myTarget == null) {
         myTarget = new PointTarget (myTargetActivity);
      }
      myTarget.setTargetVel (vel);
   }

   public int getTargetVel (double[] velt, double s, double h, int idx) {
      if (myTarget == null) {
         return myState.getVel (velt, idx);
      }
      else {
         return myTarget.getTargetVel (velt, s, h, myState, idx);
      }
   }

   public int setTargetVel (double[] velt, int idx) {
      if (myTarget == null) {
         myTarget = new PointTarget (myTargetActivity);
      }
      return myTarget.setTargetVel (velt, idx);
   }

   public int getTargetPos (double[] post, double s, double h, int idx) {
      if (myTarget == null) {
         return myState.getPos (post, idx);
      }
      else {
         return myTarget.getTargetPos (post, s, h, myState, idx);
      }
   }

   public int setTargetPos (double[] post, int idx) {
      if (myTarget == null) {
         myTarget = new PointTarget (myTargetActivity);
      }
      return myTarget.setTargetPos (post, idx);
   }

   /** 
    * {@inheritDoc}
    */
   public int addTargetJacobian (SparseBlockMatrix J, int bi) {
      if (!isControllable()) {
         throw new IllegalStateException (
            "Target point is not controllable");
      }
      Matrix3x3DiagBlock blk = new Matrix3x3DiagBlock();
      blk.setIdentity();
      J.addBlock (bi, getSolveIndex(), blk);
      return bi++;
   }

   public double getPointDamping() {
      return myPointDamping;
   }

   public void setPointDamping (double d) {
      myPointDamping = d;
      myPointDampingMode =
         PropertyUtils.propagateValue (
            this, "pointDamping", d, myPointDampingMode);
   }

   public PropertyMode getPointDampingMode() {
      return myPointDampingMode;
   }

   public void setPointDampingMode (PropertyMode mode) {
      myPointDampingMode =
         PropertyUtils.setModeAndUpdate (
            this, "pointDamping", myPointDampingMode, mode);
   }

   /**
    * {@inheritDoc}
    */
   public void applyGravity (Vector3d gacc) {
      // subclasses with non-zero mass must override this method
   }

   /* ======== Renderable implementation ======= */

   public float[] getRenderCoords() {
      return myRenderCoords;
   }

   public void setRenderProps (RenderProps props) {
      super.setRenderProps (props);
   }

   public RenderProps getRenderProps() {
      return super.getRenderProps();
   }

   public RenderProps createRenderProps() {
      return RenderProps.createPointProps (this);
   }

   /**
    * {@inheritDoc}
    */
   public String[] getTraceables() {
      return new String[] { "position", "force" };
   }
   
   /**
    * {@inheritDoc}
    */
   public String getTraceablePositionProperty (String traceableName) {
      return "+position";
   }

   public void prerender (RenderList list) {
      Point3d pos = getPosition();
      myRenderCoords[0] = (float)pos.x;
      myRenderCoords[1] = (float)pos.y;
      myRenderCoords[2] = (float)pos.z;
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      getPosition().updateBounds (pmin, pmax);
   }

   public boolean isSelectable() {
      return true;
   }
   
   public void render (Renderer renderer, int flags) {

      // Exception handling code for Cormac's bug report, Jan 23, 2012
      try {
         renderer.drawPoint (myRenderProps, myRenderCoords, isSelected());
      }
      catch (Exception e) {
         System.out.println ("WARNING: Point.render failed: "+e);
         System.out.println ("myRenderProps=" + myRenderProps);
         System.out.println ("myRenderCoords=" + myRenderCoords);
         System.out.println ("point=" + ComponentUtils.getPathName (this));
      }
   }

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   public void scaleDistance (double s) {
      myState.scaleDistance (s);
      if (myRenderProps != null) {
         RenderableUtils.cloneRenderProps (this);
         myRenderProps.scaleDistance (s);
      }
   }

   public void scaleMass (double s) {
      myPointDamping *= s;
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {

      super.transformGeometry (gtr, context, flags);
      gtr.transformPnt (myState.pos);
   }

   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      super.addTransformableDependencies (context, flags);
   }

//   public MatrixBlock getSolveBlock() {
//      return mySolveBlock;
//   }

   public Matrix3x3DiagBlock createMassBlock() {
      return new Matrix3x3DiagBlock();
   }

   public boolean isMassConstant() {
      return true;
   }

   public double getMass (double t) {
      return 0;
   }

   public void getMass (Matrix M, double t) {
      doGetMass (M, 0);
   }

   public double getEffectiveMass () {
      return 0;   
   }
   
   public void getEffectiveMass (Matrix M, double t) {
      doGetMass (M, 0);
   }

   public int mulInverseEffectiveMass (
      Matrix M, double[] a, double[] f, int idx) {
      a[idx++] = 0;
      a[idx++] = 0;
      a[idx++] = 0;
      return idx;
   }

   public int getEffectiveMassForces (VectorNd f, double t, int idx) {
      double[] buf = f.getBuffer();
      // Note that if the point is attached to a moving frame, then that
      // will produce mass forces that are not computed here.
      buf[idx++] = 0;
      buf[idx++] = 0;
      buf[idx++] = 0;
      return idx;
   }

   public void resetEffectiveMass() {
   }

   public void addEffectiveMass (double m) {
      // subclasses must override if necessary; Point itself has no mass
   }

   protected void doGetMass (Matrix M, double m) {
      if (M instanceof Matrix3d) {
         ((Matrix3d)M).setDiagonal (m, m, m);
      }
      else {
         throw new IllegalArgumentException ("Matrix not instance of Matrix3d");
      }
   }

   public void addSolveBlock (SparseNumberedBlockMatrix S) {
      int bi = getSolveIndex();
      Matrix3x3Block blk = new Matrix3x3Block();
      mySolveBlockNum = S.addBlock (bi, bi, blk);
   }
   
   public MatrixBlock createSolveBlock () {
      Matrix3x3Block blk = new Matrix3x3Block();
      return blk;
   }
   
//   public void setState (DynamicComponent c) {
//      if (c instanceof Point) {
//         setState ((Point)c);
//      }
//      else {
//         throw new IllegalArgumentException ("component c is not a Point");
//      }
//   }
  
   public void setState (Point point) {
      myState.set (point.myState);
   }

   public int setState (VectorNd x, int idx) {
      idx = myState.set (x, idx);
      return idx;
   }

//   public void getState (PointState state) {
//      state.set (myState);
//   }

   public int getState (VectorNd x, int idx) {
      return myState.get (x, idx);
   }


   public void addToSolveBlockDiagonal (
      SparseNumberedBlockMatrix S, double d) {
      if (mySolveBlockNum != -1) {
         Matrix3x3Block blk = 
            (Matrix3x3Block)S.getBlockByNumber(mySolveBlockNum);
         blk.m00 += d;
         blk.m11 += d;
         blk.m22 += d;
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean velocityLimitExceeded (double tlimit, double rlimit) {
      Vector3d vel = getVelocity();
      if (vel.containsNaN() || vel.infinityNorm() > tlimit) {
         return true;
      }  
      else {
         return false;
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isDuplicatable() {
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public boolean getCopyReferences (
      List<ModelComponent> refs, ModelComponent ancestor) {
      return false;
   }

   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      Point comp = (Point)super.copy (flags, copyMap);
      if (myState != myWorldState) {
         comp.myState = new PointState();
         comp.myState.set (myState);
         comp.myWorldState = new PointState();
         comp.myWorldState.set (myWorldState);
      }
      else {
         comp.myState = new PointState();
         comp.myState.set (myState);
         comp.myWorldState = comp.myState;
      }
      comp.myPointFrame = (Frame)ComponentUtils.maybeCopy (flags, copyMap, myPointFrame);
      comp.myForce = new Vector3d();
      comp.myExternalForce = new Vector3d();
      comp.myRenderCoords = new float[3];
      comp.mySolveBlockValidP = false;
      comp.mySolveBlockNum = -1;
      comp.myTargetActivity = TargetActivity.Auto;
      comp.myTarget = null;
      return comp;
   }

   public int getVelStateSize() {
      return 3;
   }

   public int getPosStateSize() {
      return 3;
   }

   public void setContactConstraint (
      double[] buf, double w, Vector3d dir, ContactPoint cpnt) {

      buf[0] = w*dir.x;
      buf[1] = w*dir.y;
      buf[2] = w*dir.z;
   }

   public void addToPointVelocity (
      Vector3d vel, double w, ContactPoint cpnt) {

      vel.scaledAdd (w, myState.vel);      
   }

   public void getLocalPosition (Vector3d pos) {
      myState.getPos (pos);
   }

   public Point3d getLocalPosition() {
      return myState.getPos();
   }

   public void setLocalPosition (Vector3d pos) {
      myState.setPos (pos);
      updatePosState();
   }

   public void setLocalPosition (double x, double y, double z) {
      setLocalPosition (new Point3d (x, y, z));
   }

   public void getLocalVelocity (Vector3d vel) {
      myState.getVel (vel);
   }

   public Vector3d getLocalVelocity() {
      return myState.getVel();
   }

   public void setLocalVelocity (Vector3d vel) {
      myState.setVel (vel);
      updateVelState();
   }

   public void setLocalVelocity (double x, double y, double z) {
      setLocalVelocity (new Vector3d (x, y, z));
   }

   public void updatePosState() {
      if (myState != myWorldState) {
         myWorldState.pos.transform (myPointFrame.getPose(), myState.pos);
      }
   }      

   public void updateVelState() {
      if (myState != myWorldState) {
         myPointFrame.computePointVelocity (
            myWorldState.vel, myState.pos, myState.vel);
      }
   }

//   public boolean requiresContactVertexInfo() {
//      return false;
//   }


}
