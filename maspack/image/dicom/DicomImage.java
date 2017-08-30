/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.image.dicom;

import java.nio.ByteBuffer;
import java.util.Arrays;

import maspack.image.dicom.DicomPixelBuffer.PixelType;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;

public class DicomImage {

   /**
    * Increase image capacity at most by this much when dynamically adding slice information
    */
   private static final int MAX_CAP_ADJUSTMENT = 128;
   
   String title;
   int rows;
   int cols;
   double pixelSpacingRows;
   double pixelSpacingCols;
   double pixelSpacingSlice;
   PixelType pixelType;
   
   RigidTransform3d trans;
   
   DicomSlice[] slices;
   int timeOffsets[];
   int size;
   
   /**
    * Creates a new DICOM image, extracting common header information from the 
    * provided first slice.  The slice is also added to the image.
    * @param title title to use for DICOM image
    * @param firstSlice the first slice, which determines header information
    */
   public DicomImage(String title, DicomSlice firstSlice) {
      this.title = title;
      this.rows = firstSlice.info.rows;
      this.cols = firstSlice.info.cols;
      this.pixelSpacingRows = firstSlice.info.pixelSpacingRows;
      this.pixelSpacingCols = firstSlice.info.pixelSpacingCols;
      this.pixelSpacingSlice = firstSlice.getHeader().getDecimalValue(DicomTag.SPACING_BETWEEN_SLICES, 
         (float)firstSlice.info.pixelSpacingSlice);
      this.pixelType = firstSlice.pixelBuff.getPixelType();
      
      slices = new DicomSlice[16];
      timeOffsets = new int[1];
      timeOffsets[0] = 0;
      size = 0;
      
      this.trans = new RigidTransform3d(firstSlice.info.imagePosition);
      addSlice(firstSlice);
   }
   
   /**
    * Checks whether the provided slice has compatible image dimensions with the
    * the current image set
    * @param slice new slice to verify
    * @return true if compatible
    */
   public boolean compatible(DicomSlice slice) {
      if (slice.info.rows == rows &&
         slice.info.cols == cols &&
         slice.info.pixelSpacingRows == pixelSpacingRows &&
         slice.info.pixelSpacingCols == pixelSpacingCols &&
         slice.getPixelType() == pixelType) {
         // XXX check if same coordinate system?
         return true;
      }
      return false;
   }

   /**
    * Appends a slice to the current DICOM image set.  The order of the image
    * is extracted from header information contained in the slice, based on
    * temporal and slice position indices if possible.
    * @return true if added
    */
   public boolean addSlice(DicomSlice slice) {
      if (compatible(slice)) {
         maybeGrowCapacity(size+1);
         doAddSlice(slice);
         return true;
      } else {
         return false;
      }
   }
   
   private void doAddSlice(DicomSlice slice) { 
      
      if (size == 0) {
         slices[0] = slice;
         timeOffsets[0] = 0;
         size++;
         return;
      }
      
      // sort first by time, then by zval
      long tadd = slice.info.temporalPosition;
      long t = slices[size-1].info.temporalPosition;
      
      Vector3d zdir = new Vector3d();
      Vector3d orig = trans.p;
      trans.R.getColumn(2, zdir);
      Vector3d tmp = new Vector3d();
   
      tmp.sub(slice.info.imagePosition.p, orig);
      double zadd = tmp.dot(zdir);
      
      // check if it's the last slice
      tmp.sub(slices[size-1].info.imagePosition.p, orig);
      double z = tmp.dot(zdir);
      if (tadd > t) {
         timeOffsets = Arrays.copyOf(timeOffsets, timeOffsets.length+1);
         timeOffsets[timeOffsets.length-1] = size;
         slices[size++] = slice;
         return;
      } else if ((tadd == t) && (zadd >= z)) {
         slices[size++] = slice;
         return;
      }
      
      // look for time
      int pos = 0;
      for (int i=0; i<timeOffsets.length; i++) {
         pos = timeOffsets[i];
         if (slices[pos] == null) {
            System.err.println("hmm...");
         }
         t = slices[pos].info.temporalPosition;
         if (tadd < t) {
            
            // expand/shift time offsets
            int[] newTimeOffsets = new int[timeOffsets.length+1];
            for (int j=0; j<i; j++) {
               newTimeOffsets[j] = timeOffsets[j];
            }
            newTimeOffsets[i] = pos; 
            for (int j=i+1; j < newTimeOffsets.length; j++) {
               newTimeOffsets[j] = timeOffsets[j-1]+1;
            }
            timeOffsets = newTimeOffsets;

            break;
         } else if (tadd == t) {
            
            boolean found = false;
            for (int j=timeOffsets[i]; j<size; j++) {
               tmp.sub(slices[j].info.imagePosition.p, orig);
               t = slices[j].info.temporalPosition;
               z = tmp.dot(zdir);
               if (t > tadd || zadd < z) {
                  pos = j;
                  
                  for (int k = i+1; k < timeOffsets.length; k++) {
                     timeOffsets[k]++;
                     if (timeOffsets[k] > size+1) {
                        System.err.println("bad time offset");
                     }
                  }
                  found = true;
                  break;
               }
            }   
            
            if (found) {
               break;
            }
         }
      }
      
      // shift slices up
      for (int i=size-1; i >= pos; i--) {
         slices[i+1] = slices[i];
      }
      slices[pos] = slice;
            
      if (pos == 0) {
         trans.set(slice.info.imagePosition);
      }
      size++;
   }
   
   private void maybeGrowCapacity(int cap) {
      if (slices.length < cap) {
         ensureCapacity(Math.min(2*cap, cap+MAX_CAP_ADJUSTMENT));
      }
   }
   
   /**
    * Ensure a minimum capacity for number of slices
    * @param cap minimum number of slices to support
    */
   public void ensureCapacity(int cap) {
      if (slices.length < cap) {
         DicomSlice[] oldSlices = slices;
         slices = new DicomSlice[cap];
         for (int i=0; i<size; i++) {
            slices[i] = oldSlices[i];
         }
      }
   }
   
   /**
    * Trims the array of slices to match the number currently contained in the image
    */
   public void trim() {
      if (size < slices.length) {
         DicomSlice[] oldSlices = slices;
         slices = new DicomSlice[size];
         for (int i=0; i<size; i++) {
            slices[i] = oldSlices[i];
         }
      }
   }
   
   /**
    * Finalize DICOM image construction
    */
   public void complete() {
      trim();
   }
   
   /**
    * Determines the type of pixels stored in the image
    * @return byte grayscale, short grayscale, or byte RGB
    */
   public PixelType getPixelType() {
      return pixelType;
   }
   
   @Override
   public String toString() {
      StringBuilder str = new StringBuilder();
      str.append("Title: " + title + "\n");
      str.append("Size: " + rows + "x" + cols + "x" + size + "\n");
      str.append("Times: " + timeOffsets.length + "\n");
      str.append("Resolution: " + pixelSpacingRows + "x" + pixelSpacingCols + "x" + pixelSpacingSlice + "\n");
      str.append("Type: " + pixelType + "\n");
      str.append("Location: " + trans.toString() + "\n");
      return str.toString();
   }
   
   /**
    * Fills a buffer with pixel values from the image
    * @param x starting x voxel position
    * @param y starting y voxel position
    * @param z starting z voxel position
    * @param dx voxel step in x
    * @param dy voxel step in y
    * @param dz voxel step in z
    * @param nx number of voxels in x direction
    * @param ny number of voxels in y direction
    * @param nz number of voxels in z direction
    * @param time time index
    * @param type pixel output type
    * @param scanline offset between x-y rows 
    * @param pageline offset between slices
    * @param pixels pixel buffer to fill
    * @param interp pixel value interpolator (shifts/scales values to appropriate range)
    */
   public void getPixels(int x, int y, int z, 
      int dx, int dy, int dz,
      int nx, int ny, int nz, 
      int time,
      PixelType type,
      int scanline,
      int pageline,
      ByteBuffer pixels,
      DicomPixelInterpolator interp) {
      
      z = timeOffsets[time] + z;
      getPixels(x,y,z,dx,dy,dz,nx,ny,nz,type, scanline, pageline, pixels, interp);
   }
   
   /**
    * Fills a buffer with RGB pixel values from the image
    * @param x starting x voxel position
    * @param y starting y voxel position
    * @param z starting z voxel position
    * @param dx voxel step in x
    * @param dy voxel step in y
    * @param dz voxel step in z
    * @param nx number of voxels in x direction
    * @param ny number of voxels in y direction
    * @param nz number of voxels in z direction
    * @param time time index
    * @param pixels pixel buffer to fill
    * @param interp pixel value interpolator (shifts/scales values to appropriate range)
    * @return the next unfilled index in the pixel buffer
    */
   public int getPixelsRGB(int x, int y, int z, 
      int dx, int dy, int dz,
      int nx, int ny, int nz, 
      int time,
      byte[] pixels,
      DicomPixelInterpolator interp) {
      
      z = timeOffsets[time] + z;
      return getPixelsRGB(x,y,z,dx,dy,dz,nx,ny,nz,pixels,interp);
   }
   
   /**
    * Fills a buffer with grayscale byte pixel values from the image
    * @param x starting x voxel position
    * @param y starting y voxel position
    * @param z starting z voxel position
    * @param dx voxel step in x
    * @param dy voxel step in y
    * @param dz voxel step in z
    * @param nx number of voxels in x direction
    * @param ny number of voxels in y direction
    * @param nz number of voxels in z direction
    * @param time time index
    * @param pixels pixel buffer to fill
    * @param interp pixel value interpolator (shifts/scales values to appropriate range)
    * @return the next unfilled index in the pixel buffer
    */
   public int getPixelsByte(int x, int y, int z, 
      int dx, int dy, int dz,
      int nx, int ny, int nz, 
      int time,
      byte[] pixels,
      DicomPixelInterpolator interp) {
      z = timeOffsets[time] + z;
      
      return getPixelsByte(x,y,z,dx,dy,dz,nx,ny,nz,pixels,interp);
   }
   
   /**
    * Fills a buffer with grayscale short pixel values from the image
    * @param x starting x voxel position
    * @param y starting y voxel position
    * @param z starting z voxel position
    * @param dx voxel step in x
    * @param dy voxel step in y
    * @param dz voxel step in z
    * @param nx number of voxels in x direction
    * @param ny number of voxels in y direction
    * @param nz number of voxels in z direction
    * @param time time index
    * @param pixels pixel buffer to fill
    * @param interp pixel value interpolator (shifts/scales values to appropriate range)
    * @return the next unfilled index in the pixel buffer
    */
   public int getPixelsShort(int x, int y, int z, 
      int dx, int dy, int dz,
      int nx, int ny, int nz, 
      int time,
      short[] pixels,
      DicomPixelInterpolator interp) {
      z = timeOffsets[time] + z;
      
      return getPixelsShort(x,y,z,dx,dy,dz,nx,ny,nz,pixels,interp);
   }
   
   /**
    * Fills a buffer pixel values from the image
    * @param x starting x voxel position
    * @param y starting y voxel position
    * @param z starting z voxel position
    * @param dx voxel step in x
    * @param dy voxel step in y
    * @param dz voxel step in z
    * @param nx number of voxels in x direction
    * @param ny number of voxels in y direction
    * @param nz number of voxels in z direction
    * @param time time index
    * @param pixels pixel buffer to fill (class determines types of values)
    * @param interp pixel value interpolator (shifts/scales values to appropriate range)
    * @return the next unfilled index in the pixel buffer
    */
   public int getPixels(int x, int y, int z, 
      int dx, int dy, int dz,
      int nx, int ny, int nz, 
      int time,
      DicomPixelBuffer pixels,
      DicomPixelInterpolator interp) {
      z = timeOffsets[time] + z;
      
      return getPixels(x,y,z,dx,dy,dz,nx,ny,nz,pixels,interp);
   }
   
   /**
    * Fills a buffer with RGB pixel values from the image
    * @param x starting x voxel position
    * @param y starting y voxel position
    * @param z starting z voxel position
    * @param dx voxel step in x
    * @param dy voxel step in y
    * @param dz voxel step in z
    * @param nx number of voxels in x direction
    * @param ny number of voxels in y direction
    * @param nz number of voxels in z direction
    * @param type output pixel type
    * @param scanline xy row offset
    * @param pageline offset between slices
    * @param pixels pixel buffer to fill
    * @param interp pixel value interpolator (shifts/scales values to appropriate range)
    * @return the next unfilled index in the pixel buffer
    */
   public int getPixels(int x, int y, int z, 
      int dx, int dy, int dz,
      int nx, int ny, int nz, PixelType type, int scanline,
      int pageline, ByteBuffer pixels,
      DicomPixelInterpolator interp) {

      int offset = 0;
      for (int i=0; i<nz; i++) {
         int idx = z+dz*i;
         int pos = pixels.position ();
         slices[idx].getPixels(x, y, dx, dy, nx, ny, type, scanline, pixels, interp);
         if (pageline > 0) {
            pixels.position (pos+pageline);
         }
      }
      return offset;
   }
   
   /**
    * Fills a buffer with RGB pixel values from the image
    * @param x starting x voxel position
    * @param y starting y voxel position
    * @param z starting z voxel position
    * @param dx voxel step in x
    * @param dy voxel step in y
    * @param dz voxel step in z
    * @param nx number of voxels in x direction
    * @param ny number of voxels in y direction
    * @param nz number of voxels in z direction
    * @param pixels pixel buffer to fill
    * @param interp pixel value interpolator (shifts/scales values to appropriate range)
    * @return the next unfilled index in the pixel buffer
    */
   public int getPixelsRGB(int x, int y, int z, 
      int dx, int dy, int dz,
      int nx, int ny, int nz, byte[] pixels,
      DicomPixelInterpolator interp) {

      int offset = 0;
      for (int i=0; i<nz; i++) {
         int idx = z+dz*i;
         offset = slices[idx].getPixelsRGB(x, y, dx, dy, nx, ny, pixels, offset, interp);
      }
      return offset;
   }
   
   /**
    * Fills a buffer with grayscale byte pixel values from the image
    * @param x starting x voxel position
    * @param y starting y voxel position
    * @param z starting z voxel position
    * @param dx voxel step in x
    * @param dy voxel step in y
    * @param dz voxel step in z
    * @param nx number of voxels in x direction
    * @param ny number of voxels in y direction
    * @param nz number of voxels in z direction
    * @param pixels pixel buffer to fill
    * @param interp pixel value interpolator (shifts/scales values to appropriate range)
    * @return the next unfilled index in the pixel buffer
    */
   public int getPixelsByte(int x, int y, int z, 
      int dx, int dy, int dz,
      int nx, int ny, int nz, byte[] pixels,
      DicomPixelInterpolator interp) {
      
      int offset = 0;
      for (int i=0; i<nz; i++) {
         int idx = z+dz*i;
         offset = slices[idx].getPixelsByte(x, y, dx, dy, nx, ny, pixels, offset, interp);
      }
      return offset;
   }
   
   /**
    * Fills a buffer with grayscale short pixel values from the image
    * @param x starting x voxel position
    * @param y starting y voxel position
    * @param z starting z voxel position
    * @param dx voxel step in x
    * @param dy voxel step in y
    * @param dz voxel step in z
    * @param nx number of voxels in x direction
    * @param ny number of voxels in y direction
    * @param nz number of voxels in z direction
    * @param pixels pixel buffer to fill
    * @param interp pixel value interpolator (shifts/scales values to appropriate range)
    * @return the next unfilled index in the pixel buffer
    */
   public int getPixelsShort(int x, int y, int z, 
      int dx, int dy, int dz,
      int nx, int ny, int nz, short[] pixels,
      DicomPixelInterpolator interp) {
      
      int offset = 0;
      for (int i=0; i<nz; i++) {
         int idx = z+dz*i;
         offset = slices[idx].getPixelsShort(x, y, dx, dy, nx, ny, pixels, offset, interp);
      }
      return offset;
   }
   
   /**
    * Fills a buffer pixel values from the image
    * @param x starting x voxel position
    * @param y starting y voxel position
    * @param z starting z voxel position
    * @param dx voxel step in x
    * @param dy voxel step in y
    * @param dz voxel step in z
    * @param nx number of voxels in x direction
    * @param ny number of voxels in y direction
    * @param nz number of voxels in z direction
    * @param pixels pixel buffer to fill (class determines types of values)
    * @param interp pixel value interpolator (shifts/scales values to appropriate range)
    * @return the next unfilled index in the pixel buffer
    */
   public int getPixels(int x, int y, int z,  
      int dx, int dy, int dz,
      int nx, int ny, int nz,
      DicomPixelBuffer pixels,
      DicomPixelInterpolator interp) {
      
      int offset = 0;
      for (int i=0; i<nz; i++) {
         int idx = z+dz*i;
         offset = slices[idx].getPixels(x, y, dx, dy, nx, ny, pixels, offset, interp);
      }
      return offset;
   }
   
   /**
    * @return Number of y-positions (rows) in each image slice
    */
   public int getNumRows() {
      return rows;
   }
   
   /**
    * @return Number of x-positions (columns) in each image slice
    */
   public int getNumCols() {
      return cols;
   }
   
   /**
    * @return Number of time instances, each representing a 3D DICOM image stack
    */
   public int getNumTimes() {
      return timeOffsets.length;
   }
   
   /**
    * @return Number of (z-)slices in a single 3D DICOM image stack (for DICOM images
    * with multiple time points, assumes all times have the same number of slices as
    * the image at the first time index)
    */
   public int getNumSlices() {
      if (timeOffsets.length == 1) {
         return size;
      } else {
         // number of slices before first offset?
         return timeOffsets[1];
      }
   }
   
   /**
    * @return the total number of slices, including all times
    */
   public int size() {
      return size;
   }
   
   /**
    * @return Maximum intensity value in the entire image set, useful for
    * adjusting an intensity window for display
    */
   public int getMaxIntensity() {
      
      int max = 0;
      for (int i=0; i<size; i++) {
         int maxb = slices[i].getMaxIntensity();
         if (maxb > max) {
            max = maxb;
         }
      }
      return max;
   }
   
   /**
    * @return Minimum intensity value in the entire image set, useful for
    * adjusting an intensity window for display
    */
   public int getMinIntensity() {
      int min = Integer.MAX_VALUE;
      for (int i=0; i<size; i++) {
         int minb = slices[i].getMinIntensity();
         if (minb < min) {
            min = minb;
         }
      }
      return min;
   }
   
   /**
    * Extracts a DICOM slice at a given index
    * @param slice index of slice
    * @return the DICOM slice at the given index
    */
   public DicomSlice getSlice(int slice) {
      return slices[slice];
   }
   
   /**
    * Extracts a DICOM slice at a given time point and slice index
    * @param timeIdx time point (identifies 3D stack)
    * @param slice slice index (identifies z position in stack)
    * @return the appropriate DICOM slice
    */
   public DicomSlice getSlice(int timeIdx, int slice ) {
      return getSlice(timeOffsets[timeIdx]+slice);
   }
   
   /**
    * Spatial location of the first slice
    * @return the 3D transform of the voxel (0,0,0) in the DICOM image stack
    */
   public RigidTransform3d getTransform() {
      return trans;
   }
   
   /**
    * Transform for converting integer voxel locations into spatial locations
    * @return the 3D affine transform for converting voxels to spatial locations
    */
   public AffineTransform3d getVoxelTransform() {
      
      int nSlices = getNumSlices();
      // compute slice thickness directly from slice separation
      // (slice thickness is sometimes different)
      if (nSlices > 0) {
         double zsize = slices[nSlices-1].info.imagePosition.p.distance(slices[0].info.imagePosition.p);
         pixelSpacingSlice = zsize/(nSlices-1);
      } 
      if (pixelSpacingSlice == 0) {
         pixelSpacingSlice = slices[0].info.pixelSpacingSlice;
      }
      
      AffineTransform3d pixelTrans = new AffineTransform3d(trans);
      pixelTrans.applyScaling(pixelSpacingRows, pixelSpacingCols, pixelSpacingSlice);
      return pixelTrans;
   }

   /**
    * @return the title of the image
    */
   public String getTitle() {
      return title;
   }
   
}
