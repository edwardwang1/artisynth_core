/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A set of static methods to help scan values from a ReaderTokenizer.
 */
public class Scan {
   /**
    * Reads and returns a Color from a tokenizer. The color should be
    * represented as either three or four numbers surrounded by square brackets
    * <code>[ ]</code>, representing red, green, and blue values, and an
    * optional alpha value, in the range 0 to 1. If alpha is not present, it
    * defaults to 1.
    * 
    * @param rtok
    * Tokenizer from which the color is read
    * @return Color object produced by the input
    * @throws IOException
    * if the color is formatted incorrectly or if an I/O error occured
    */
   public static Color scanColor (ReaderTokenizer rtok) throws IOException {
      rtok.scanToken ('[');
      float r = (float)rtok.scanNumber();
      float g = (float)rtok.scanNumber();
      float b = (float)rtok.scanNumber();

      if (rtok.nextToken() == ']') {
         return new Color (r, g, b);
      }
      else if (rtok.tokenIsNumber()) {
         float a = (float)rtok.nval;
         rtok.scanToken (']');
         return new Color (r, g, b, a);
      }
      else {
         throw new IOException ("unexpected token: " + rtok);
      }
   }

//   /**
//    * Reads and returns am AxisAngle from a tokenizer. The AxisAngle should be
//    * represented as four numbers surrounded by square brackets <code>[ ]</code>,
//    * representing x, y, and z axis values, and the angle in degrees.
//    * 
//    * @param rtok
//    * Tokenizer from which the color is read
//    * @return AxisAngle object produced by the input
//    * @throws IOException
//    * if the AxisAngle is formatted incorrectly or if an I/O error occured
//    */
//   public static AxisAngle scanAxisAngle (ReaderTokenizer rtok)
//      throws IOException {
//      rtok.scanToken ('[');
//      double x = rtok.scanNumber();
//      double y = rtok.scanNumber();
//      double z = rtok.scanNumber();
//      double ang = rtok.scanNumber();
//      rtok.scanToken (']');
//
//      return new AxisAngle (x, y, z, Math.toRadians (ang));
//   }

   /**
    * Reads a set of double values, enclosed in square brackets <code>[
    * ]</code>.
    * Values are placed in an argument array and the number of values actually
    * read is returned.
    * 
    * @param vals
    * array in which values are stored
    * @param rtok
    * Tokenizer from which values are read
    * @return number of values read
    * @throws IOException
    * if the number of values exceeds the size of the array, the values are not
    * numbers or are not enclosed in square brackets, or an I/O error occured
    */
   public static int scanDoubles (double[] vals, ReaderTokenizer rtok)
      throws IOException {
      rtok.scanToken ('[');
      int nvals = 0;
      while (rtok.nextToken() == ReaderTokenizer.TT_NUMBER) {
         if (nvals >= vals.length) {
            throw new IOException (
               "number of values exceeds expected maximum of " + vals.length
               + ", line " + rtok.lineno());
         }
         vals[nvals++] = rtok.nval;
      }
      if (rtok.ttype != ']') {
         throw new IOException ("expected token ']', got: " + rtok);
      }
      return nvals;
   }

   /**
    * Reads a set of double values, enclosed in square brackets <code>[
    * ]</code>,
    * and returns them in an array.
    * 
    * @param rtok
    * Tokenizer from which values are read
    * @return array of values read
    * @throws IOException
    * if the values are not numbers or are not enclosed in square brackets, or
    * an I/O error occured
    */
   public static double[] scanDoubles (ReaderTokenizer rtok) throws IOException {
      ArrayList<Double> vals = new ArrayList<Double>();
      rtok.scanToken ('[');
      while (rtok.nextToken() == ReaderTokenizer.TT_NUMBER) {
         vals.add (rtok.nval);
      }
      if (rtok.ttype != ']') {
         throw new IOException ("expected token ']', got: " + rtok);
      }
      double[] array = new double[vals.size()];
      for (int i = 0; i < array.length; i++) {
         array[i] = vals.get(i);
      }
      return array;
   }

   /**
    * Reads a set of float values, enclosed in square brackets <code>[
    * ]</code>.
    * Values are placed in an argument array and the number of values actually
    * read is returned.
    * 
    * @param vals
    * array in which values are stored
    * @param rtok
    * Tokenizer from which values are read
    * @return number of values read
    * @throws IOException
    * if the number of values exceeds the size of the array, the values are not
    * numbers or are not enclosed in square brackets, or an I/O error occured
    */
   public static int scanFloats (float[] vals, ReaderTokenizer rtok)
      throws IOException {
      rtok.scanToken ('[');
      int nvals = 0;
      while (rtok.nextToken() == ReaderTokenizer.TT_NUMBER) {
         if (nvals >= vals.length) {
            throw new IOException (
               "number of values exceeds expected maximum of " + vals.length
               + ", line " + rtok.lineno());
         }
         vals[nvals++] = (float)rtok.nval;
      }
      if (rtok.ttype != ']') {
         throw new IOException ("expected token ']', got: " + rtok);
      }
      return nvals;
   }

   /**
    * Reads a set of float values, enclosed in square brackets <code>[
    * ]</code>,
    * and returns them in an array.
    * 
    * @param rtok
    * Tokenizer from which values are read
    * @return array of values read
    * @throws IOException
    * if the values are not numbers or are not enclosed in square brackets, or
    * an I/O error occured
    */
   public static float[] scanFloats (ReaderTokenizer rtok) throws IOException {
      ArrayList<Float> vals = new ArrayList<Float>();
      rtok.scanToken ('[');
      while (rtok.nextToken() == ReaderTokenizer.TT_NUMBER) {
         vals.add ((float)rtok.nval);
      }
      if (rtok.ttype != ']') {
         throw new IOException ("expected token ']', got: " + rtok);
      }
      float[] array = new float[vals.size()];
      for (int i = 0; i < array.length; i++) {
         array[i] = vals.get(i);
      }
      return array;
   }

   /**
    * Reads a set of integer values, enclosed in square brackets <code>[
    * ]</code>.
    * Values are placed in an argument array and the number of values actually
    * read is returned. Values which exceed the allowed range for integers will
    * be truncated in the high order bits.
    * 
    * @param vals
    * array in which values are stored
    * @param rtok
    * Tokenizer from which values are read
    * @return number of values read
    * @throws IOException
    * if the number of values exceeds the size of the array, the values are not
    * integers or are not enclosed in square brackets, or an I/O error occured
    */
   public static int scanInts (int[] vals, ReaderTokenizer rtok)
      throws IOException {
      rtok.scanToken ('[');
      int nvals = 0;
      while (rtok.nextToken() == ReaderTokenizer.TT_NUMBER) {
         if (nvals >= vals.length) {
            throw new IOException (
               "number of values exceeds expected maximum of " + vals.length
               + ", line " + rtok.lineno());
         }
         if (!rtok.tokenIsInteger()) {
            throw new IOException ("integer value expected, got " + rtok);
         }
         vals[nvals++] = (int)rtok.lval;
      }
      if (rtok.ttype != ']') {
         throw new IOException ("expected token ']', got: " + rtok);
      }
      return nvals;
   }

   /**
    * Reads a set of integer values, enclosed in square brackets <code>[
    * ]</code>,
    * and returns them in an array.Values which exceed the allowed range for
    * integers will be truncated in the high order bits.
    * 
    * @param rtok
    * Tokenizer from which values are read
    * @return array of values read
    * @throws IOException
    * if the values are not integers or are not enclosed in square brackets, or
    * an I/O error occured
    */
   public static int[] scanInts (ReaderTokenizer rtok) throws IOException {
      ArrayList<Integer> vals = new ArrayList<Integer>();
      rtok.scanToken ('[');
      while (rtok.nextToken() == ReaderTokenizer.TT_NUMBER) {
         if (!rtok.tokenIsInteger()) {
            throw new IOException ("integer value expected, got " + rtok);
         }
         vals.add ((int)rtok.lval);
      }
      if (rtok.ttype != ']') {
         throw new IOException ("expected token ']', got: " + rtok);
      }
      int[] array = new int[vals.size()];
      for (int i = 0; i < array.length; i++) {
         array[i] = vals.get(i);
      }
      return array;
   }

   /**
    * Reads a set of short integer values, enclosed in square brackets
    * <code>[ ]</code>. Values are placed in an argument array and the number
    * of values actually read is returned. Values which exceed the allowed range
    * for shorts will be truncated in the high order bits.
    * 
    * @param vals
    * array in which values are stored
    * @param rtok
    * Tokenizer from which values are read
    * @return number of values read
    * @throws IOException
    * if the number of values exceeds the size of the array, the values are not
    * integers or are not enclosed in square brackets, or an I/O error occured
    */
   public static int scanShorts (short[] vals, ReaderTokenizer rtok)
      throws IOException {
      rtok.scanToken ('[');
      int nvals = 0;
      while (rtok.nextToken() == ReaderTokenizer.TT_NUMBER) {
         if (nvals >= vals.length) {
            throw new IOException (
               "number of values exceeds expected maximum of " + vals.length
               + ", line " + rtok.lineno());
         }
         if (!rtok.tokenIsInteger()) {
            throw new IOException ("integer value expected, got " + rtok);
         }
         vals[nvals++] = (short)rtok.lval;
      }
      if (rtok.ttype != ']') {
         throw new IOException ("expected token ']', got: " + rtok);
      }
      return nvals;
   }

   /**
    * Reads a set of short integer values, enclosed in square brackets
    * <code>[ ]</code>, and returns them in an array. Values which exceed the
    * allowed range for shorts will be truncated in the high order bits.
    * 
    * @param rtok
    * Tokenizer from which values are read
    * @return array of values read
    * @throws IOException
    * if the values are not integers or are not enclosed in square brackets, or
    * an I/O error occured
    */
   public static short[] scanShorts (ReaderTokenizer rtok) throws IOException {
      ArrayList<Short> vals = new ArrayList<Short>();
      rtok.scanToken ('[');
      while (rtok.nextToken() == ReaderTokenizer.TT_NUMBER) {
         if (!rtok.tokenIsInteger()) {
            throw new IOException ("integer value expected, got " + rtok);
         }
         vals.add ((short)rtok.lval);
      }
      if (rtok.ttype != ']') {
         throw new IOException ("expected token ']', got: " + rtok);
      }
      short[] array = new short[vals.size()];
      for (int i = 0; i < array.length; i++) {
         array[i] = vals.get(i);
      }
      return array;
   }

   /**
    * Reads a set of long integer values, enclosed in square brackets
    * <code>[ ]</code>. Values are placed in an argument array and the number
    * of values actually read is returned. Values which exceed the allowed range
    * for longs will be truncated in the high order bits.
    * 
    * @param vals
    * array in which values are stored
    * @param rtok
    * Tokenizer from which values are read
    * @return number of values read
    * @throws IOException
    * if the number of values exceeds the size of the array, the values are not
    * integers or are not enclosed in square brackets, or an I/O error occured
    */
   public static int scanLongs (long[] vals, ReaderTokenizer rtok)
      throws IOException {
      rtok.scanToken ('[');
      int nvals = 0;
      while (rtok.nextToken() == ReaderTokenizer.TT_NUMBER) {
         if (nvals >= vals.length) {
            throw new IOException (
               "number of values exceeds expected maximum of " + vals.length
               + ", line " + rtok.lineno());
         }
         if (!rtok.tokenIsInteger()) {
            throw new IOException ("integer value expected, got " + rtok);
         }
         vals[nvals++] = (long)rtok.lval;
      }
      if (rtok.ttype != ']') {
         throw new IOException ("expected token ']', got: " + rtok);
      }
      return nvals;
   }

   /**
    * Reads a set of long integer values, enclosed in square brackets
    * <code>[ ]</code>, and returns them in an array. Values which exceed the
    * allowed range for longs will be truncated in the high order bits.
    * 
    * @param rtok
    * Tokenizer from which values are read
    * @return array of values read
    * @throws IOException
    * if the values are not integers or are not enclosed in square brackets, or
    * an I/O error occured
    */
   public static long[] scanLongs (ReaderTokenizer rtok) throws IOException {
      ArrayList<Long> vals = new ArrayList<Long>();
      rtok.scanToken ('[');
      while (rtok.nextToken() == ReaderTokenizer.TT_NUMBER) {
         if (!rtok.tokenIsInteger()) {
            throw new IOException ("integer value expected, got " + rtok);
         }
         vals.add ((long)rtok.lval);
      }
      if (rtok.ttype != ']') {
         throw new IOException ("expected token ']', got: " + rtok);
      }
      long[] array = new long[vals.size()];
      for (int i = 0; i < array.length; i++) {
         array[i] = vals.get(i);
      }
      return array;
   }

   /**
    * Reads a set of words values, enclosed in square brackets <code>[ ]</code>,
    * and returns them as an array of Strings.
    * 
    * @param rtok
    * Tokenizer from which values are read
    * @return array of values read
    * @throws IOException
    * if the values are not words or are not enclosed in square brackets, or an
    * I/O error occured
    */
   public static String[] scanWords (ReaderTokenizer rtok) throws IOException {
      ArrayList<String> words = new ArrayList<String>();
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         if (!rtok.tokenIsWord()) {
            throw new IOException ("word expected, got " + rtok);
         }
         words.add (rtok.sval);
      }
      return words.toArray (new String[0]);
   }

   /**
    * Reads a set of quoted strings, enclosed in square brackets
    * <code>[ ]</code>, and returns them as an array of Strings.
    * 
    * @param rtok
    * Tokenizer from which values are read
    * @param quoteChar
    * quote character for the strings
    * @return array of strings read
    * @throws IOException
    * if the strings are not quoted or are not enclosed in square brackets, or
    * an I/O error occured
    */
   public static String[] scanQuotedStrings (
      ReaderTokenizer rtok, char quoteChar) throws IOException {
      ArrayList<String> strings = new ArrayList<String>();
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         if (!rtok.tokenIsQuotedString (quoteChar)) {
            throw new IOException ("quoted string expected, got " + rtok);
         }
         strings.add (rtok.sval);
      }
      return strings.toArray (new String[0]);
   }

   /** 
    * Scans a Scannable object, which is preceeded by it's class name.  An
    * instance of the object is then created, and is itself then scanned.
    * 
    * @param rtok
    * Tokenizer from which values are read
    * @return scannable object that was read.
    * @throws IOException
    * if there was an I/O error or if the Scannable object could not
    * be instantiated.
    */
   public static Scannable scanScannable (ReaderTokenizer rtok, Object ref)
      throws IOException {

      Scannable sobj = null;
      int savedDotSetting = rtok.getCharSetting ('.');
      rtok.wordChar ('.');
      String className = rtok.scanWord();
      if (!className.equals ("null")) {
         Class<?> sclass = null;
         try {
            sclass = Class.forName (className);
         }
         catch (Exception e) {
            // ignore
         }
         if (sclass == null) {
            rtok.setCharSetting ('.', savedDotSetting);
            throw new IOException ("Cannot resolve class " + className);
         }
         try {
            sobj = (Scannable)sclass.newInstance();
         }
         catch (Exception e) {
            e.printStackTrace();
            rtok.setCharSetting ('.', savedDotSetting);
            throw new IOException (
               "Cannot instantiate class " + sclass.getName());
         }
         sobj.scan (rtok, ref);         
      }
      rtok.setCharSetting ('.', savedDotSetting);
      return sobj;
   }

   /**
    * Checks to see if the next input token corresponds to a specific field
    * name. If not, the token is pushed back and the method returns false.
    * Otherwise, an '=' token is read from the input and the method retuns true.
    * 
    * @param rtok
    * Tokenizer from which input is read
    * @param name
    * field name to check for
    * @return true if the next input token corresponds to the specified field
    * name
    * @throws IOException
    * if the field name was present but not followed by an '=' token, or an I/O
    * error occured
    */
   public static boolean checkForFieldName (ReaderTokenizer rtok, String name)
      throws IOException {
      if (rtok.nextToken() == ReaderTokenizer.TT_WORD) {
         if (!rtok.sval.equals (name)) {
            rtok.pushBack();
            return false;
         }
         if (rtok.nextToken() != '=') {
            throw new IOException ("'=' expected following field name '" + name
            + "', line " + rtok.lineno());
         }
      }
      return true;
   }
   
   private static HashMap<String,Font> myFontMap = null; 
   
   public static Font scanFont(ReaderTokenizer rtok) throws IOException {
      rtok.scanToken ('[');
      rtok.scanWord ("fontName");
      rtok.scanToken ('=');
      String fontName = rtok.scanQuotedString ('"');
      rtok.scanToken (',');
      rtok.scanWord ("size");
      rtok.scanToken ('=');
      double fontSize = rtok.scanNumber ();
      rtok.scanToken (',');
      rtok.scanWord ("style");
      rtok.scanToken ('=');
      int fontStyle = rtok.scanInteger ();
      rtok.scanToken (']');
      
      // set of built-in fonts
      if (Font.MONOSPACED.equals (fontName) || Font.SERIF.equals (fontName) ||
         Font.SANS_SERIF.equals (fontName)) {
         Font out = new Font(fontName, fontStyle, (int)fontSize);
         if ((fontSize - (int)fontSize) > 0) {
            out = out.deriveFont ((float)fontSize);
         }
         return out;
      }
      
      // map to fonts
      if (myFontMap == null) {
         HashMap<String,Font> fontMap = new HashMap<> ();
         for (Font font : GraphicsEnvironment.getLocalGraphicsEnvironment ().getAllFonts ()) {
            fontMap.put (font.getFontName (), font);
         }
         myFontMap = fontMap;
      }
      
      Font font = myFontMap.get (fontName);
      if (font == null) {
         System.err.println ("Warning: font `" + fontName + "' is not available on this system.  Replacing with SERIF.");
         font = new Font(Font.SERIF, Font.PLAIN, 1);
      }
      font = font.deriveFont (fontStyle, (float)fontSize);
      return font;
   }
}
