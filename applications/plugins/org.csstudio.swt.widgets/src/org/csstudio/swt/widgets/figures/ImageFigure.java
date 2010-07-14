/* 
 * Copyright (c) 2008 Stiftung Deutsches Elektronen-Synchrotron, 
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS. 
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED 
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND 
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE 
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE 
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR 
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE. 
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, 
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION, 
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS 
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY 
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */
package org.csstudio.swt.widgets.figures;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.io.InputStream;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.csstudio.platform.ExecutionService;
import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.swt.widgets.introspection.DefaultWidgetIntrospector;
import org.csstudio.swt.widgets.introspection.Introspectable;
import org.csstudio.swt.widgets.util.ResourceUtil;
import org.csstudio.swt.widgets.util.TextPainter;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;


/**
 * An image figure.
 * 
 * @author jbercic, Xihui Chen
 * 
 */

public final class ImageFigure extends Figure implements Introspectable {
	
	
	/**
	 * The {@link IPath} to the image.
	 */
	private IPath filePath = new Path("");
	/**
	 * The image itself.
	 */
	private Image staticImage=null;
	/**
	 * The width of the image.
	 */
	private int imgWidth=0;
	/**
	 * The height of the image.
	 */
	private int imgHeight=0;
	
	/**
	 * The amount of pixels, which are cropped from the top.
	 */
	private int topCrop=0;

	/**
	 * The amount of pixels, which are cropped from the bottom.
	 */
	private int bottomCrop=0;
	/**
	 * The amount of pixels, which are cropped from the left.
	 */
	private int leftCrop=0;
	/**
	 * The amount of pixels, which are cropped from the right.
	 */
	private int rightCrop=0;
	/**
	 * The stretch state for the image.
	 */
	private boolean stretch=true;
	/**
	 * If this is an animated image
	 */
	private boolean animated = false;
	
	private Image offScreenImage;
	
	private GC offScreenImageGC;
	
	/**
	 * The imaged data array for animated image
	 */
	private ImageData[] imageDataArray;
	private ImageData[] originalImageDataArray;
	
	/**
	 * The index in image data array
	 */
	private int showIndex = 0;

	/**
	 * The animated image is being refreshed by editpart 
	 */
	private boolean refreshing = false;
	
	private boolean animationDisabled = false;
	
	private boolean loadingError = false;
	
	private ImageLoader loader = new ImageLoader();	
	
	//private boolean useGIFBackground = false;
	
	private ImageData originalStaticImageData = null;
	
	private int repeatCount;
	
	
	
	private int animationIndex = 0;
	private long lastUpdateTime;
	private long interval_ms;
	private ScheduledFuture<?> scheduledFuture;
	/**
	 * dispose the resources used by this figure
	 */
	public void dispose(){
		if (offScreenImage != null && !offScreenImage.isDisposed()) {
			offScreenImage.dispose();
			offScreenImage = null;
		}
		
		if (offScreenImageGC != null && !offScreenImageGC.isDisposed()) {
			offScreenImageGC.dispose();
			offScreenImage = null;
		}
			
		if (staticImage != null && !staticImage.isDisposed()) { 
			staticImage.dispose();
			staticImage = null;
		}
	}

	/**
	 * @return the auto sized widget dimension according to the static imageSize
	 */
	public Dimension getAutoSizedDimension() {
		if(originalStaticImageData != null)
			return new Dimension(originalStaticImageData.width + getInsets().getWidth() - leftCrop - rightCrop,
						originalStaticImageData.height + getInsets().getHeight() - topCrop - bottomCrop);
		else
			return null;
	}
	
	/**
	 * Returns the amount of pixels, which are cropped from the top.
	 * @return The amount of pixels
	 */
	public int getBottomCrop() {
		return bottomCrop;
	}
	
	/**
	 * Returns the path to the image.
	 * @return The path to the image
	 */
	public IPath getFilePath() {
		return filePath;
	}
	
	/**
	 * Returns the amount of pixels, which are cropped from the top.
	 * @return The amount of pixels
	 */
	public int getLeftCrop() {
		return leftCrop;
	}

	/**
	 * Returns the amount of pixels, which are cropped from the top.
	 * @return The amount of pixels
	 */
	public int getRightCrop() {
		return rightCrop;
	}

	/**
	 * Returns the stretch state for the image.
	 * @return True, if it should be stretched, false otherwise
	 */
	public boolean getStretch() {
		return stretch;
	}
	
	/**
	 * Returns the amount of pixels, which are cropped from the top.
	 * @return The amount of pixels
	 */
	public int getTopCrop() {
		return topCrop;
	}
	
	/**
	 * @return the animationDisabled
	 */
	public boolean isAnimationDisabled() {
		return animationDisabled;
	}
	
	private void loadImage(IPath path) throws Exception {
		Image temp = null;
		try {
			InputStream stream = ResourceUtil.pathToInputStream(filePath);
			temp=new Image(null,stream); 
			originalStaticImageData = temp.getImageData();
			stream.close();
			stream = ResourceUtil.pathToInputStream(filePath); // reopen stream
			originalImageDataArray = loader.load(stream);
			stream.close();
			animated = (originalImageDataArray.length > 1);	
		}finally {
			if (temp != null && !temp.isDisposed()) 
				temp.dispose();
		}		
			
	}
	
	/**
	 * 
	 */
	private void loadImageFromFile() {
		//load image from file
		try {
			if (staticImage==null && !filePath.isEmpty()) {	
				
				//loading by stream					
				loadImage(filePath);
			}		
		} catch (Exception e) {
			loadingError = true;
			CentralLogger.getInstance().error(this, "ERROR in loading image\n"+filePath, e);
		}
	}
	
	/**
	 * The main drawing routine.
	 * @param gfx The {@link Graphics} to use
	 */
	public void paintFigure(final Graphics gfx) {
		Rectangle bound=getBounds().getCopy();
		bound.crop(this.getInsets());
		if(loadingError) {
				if (staticImage!=null) {
					staticImage.dispose();
				}
				staticImage=null;
				if (!filePath.isEmpty()) {
					/*Font f=gfx.getFont();
					FontData fd=f.getFontData()[0];
					
					if (bound.width>=20*30) {
						fd.setHeight(30);
					} else {
						if (bound.width/20+1<7) {
							fd.setHeight(7);
						} else {
							fd.setHeight(bound.width/20+1);
						}
					}
					f=new Font(Display.getDefault(),fd);
					gfx.setFont(f);*/
					gfx.setBackgroundColor(getBackgroundColor());
					gfx.setForegroundColor(getForegroundColor());
					gfx.fillRectangle(bound);
					gfx.translate(bound.getLocation());
					TextPainter.drawText(gfx,"ERROR in loading image\n"+filePath,bound.width/2,bound.height/2,TextPainter.CENTER);
					//f.dispose();
				}
				return;
		}
		
		//create static image
		if(staticImage == null && originalStaticImageData !=null){
				if (stretch) {
					staticImage=new Image(Display.getDefault(),
							originalStaticImageData.scaledTo(bound.width+leftCrop+rightCrop,
									bound.height+topCrop+bottomCrop));
					if(animated) {
						imageDataArray = new ImageData[originalImageDataArray.length];
						double widthScaleRatio = (double)(bound.width+leftCrop+rightCrop) / (double)originalStaticImageData.width;
						double heightScaleRatio = (double)(bound.height+topCrop+bottomCrop) / (double)originalStaticImageData.height;
						for (int i = 0; i < originalImageDataArray.length; i++){
							int scaleWidth = (int) (originalImageDataArray[i].width * widthScaleRatio);
							int scaleHeight = (int) (originalImageDataArray[i].height * heightScaleRatio);
							int x = (int) (originalImageDataArray[i].x * widthScaleRatio);
							int y = (int) (originalImageDataArray[i].y * heightScaleRatio);
	
							imageDataArray[i] = originalImageDataArray[i].scaledTo(scaleWidth, scaleHeight);
							imageDataArray[i].x = x;
							imageDataArray[i].y = y;
						}
																					
					}
				} else {
					staticImage=new Image(Display.getDefault(),originalStaticImageData);
					if(animated)
						imageDataArray = originalImageDataArray;											
				}
				imgWidth=staticImage.getBounds().width;
				imgHeight=staticImage.getBounds().height;				
				
				if(animated) {
					if (offScreenImage != null && !offScreenImage.isDisposed()) 
						offScreenImage.dispose();
					offScreenImage = new Image(Display.getDefault(), imgWidth, 
							imgHeight);
					
					if (offScreenImageGC != null && !offScreenImageGC.isDisposed()) 
						offScreenImageGC.dispose();
					offScreenImageGC = new GC(offScreenImage);
				}			
			}
		
		//avoid negative number
		leftCrop = leftCrop > imgWidth ? 0 : leftCrop;
		topCrop = topCrop > imgWidth ? 0 : topCrop;
		int cropedWidth = (imgWidth-leftCrop-rightCrop) > 0 ? 
				(imgWidth-leftCrop-rightCrop) : imgWidth;
		int cropedHeight = (imgHeight-topCrop-bottomCrop) > 0 ?
				(imgHeight-topCrop-bottomCrop) : imgHeight;
			
		if(animated) {   //draw refreshing image
			ImageData imageData = imageDataArray[showIndex];
			Image refresh_image = new Image(Display.getDefault(), imageData);
			switch (imageData.disposalMethod) {
			case SWT.DM_FILL_BACKGROUND:
				/* Fill with the background color before drawing. */
				Color bgColor = null;
//				if (useGIFBackground  && loader.backgroundPixel != -1) {
//					bgColor = new Color(Display.getDefault(), imageData.palette.getRGB(loader.backgroundPixel));
//				}
				offScreenImageGC.setBackground(bgColor != null ? bgColor : getBackgroundColor());
				offScreenImageGC.fillRectangle(
						imageData.x, imageData.y, imageData.width, imageData.height);
				if (bgColor != null) bgColor.dispose();
				break;
			case SWT.DM_FILL_PREVIOUS:
				/* Restore the previous image before drawing. */
				Image startImage = new Image(Display.getDefault(), imageDataArray[0]);
				offScreenImageGC.drawImage(
					startImage,
					0,
					0,
					imageData.width,
					imageData.height,
					imageData.x,
					imageData.y,
					imageData.width,
					imageData.height);
				startImage.dispose();
				break;
			}
			
			offScreenImageGC.drawImage(refresh_image,  
					0,
					0,
					imageData.width,
					imageData.height,
					imageData.x,
					imageData.y,
					imageData.width,
					imageData.height);
		
			gfx.drawImage(offScreenImage, leftCrop,topCrop,
					cropedWidth,cropedHeight,
					bound.x,bound.y,
					cropedWidth,cropedHeight);
			refresh_image.dispose();			
		} else { // draw static image
			if(animated && animationDisabled && offScreenImage != null && showIndex!=0){
				gfx.drawImage(offScreenImage, leftCrop,topCrop,
					cropedWidth,cropedHeight,
					bound.x,bound.y,
					cropedWidth,cropedHeight);
			}else
				gfx.drawImage(staticImage,  
						leftCrop,topCrop,
						cropedWidth,cropedHeight,
						bound.x,bound.y,
						cropedWidth,cropedHeight);
		}
	}
	
	/**
	 * Resizes the image.
	 */
	public void resizeImage() {
		if (staticImage!=null && !staticImage.isDisposed()) {
			staticImage.dispose();
		}
		staticImage=null;	
		if(refreshing && animated){
			stopAnimation();
			startAnimation();
		}
		repaint();
	}
	
	public void setAnimationDisabled(final boolean stop){
		if(animationDisabled == stop)
			return;
		animationDisabled = stop;
		if(stop){
			stopAnimation();
		}else if(animated){
			startAnimation();
		}
	}
	
	/**
	 * Automatically make the widget bounds be adjusted to the size of the static image 
	 * @param autoSize
	 */
//	public void setAutoSize(final boolean autoSize){
//		if(!stretch && autoSize)
//				resizeImage();			
//	}
	
	/**
	 * Sets the amount of pixels, which are cropped from the bottom. 
	 * @param newval The amount of pixels
	 */
	public void setBottomCrop(final int newval) {
		if(bottomCrop == newval)
			return;
		bottomCrop=newval;
		resizeImage();
	}
	
	/**
	 * Sets the path to the image.
	 * @param newval The path to the image
	 */
	public void setFilePath(final IPath newval) {
		if(this.filePath != null && this.filePath.equals(newval))
			return;
		if(animated){
			stopAnimation();		
			animationIndex = 0;
		}
		loadingError = false;
		filePath=newval;
		if (staticImage!=null  && !staticImage.isDisposed()) {
			staticImage.dispose();
		}
		staticImage=null;
		
		loadImageFromFile();
		if(animated){
			startAnimation();
		}
	}
	/**
	 * Sets the amount of pixels, which are cropped from the left. 
	 * @param newval The amount of pixels
	 */
	public void setLeftCrop(final int newval) {
		if(leftCrop == newval)
			return;
		leftCrop=newval;
		resizeImage();
	}
	
	/**
	 * Sets the amount of pixels, which are cropped from the right. 
	 * @param newval The amount of pixels
	 */
	public void setRightCrop(final int newval) {
		if(rightCrop == newval)
			return;
		rightCrop=newval;
		resizeImage();
	}
	


	/**
	 * @param showIndex the showIndex to set
	 */
	protected void setShowIndex(int showIndex) {
		if(showIndex >= imageDataArray.length || this.showIndex == showIndex)
			return;
		this.showIndex = showIndex;
		repaint();
	}
	
	
	/**
	 * Sets the stretch state for the image.
	 * @param newval The new state (true, if it should be stretched, false otherwise)
	 */
	public void setStretch(final boolean newval) {
		if(stretch == newval)
			return;
		stretch=newval;
		if (staticImage!=null  && !staticImage.isDisposed()) {
			staticImage.dispose();
		}
		staticImage=null;
		if(refreshing && animated){
			stopAnimation();
			startAnimation();
		}
		repaint();
	}
	
	@Override
	public void setBounds(Rectangle rect) {
		super.setBounds(rect);
		if(stretch)
			if (staticImage!=null  && !staticImage.isDisposed()) {
				staticImage.dispose();
			}
			staticImage=null; 
	}
	
	/**
	 * Sets the amount of pixels, which are cropped from the top. 
	 * @param newval The amount of pixels
	 */
	public void setTopCrop(final int newval) {
		if(topCrop == newval)
			return;
		topCrop=newval;
		resizeImage();
	}



	
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if(visible)
			startAnimation();
		else {
			stopAnimation();						
		}		
	}
	
	/**
	 * start the animation if the image is an animated GIF image.
	 */
	public void startAnimation(){		
		if(animated && !refreshing && !animationDisabled) {
			repeatCount = loader.repeatCount;
			//animationIndex = 0;
			lastUpdateTime=0;
			interval_ms =0;
			refreshing = true;
			Runnable animationTask = new Runnable() {
				public void run() {
					Display.getDefault().asyncExec(new Runnable(){
						
						public void run() {				
				
							if(refreshing && (loader.repeatCount ==0 || repeatCount >0)) {
								long currentTime = System.currentTimeMillis();
								//use Math.abs() to ensure that the system time adjust won't cause problem
								if(Math.abs(currentTime - lastUpdateTime) >= interval_ms) {
									setShowIndex(animationIndex);
									lastUpdateTime = currentTime;
									int ms = originalImageDataArray[animationIndex].delayTime * 10;
									animationIndex = (animationIndex + 1) % originalImageDataArray.length;
									if (ms < 20) ms += 30;
									if (ms < 30) ms += 10;
									interval_ms = ms;								
									/* If we have just drawn the last image, decrement the repeat count and start again. */
									if(loader.repeatCount > 0 &&
											animationIndex == originalImageDataArray.length -1) 
										repeatCount--;									}															
							}else if(loader.repeatCount > 0 && repeatCount <=0){ // stop thread when animation finished
								if(scheduledFuture !=null){
									scheduledFuture.cancel(true);
									scheduledFuture = null;
								}	
							}			
						}
					});
			}
		};
			
			if(scheduledFuture !=null){
				scheduledFuture.cancel(true);
				scheduledFuture = null;
			}				
			scheduledFuture = ExecutionService.getInstance().
								getScheduledExecutorService().scheduleAtFixedRate(
								animationTask, 100, 10, TimeUnit.MILLISECONDS);
		}		
	}
	
	/**
	 * stop the animation if the image is an animated GIF image.
	 */
	public void stopAnimation(){		
		
		if (scheduledFuture != null) {
			scheduledFuture.cancel(true);
			scheduledFuture = null;
		}		
		refreshing = false;
	}
	
	/**
	 * We want to have local coordinates here.
	 * @return True if here should used local coordinates
	 */
	protected boolean useLocalCoordinates() {
		return true;
	}

	public BeanInfo getBeanInfo() throws IntrospectionException {
		return new DefaultWidgetIntrospector().getBeanInfo(this.getClass());
	}
	
	
}
