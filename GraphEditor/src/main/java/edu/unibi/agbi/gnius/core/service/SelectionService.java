/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.unibi.agbi.gnius.core.service;

import edu.unibi.agbi.gnius.business.controller.MainController;
import edu.unibi.agbi.gnius.core.model.dao.SelectionDao;
import edu.unibi.agbi.gnius.core.model.entity.data.IDataElement;
import edu.unibi.agbi.gnius.core.model.entity.graph.IGraphArc;
import edu.unibi.agbi.gnius.core.model.entity.graph.IGraphNode;
import edu.unibi.agbi.gnius.core.model.entity.graph.IGraphElement;
import edu.unibi.agbi.gravisfx.entity.IGravisElement;
import edu.unibi.agbi.gravisfx.entity.util.GravisShapeHandle;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import edu.unibi.agbi.gravisfx.entity.IGravisChild;

/**
 *
 * @author PR
 */
@Service
public class SelectionService
{
    @Autowired private MainController mainController;
    
    private final SelectionDao selectionDao;
    
    private List<IGraphNode> selectedNodesCopy;
    private List<GravisShapeHandle> highlightedElementsHandles;
    
    @Autowired
    public SelectionService(SelectionDao selectionDao) {
        this.selectionDao = selectionDao;
    }
    
    /**
     * Creates a copy of the selected elements.
     */
    public void copy() {
        
        selectedNodesCopy = new ArrayList();
        
        for (IGraphElement element : getSelectedElements()) {
            if (element instanceof IGraphNode) {
                selectedNodesCopy.add((IGraphNode) element);
            }
        }
    }
    
    /**
     * Enables hovering for the given element. Also enables hovering for
     * all related subelements.
     * @param element 
     */
    public void hover(IGravisElement element) {

        if (highlightedElementsHandles != null) {
            
            for (GravisShapeHandle handle : highlightedElementsHandles) {
                handle.setHovered(false);
            }
            highlightedElementsHandles = null;
        }
        
        if (element != null) {

            if (element instanceof IGraphNode || element instanceof IGraphArc) {
                highlightedElementsHandles = element.getElementHandles();
            } else { // not a node, so must be subelement
                highlightedElementsHandles = ((IGravisChild) element).getParentShape().getElementHandles();
            }

            for (GravisShapeHandle handle : highlightedElementsHandles) {
                if (!handle.isSelected()) {
                    handle.setHovered(true);
                }
            }
        }
    }
    
    /**
     * Highlight the given element.
     * @param element 
     */
    public void highlight(IGraphElement element) {
        element.getElementHandles().forEach(ele -> {
            ele.setHighlighted(true);
        });
        selectionDao.addHighlight(element);
    }
    
    /**
     * Highlight related objects. Only if those are not already selected.
     * @param element 
     */
    public void highlightRelated(IGraphElement element) {
        IDataElement dataElement = element.getDataElement();
        if (dataElement == null) {
            return;
        }
        for (IGraphElement shape : dataElement.getShapes()) {
            if (!shape.getElementHandles().get(0).isSelected()) {
                if (!shape.getElementHandles().get(0).isHighlighted()) {
                    highlight(shape);
                }
            }
        }
    }
    
    /**
     * Remove element highlight.
     * @param element
     */
    public void unhighlight(IGraphElement element) {
        element.getElementHandles().forEach(ele -> {
            ele.setHighlighted(false);
        });
        selectionDao.removeHighlight(element);
    }
    
    /**
     * Removes element highlight from all related. Only if no other related
     * element is selected anymore.
     * @param element 
     */
    public void unhighlightRelated(IGraphElement element) {
        
        IDataElement dataElement = element.getDataElement();
        
        boolean isStillSelected = false;
        for (IGraphElement relatedShape : dataElement.getShapes()) {
            if (relatedShape.getElementHandles().get(0).isSelected()) {
                isStillSelected = true;
                break;
            }
        }
        if (!isStillSelected) {
            for (IGraphElement relatedShape : dataElement.getShapes()) {
                unhighlight(relatedShape);
            }
        }
    }
    
    /**
     * Selects the given element.
     * @param element
     */
    public void select(IGraphElement element) {
        
        hover(null);

        if (element.getElementHandles().get(0).isHighlighted()) {
            selectionDao.removeHighlight(element);
            element.getElementHandles().forEach(ele -> {
                ele.setHighlighted(false);
            });
        }
        if (!element.getElementHandles().get(0).isSelected()) {
            selectionDao.addSelection(element);
            element.getElementHandles().forEach(ele -> {
                ele.setSelected(true);
            });
        }
        element.getElementHandles().forEach(ele -> {
            ele.putOnTop();
        });
    }
    
    /**
     * Select element and all related.
     * @param element 
     */
    public void selectAll(IGraphElement element) {
        IDataElement dataElement = element.getDataElement();
        for (IGraphElement relatedElement : dataElement.getShapes()) {
            select(relatedElement);
        }
    }
    
    /**
     * Selects the given nodes and all their related.
     * @param nodes
     */
    public void selectAll(List<IGraphNode> nodes) {
        for (IGraphElement element : nodes) {
            selectAll(element);
        }
    }
    
    /**
     * Remove selection.
     * @param arc
     * @return 
     */
    public boolean unselect(IGraphElement arc) {
        arc.getElementHandles().forEach(ele -> {
            ele.setSelected(false);
        });
        return selectionDao.removeSelection(arc);
    }
    
    /**
     * Clear selected and highlighted elements.
     */
    public void unselectAll() {
        for (IGraphElement selected : selectionDao.getSelectedElements()) {
            selected.getElementHandles().forEach(ele -> {
                ele.setSelected(false);
            });
        }
        for (IGraphElement hightlighted : selectionDao.getHightlightedElements()) {
            hightlighted.getElementHandles().forEach(ele -> {
                ele.setHighlighted(false);
            });
        }
        selectionDao.clear();
        mainController.HideElementBox();
    }
    
    /**
     * Get selected elements.
     * @return 
     */
    public List<IGraphElement> getSelectedElements() {
        return selectionDao.getSelectedElements();
    }
    
    /**
     * Get copied nodes.
     * @return 
     */
    public List<IGraphNode> getCopiedNodes() {
        return selectedNodesCopy;
    }
}
