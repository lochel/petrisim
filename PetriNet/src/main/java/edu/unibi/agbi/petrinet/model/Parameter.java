/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.unibi.agbi.petrinet.model;

import edu.unibi.agbi.petrinet.entity.IElement;
import java.util.HashSet;
import java.util.Set;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * @author PR
 */
public class Parameter implements Comparable
{
    private final String id;
    private final StringProperty value;
    private final StringProperty note;
    private final Type type;

    private final Set<IElement> referingNodes;
    private final IntegerProperty referingNodesCount;

    public Parameter(String id, String note, String value, Type type) {
        this.id = id;
        this.note = new SimpleStringProperty(note);
        this.value = new SimpleStringProperty(value);
        this.type = type;
        this.referingNodes = new HashSet();
        this.referingNodesCount = new SimpleIntegerProperty(0);
    }

    /**
     * Gets the identifier for this parameter.
     *
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the note for this parameter.
     *
     * @param note
     */
    public void setNote(String note) {
        this.note.set(note);
    }

    /**
     * Gets the note for this parameter.
     *
     * @return
     */
    public String getNote() {
        return note.get();
    }

    /**
     * Gets the note string property.
     *
     * @return
     */
    public StringProperty getNoteProperty() {
        return note;
    }

    /**
     * Sets the value for this parameter.
     *
     * @param value
     */
    public void setValue(String value) {
        this.value.set(value);
    }

    /**
     * Gets the value set to this parameter.
     *
     * @return
     */
    public String getValue() {
        return value.get();
    }

    /**
     * Gets the value string property.
     *
     * @return
     */
    public StringProperty getValueProperty() {
        return value;
    }

    /**
     * Gets the type indicating wether this parameter is used in a local or
     * global scope.
     *
     * @return
     */
    public Type getType() {
        return type;
    }

    /**
     * Gets the set of nodes currently using this parameter.
     *
     * @return
     */
    public Set<IElement> getReferingNodes() {
        return referingNodes;
    }

    public void addReferingNode(IElement elem) {
        referingNodes.add(elem);
        referingNodesCount.set(referingNodes.size());
    }

    public void removeReferingNode(IElement elem) {
        referingNodes.remove(elem);
        referingNodesCount.set(referingNodes.size());
    }

    /**
     * Gets the count of nodes refering to this parameter.
     *
     * @return
     */
    public int getReferingNodesCount() {
        return referingNodesCount.get();
    }

    /**
     * Gets the property for the count of nodes refering to this parameter.
     *
     * @return
     */
    public IntegerProperty getReferingNodesCountProperty() {
        return referingNodesCount;
    }

    /**
     * Compares the name strings of two parameters lexicographically. Uses the
     * compareTo method for strings to compare the parameter's ids.
     *
     * @param o
     * @return
     */
    @Override
    public int compareTo(Object o) {
        Parameter param = (Parameter) o;
        if (this.getType() != param.getType()) {
            if (this.getType() == Type.LOCAL) {
                return -1;
            } else {
                return 1;
            }
        }
        return this.getId().compareTo(param.getId());
    }

    public enum Type
    {
        LOCAL, GLOBAL;
    }
}
