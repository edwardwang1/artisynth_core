package artisynth.core.materials;

import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Matrix6d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;

public class FungMaterial extends IncompressibleMaterial {

   public static PropertyList myProps =
      new PropertyList (FungMaterial.class, IncompressibleMaterial.class);

   protected static double DEFAULT_MU1 = 1000.0;
   protected static double DEFAULT_MU2 = 2000.0;
   protected static double DEFAULT_MU3 = 3000.0;
   protected static double DEFAULT_L11 = 2000.0;
   protected static double DEFAULT_L22 = 3000.0;
   protected static double DEFAULT_L33 = 4000.0;
   protected static double DEFAULT_L12 = 2000;
   protected static double DEFAULT_L23 = 2000;
   protected static double DEFAULT_L31 = 2000;
   protected static double DEFAULT_CC  = 1500.0;

   private double myMU1 = DEFAULT_MU1; 
   private double myMU2 = DEFAULT_MU2; 
   private double myMU3 = DEFAULT_MU3; 
   private double myL11 = DEFAULT_L11; 
   private double myL22 = DEFAULT_L22; 
   private double myL33 = DEFAULT_L33; 
   private double myL12 = DEFAULT_L12; 
   private double myL23 = DEFAULT_L23; 
   private double myL31 = DEFAULT_L31; 
   private double myCC  = DEFAULT_CC; 

   private double[]   mu = new double[3]; 
   private double[][] lam = new double[3][3]; 

   PropertyMode myMU1Mode = PropertyMode.Inherited;
   PropertyMode myMU2Mode = PropertyMode.Inherited;
   PropertyMode myMU3Mode = PropertyMode.Inherited;
   PropertyMode myL11Mode = PropertyMode.Inherited;
   PropertyMode myL22Mode = PropertyMode.Inherited;
   PropertyMode myL33Mode = PropertyMode.Inherited;
   PropertyMode myL12Mode = PropertyMode.Inherited;
   PropertyMode myL23Mode = PropertyMode.Inherited;
   PropertyMode myL31Mode = PropertyMode.Inherited;
   PropertyMode myCCMode  = PropertyMode.Inherited;

   private SymmetricMatrix3d myB;
   private SymmetricMatrix3d myC;
   private SymmetricMatrix3d myC2;

   static {
      myProps.addInheritable (
         "MU1:Inherited", "MU1", DEFAULT_MU1, "[0,inf]");
      myProps.addInheritable (
         "MU2:Inherited", "MU2", DEFAULT_MU2, "[0,inf]");
      myProps.addInheritable (
         "MU3:Inherited", "MU3", DEFAULT_MU3, "[0,inf]");
      myProps.addInheritable (
         "L11:Inherited", "L11", DEFAULT_L11, "[0,inf]");
      myProps.addInheritable (
         "L22:Inherited", "L22", DEFAULT_L22, "[0,inf]");
      myProps.addInheritable (
         "L33:Inherited", "L33", DEFAULT_L33, "[0,inf]");
      myProps.addInheritable (
         "L12:Inherited", "L12", DEFAULT_L12, "[0,1]");
      myProps.addInheritable (
         "L23:Inherited", "L23", DEFAULT_L23, "[0,1]");
      myProps.addInheritable (
         "L31:Inherited", "L31", DEFAULT_L31, "[0,1]");
      myProps.addInheritable (
         "CC:Inherited", "CC", DEFAULT_CC, "[0,inf]");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public FungMaterial () {
      myB   = new SymmetricMatrix3d();
      myC   = new SymmetricMatrix3d();
      myC2  = new SymmetricMatrix3d();
   }

   public FungMaterial (double MU1, double MU2, double MU3, double L11, double L22, 
      double L33, double L12, double L23, double L31, double CC, double kappa) {
      this();
      setMU1 (MU1);
      setMU2 (MU2);
      setMU3 (MU3);
      setL11 (L11);
      setL22 (L22);
      setL33 (L33);
      setL12 (L12);
      setL23 (L23);
      setL31 (L31);
      setCC (CC);
      setBulkModulus (kappa);
   }

   public synchronized void setMU1 (double MU1) {
      myMU1 = MU1;
      myMU1Mode =
         PropertyUtils.propagateValue (this, "MU1", myMU1, myMU1Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setMU2 (double MU2) {
      myMU2 = MU2;
      myMU2Mode =
         PropertyUtils.propagateValue (this, "MU2", myMU2, myMU2Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setMU3 (double MU3) {
      myMU3 = MU3;
      myMU3Mode =
         PropertyUtils.propagateValue (this, "MU3", myMU3, myMU3Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setL11 (double L11) {
      myL11 = L11;
      myL11Mode =
         PropertyUtils.propagateValue (this, "L11", myL11, myL11Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setL22 (double L22) {
      myL22 = L22;
      myL22Mode =
         PropertyUtils.propagateValue (this, "L22", myL22, myL22Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setL33 (double L33) {
      myL33 = L33;
      myL33Mode =
         PropertyUtils.propagateValue (this, "L33", myL33, myL33Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setL12 (double L12) {
      myL12 = L12;
      myL12Mode =
         PropertyUtils.propagateValue (this, "L12", myL12, myL12Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setL23 (double L23) {
      myL23 = L23;
      myL23Mode =
         PropertyUtils.propagateValue (this, "L23", myL23, myL23Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setL31 (double L31) {
      myL31 = L31;
      myL31Mode =
         PropertyUtils.propagateValue (this, "L31", myL31, myL31Mode);
      notifyHostOfPropertyChange();
   }

   public synchronized void setCC (double CC) {
      myCC = CC;
      myCCMode =
         PropertyUtils.propagateValue (this, "CC", myCC, myCCMode);
      notifyHostOfPropertyChange();
   }

   public double getMU1() {
      return myMU1;
   }

   public double getMU2() {
      return myMU2;
   }

   public double getMU3() {
      return myMU3;
   }

   public double getL11() {
      return myL11;
   }

   public double getL22() {
      return myL22;
   }

   public double getL33() {
      return myL33;
   }

   public double getL12() {
      return myL12;
   }

   public double getL23() {
      return myL23;
   }

   public double getL31() {
      return myL31;
   }

   public double getCC() {
      return myCC;
   }

   public void setMU1Mode (PropertyMode mode) {
      myMU1Mode =
         PropertyUtils.setModeAndUpdate (this, "MU1", myMU1Mode, mode);
   }

   public void setMU2Mode (PropertyMode mode) {
      myMU2Mode =
         PropertyUtils.setModeAndUpdate (this, "MU2", myMU2Mode, mode);
   }

   public void setMU3Mode (PropertyMode mode) {
      myMU3Mode =
         PropertyUtils.setModeAndUpdate (this, "MU3", myMU3Mode, mode);
   }

   public void setL11Mode (PropertyMode mode) {
      myL11Mode =
         PropertyUtils.setModeAndUpdate (this, "L11", myL11Mode, mode);
   }

   public void setL22Mode (PropertyMode mode) {
      myL22Mode =
         PropertyUtils.setModeAndUpdate (this, "L22", myL22Mode, mode);
   }

   public void setL33Mode (PropertyMode mode) {
      myL33Mode =
         PropertyUtils.setModeAndUpdate (this, "L33", myL33Mode, mode);
   }

   public void setL12Mode (PropertyMode mode) {
      myL12Mode =
         PropertyUtils.setModeAndUpdate (this, "L12", myL12Mode, mode);
   }

   public void setL23Mode (PropertyMode mode) {
      myL23Mode =
         PropertyUtils.setModeAndUpdate (this, "L23", myL23Mode, mode);
   }

   public void setL31Mode (PropertyMode mode) {
      myL31Mode =
         PropertyUtils.setModeAndUpdate (this, "L31", myL31Mode, mode);
   }
   public void setCCMode (PropertyMode mode) {
      myCCMode =
         PropertyUtils.setModeAndUpdate (this, "CC", myCCMode, mode);
   }

   public PropertyMode getMU1Mode() {
      return myMU1Mode;
   }

   public PropertyMode getMU2Mode() {
      return myMU2Mode;
   }

   public PropertyMode getMU3Mode() {
      return myMU3Mode;
   }

   public PropertyMode getL11Mode() {
      return myL11Mode;
   }

   public PropertyMode getL22Mode() {
      return myL22Mode;
   }

   public PropertyMode getL33Mode() {
      return myL33Mode;
   }

   public PropertyMode getL12Mode() {
      return myL12Mode;
   }

   public PropertyMode getL23Mode() {
      return myL23Mode;
   }

   public PropertyMode getL31Mode() {
      return myL31Mode;
   }

   public PropertyMode getCCMode() {
      return myCCMode;
   }

   public void computeStress (
      SymmetricMatrix3d sigma, SolidDeformation def, Matrix3d Q,
      FemMaterial baseMat) {

      sigma.setZero();

      double[] K = new double[3];
      double[] L = new double[3];
      
      Vector3d[] a0 = {new Vector3d(), new Vector3d(), new Vector3d()};
      Vector3d[] a  = {new Vector3d(), new Vector3d(), new Vector3d()};
      
      Vector3d vtmp = new Vector3d();
      
      // Matrix3d Q = Matrix3d.IDENTITY;
      // if (dt.getFrame() != null) {
      //   Q = dt.getFrame(); 
      // }
      SymmetricMatrix3d[] A  = {new SymmetricMatrix3d(), new SymmetricMatrix3d(), new SymmetricMatrix3d()};
      
      Matrix3d tmpMatrix  = new Matrix3d();
      Matrix3d tmpMatrix2 = new Matrix3d();
      SymmetricMatrix3d tmpSymmMatrix  = new SymmetricMatrix3d();
      
      SymmetricMatrix3d sigmaFung = new SymmetricMatrix3d();

      // Evaluate Lame coefficients
      mu[0] = myMU1;
      mu[1] = myMU2;
      mu[2] = myMU3;
      
      lam[0][0] = myL11; 
      lam[0][1] = myL12; 
      lam[0][2] = myL31;
      lam[1][0] = myL12; 
      lam[1][1] = myL22; 
      lam[1][2] = myL23;
      lam[2][0] = myL31; 
      lam[2][1] = myL23; 
      lam[2][2] = myL33;

      double J = def.getDetF();
      double avgp = def.getAveragePressure();

      // Calculate deviatoric left Cauchy-Green tensor
      def.computeDevLeftCauchyGreen(myB);

      // Calculate deviatoric right Cauchy-Green tensor
      def.computeDevRightCauchyGreen(myC);

      // calculate square of C
      myC2.mulTransposeLeft (myC);

      Matrix3d mydevF = new Matrix3d(def.getF());
      mydevF.scale(Math.pow(J,-1.0 / 3.0));

      for (int i=0; i<3; i++) {
         // Copy the texture direction in the reference configuration to a0
         a0[i].x = Q.get(0,i);
         a0[i].y = Q.get(1,i);
         a0[i].z = Q.get(2,i);

         vtmp.mul(myC,a0[i]);
         K[i] = a0[i].dot(vtmp);

         vtmp.mul(myC2,a0[i]);
         L[i] = a0[i].dot(vtmp);

         a[i].mul(mydevF,a0[i]);
         a[i].scale(Math.pow(K[i], -0.5));

         A[i].set(a[i].x*a[i].x, a[i].y*a[i].y, a[i].z*a[i].z, 
            a[i].x*a[i].y, a[i].x*a[i].z, a[i].y*a[i].z);

      }

      // Evaluate exp(Q)
      double eQ = 0.0;
      for (int i=0; i<3; i++) {
         eQ += 2.0*mu[i]*(L[i]-2.0*K[i]+1.0);
         for (int j=0; j<3; j++)
            eQ += lam[i][j]*(K[i]-1.0)*(K[j]-1.0);
      }
      eQ = Math.exp(eQ/(4.*myCC));

      // Evaluate the stress
      SymmetricMatrix3d bmi = new SymmetricMatrix3d(); 
      bmi.sub(myB,SymmetricMatrix3d.IDENTITY);
      for (int i=0; i<3; i++) {
         //       s += mu[i]*K[i]*(A[i]*bmi + bmi*A[i]);
         tmpMatrix.mul(A[i], bmi);
         tmpMatrix2.mul(bmi,A[i]);
         tmpMatrix.add(tmpMatrix2);
         tmpMatrix.scale(mu[i]*K[i]);
         tmpSymmMatrix.setSymmetric(tmpMatrix);

         sigmaFung.add(tmpSymmMatrix);

         for (int j=0; j<3; j++) {
            // s += lam[i][j]*((K[i]-1)*K[j]*A[j]+(K[j]-1)*K[i]*A[i])/2.;
            sigmaFung.scaledAdd(lam[i][j]/2.0*(K[i]-1.0)*K[j], A[j]);
            sigmaFung.scaledAdd(lam[i][j]/2.0*(K[j]-1.0)*K[i], A[i]);
         }
      }
      sigmaFung.scale(eQ / (2.0 * J));

      sigmaFung.deviator();

      sigmaFung.m10 = sigmaFung.m01;
      sigmaFung.m20 = sigmaFung.m02;
      sigmaFung.m21 = sigmaFung.m12;

      sigma.add(sigmaFung);
      
      sigma.m00 += avgp;
      sigma.m11 += avgp;
      sigma.m22 += avgp;

   }

   public void computeTangent (
      Matrix6d c, SymmetricMatrix3d stress, SolidDeformation def, 
      Matrix3d Q, FemMaterial baseMat) {
      
      c.setZero();

      double[] K = new double[3];
      double[] L = new double[3];
      
      Vector3d[] a0 = {new Vector3d(), new Vector3d(), new Vector3d()};
      Vector3d[] a  = {new Vector3d(), new Vector3d(), new Vector3d()};
      Vector3d vtmp = new Vector3d();
      
      // Matrix3d Q = Matrix3d.IDENTITY;
      // if (dt.getFrame() != null) {
      //    Q = dt.getFrame(); 
      //  }
      SymmetricMatrix3d[] A  = {new SymmetricMatrix3d(), 
         new SymmetricMatrix3d(), new SymmetricMatrix3d()};
      
      Matrix3d tmpMatrix  = new Matrix3d();
      Matrix3d tmpMatrix2 = new Matrix3d();
      
      SymmetricMatrix3d tmpSymmMatrix  = new SymmetricMatrix3d();
      Matrix6d tmpMatrix6d  = new Matrix6d();

      Matrix6d cFung = new Matrix6d();
      
      // Evaluate Lame coefficients
      mu[0] = myMU1;
      mu[1] = myMU2;
      mu[2] = myMU3;
      
      lam[0][0] = myL11; 
      lam[0][1] = myL12; 
      lam[0][2] = myL31;
      lam[1][0] = myL12; 
      lam[1][1] = myL22; 
      lam[1][2] = myL23;
      lam[2][0] = myL31; 
      lam[2][1] = myL23; 
      lam[2][2] = myL33;

      double J = def.getDetF();
      double avgp = def.getAveragePressure();

      // Calculate deviatoric left Cauchy-Green tensor
      def.computeDevLeftCauchyGreen(myB);

      // Calculate deviatoric right Cauchy-Green tensor
      def.computeDevRightCauchyGreen(myC);

      // calculate square of C
      myC2.mulTransposeLeft (myC);

      Matrix3d mydevF = new Matrix3d(def.getF());
      mydevF.scale(Math.pow(J,-1.0 / 3.0));

      for (int i=0; i<3; i++) {
         // Copy the texture direction in the reference configuration to a0
         a0[i].x = Q.get(0,i);
         a0[i].y = Q.get(1,i);
         a0[i].z = Q.get(2,i);

         vtmp.mul(myC,a0[i]);
         K[i] = a0[i].dot(vtmp);
         
         vtmp.mul(myC2,a0[i]);
         L[i] = a0[i].dot(vtmp);

         a[i].mul(mydevF,a0[i]);
         a[i].scale(Math.pow(K[i], -0.5));

         A[i].set(a[i].x*a[i].x, a[i].y*a[i].y, a[i].z*a[i].z, 
            a[i].x*a[i].y, a[i].x*a[i].z, a[i].y*a[i].z);

      }

      // Evaluate exp(Q)
      double eQ = 0.0;
      for (int i=0; i<3; i++) {
         eQ += 2.0*mu[i]*(L[i]-2.0*K[i]+1.0);
         for (int j=0; j<3; j++)
            eQ += lam[i][j]*(K[i]-1.0)*(K[j]-1.0);
      }
      eQ = Math.exp(eQ/(4.*myCC));
      
      // Evaluate the distortional part of the Cauchy stress
      SymmetricMatrix3d sd = new SymmetricMatrix3d();

      SymmetricMatrix3d bmi = new SymmetricMatrix3d(); 
      bmi.sub(myB,SymmetricMatrix3d.IDENTITY);
      
      for (int i=0; i<3; i++) {

         //       sd += mu[i]*K[i]*(A[i]*bmi + bmi*A[i]);
         tmpMatrix.mul(A[i], bmi);
         tmpMatrix2.mul(bmi,A[i]);
         tmpMatrix.add(tmpMatrix2);
         tmpMatrix.scale(mu[i]*K[i]);
         tmpSymmMatrix.setSymmetric(tmpMatrix);

         sd.add(tmpSymmMatrix);

         for (int j=0; j<3; j++) {
            // s += lam[i][j]*((K[i]-1)*K[j]*A[j]+(K[j]-1)*K[i]*A[i])/2.;
            sd.scaledAdd(lam[i][j]/2.0*(K[i]-1.0)*K[j], A[j]);
            sd.scaledAdd(lam[i][j]/2.0*(K[j]-1.0)*K[i], A[i]);
         }
         addTensorProduct4(cFung, mu[i]*K[i], A[i], myB);
         
         // C += mu[i]*K[i]*dyad4s(A[i],b);
         for (int j=0; j<3; j++) {
            TensorUtils.addSymmetricTensorProduct (cFung, lam[i][j]*K[i]*K[j]/2.0, 
               A[i], A[j]);
            // C += lam[i][j]*K[i]*K[j]*dyad1s(A[i],A[j])/2.;
         }
      }
      sd.scale(eQ / (2.0 * J));

      // This is the distortional part of the elasticity tensor
      //      C = (eQ / J)*C + (2.0*J/myc/eQ)*dyad1s(sd);
      cFung.scale(eQ / J);
      TensorUtils.addSymmetricTensorProduct (cFung, J/myCC/eQ, sd, sd); // Factor of two diff between FEBio and Artisynth tensor utility Taken into account with scalefactor

      // This is the final value of the elasticity tensor
      //tens4ds IxI = dyad1s(I);
      //tens4ds I4  = dyad4s(I);

      double cTrace = cFung.m00 + cFung.m11 + cFung.m22 + cFung.m33 + cFung.m44 + cFung.m55;

      //      C += - 1./3.*(ddots(C,IxI) - IxI*(C.tr()/3.))
      //      + 2./3.*((I4-IxI/3.)*sd.tr()-dyad1s(sd.dev(),I));

      TensorUtils.addScaledIdentityProduct (tmpMatrix6d, -1.0 / 3.0);
      cFung.add(ddots(cFung,tmpMatrix6d));

      TensorUtils.addScaledIdentityProduct (cFung, 1.0/9.0*cTrace);

      TensorUtils.addTensorProduct4 (cFung, 2.0/3.0*sd.trace(), 
         Matrix3d.IDENTITY); // check
      TensorUtils.addScaledIdentityProduct (cFung, -2.0/9.0*sd.trace());

      sd.deviator();
      tmpMatrix6d.setZero();
      TensorUtils.addSymmetricIdentityProduct (tmpMatrix6d, sd);
      tmpMatrix6d.scale(-2.0 / 3.0);

      cFung.add(tmpMatrix6d);
      tmpMatrix6d.setZero();
      
      cFung.setLowerToUpper();

      c.add(cFung);

      c.m00 += - avgp;
      c.m11 += - avgp;
      c.m22 += - avgp;

      c.m01 += avgp;
      c.m02 += avgp;
      c.m12 += avgp;

      c.m33 += - avgp;
      c.m44 += - avgp;
      c.m55 += - avgp;

      c.setLowerToUpper();
   }

   public boolean equals (FemMaterial mat) {
      if (!(mat instanceof FungMaterial)) {
         return false;
      }
      FungMaterial fung = (FungMaterial)mat;
      if (myMU1 != fung.myMU1 ||
         myMU2  != fung.myMU2 ||
         myMU3  != fung.myMU3 ||
         myL11 != fung.myL11 ||
         myL22 != fung.myL22 ||
         myL33 != fung.myL33 ||
         myL12 != fung.myL12 ||
         myL23 != fung.myL23 ||
         myL31 != fung.myL31 ||
         myCC  != fung.myCC) {
         return false;
      }
      else {
         return super.equals (mat);
      }
   }

   public FungMaterial clone() {
      FungMaterial mat = (FungMaterial)super.clone();
      mat.myB = new SymmetricMatrix3d();
      return mat;
   }

   public static void main (String[] args) {
      FungMaterial mat = new FungMaterial();

      RotationMatrix3d R = new RotationMatrix3d();
      R.setRpy (1, 2, 3);

      SolidDeformation def = new SolidDeformation();
      Matrix3d Q = new Matrix3d();
      def.setF (new Matrix3d (1, 3, 5, 2, 1, 4, 6, 1, 2));
      Q.set (R);

      Matrix6d D = new Matrix6d();
      SymmetricMatrix3d sig = new SymmetricMatrix3d();

      // mat.computeStress (sig, pt, dt, null);
      // System.out.println ("sig=\n" + sig.toString ("%12.6f"));
      // mat.computeTangent (D, sig, pt, dt, null);
      // System.out.println ("D=\n" + D.toString ("%12.6f"));

      mat.computeStress (sig, def, Q, null);
      System.out.println ("sig=\n" + sig.toString ("%12.6f"));
      mat.computeTangent (D, sig, def, Q, null);
      System.out.println ("D=\n" + D.toString ("%12.6f"));

   }

   @Override
   public void scaleDistance (double s) {
      super.scaleDistance (s);
      myMU1  /= s;
      myMU2  /= s;
      myMU3  /= s;
      myL11 /= s;
      myL22 /= s;
      myL33 /= s;
      myL12 /= s;
      myL23 /= s;
      myL31 /= s;
      myCC  /= s;
   }

   @Override
   public void scaleMass (double s) {
      super.scaleMass (s);
      myMU1  *= s;
      myMU2  *= s;
      myMU3  *= s;
      myL11 *= s;
      myL22 *= s;
      myL33 *= s;
      myL12 *= s;
      myL23 *= s;
      myL31 *= s;
      myCC  *= s;
   }

   public boolean isIncompressible() {
      return true;
   }
   public static Matrix6d ddots (Matrix6d a, Matrix6d b) {
      Matrix6d c = new Matrix6d();

      c.m00 = 2*(a.m00*b.m00 + a.m01*b.m01 + a.m02*b.m02 + 2*a.m03*b.m03 
         + 2*a.m04*b.m04 + 2*a.m05*b.m05);

      c.m01 = a.m00*b.m01 + a.m11*b.m01 + a.m01*(b.m00 + b.m11) + a.m12*b.m02 
      + a.m02*b.m12 + 2*a.m13*b.m03 + 2*a.m03*b.m13 + 2*a.m14*b.m04 
      + 2*a.m04*b.m14 + 2*a.m15*b.m05 + 2*a.m05*b.m15;

      c.m02 = a.m12*b.m01 + a.m00*b.m02 + a.m22*b.m02 + a.m01*b.m12 
      + a.m02*(b.m00 + b.m22) + 2*a.m23*b.m03 + 2*a.m03*b.m23 + 2*a.m24*b.m04 
      + 2*a.m04*b.m24 + 2*a.m25*b.m05 + 2*a.m05*b.m25;

      c.m03 = a.m13*b.m01 + a.m23*b.m02 + a.m00*b.m03 + 2*a.m33*b.m03 
      + a.m01*b.m13 + a.m02*b.m23 + a.m03*(b.m00 + 2*b.m33) 
      + 2*a.m34*b.m04 + 2*a.m04*b.m34 + 2*a.m35*b.m05 + 2*a.m05*b.m35;

      c.m04 = a.m14*b.m01 + a.m24*b.m02 + 2*a.m34*b.m03 + a.m00*b.m04 
      + 2*a.m44*b.m04 + a.m01*b.m14 + a.m02*b.m24 + 2*a.m03*b.m34 
      + a.m04*(b.m00 + 2*b.m44) + 2*a.m45*b.m05 + 2*a.m05*b.m45;

      c.m05 = a.m15*b.m01 + a.m25*b.m02 + 2*a.m35*b.m03 + 2*a.m45*b.m04 
      + a.m00*b.m05 + 2*a.m55*b.m05 + a.m01*b.m15 + a.m02*b.m25 
      + 2*a.m03*b.m35 + 2*a.m04*b.m45 + a.m05*(b.m00 + 2*b.m55);

      c.m11 = 2*(a.m01*b.m01 + a.m11*b.m11 + a.m12*b.m12 + 2*a.m13*b.m13 
         + 2*a.m14*b.m14 + 2*a.m15*b.m15);

      c.m12 = a.m02*b.m01 + a.m01*b.m02 + a.m11*b.m12 + a.m22*b.m12 
      + a.m12*(b.m11 + b.m22) + 2*a.m23*b.m13 + 2*a.m13*b.m23 + 2*a.m24*b.m14 
      + 2*a.m14*b.m24 + 2*a.m25*b.m15 + 2*a.m15*b.m25;

      c.m13 = a.m03*b.m01 + a.m23*b.m12 + a.m01*b.m03 + a.m11*b.m13 
      + 2*a.m33*b.m13 + a.m12*b.m23 + a.m13*(b.m11 + 2*b.m33) + 2*a.m34*b.m14 
      + 2*a.m14*b.m34 + 2*a.m35*b.m15 + 2*a.m15*b.m35;

      c.m14 = a.m04*b.m01 + a.m24*b.m12 + 2*a.m34*b.m13 + a.m01*b.m04 
      + a.m11*b.m14 + 2*a.m44*b.m14 + a.m12*b.m24 + 2*a.m13*b.m34 
      + a.m14*(b.m11 + 2*b.m44) + 2*a.m45*b.m15 + 2*a.m15*b.m45;

      c.m15 = a.m05*b.m01 + a.m25*b.m12 + 2*a.m35*b.m13 + 2*a.m45*b.m14 
      + a.m01*b.m05 + a.m11*b.m15 + 2*a.m55*b.m15 + a.m12*b.m25 
      + 2*a.m13*b.m35 + 2*a.m14*b.m45 + a.m15*(b.m11 + 2*b.m55);

      c.m22 = 2*(a.m02*b.m02 + a.m12*b.m12 + a.m22*b.m22 + 2*a.m23*b.m23 
         + 2*a.m24*b.m24 + 2*a.m25*b.m25);

      c.m23 = a.m03*b.m02 + a.m13*b.m12 + a.m23*b.m22 + a.m02*b.m03 
      + a.m12*b.m13 + a.m22*b.m23 + 2*a.m33*b.m23 + 2*a.m23*b.m33 
      + 2*a.m34*b.m24 + 2*a.m24*b.m34 + 2*a.m35*b.m25 + 2*a.m25*b.m35;

      c.m24 = a.m04*b.m02 + a.m14*b.m12 + a.m24*b.m22 + 2*a.m34*b.m23 
      + a.m02*b.m04 + a.m12*b.m14 + a.m22*b.m24 + 2*a.m44*b.m24 + 2*a.m23*b.m34 
      + 2*a.m24*b.m44 + 2*a.m45*b.m25 + 2*a.m25*b.m45;

      c.m25 = a.m05*b.m02 + a.m15*b.m12 + a.m25*b.m22 + 2*a.m35*b.m23 
      + 2*a.m45*b.m24 + a.m02*b.m05 + a.m12*b.m15 + a.m22*b.m25 
      + 2*a.m55*b.m25 + 2*a.m23*b.m35 + 2*a.m24*b.m45 + 2*a.m25*b.m55;

      c.m33 = 2*(a.m03*b.m03 + a.m13*b.m13 + a.m23*b.m23 + 2*a.m33*b.m33 
         + 2*a.m34*b.m34 + 2*a.m35*b.m35);

      c.m34 = a.m04*b.m03 + a.m14*b.m13 + a.m24*b.m23 + 2*a.m34*b.m33 
      + a.m03*b.m04 + a.m13*b.m14 + a.m23*b.m24 + 2*a.m33*b.m34 
      + 2*a.m44*b.m34 + 2*a.m34*b.m44 + 2*a.m45*b.m35 + 2*a.m35*b.m45;

      c.m35 = a.m05*b.m03 + a.m15*b.m13 + a.m25*b.m23 + 2*a.m35*b.m33 
      + 2*a.m45*b.m34 + a.m03*b.m05 + a.m13*b.m15 + a.m23*b.m25 
      + 2*a.m33*b.m35 + 2*a.m55*b.m35 + 2*a.m34*b.m45 + 2*a.m35*b.m55;

      c.m44 = 2*(a.m04*b.m04 + a.m14*b.m14 + a.m24*b.m24 
         + 2*a.m34*b.m34 + 2*a.m44*b.m44 + 2*a.m45*b.m45);

      c.m45 = a.m05*b.m04 + a.m15*b.m14 + a.m25*b.m24 
      + 2*a.m35*b.m34 + 2*a.m45*b.m44 + a.m04*b.m05 + a.m14*b.m15 
      + a.m24*b.m25 + 2*a.m34*b.m35 + 2*a.m44*b.m45 + 2*a.m55*b.m45 
      + 2*a.m45*b.m55;

      c.m55 = 2*(a.m05*b.m05 + a.m15*b.m15 + a.m25*b.m25 
         + 2*a.m35*b.m35 + 2*a.m45*b.m45 + 2*a.m55*b.m55);

      return c;
   }

   //-----------------------------------------------------------------------------
   // (a dyad4s b)_ijkl = s (a_ik b_jl + a_il b_jk)/2 +  s (b_ik a_jl + b_il a_jk)/2
   public static void addTensorProduct4(Matrix6d c, double s, Matrix3dBase A, Matrix3dBase B)
   {

      double a00 = s*A.m00;
      double a11 = s*A.m11;
      double a22 = s*A.m22;
      double a01 = s*A.m01;
      double a02 = s*A.m02;
      double a12 = s*A.m12;

      double b00 = B.m00;
      double b11 = B.m11;
      double b22 = B.m22;
      double b01 = B.m01;
      double b02 = B.m02;
      double b12 = B.m12;

      c.m00 += 2.0*a00*b00;
      c.m01 += 2.0*a01*b01;
      c.m02 += 2.0*a02*b02;
      c.m03 += a01*b00 + a00*b01;
      c.m04 += a02*b01 + a01*b02;
      c.m05 += a02*b00 + a00*b02;

      c.m11 += 2.0*a11*b11;
      c.m12 += 2.0*a12*b12;
      c.m13 += a11*b01 + a01*b11;
      c.m14 += a12*b11 + a11*b12;
      c.m15 += a12*b01 + a01*b12;

      c.m22 += 2.0*a22*b22;
      c.m23 += a12*b02 + a02*b12;
      c.m24 += a22*b12 + a12*b22;
      c.m25 += a22*b02 + a02*b22;

      c.m33 += 0.5*(a11*b00 + 2.0*a01*b01 + a00*b11);
      c.m34 += 0.5*(a12*b01 + a11*b02 + a02*b11 + a01*b12);
      c.m35 += 0.5*(a12*b00 + a02*b01 + a01*b02 + a00*b12);

      c.m44 += 0.5*(a22*b11 + 2.0*a12*b12 + a11*b22);
      c.m45 += 0.5*(a22*b01 + a12*b02 + a02*b12 + a01*b22);

      c.m55 += 0.5*(a22*b00 + 2.0*a02*b02 + a00*b22);

   }
}
