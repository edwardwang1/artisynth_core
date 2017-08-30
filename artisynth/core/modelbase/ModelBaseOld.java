/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.util.Map;

import maspack.properties.PropertyList;

/**
 * Base class providing some default implementations of the Model interface.
 */
public abstract class ModelBaseOld extends CompositeComponentBase
   implements Model {

   protected static double DEFAULT_MAX_STEP_SIZE = 0.01;

   protected double myMaxStepSize = DEFAULT_MAX_STEP_SIZE;
   protected String myAdvanceDiagnostic = null;

   public static PropertyList myProps =
      new PropertyList (ModelBaseOld.class, CompositeComponentBase.class);

   static {
      myProps.add (
         "maxStepSize", "maximum step size for this component (seconds)",
         DEFAULT_MAX_STEP_SIZE);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public ModelBaseOld (String name) {
      super (name);
   }

   public ModelBaseOld() {
      super (null);
   }

   /**
    * {@inheritDoc}
    */
   public void setState (ComponentState state) {
   }

   /**
    * {@inheritDoc}
    */
   public void getState (ComponentState state) {
   }

   public void setInitialState (ComponentState state) {
      setState (state);
   }

   public void getInitialState (ComponentState state) {
      getState (state);
   }

   public void getInitialState (
      ComponentState newstate, ComponentState oldstate) {
      if (oldstate == null) {
         getState (newstate);
      }
      else {
         newstate.set (oldstate);
      }
   }
   
   /**
    * {@inheritDoc}
    */
   public void initialize (double t) {
   }
   
   /**
    * {@inheritDoc}
    */
   public StepAdjustment preadvance (double t0, double t1, int flags) {
      // default implementation - does nothing
      return null;
   }
   
   /**
    * {@inheritDoc}
    */
   public abstract StepAdjustment advance (double t0, double t1, int flags);

   /**
    * Returns the maximum step size by which this model should be advanced
    * within a simulation loop.
    * 
    * @return maximum step size (seconds)
    */
   public double getMaxStepSize() {
      return myMaxStepSize;
   }

   /**
    * Sets the maximum step size by which this model should be advanced within a
    * simulation loop.
    * 
    * @param sec
    * maximum step size (seconds)
    */
   public void setMaxStepSize (double sec) {
      myMaxStepSize = sec;
   }

   /**
    * {@inheritDoc}
    */
   public ComponentState createState(ComponentState prevState) {
      return null;
   }

   /**
    * {@inheritDoc}
    */
   public void dispose() {
   }

   /**
    * {@inheritDoc}
    */
   @Override
      public boolean hierarchyContainsReferences() {
      return true;
   }

   public ModelBaseOld copy (
      Map<ModelComponent,ModelComponent> copyMap,int flags) {
      ModelBaseOld mb = (ModelBaseOld)super.copy (flags, copyMap);

      mb.setMaxStepSize (myMaxStepSize);

      return mb;
   }


}
