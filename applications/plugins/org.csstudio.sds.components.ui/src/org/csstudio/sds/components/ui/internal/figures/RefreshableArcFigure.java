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
package org.csstudio.sds.components.ui.internal.figures;

import org.csstudio.sds.ui.figures.BorderAdapter;
import org.csstudio.sds.ui.figures.IBorderEquippedWidget;
import org.csstudio.sds.ui.figures.ICrossedFigure;
import org.csstudio.sds.util.AntialiasingUtil;
import org.eclipse.draw2d.AbstractBorder;
import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

/**
 * An arc figure.
 * 
 * @author jbercic
 * 
 */
public final class RefreshableArcFigure extends Shape implements ICrossedFigure {
    /**
     * start angle and length (in degrees) of the arc should it be drawn filled? (using fill_color)
     */
    private int start_angle = 0, angle = 90;
    private Color fill_color;

    /**
     * A border adapter, which covers all border handlings.
     */
    private IBorderEquippedWidget _borderAdapter;

    /**
     * Is the background transparent or not?
     */
    private boolean transparent = true;

    /**
     * Border properties.
     */
    private int border_width;
    
    private boolean filled;
    
    private CrossedPaintHelper _crossedPaintHelper;

    public RefreshableArcFigure() {
        _crossedPaintHelper = new CrossedPaintHelper();
    }
    
    /**
     * {@inheritDoc}
     */
    protected boolean useLocalCoordinates() {
        return true;
    }

    /**
     * Fills the arc.
     */
    protected void fillShape(Graphics gfx) {
        // Fix HR: The background paint over the fillArc.
        // (The fillShape paint first then the outlineShape).
        filled = true;
        if (transparent == false) {
            gfx.setBackgroundColor(getBackgroundColor());
            gfx.fillOval(getBounds().getCropped(new Insets(border_width/2)));
        }
        gfx.setBackgroundColor(fill_color);
        gfx.fillArc(getBounds()
                .getCropped(new Insets(lineWidth / 2 + lineWidth % 2 + border_width)), start_angle,
                angle);
    }

    /**
     * Draws the arc.
     */
    protected void outlineShape(Graphics gfx) {
        if (filled == false && transparent == false) {
            gfx.setBackgroundColor(getBackgroundColor());
            gfx.fillOval(getBounds().getCropped(new Insets(border_width/2)));
        }
        if (lineWidth > 0) {
            gfx.setLineWidth(lineWidth);
            gfx.setLineCap(SWT.CAP_FLAT);
            gfx.setLineJoin(SWT.JOIN_MITER);
            gfx.drawArc(getBounds().getCropped(
                    new Insets(lineWidth / 2 - lineWidth % 2 + border_width)), start_angle, angle);
        }
        filled = false;
    }

    /**
     * The main drawing routine.
     */
    public void paintFigure(Graphics gfx) {
        Rectangle figureBounds = getBounds().getCopy();
        AntialiasingUtil.getInstance().enableAntialiasing(gfx);
        super.paintFigure(gfx);
        _crossedPaintHelper.paintCross(gfx, figureBounds);
    }

    public void setTransparent(final boolean newval) {
        transparent = newval;
    }

    public boolean getTransparent() {
        return transparent;
    }

    public void setBorderWidth(final int newval) {
        border_width = newval;
    }

    public int getBorderWidth() {
        return border_width;
    }

    public void setStartAngle(final int newval) {
        start_angle = newval;
    }

    public int getStartAngle() {
        return start_angle;
    }

    public void setAngle(final int newval) {
        angle = newval;
    }

    public int getAngle() {
        return angle;
    }

    public void setFillColor(final Color color) {
        fill_color = color;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Object getAdapter(final Class adapter) {
        if (adapter == IBorderEquippedWidget.class) {
            if (_borderAdapter == null) {
                _borderAdapter = new BorderAdapter(this) {
                    
                    @Override
                    protected AbstractBorder createShapeBorder(int borderWidth, Color borderColor) {
                        if (borderWidth>0) {
                            ArcBorder border = new ArcBorder(borderWidth, borderColor);
                            return border;  
                        }
                        return null;
                    }
                };
            }
            return _borderAdapter;
        }
        return null;
    }

    @Override
    public void setBorder(Border border) {
        super.setBorder(border);
    }
    
    private final class ArcBorder extends AbstractBorder {

        private final Color _borderColor;
        private final int _borderWidth;

        public ArcBorder(int borderWidth,Color borderColor) {
            _borderColor = borderColor;
            _borderWidth = borderWidth;
        }

        public Insets getInsets(IFigure arg0) {
            return new Insets(_borderWidth);
        }

        public void paint(IFigure figure, Graphics gfx, Insets arg2) {
            gfx.setBackgroundColor(_borderColor);
            gfx.setForegroundColor(_borderColor);
            gfx.setLineWidth(_borderWidth);
            Rectangle bounds = figure.getBounds();
            Rectangle bounds2 = new Rectangle(bounds.x+_borderWidth/2,bounds.y+_borderWidth/2,bounds.width-_borderWidth,bounds.height-_borderWidth);
            gfx.drawOval(bounds2);

        }
    }

    @Override
    public void setCrossedOut(boolean newValue) {
        _crossedPaintHelper.setCrossed(newValue);        
    }
}
