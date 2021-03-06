/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.unibi.agbi.gnius.core.model.entity.graph.impl;

import edu.unibi.agbi.gnius.core.model.entity.data.IDataArc;
import edu.unibi.agbi.gnius.core.model.entity.graph.IGraphArc;
import edu.unibi.agbi.gnius.core.model.entity.graph.IGraphNode;
import edu.unibi.agbi.gravisfx.entity.parent.connection.GravisEdge;

/**
 *
 * @author PR
 */
public class GraphEdge extends GravisEdge implements IGraphArc
{
    private final IDataArc dataArc;
    
    public GraphEdge(IGraphNode source, IDataArc dataArc) {
        super(source);
        this.dataArc = dataArc;
    }
    
    public GraphEdge(IGraphNode source, IGraphNode target, IDataArc dataArc) {
        super(source, target);
        this.dataArc = dataArc;
    }
    
    @Override
    public IDataArc getDataElement() {
        return dataArc;
    }

    @Override
    public IGraphNode getSource() {
        return (IGraphNode) super.getSource();
    }

    @Override
    public IGraphNode getTarget() {
        return (IGraphNode) super.getTarget();
    }
}
