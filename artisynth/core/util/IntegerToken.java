/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.util;

/**
 * Parsing token that holds an integer value.
 */
public class IntegerToken extends ScanToken {

   int myValue;

   public IntegerToken (int value, int lineno) {
      super (lineno);
      myValue = value;
   }

   public IntegerToken (int value) {
      super ();
      myValue = value;
   }

   public Integer value() {
      return myValue;
   }

   public String toString() {
      return "IntegerToken['"+value()+"' line "+lineno()+"]";
   }


}
