/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.unibi.agbi.gravisfx.entity.parent.node;

import edu.unibi.agbi.gravisfx.GravisProperties;
import edu.unibi.agbi.gravisfx.entity.IGravisConnection;
import edu.unibi.agbi.gravisfx.entity.IGravisNode;
import edu.unibi.agbi.gravisfx.entity.child.GravisChildLabel;
import edu.unibi.agbi.gravisfx.entity.util.GravisShapeHandle;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import edu.unibi.agbi.gravisfx.entity.IGravisParent;
import edu.unibi.agbi.gravisfx.entity.child.GravisChildRectangle;

/**
 *
 * @author PR
 */
public class GravisRectangle extends Rectangle implements IGravisNode, IGravisParent
{
    private final List<GravisShapeHandle> shapeHandles = new ArrayList();
    private final List<Shape> shapes = new ArrayList();

    private final List<IGravisNode> children = new ArrayList();
    private final List<IGravisNode> parents = new ArrayList();
    private final List<IGravisConnection> edges = new ArrayList();

    private final GravisChildLabel label;
    
    private final GravisChildRectangle rectangle;

    private int exportId = 0;

    public GravisRectangle() {

        super();

        setWidth(GravisProperties.RECTANGLE_WIDTH);
        setHeight(GravisProperties.RECTANGLE_HEIGHT);
        setArcWidth(GravisProperties.RECTANGLE_ARC_WIDTH);
        setArcHeight(GravisProperties.RECTANGLE_ARC_HEIGHT);

        label = new GravisChildLabel(this);
        label.xProperty().bind(translateXProperty().add(getOffsetX() + GravisProperties.LABEL_OFFSET_X));
        label.yProperty().bind(translateYProperty().add(getOffsetY() + GravisProperties.LABEL_OFFSET_Y));
        
        rectangle = new GravisChildRectangle(this);
        rectangle.setWidth(GravisProperties.RECTANGLE_WIDTH - GravisProperties.BASE_INNER_DISTANCE * 2);
        rectangle.setHeight(GravisProperties.RECTANGLE_HEIGHT - GravisProperties.BASE_INNER_DISTANCE * 2);
        rectangle.translateXProperty().bind(translateXProperty().add(GravisProperties.BASE_INNER_DISTANCE));
        rectangle.translateYProperty().bind(translateYProperty().add(GravisProperties.BASE_INNER_DISTANCE));
        
        shapes.add(this);
        shapes.add(rectangle);

        shapeHandles.add(new GravisShapeHandle(this));
        shapeHandles.addAll(rectangle.getElementHandles());
    }

    @Override
    public Object getBean() {
        return GravisRectangle.this;
    }

    @Override
    public Shape getShape() {
        return this;
    }

    @Override
    public List<Shape> getShapes() {
        return shapes;
    }

    @Override
    public final List<GravisShapeHandle> getElementHandles() {
        return shapeHandles;
    }

    @Override
    public final double getOffsetX() {
        return getWidth() / 2;
    }

    @Override
    public final double getOffsetY() {
        return getHeight() / 2;
    }

    @Override
    public final List<IGravisNode> getParents() {
        return parents;
    }

    @Override
    public final List<IGravisNode> getChildren() {
        return children;
    }

    @Override
    public final List<IGravisConnection> getConnections() {
        return edges;
    }

    @Override
    public final GravisChildLabel getLabel() {
        return label;
    }

    @Override
    public void setInnerCircleVisible(boolean value) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setInnerRectangleVisible(boolean value) {
        this.rectangle.setVisible(value);
    }
    
    @Override
    public int getExportId() {
        return exportId;
    }
    
    @Override
    public void setExportId(int id) {
        this.exportId = id;
    }

    @Override
    public List<GravisShapeHandle> getParentElementHandles() {
        List<GravisShapeHandle> handles = new ArrayList();
        handles.add(shapeHandles.get(0));
        return handles;
    }

    @Override
    public List<GravisShapeHandle> getChildElementHandles() {
        return rectangle.getElementHandles();
    }
}
