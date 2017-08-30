/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.matrix;

import maspack.matrix.*;
import maspack.matrix.Matrix.Partition;

/**
 * Implements a 6 x 6 matrix block using a single Matrix6d object.
 */
public class Matrix6dBlock extends Matrix6d implements MatrixBlock {
   protected MatrixBlock myNext;
   protected MatrixBlock myDown;

   protected int myBlkRow;
   protected int myBlkCol;
   protected int myRowOff;
   protected int myColOff;

   protected int myNumber;

   private void initBlockVariables() {
      myNext = null;
      myDown = null;
      myBlkRow = -1;
      myBlkCol = -1;
      myRowOff = -1;
      myColOff = -1;
      myNumber = -1;
   }

   /**
    * Creates a new Matrix6dBlock.
    */
   public Matrix6dBlock() {
      super();
      initBlockVariables();
   }

   /**
    * {@inheritDoc}
    */
   public MatrixBlock next() {
      return myNext;
   }

   /**
    * {@inheritDoc}
    */
   public void setNext (MatrixBlock blk) {
      myNext = blk;
   }

   /**
    * {@inheritDoc}
    */
   public MatrixBlock down() {
      return myDown;
   }

   /**
    * {@inheritDoc}
    */
   public void setDown (MatrixBlock blk) {
      myDown = blk;
   }

   /**
    * {@inheritDoc}
    */
   public int getBlockRow() {
      return myBlkRow;
   }

   /**
    * {@inheritDoc}
    */
   public void setBlockRow (int blkRow) {
      myBlkRow = blkRow;
   }

   /**
    * {@inheritDoc}
    */
   public int getBlockCol() {
      return myBlkCol;
   }

   /**
    * {@inheritDoc}
    */
   public void setBlockCol (int blkCol) {
      myBlkCol = blkCol;
   }

   /**
    * {@inheritDoc}
    */
   public int getBlockNumber() {
      return myNumber;
   }

   /**
    * {@inheritDoc}
    */
   public void setBlockNumber (int num) {
      myNumber = num;
   }

   /**
    * {@inheritDoc}
    */
   public void mulAdd (double[] y, int yIdx, double[] x, int xIdx) {
      double x0 = x[xIdx + 0];
      double x1 = x[xIdx + 1];
      double x2 = x[xIdx + 2];
      double x3 = x[xIdx + 3];
      double x4 = x[xIdx + 4];
      double x5 = x[xIdx + 5];

      y[yIdx + 0] += m00*x0 + m01*x1 + m02*x2 + m03*x3 + m04*x4 + m05*x5;
      y[yIdx + 1] += m10*x0 + m11*x1 + m12*x2 + m13*x3 + m14*x4 + m15*x5;
      y[yIdx + 2] += m20*x0 + m21*x1 + m22*x2 + m23*x3 + m24*x4 + m25*x5;
      y[yIdx + 3] += m30*x0 + m31*x1 + m32*x2 + m33*x3 + m34*x4 + m35*x5;
      y[yIdx + 4] += m40*x0 + m41*x1 + m42*x2 + m43*x3 + m44*x4 + m45*x5;
      y[yIdx + 5] += m50*x0 + m51*x1 + m52*x2 + m53*x3 + m54*x4 + m55*x5;
   }

   /**
    * {@inheritDoc}
    */
   public void mulTransposeAdd (double[] y, int yIdx, double[] x, int xIdx) {
      double x0 = x[xIdx + 0];
      double x1 = x[xIdx + 1];
      double x2 = x[xIdx + 2];
      double x3 = x[xIdx + 3];
      double x4 = x[xIdx + 4];
      double x5 = x[xIdx + 5];

      y[yIdx + 0] += m00*x0 + m10*x1 + m20*x2 + m30*x3 + m40*x4 + m50*x5;
      y[yIdx + 1] += m01*x0 + m11*x1 + m21*x2 + m31*x3 + m41*x4 + m51*x5;
      y[yIdx + 2] += m02*x0 + m12*x1 + m22*x2 + m32*x3 + m42*x4 + m52*x5;
      y[yIdx + 3] += m03*x0 + m13*x1 + m23*x2 + m33*x3 + m43*x4 + m53*x5;
      y[yIdx + 4] += m04*x0 + m14*x1 + m24*x2 + m34*x3 + m44*x4 + m54*x5;
      y[yIdx + 5] += m05*x0 + m15*x1 + m25*x2 + m35*x3 + m45*x4 + m55*x5;
   }

   /**
    * {@inheritDoc}
    */
   public void add (Matrix M) {
      if (M instanceof Matrix6dBase) {
         add ((Matrix6dBase)M);
      }
      else {
         if (M.rowSize() != 6 || M.colSize() != 6) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += M.get (0, 0);
         m01 += M.get (0, 1);
         m02 += M.get (0, 2);
         m03 += M.get (0, 3);
         m04 += M.get (0, 4);
         m05 += M.get (0, 5);

         m10 += M.get (1, 0);
         m11 += M.get (1, 1);
         m12 += M.get (1, 2);
         m13 += M.get (1, 3);
         m14 += M.get (1, 4);
         m15 += M.get (1, 5);

         m20 += M.get (2, 0);
         m21 += M.get (2, 1);
         m22 += M.get (2, 2);
         m23 += M.get (2, 3);
         m24 += M.get (2, 4);
         m25 += M.get (2, 5);

         m30 += M.get (3, 0);
         m31 += M.get (3, 1);
         m32 += M.get (3, 2);
         m33 += M.get (3, 3);
         m34 += M.get (3, 4);
         m35 += M.get (3, 5);

         m40 += M.get (4, 0);
         m41 += M.get (4, 1);
         m42 += M.get (4, 2);
         m43 += M.get (4, 3);
         m44 += M.get (4, 4);
         m45 += M.get (4, 5);

         m50 += M.get (5, 0);
         m51 += M.get (5, 1);
         m52 += M.get (5, 2);
         m53 += M.get (5, 3);
         m54 += M.get (5, 4);
         m55 += M.get (5, 5);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void scaledAdd (double s, Matrix M) {
      if (M instanceof Matrix6dBase) {
         scaledAdd (s, (Matrix6dBase)M);
      }
      else {
         if (M.rowSize() != 6 || M.colSize() != 6) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 += s*M.get (0, 0);
         m01 += s*M.get (0, 1);
         m02 += s*M.get (0, 2);
         m03 += s*M.get (0, 3);
         m04 += s*M.get (0, 4);
         m05 += s*M.get (0, 5);

         m10 += s*M.get (1, 0);
         m11 += s*M.get (1, 1);
         m12 += s*M.get (1, 2);
         m13 += s*M.get (1, 3);
         m14 += s*M.get (1, 4);
         m15 += s*M.get (1, 5);

         m20 += s*M.get (2, 0);
         m21 += s*M.get (2, 1);
         m22 += s*M.get (2, 2);
         m23 += s*M.get (2, 3);
         m24 += s*M.get (2, 4);
         m25 += s*M.get (2, 5);

         m30 += s*M.get (3, 0);
         m31 += s*M.get (3, 1);
         m32 += s*M.get (3, 2);
         m33 += s*M.get (3, 3);
         m34 += s*M.get (3, 4);
         m35 += s*M.get (3, 5);

         m40 += s*M.get (4, 0);
         m41 += s*M.get (4, 1);
         m42 += s*M.get (4, 2);
         m43 += s*M.get (4, 3);
         m44 += s*M.get (4, 4);
         m45 += s*M.get (4, 5);

         m50 += s*M.get (5, 0);
         m51 += s*M.get (5, 1);
         m52 += s*M.get (5, 2);
         m53 += s*M.get (5, 3);
         m54 += s*M.get (5, 4);
         m55 += s*M.get (5, 5);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void sub (Matrix M) {
      if (M instanceof Matrix6dBase) {
         sub ((Matrix6dBase)M);
      }
      else {
         if (M.rowSize() != 6 || M.colSize() != 6) {
            throw new ImproperSizeException ("matrix sizes do not conform");
         }
         m00 -= M.get (0, 0);
         m01 -= M.get (0, 1);
         m02 -= M.get (0, 2);
         m03 -= M.get (0, 3);
         m04 -= M.get (0, 4);
         m05 -= M.get (0, 5);

         m10 -= M.get (1, 0);
         m11 -= M.get (1, 1);
         m12 -= M.get (1, 2);
         m13 -= M.get (1, 3);
         m14 -= M.get (1, 4);
         m15 -= M.get (1, 5);

         m20 -= M.get (2, 0);
         m21 -= M.get (2, 1);
         m22 -= M.get (2, 2);
         m23 -= M.get (2, 3);
         m24 -= M.get (2, 4);
         m25 -= M.get (2, 5);

         m30 -= M.get (3, 0);
         m31 -= M.get (3, 1);
         m32 -= M.get (3, 2);
         m33 -= M.get (3, 3);
         m34 -= M.get (3, 4);
         m35 -= M.get (3, 5);

         m40 -= M.get (4, 0);
         m41 -= M.get (4, 1);
         m42 -= M.get (4, 2);
         m43 -= M.get (4, 3);
         m44 -= M.get (4, 4);
         m45 -= M.get (4, 5);

         m50 -= M.get (5, 0);
         m51 -= M.get (5, 1);
         m52 -= M.get (5, 2);
         m53 -= M.get (5, 3);
         m54 -= M.get (5, 4);
         m55 -= M.get (5, 5);
      }
   }

   /**
    * {@inheritDoc}
    */
   public int getBlockCRSIndices (
      int[] colIdxs, int colOff, int[] offsets, Partition part) {
      return MatrixBlockBase.getBlockCRSIndices (
         this, colIdxs, colOff, offsets, part);
   }

   /**
    * {@inheritDoc}
    */
   public void addNumNonZerosByRow (int[] offsets, int idx, Partition part) {
      MatrixBlockBase.addNumNonZerosByRow (this, offsets, idx, part);
   }

   /**
    * {@inheritDoc}
    */
   public int getBlockCCSIndices (
      int[] rowIdxs, int rowOff, int[] offsets, Partition part) {
      return MatrixBlockBase.getBlockCCSIndices (
         this, rowIdxs, rowOff, offsets, part);
   }

   /**
    * {@inheritDoc}
    */
   public void addNumNonZerosByCol (int[] offsets, int idx, Partition part) {
      MatrixBlockBase.addNumNonZerosByCol (this, offsets, idx, part);
   }

   /**
    * {@inheritDoc}
    */
   public int getBlockCRSValues (double[] vals, int[] offsets, Partition part) {
      int off;

      if (part == Partition.UpperTriangular) {
         off = offsets[0];
         vals[off] = m00;
         vals[off + 1] = m01;
         vals[off + 2] = m02;
         vals[off + 3] = m03;
         vals[off + 4] = m04;
         vals[off + 5] = m05;
         offsets[0] = off + 6;

         off = offsets[1];
         vals[off] = m11;
         vals[off + 1] = m12;
         vals[off + 2] = m13;
         vals[off + 3] = m14;
         vals[off + 4] = m15;
         offsets[1] = off + 5;

         off = offsets[2];
         vals[off] = m22;
         vals[off + 1] = m23;
         vals[off + 2] = m24;
         vals[off + 3] = m25;
         offsets[2] = off + 4;

         off = offsets[3];
         vals[off] = m33;
         vals[off + 1] = m34;
         vals[off + 2] = m35;
         offsets[3] = off + 3;

         off = offsets[4];
         vals[off] = m44;
         vals[off + 1] = m45;
         offsets[4] = off + 2;

         off = offsets[5];
         vals[off] = m55;
         offsets[5] = off + 1;

         return 21;
      }
      else {
         off = offsets[0];
         vals[off] = m00;
         vals[off + 1] = m01;
         vals[off + 2] = m02;
         vals[off + 3] = m03;
         vals[off + 4] = m04;
         vals[off + 5] = m05;
         offsets[0] = off + 6;

         off = offsets[1];
         vals[off] = m10;
         vals[off + 1] = m11;
         vals[off + 2] = m12;
         vals[off + 3] = m13;
         vals[off + 4] = m14;
         vals[off + 5] = m15;
         offsets[1] = off + 6;

         off = offsets[2];
         vals[off] = m20;
         vals[off + 1] = m21;
         vals[off + 2] = m22;
         vals[off + 3] = m23;
         vals[off + 4] = m24;
         vals[off + 5] = m25;
         offsets[2] = off + 6;

         off = offsets[3];
         vals[off] = m30;
         vals[off + 1] = m31;
         vals[off + 2] = m32;
         vals[off + 3] = m33;
         vals[off + 4] = m34;
         vals[off + 5] = m35;
         offsets[3] = off + 6;

         off = offsets[4];
         vals[off] = m40;
         vals[off + 1] = m41;
         vals[off + 2] = m42;
         vals[off + 3] = m43;
         vals[off + 4] = m44;
         vals[off + 5] = m45;
         offsets[4] = off + 6;

         off = offsets[5];
         vals[off] = m50;
         vals[off + 1] = m51;
         vals[off + 2] = m52;
         vals[off + 3] = m53;
         vals[off + 4] = m54;
         vals[off + 5] = m55;
         offsets[5] = off + 6;

         return 36;
      }
   }

   /**
    * {@inheritDoc}
    */
   public int getBlockCCSValues (double[] vals, int[] offsets, Partition part) {
      int off;

      if (part == Partition.LowerTriangular) {
         off = offsets[0];
         vals[off] = m00;
         vals[off + 1] = m10;
         vals[off + 2] = m20;
         vals[off + 3] = m30;
         vals[off + 4] = m40;
         vals[off + 5] = m50;
         offsets[0] = off + 6;

         off = offsets[1];
         vals[off] = m11;
         vals[off + 1] = m21;
         vals[off + 2] = m31;
         vals[off + 3] = m41;
         vals[off + 4] = m51;
         offsets[1] = off + 5;

         off = offsets[2];
         vals[off] = m22;
         vals[off + 1] = m32;
         vals[off + 2] = m42;
         vals[off + 3] = m52;
         offsets[2] = off + 4;

         off = offsets[3];
         vals[off] = m33;
         vals[off + 1] = m43;
         vals[off + 2] = m53;
         offsets[3] = off + 3;

         off = offsets[4];
         vals[off] = m44;
         vals[off + 1] = m54;
         offsets[4] = off + 2;

         off = offsets[5];
         vals[off] = m55;
         offsets[5] = off + 1;

         return 21;
      }
      else {
         off = offsets[0];
         vals[off] = m00;
         vals[off + 1] = m10;
         vals[off + 2] = m20;
         vals[off + 3] = m30;
         vals[off + 4] = m40;
         vals[off + 5] = m50;
         offsets[0] = off + 6;

         off = offsets[1];
         vals[off] = m01;
         vals[off + 1] = m11;
         vals[off + 2] = m21;
         vals[off + 3] = m31;
         vals[off + 4] = m41;
         vals[off + 5] = m51;
         offsets[1] = off + 6;

         off = offsets[2];
         vals[off] = m02;
         vals[off + 1] = m12;
         vals[off + 2] = m22;
         vals[off + 3] = m32;
         vals[off + 4] = m42;
         vals[off + 5] = m52;
         offsets[2] = off + 6;

         off = offsets[3];
         vals[off] = m03;
         vals[off + 1] = m13;
         vals[off + 2] = m23;
         vals[off + 3] = m33;
         vals[off + 4] = m43;
         vals[off + 5] = m53;
         offsets[3] = off + 6;

         off = offsets[4];
         vals[off] = m04;
         vals[off + 1] = m14;
         vals[off + 2] = m24;
         vals[off + 3] = m34;
         vals[off + 4] = m44;
         vals[off + 5] = m54;
         offsets[4] = off + 6;

         off = offsets[5];
         vals[off] = m05;
         vals[off + 1] = m15;
         vals[off + 2] = m25;
         vals[off + 3] = m35;
         vals[off + 4] = m45;
         vals[off + 5] = m55;
         offsets[5] = off + 6;

         return 36;
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean valueIsNonZero (int i, int j) {
      return true;
   }
 
   /**
    * Creates a transpose of this matrix block.
    */
   public Matrix6dBlock createTranspose() {
      Matrix6dBlock M = new Matrix6dBlock();
      M.transpose (this);
      return M;
   }

  /**
    * Creates a clone of this matrix block, with the link and offset information
    * set to be undefined.
    */
   public Matrix6dBlock clone() {
      Matrix6dBlock blk = (Matrix6dBlock)super.clone();
      blk.initBlockVariables();
      return blk;
   }

}
