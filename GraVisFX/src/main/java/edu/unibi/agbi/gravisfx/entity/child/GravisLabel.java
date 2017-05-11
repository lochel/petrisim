/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.unibi.agbi.gravisfx.entity.child;

import edu.unibi.agbi.gravisfx.entity.IGravisNode;
import edu.unibi.agbi.gravisfx.entity.util.ElementHandle;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import edu.unibi.agbi.gravisfx.entity.IGravisChildElement;

/**
 *
 * @author PR
 */
public class GravisLabel extends Text implements IGravisChildElement
{
    private final List<ElementHandle> elementHandles;
    private final IGravisNode parentElement;

    public GravisLabel(IGravisNode parentElement) {

        super();

        this.parentElement = parentElement;

        elementHandles = new ArrayList();
        elementHandles.add(new ElementHandle(this));
    }

    @Override
    public IGravisNode getParentElement() {
        return parentElement;
    }

    @Override
    public Object getBean() {
        return this;
    }

    @Override
    public List<ElementHandle> getElementHandles() {
        return elementHandles;
    }

    @Override
    public Shape getShape() {
        return this;
    }

    @Override
    public List<Shape> getShapes() {
        List<Shape> shapes = new ArrayList();
        shapes.add(this);
        return shapes;
    }
}
