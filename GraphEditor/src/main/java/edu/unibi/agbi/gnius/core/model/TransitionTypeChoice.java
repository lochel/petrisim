/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.unibi.agbi.gnius.core.model;

import edu.unibi.agbi.gnius.core.model.entity.DataTransition;

/**
 *
 * @author PR
 */
public class TransitionTypeChoice
{
    private final DataTransition.Type type;
    private final String typeName;

    public TransitionTypeChoice(DataTransition.Type type , String typeName) {
        this.type = type;
        this.typeName = typeName;
    }

    public DataTransition.Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return typeName;
    }
}
