/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import artisynth.core.probes.PlotTraceInfo.TraceColor;

import java.awt.Color;
import java.util.*;
import artisynth.core.modelbase.*;
import maspack.util.*;
import maspack.properties.*;

/**
 * Maintains a PlotTraceInfo for each entry in the data vector of a
 * NumericProbe. This includes selecting default values, and updating
 * values with respect to changes and edits requested by the user.
 */
public class PlotTraceManager {
   LinkedHashMap<String,PlotTraceInfo[]> myPlotTraceMap;
   ArrayList<PlotTraceInfo> myPlotTraceList;
   int[] myPlotTraceOrdering;

   private int myFreeColorIdx = 0;
   LinkedList<TraceColor> myFreeColors = new LinkedList<TraceColor>();

   private TraceColor allocColor() {
      if (myFreeColors.size() > 0) {
         return myFreeColors.removeLast();
      }
      else {
         TraceColor[] palette = PlotTraceInfo.getPaletteColors();
         return palette[myFreeColorIdx++ % palette.length];
      }
   }

   private void freeColor (TraceColor color) {
      myFreeColors.add (color);
   }

   private void freeColors (PlotTraceInfo[] infos) {
      for (PlotTraceInfo pti : infos) {
         if (pti.getColor() instanceof TraceColor) {
            freeColor ((TraceColor)pti.getColor());
         }
      }
   }

   private String myDefaultPrefix = null;

   public PlotTraceManager (String defaultPrefix) {
      myPlotTraceMap = new LinkedHashMap<String,PlotTraceInfo[]>();
      myPlotTraceList = new ArrayList<PlotTraceInfo>();
      myPlotTraceOrdering = new int[16];
      myDefaultPrefix = defaultPrefix;
   }

   private void growOrderingListIfNecessary (int size) {
      if (myPlotTraceOrdering.length < size) {
         int[] newlist = new int[(size*5)/4];
         for (int i=0; i<myPlotTraceOrdering.length; i++) {
            newlist[i] = myPlotTraceOrdering[i];
         }
         myPlotTraceOrdering = newlist;
      }
   }

   private void remove (PlotTraceInfo[] infos) {
      int startIdx = myPlotTraceList.indexOf (infos[0]);
      if (startIdx == -1) {
         throw new InternalErrorException ("info block not found");
      }
      int stopIdx = startIdx + infos.length;
      int neworder = 0;
      for (int order=0; order<myPlotTraceList.size(); order++) {
         int idx = myPlotTraceOrdering[order];
         if (idx < startIdx) {
            myPlotTraceOrdering[neworder++] = idx;            
         }
         else if (idx > stopIdx) {
            myPlotTraceOrdering[neworder++] = (idx-infos.length);
         }
      }
      // myPlotTraceList.removeRange (startIdx, stopIdx);
      Iterator<PlotTraceInfo> it =myPlotTraceList.iterator();
      int idx = 0;
      while (it.hasNext()) {
         it.next();
         if (idx >= startIdx && idx < stopIdx) {
            it.remove();
         }
         idx++;
      }
      for (int order=0; order<myPlotTraceList.size(); order++) {
         myPlotTraceList.get(myPlotTraceOrdering[order]).setOrder (order);
      }
      for (Map.Entry<String,PlotTraceInfo[]> entry : myPlotTraceMap.entrySet()) {
         if (entry.getValue() == infos) {
            myPlotTraceMap.remove (entry.getKey());
            break;
         }
      }
   }
   
   private void add (String fullname, PlotTraceInfo[] infos) {
      myPlotTraceMap.put (fullname, infos);
      growOrderingListIfNecessary (myPlotTraceList.size() + infos.length);
      for (PlotTraceInfo pti : infos) {
         int idx = myPlotTraceList.size();
         pti.setOrder (idx);
         myPlotTraceList.add (pti);
         myPlotTraceOrdering[idx] = idx;
      }
   }

   private String getFullName (Object propOrDimen, int idx) {
      if (propOrDimen instanceof Integer) {
         return myDefaultPrefix + idx;
      }
      else if (propOrDimen instanceof Property) {
         return ComponentUtils.getPropertyPathName ((Property)propOrDimen);
      }
      else {
         throw new InternalErrorException (
            "Unknown argument type: " + propOrDimen.getClass());
      }
   }

   private String getLabelName (String fullname) {
      int idx = fullname.lastIndexOf ('/');
      if (idx != -1) {
         while ((idx-1) >= 0 && fullname.charAt(idx-1) != '/') {
            idx--;
         }
         return fullname.substring (idx);
      }
      else {
         return fullname;
      }
   }

   private String[] createLabels (String labelName, Object propOrDimen) {
      int dimen = 0;
      String[] sublabels = null;
           
      if (propOrDimen instanceof Integer) {
         dimen = ((Integer)propOrDimen).intValue();
      }
      else if (propOrDimen instanceof Property) {
         Property prop = (Property)propOrDimen;
         Class propValueClass = prop.getInfo().getValueClass();
         sublabels = NumericConverter.getFieldNames (propValueClass);
         dimen = NumericConverter.getDimension(prop.get());
      }
      else {
         throw new InternalErrorException (
            "Unknown argument type: " + propOrDimen.getClass());
      }
      String[] labels = new String[dimen];
      for (int i=0; i<dimen; i++) {
         if (sublabels != null) {
            labels[i] = labelName + "." + sublabels[i];
         }
         else if (dimen > 1) {
            labels[i] = labelName + "[" + i + "]";
         }
         else {
            labels[i] = labelName;
         }
         
      }
      return labels;
   }

   private int getDimension (Object propOrDimen) {
      if (propOrDimen instanceof Integer) {
         return ((Integer)propOrDimen).intValue();
      }
      else if (propOrDimen instanceof Property) {
         Property prop = (Property)propOrDimen;
         return NumericConverter.getDimension(prop.get());
      }
      else {
         throw new InternalErrorException (
            "Unknown argument type: " + propOrDimen.getClass());
      }
   }

   private PlotTraceInfo[] createPlotTraces (
      String fullname, Object propOrDimen) {

      int dimen = getDimension (propOrDimen);
      String[] labels = createLabels (getLabelName(fullname), propOrDimen);
      PlotTraceInfo[] infos = new PlotTraceInfo[dimen];
      for (int i=0; i<dimen; i++) {
         PlotTraceInfo pti = new PlotTraceInfo();
         pti.setLabel (labels[i]);
         pti.setColor (allocColor());
         infos[i] = pti;
      }      
      return infos;
   }

   int getNumTraces (Object[] propsOrDimens) {
      int num = 0;
      for (int i=0; i<propsOrDimens.length; i++){
         num += getDimension (propsOrDimens[i]);
      }
      return num;
   }

   public boolean hasDefaultSettings (Object[] propsOrDimens) {
      int k = 0;
      TraceColor[] palette = PlotTraceInfo.getPaletteColors();
      for (int i=0; i<propsOrDimens.length; i++){
         String fullname = getFullName (propsOrDimens[i], i);         
         PlotTraceInfo[] infos = myPlotTraceMap.get(fullname);
         if (infos == null) {
            throw new InternalErrorException (
               "No plot traces for " + fullname);
         }
         String[] labels = createLabels (
            getLabelName(fullname), propsOrDimens[i]);
         for (int j=0; j<infos.length; j++) {
            PlotTraceInfo pti = infos[j];
            if (pti.getOrder() != k ||
                pti.getColor() != palette[k % palette.length] ||
                !pti.isVisible() ||
                !pti.getLabel().equals (labels[j])) {
               return false;
            }
            k++;
         }
      }
      return false;
   }

   public PlotTraceInfo[] getAllTraceInfo (Object[] propsOrDimens) {
      return myPlotTraceList.toArray (new PlotTraceInfo[0]);
//       int k = 0;
//       PlotTraceInfo[] allInfo = new PlotTraceInfo[getNumTraces(propsOrDimens)];
//       TraceColor[] palette = PlotTraceInfo.getPaletteColors();
//       for (int i=0; i<propsOrDimens.length; i++){
//          String fullname = getFullName (propsOrDimens[i], i);         
//          PlotTraceInfo[] infos = myPlotTraceMap.get(fullname);
//          if (infos == null) {
//             throw new InternalErrorException (
//                "No plot traces for " + fullname);
//          }
//          for (PlotTraceInfo pti : infos) {
//             allInfo[k++] = pti;
//          }
//       }
//       return allInfo;
   }

   private void checkBounds (int idx) {
      if (idx < 0 || idx >= myPlotTraceList.size()) {
         throw new ArrayIndexOutOfBoundsException (
            "idx="+idx+", size="+myPlotTraceList.size());
      }
   }

   public PlotTraceInfo getTraceInfo (int idx) {
      checkBounds (idx);
      return myPlotTraceList.get (idx);
   }

   public int getOrderedTraceIndex (int order) {
      return myPlotTraceOrdering[order];
   }

   /**
    * Sets a new ordering for the plot traces. This is specified by an array
    * giving the indices of the plot traces in the order they should be
    * plotted.
    */
   public void setTraceOrder (int[] indices) {
      if (indices.length < myPlotTraceList.size()) {
         throw new IllegalArgumentException (
            "new order list is insufficiently long");
      }
      for (int order=0; order<indices.length; order++) {
         int idx = indices[order];
         if (idx < 0 || idx >= myPlotTraceList.size()) {
            throw new ArrayIndexOutOfBoundsException (
               "idx=" + idx + ", size=" + myPlotTraceList.size());
         }
         PlotTraceInfo pti = myPlotTraceList.get(idx);
         pti.setOrder (order);
         myPlotTraceOrdering[order] = idx;
      }
   }

   public void swapTraceOrder (PlotTraceInfo info0, PlotTraceInfo info1) {
      if (info0 == info1) {
         return;
      }
      int order0 = info0.getOrder();
      int order1 = info1.getOrder();
      myPlotTraceOrdering[order0] = myPlotTraceList.indexOf (info1);
      myPlotTraceOrdering[order1] = myPlotTraceList.indexOf (info0);
      info0.setOrder (order1);
      info1.setOrder (order0);
   }

   public void resetTraceOrder () {
      for (int i=0; i<myPlotTraceList.size(); i++) {
         PlotTraceInfo pti = myPlotTraceList.get(i);
         pti.setOrder (i);
         myPlotTraceOrdering[i] = i;         
      }
   }

   public void resetTraceColors () {
      myFreeColors.clear();
      TraceColor[] palette = PlotTraceInfo.getPaletteColors();      
      for (int i=0; i<myPlotTraceList.size(); i++) {
         myPlotTraceList.get(i).setColor (palette[i % palette.length]);
      }
      myFreeColorIdx = myPlotTraceList.size();
   }

   public void setTraceVisible (int idx, boolean visible) {
      myPlotTraceList.get(idx).setVisible (visible);
   }

   public boolean isTraceVisible (int idx) {
      return myPlotTraceList.get(idx).isVisible ();
   }

   public void setTraceColor (int idx, Color color) {
      myPlotTraceList.get(idx).setColor (color);
   }

   public Color getTraceColor (int idx) {
      return myPlotTraceList.get(idx).getColor();
   }

   public void rebuild (Object[] propsOrDimens, PlotTraceInfo[] allInfos) {
      
      myPlotTraceMap.clear();
      myPlotTraceList.clear();
      if (getNumTraces (propsOrDimens) != allInfos.length) {
         throw new IllegalArgumentException (
            "Number of traces "+allInfos.length+
            " inconsistent with total dimension "+getNumTraces (propsOrDimens));
      }
      myFreeColors.clear();
      growOrderingListIfNecessary (allInfos.length);
      for (int i=0; i<allInfos.length; i++) {
         PlotTraceInfo pti = allInfos[i];
         myPlotTraceList.add (pti);
         myPlotTraceOrdering[pti.getOrder()] = i;
         if (pti.getColor() instanceof TraceColor) {
            myFreeColorIdx++;
         }
      }
      int k = 0;
      TraceColor[] palette = PlotTraceInfo.getPaletteColors();
      for (int i=0; i<propsOrDimens.length; i++){
         String fullname = getFullName (propsOrDimens[i], i);         
         int dimen = getDimension (propsOrDimens[i]);
         PlotTraceInfo[] infos = new PlotTraceInfo[dimen];
         for (int j=0; j<dimen; j++) {
            infos[j] = allInfos[k++];
         }
         myPlotTraceMap.put (fullname, infos);
      }
   }

   public void rebuild (Object[] propsOrDimens) {

      HashMap<String,Object> newSet = new HashMap<String,Object>();
      for (int i=0; i<propsOrDimens.length; i++){
         newSet.put (getFullName (propsOrDimens[i], i), propsOrDimens[i]);
      }
      for (String fullname : myPlotTraceMap.keySet()) {
         PlotTraceInfo[] infos = myPlotTraceMap.get (fullname);
         Object obj = newSet.get (fullname);
         if (obj != null &&
             obj instanceof Integer &&
             ((Integer)obj).intValue() != infos.length) {
            obj = null;
         }
         if (obj == null) {
            freeColors (infos);
            remove (infos);
         }
      }
      for (int i=0; i<propsOrDimens.length; i++){
         String fullname = getFullName (propsOrDimens[i], i);
         PlotTraceInfo[] infos = myPlotTraceMap.get (fullname);
         if (infos == null) {
            infos = createPlotTraces (fullname, propsOrDimens[i]);
            add (fullname, infos);
         }
      }
   }

   // describing the possible changes that can be made to an
   // existing set of variables:

   // A, B, C

   // add new at the end: A, B, C, D
   //
   // order=last, color=default, visible=true, label=default
   
   // add new elsewhere: A, D, B, C
   //
   // order=last, color=default, visible=true, label=default

   // remove element: A, C
   //
   // free colors, compress order

   // move element: A, C, B
   //
   // order=existing, color=existing, visible=exitsing, label=existing

   // alter element (non-propety): A, B*, C
   //
   // alter only happens if dimension is changed. Then we can treat
   // this like a remove and add
}
