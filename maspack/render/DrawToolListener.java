/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

public interface DrawToolListener {

   public void drawToolAdded (DrawToolEvent e);

   public void drawToolBegin (DrawToolEvent e);

   public void drawToolEnd (DrawToolEvent e);

   public void drawToolRemoved (DrawToolEvent e);
}
