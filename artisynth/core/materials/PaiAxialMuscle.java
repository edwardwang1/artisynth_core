package artisynth.core.materials;

public class PaiAxialMuscle extends AxialMuscleMaterial {
   

   public double computeF(double l, double ldot, double l0, double ex) {
      double passive = 0, active = 0;

      // active part
      double normFibreLen = (l - myOptLength * myTendonRatio)
	    / (myOptLength * (1 - myTendonRatio));
      if (normFibreLen > minStretch && normFibreLen < 1.0) {  // 1.0 instead of maxStretch
	 active = 0.5 * (1 + Math.cos(2 * Math.PI * normFibreLen));
      }
      else if (normFibreLen >= 1.0) {
         active = 1.0;
      }

      // passive part
      if (l > myMaxLength) {
	 passive = 1.0;
      } else if (l > myOptLength) {
	 passive = (l - myOptLength) / (myMaxLength - myOptLength);
      }

      return forceScaling * (
	      myMaxForce * (active * ex + passive * myPassiveFraction)
	    + myDamping * ldot);
   }

   public double computeDFdl(double l, double ldot, double l0, double ex) {
      double active_dFdl = 0.0, passive_dFdl = 0.0;
      double normFibreLen = (l - myOptLength * myTendonRatio)
	    / (myOptLength * (1 - myTendonRatio));
      
      // active part
      if (normFibreLen > minStretch && normFibreLen < 1.0) { // 1.0 instead of maxStretch
	 active_dFdl = -myMaxForce * ex * Math.PI
	       * Math.sin(2 * Math.PI * normFibreLen)
	       / (myOptLength * (1 - myTendonRatio));
      }

      // passive part
      if (l > myOptLength && l < myMaxLength) {
	 passive_dFdl = myMaxForce * myPassiveFraction
	       / (myMaxLength - myOptLength);
      }
      return forceScaling * (passive_dFdl + active_dFdl);
   }

   public double computeDFdldot(double l, double ldot, double l0, double ex) {
      return forceScaling * myDamping;
   }

   public boolean isDFdldotZero() {
      return myDamping == 0;
   }
}
