/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Deque;
import java.io.PrintWriter;
import java.io.IOException;

import maspack.matrix.Matrix;
import maspack.matrix.MatrixBlock;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyInfo.Edit;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.Renderer;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer.LineStyle;
import maspack.render.color.ColorUtils;
import maspack.render.Renderable;
import maspack.spatialmotion.Wrench;
import maspack.util.*;
import artisynth.core.materials.AxialMaterial;
import artisynth.core.materials.AxialMuscleMaterial;
import artisynth.core.materials.ConstantAxialMuscle;
import artisynth.core.materials.LinearAxialMuscle;
import artisynth.core.materials.PeckAxialMuscle;
import artisynth.core.materials.SimpleAxialMuscle;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.DynamicActivityChangeEvent;
import artisynth.core.modelbase.PropertyChangeListener;
import artisynth.core.modelbase.PropertyChangeEvent;
import artisynth.core.util.ScanToken;

public class Muscle extends AxialSpring
   implements ExcitationComponent, HasAuxState, PropertyChangeListener {

   protected ExcitationSourceList myExcitationSources;
   protected CombinationRule myComboRule = CombinationRule.Sum;

   protected boolean enabled = true;
   private static final Color disabledLineColor = Color.LIGHT_GRAY;
   private static final LineStyle disabledLineStyle = LineStyle.LINE;
   private Color enabledLineColor = null;
   private LineStyle enabledLineStyle = null;
   
   protected double myExcitation; // default = 0.0;

   // minimum activation level
   protected static final double minActivation = 0.0;
   // maximum activation level
   protected static final double maxActivation = 1.0;

   protected MatrixBlock myActBlk0;
   protected MatrixBlock myActBlk1;
   protected Wrench tmpBodyWrench = new Wrench();

   protected float[] myExcitationColor = null;
   protected PropertyMode myExcitationColorMode = PropertyMode.Inherited;
   protected double myMaxColoredExcitation = 1.0;
   protected PropertyMode myMaxColoredExcitationMode = PropertyMode.Inherited;
   
   // if myMaterial implements HasAuxState, myAuxStateMat is set to its value
   // as a cached reference for use in implementing HasAuxState
   protected HasAuxState myAuxStateMat;

   protected float[] myRenderColor = null;

   public static PropertyList myProps =
      new PropertyList (Muscle.class, AxialSpring.class);

   public Muscle() {
      this (null);
   }
   
   public Muscle (String name) {
      super (name);
      setMaterial (new SimpleAxialMuscle (1, 0, /*maxf=*/1));
   }

   public Muscle (String name, double l0) {
      super (name, l0);
      setMaterial (new SimpleAxialMuscle (1, 0, /*maxf=*/1));
   }

   public Muscle (Point p1, Point p2) {
      this(null);
      setFirstPoint (p1);
      setSecondPoint (p2);
   }

   public void setMaterial (AxialMaterial mat) {
      super.setMaterial (mat);
      boolean possibleStateChange = false;
      if (mat instanceof HasAuxState) {
         possibleStateChange = true;
         // use getMaterial() since mat may have been copied
         myAuxStateMat = (HasAuxState)getMaterial();
      }
      else {
         if (myAuxStateMat != null) {
            possibleStateChange = true;
         }
         myAuxStateMat = null;
      }
      // if possible state change, issue an event to invalidate waypoints.
      if (possibleStateChange) {
         notifyParentOfChange (new DynamicActivityChangeEvent(this));
      }
   }
  
   public void setConstantMuscleMaterial (double maxF) {
      ConstantAxialMuscle mat = new ConstantAxialMuscle();
      mat.setMaxForce(maxF);
      setMaterial(mat);
   }
   
   public void setConstantMuscleMaterial (double maxF, double forceScaling) {
      ConstantAxialMuscle mat = new ConstantAxialMuscle();
      mat.setMaxForce(maxF);
      mat.setForceScaling(forceScaling);
      setMaterial(mat);
   }
   
   public void setLinearMuscleMaterial (double maxF, double optL, double maxL, double pf) {
      LinearAxialMuscle mat = new LinearAxialMuscle();
      mat.setMaxForce(maxF);
      mat.setOptLength(optL);
      mat.setMaxLength(maxL);
      mat.setPassiveFraction(pf);
      setMaterial (mat);
   }
   
   public void setPeckMuscleMaterial (double maxF, double optL, double maxL, double tendonRatio) {
      PeckAxialMuscle mat = new PeckAxialMuscle();
      mat.setMaxForce (maxF);
      mat.setOptLength (optL);
      mat.setMaxLength (maxL);
      mat.setTendonRatio (tendonRatio);
      mat.setPassiveFraction(0.015); //Peck value = 0.0115
      setMaterial(mat);
   }
   
   public void setPeckMuscleMaterial (double maxF, double optL, double maxL, 
	 double tendonRatio, double passiveFraction, double damping) {
      PeckAxialMuscle mat = new PeckAxialMuscle();
      mat.setAxialMuscleMaterialProps(maxF, optL, maxL, passiveFraction, 
	    tendonRatio, damping, AxialMuscleMaterial.DEFAULT_SCALING);
      setMaterial(mat);
   }

   static {
      myProps.add ("enabled isEnabled *", "muscle is enabled", true);
      myProps.add ("excitation", "internal muscle excitation", 0.0, "[0,1] NW");
      myProps.addReadOnly (
         "netExcitation", "total excitation including excitation sources");
      myProps.addReadOnly (
         "forceNorm *", "norm of total force applied by muscle (N)", "%.8g AE");
      myProps.addReadOnly (
         "passiveForceNorm *", "norm of passive force generated by muscle (N)",
         "%.8g AE");
      myProps.addReadOnly (
         "force *", "total force vector applied by muscle (N)", "NE");
      myProps.addReadOnly (
         "passiveForce *", "passive force vector generated by muscle (N)", "NE");
//      myProps.add (
//         "maxForce * *", "maximum force applied by muscle", AxialMuscleMaterial.DEFAULT_MAX_FORCE);
//      myProps.add ("optLength * *", "length for max force capacity", AxialMuscleMaterial.DEFAULT_OPT_LENGTH, "%.8g");
//      myProps.add ("maxLength * *", "max length of muscle stretch", AxialMuscleMaterial.DEFAULT_MAX_LENGTH, "%.8g");
//      myProps.add ("tendonRatio * *", "tendon to fibre length ratio", AxialMuscleMaterial.DEFAULT_TENDON_RATIO, "%.8g");
//      myProps.add (
//         "passiveFraction * *", "percentage of maxForce applied passively",
//         AxialMuscleMaterial.DEFAULT_PASSIVE_FRACTION, "%.8g");
//      myProps.add (
//         "forceScaling * *", "scale factor from nominal force units",
//         AxialMuscleMaterial.DEFAULT_SCALING, "%.8g");
      myProps.get ("length").setFormat (new NumberFormat ("%.8g"));

      // set unused props from AxialSpring to never edit
      myProps.get ("restLength").setEditing (Edit.Never);
      //myProps.get ("stiffness").setEditing (Edit.Never);
      myProps.addInheritable (
         "excitationColor", "color of activated muscles", null);
       myProps.addInheritable (
         "maxColoredExcitation",
         "excitation value for maximum colored excitation", 1.0, "[0,1]");
   }

   public Color getExcitationColor() {
      if (myExcitationColor == null) {
         return null;
      }
      else {
         return new Color (
            myExcitationColor[0], myExcitationColor[1], myExcitationColor[2]);
      }
   }

   public void setExcitationColor (Color color) {
      if (color == null) {
         myExcitationColor = null;
      }
      else {
         myExcitationColor = color.getRGBColorComponents(null);
      }
      myExcitationColorMode =
         PropertyUtils.propagateValue (
            this, "excitationColor", color, myExcitationColorMode);
   }

   public PropertyMode getExcitationColorMode() {
      return myExcitationColorMode;
   }

   public void setExcitationColorMode (PropertyMode mode) {
      myExcitationColorMode =
         PropertyUtils.setModeAndUpdate (
            this, "excitationColor", myExcitationColorMode, mode);
   }

   public double getMaxColoredExcitation() {
      return myMaxColoredExcitation;
   }

   public void setMaxColoredExcitation (double excitation) {
      myMaxColoredExcitation = excitation;
      myMaxColoredExcitationMode =
         PropertyUtils.propagateValue (
            this, "maxColoredExcitation", excitation, myMaxColoredExcitationMode);
   }

   public PropertyMode getMaxColoredExcitationMode() {
      return myMaxColoredExcitationMode;
   }

   public void setMaxColoredExcitationMode (PropertyMode mode) {
      myMaxColoredExcitationMode =
         PropertyUtils.setModeAndUpdate (
            this, "maxColoredExcitation", myMaxColoredExcitationMode, mode);
   }


   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * {@inheritDoc}
    */
   public double getExcitation() {
      return myExcitation;
   }

   /**
    * {@inheritDoc}
    */
   public void initialize (double t) {
      if (t == 0) {
         setExcitation (0);         
      }
   }

   /**
    * {@inheritDoc}
    */
   public void setExcitation (double a) {
      // set activation within valid range
      double valid_a = a;
      valid_a = (valid_a > maxActivation) ? maxActivation : valid_a;
      valid_a = (valid_a < minActivation) ? minActivation : valid_a;
      myExcitation = valid_a;
   }

   /**
    * {@inheritDoc}
    */
   public void addExcitationSource (ExcitationComponent ex) {
      addExcitationSource (ex, 1);
   }

   /**
    * {@inheritDoc}
    */
   public void addExcitationSource (ExcitationComponent ex, double gain) {
      if (myExcitationSources == null) {
         myExcitationSources = new ExcitationSourceList();
      }
      myExcitationSources.add (ex, gain);
   }

   /**
    * {@inheritDoc}
    */
   public boolean removeExcitationSource (ExcitationComponent ex) {
      boolean removed = false;
      if (myExcitationSources != null) {
         removed = myExcitationSources.remove (ex);
         if (myExcitationSources.size() == 0) {
            myExcitationSources = null;
         }
      }
      return removed;
   }

   /**
    * {@inheritDoc}
    */
   public double getExcitationGain (ExcitationComponent ex) {
      return ExcitationUtils.getGain (myExcitationSources, ex);
   }

   /**
    * {@inheritDoc}
    */
   public boolean setExcitationGain (ExcitationComponent ex, double gain) {
      return ExcitationUtils.setGain (myExcitationSources, ex, gain);
   }

   /**
    * {@inheritDoc}
    */
   public void setCombinationRule (CombinationRule rule) {
      myComboRule = rule;
   }

   /**
    * {@inheritDoc}
    */
   public CombinationRule getCombinationRule() {
      return myComboRule;
   }

   public float[] getRenderColor() {
      return myRenderColor;
   }

   @Override
   public void prerender (RenderList list) {
      RenderProps props = myRenderProps;
      if (props == null) {
         if (getParent() instanceof Renderable) {
            props = ((Renderable)getParent()).getRenderProps();
         }
      }
      if (props != null && myExcitationColor != null) {
         if (myRenderColor == null) {
            myRenderColor = new float[3];
         }
         float[] baseColor = props.getLineColorF();
         double s = Math.min(getNetExcitation()/getMaxColoredExcitation(), 1);
         ColorUtils.interpolateColor (
            myRenderColor, baseColor, myExcitationColor, s);
      }
      else {
         myRenderColor = null;
      }
   }

   @Override
   public void render (Renderer renderer, int flags) {
      renderer.drawLine (
         myRenderProps, myPnt0.myRenderCoords, myPnt1.myRenderCoords,
         myRenderColor, /*capped=*/false, isSelected());
   }

   /**
    * {@inheritDoc}
    */
   public double getNetExcitation() {
      double net = ExcitationUtils.combineWithAncestor (
         this, myExcitationSources, /*up to grandparent=*/2, myComboRule);
      return net;
   }

   /**
    * Computes the force magnitude acting along the unit vector from the first
    * to the second particle.
    * 
    * @return force magnitude
    */
   public double computeF (double l, double ldot) {
      AxialMaterial mat = getEffectiveMaterial();
      if (enabled && mat != null) {
         return mat.computeF (l, ldot, myRestLength, getNetExcitation());
      }
      else {
         return 0;
      }
   }
   
   /**
    * Computes the force magnitude acting along the unit vector from the first
    * to the second particle with zero excitation.
    * 
    * @return force magnitude
    */
   public double computePassiveF (double l, double ldot) {
      AxialMaterial mat = getEffectiveMaterial();
      if (enabled && mat != null) {
	 return mat.computeF(l, ldot, myRestLength, 0);
      }
      else {
	 return 0;
      }
   }

   /**
    * Computes the derivative of spring force magnitude (acting along the unit
    * vector from the first to the second particle) with respect to spring
    * length.
    * 
    * @return force magnitude derivative with respect to length
    */
   public double computeDFdl (double l, double ldot) {
      AxialMaterial mat = getEffectiveMaterial();
      if (enabled && mat != null) {
         return mat.computeDFdl (l, ldot, myRestLength, getNetExcitation());
      }
      else {
         return 0;
      }
   }

   /**
    * Computes the derivative of spring force magnitude (acting along the unit
    * vector from the first to the second particle)with respect to the time
    * derivative of spring length.
    * 
    * @return force magnitude derivative with respect to length time derivative
    */
   public double computeDFdldot (double l, double ldot) {
      AxialMaterial mat = getEffectiveMaterial();
      if (enabled && mat != null) {
         return mat.computeDFdldot (l, ldot, myRestLength, getNetExcitation());
      }
      else {
         return 0;
      }
   }


   /**
    * sets the opt length to current muscle length and max length with the
    * original ratio of opt to max length
    * 
    */
   public void resetLengthProps() {
      if (myMaterial instanceof AxialMuscleMaterial) {
	 AxialMuscleMaterial mat = (AxialMuscleMaterial)myMaterial;
	 double length = getLength();
	 double optLength = mat.getOptLength();
	 double maxLength = mat.getMaxLength();
	 double maxOptRatio =
	       (optLength != 0.0) ? maxLength / optLength : 1.0;
	 mat.setOptLength(length);
	 mat.setMaxLength(length * maxOptRatio); 

      }
      else {
         System.err.println("resetLengthProps(), current material not AxialMuscleMaterial");
      }
   }

   public double getForceNorm() {
      computeForce (myTmp);
      return myTmp.norm() / getForceScaling();
   }

   public Vector3d getForce() {
      computeForce (myTmp);
      myTmp.scale (1 / getForceScaling());
      return new Vector3d(myTmp);
   }

   public void computePassiveForce (Vector3d f) {
      double l = getLength();
      double ldot = getLengthDot();
      if (l == 0) {
         f.setZero();
         return;
      }

      double Fp = computePassiveF(l, ldot);
      f.scale (Fp, mySeg.uvec);
   }
   
   public double getPassiveForceNorm() {
      computePassiveForce (myTmp);
      return myTmp.norm() / getForceScaling();
   }

   public Vector3d getPassiveForce() {
      computePassiveForce (myTmp);
      myTmp.scale (1 / getForceScaling());
      return new Vector3d(myTmp);
   }

   public void scaleDistance (double s) {
      super.scaleDistance (s);
//      forceScaling *= s;
//      myOptLength *= s;
//      myMaxLength *= s;
   }

   public void scaleMass (double s) {
      super.scaleMass (s);
//      forceScaling *= s;
   }

   public void printMuscleDirection() {
      System.out.println (myName + ": " + "org = ("
      + myPnt0.getPosition().toString ("%6.2f") + ")   ins = ("
      + myPnt1.getPosition().toString ("%6.2f") + ")");
      this.updateU();
      Vector3d dir = new Vector3d (myU);
      dir.negate();
      System.out.println (myName + " unit vector = (" + dir.toString ("%8.4f")
      + " )");
   }

   public int getJacobianType() {
      return Matrix.SYMMETRIC;
   }

//   public MuscleType getMuscleType() {
//      return myType;
//   }
//
//   public void setMuscleType (MuscleType muscleType) {
//      myType = muscleType;
//   }

   public boolean isEnabled() {
      return enabled;
   }

   public void setEnabled (boolean enabled) {
      if (this.enabled != enabled) {
	 this.enabled = enabled;
	 updateLineRenderProps(enabled);
      }
   }

   // begin HasAuxState interface

   public void advanceAuxState (double t0, double t1) {
      if (myAuxStateMat != null) {
         myAuxStateMat.advanceAuxState (t0, t1);
      }
   }
   
   public void skipAuxState (DataBuffer data) {
      if (myAuxStateMat != null) {
         myAuxStateMat.skipAuxState (data);
      }
   }

   public void getAuxState (DataBuffer data) {
      if (myAuxStateMat != null) {
         myAuxStateMat.getAuxState (data);
      }
   }

   public void getInitialAuxState (
      DataBuffer newData, DataBuffer oldData) {
      if (myAuxStateMat != null) {
         myAuxStateMat.getInitialAuxState (newData, oldData);
      }
   }

   public void setAuxState (DataBuffer data) {
      if (myAuxStateMat != null) {
         myAuxStateMat.setAuxState (data);
      }
   }

   // end HasAuxState interface
   
   // PropertyChangeListener interface:

   public void propertyChanged (PropertyChangeEvent e) {
      if (e.getHost() instanceof AxialMaterial) {
         if (e.getPropertyName().equals ("material")) {
            // issue a dynamic change event in order to invalidate WayPoints
            notifyParentOfChange (new DynamicActivityChangeEvent(this));
         }
      }
   }


   private void updateLineRenderProps(boolean enabled) {
      if (enabled) {
	 if (enabledLineColor == null)
	    RenderProps.setLineColorMode(this, PropertyMode.Inherited);
	 else
	    RenderProps.setLineColor(this, enabledLineColor);
	 
	 if (enabledLineStyle == null) 
	    RenderProps.setLineStyleMode(this, PropertyMode.Inherited);
	 else
	    RenderProps.setLineStyle(this, enabledLineStyle);	 
      }
      else {
	 if (getRenderProps() != null && 
	     getRenderProps().getLineColorMode() == PropertyMode.Explicit)
	    enabledLineColor = getRenderProps().getLineColor();
	 
	 if (getRenderProps() != null && 
	     getRenderProps().getLineStyleMode() == PropertyMode.Explicit) 
	    enabledLineStyle = getRenderProps().getLineStyle();
	 
	 RenderProps.setLineColor(this, disabledLineColor);
	 RenderProps.setLineStyle(this, disabledLineStyle);
      }
	 
   }

   private double getForceScaling() {
      if (myMaterial instanceof AxialMuscleMaterial) {
         return ((AxialMuscleMaterial)myMaterial).getForceScaling();
      }
      else {
         return 1;
      }   
   }

   public double getDefaultActivationWeight () {
      return 1.0/Muscle.getMaxForce(this);
   }

   public void getSoftReferences (List<ModelComponent> refs) {
      super.getSoftReferences (refs);
      if (myExcitationSources != null) {
         myExcitationSources.getSoftReferences (refs);
      }
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor) 
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      if (myExcitationSources != null) {
         myExcitationSources.write (pw, "excitationSources", fmt, ancestor);
      }
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {
      rtok.nextToken();
      if (scanAttributeName (rtok, "excitationSources")) {
         myExcitationSources =
            ExcitationUtils.scan (rtok, "excitationSources", tokens);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "excitationSources")) {
         myExcitationSources.postscan (tokens, ancestor);
         return true;
      }   
      return super.postscanItem (tokens, ancestor);
   }

}
