/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.unibi.agbi.petrinet.util;

import edu.unibi.agbi.petrinet.entity.IPN_Arc;
import edu.unibi.agbi.petrinet.entity.IPN_Element;
import edu.unibi.agbi.petrinet.entity.IPN_Node;
import edu.unibi.agbi.petrinet.entity.abstr.Place;
import edu.unibi.agbi.petrinet.entity.abstr.Transition;
import edu.unibi.agbi.petrinet.model.Colour;
import edu.unibi.agbi.petrinet.model.Function;
import edu.unibi.agbi.petrinet.model.PetriNet;
import edu.unibi.agbi.petrinet.model.Token;
import edu.unibi.agbi.petrinet.model.Weight;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

import java.util.Collection;

/**
 *
 * @author PR
 */
public class OpenModelicaExport
{
    private static final String ENDL = System.getProperty("line.separator");
    private static final String INDENT = "  ";
    
    private static final String OM_SIM_LIB = "PNlib";
    
    private static final String identPlaceContinuous = "PNlib.PC";
    private static final String identPlaceDiscrete = "PNlib.PD";
    
    private static final String identTransitionContinuous = "PNlib.TC";
    private static final String identTransitionDiscrete = "PNlib.TD";
    private static final String identTransitionStochastic = "PNlib.TS";
    
    private static final String identPlaceContinuousColored = "PNlib.Examples.Models.ColoredPlaces.CPC";
    private static final String identColoredTransitionContinuous = "PNlib.Examples.Models.ColoredPlaces.CTC";
    
    public static File exportMO(PetriNet petriNet , File file) throws IOException {

        Collection<IPN_Arc> arcs = petriNet.getArcs();
        Collection<Colour> colors = petriNet.getColours();
        Collection<IPN_Node> places = petriNet.getPlaces();
        Collection<IPN_Node> transitions = petriNet.getTransitions();

        boolean isColoredPn = colors.size() != 1;

        PrintWriter writer = new PrintWriter(file);

        /**
         * Model name.
         */
        writer.append("model '" + petriNet.getName() + "'");
        writer.println();
        
        /**
         * Functions.
         */
        if (isColoredPn) {
            writer.append("function g1" + ENDL + "    input Real[2] inColors;" + ENDL + "    output Real[2] outWeights;" + ENDL + "  algorithm" + ENDL
                          + "    if sum(inColors) < 1e-12 then" + ENDL + "      outWeights := fill(1, 2);" + ENDL + "    else" + ENDL
                          + "      outWeights[1] := inColors[1] / sum(inColors);" + ENDL + "      outWeights[2] := inColors[2] / sum(inColors);" + ENDL + "    end if;" + ENDL
                          + "  end g1;" + ENDL + "  function g2" + ENDL + "    input Real[2] inColors1;" + ENDL + "    input Real[2] inColors2;" + ENDL + "    output Real[2] outWeights;" + ENDL
                          + "  algorithm" + ENDL + "    if sum(inColors1) < 1e-12 then" + ENDL + "      outWeights := fill(0.5, 2);" + ENDL + "    else" + ENDL
                          + "      outWeights[1] := inColors1[1] / sum(inColors1) / 2;" + ENDL + "      outWeights[2] := inColors1[2] / sum(inColors1) / 2;" + ENDL
                          + "    end if;" + ENDL + "" + ENDL + "    if sum(inColors2) < 1e-12 then" + ENDL + "      outWeights[1] := outWeights[1] + 0.5;" + ENDL
                          + "      outWeights[2] := outWeights[2] + 0.5;" + ENDL + "    else" + ENDL
                          + "      outWeights[1] := outWeights[1] + inColors2[1] / sum(inColors2) / 2;" + ENDL
                          + "      outWeights[2] := outWeights[2] + inColors2[2] / sum(inColors2) / 2;" + ENDL + "    end if;" + ENDL + "  end g2;" + ENDL);
            writer.println();
        }

        /**
         * Settings.
         */
        writer.append("  inner PNlib.Settings");
        writer.append(" settings(showTokenFlow = true)");
        //writer.append(" annotation(Placement(visible=true, transformation(origin={0.0,0.0}, extent={{0,0}, {0,0}}, rotation=0)))");
        writer.append(";");
        writer.println();

        /**
         * Places.
         */
        boolean isFirst, isInnerFirst;
        String tmp1, tmp2, tmp3, tokenType, functionType;
        Function function;
        Token token;
        Weight weight;
        IPN_Node arcSource, arcTarget;
        Place place;
        Transition transition;
        int index;

        for (IPN_Node nPlace : places) {

            place = (Place)nPlace;

            tmp1 = "";
            tmp2 = "";
            tmp3 = "";

            writer.append("  ");
            if (place.getPlaceType() == Place.Type.CONTINUOUS) {
                if (isColoredPn) {
                    writer.append(identPlaceContinuousColored);
                } else {
                    writer.append(identPlaceContinuous);
                }
                tokenType = "Marks";
            } else {
                if (isColoredPn) {
                    writer.append(identPlaceDiscrete); // TODO ?
                } else {
                    writer.append(identPlaceDiscrete);
                }
                tokenType = "Tokens";
            }
            writer.append(" '" + place.getId() + "'");
            writer.append("(nIn=" + place.getArcsIn().size());
            writer.append(",nOut=" + place.getArcsOut().size());

            isFirst = true;

            /**
             * Token
             */
            for (Colour color : colors) {

                token = place.getToken(color);

                if (isColoredPn) {

                    if (isFirst) {

                        isFirst = false;

                    } else {

                        tmp1 += ",";
                        tmp2 += ",";
                        tmp3 += ",";
                    }

                    if (token != null) {

                        if (place.getPlaceType() == Place.Type.CONTINUOUS) {
                            tmp1 += token.getValueStart();
                            tmp2 += token.getValueMin();
                            tmp3 += token.getValueMax();
                        } else {
                            tmp1 += (int)token.getValueStart();
                            tmp2 += (int)token.getValueMin();
                            tmp3 += (int)token.getValueMax();
                        }

                    } else {

                        tmp1 += "0";
                        tmp2 += "0";
                        tmp3 += "0";

                    }

                } else {

                    if (place.getPlaceType() == Place.Type.CONTINUOUS) {
                        tmp1 += token.getValueStart();
                        tmp2 += token.getValueMin();
                        tmp3 += token.getValueMax();
                    } else {
                        tmp1 += (int)token.getValueStart();
                        tmp2 += (int)token.getValueMin();
                        tmp3 += (int)token.getValueMax();
                    }
                }

            }
            
            if (isColoredPn) {
                tmp1 = "{" + tmp1 + "}";
                tmp2 = "{" + tmp2 + "}";
                tmp3 = "{" + tmp3 + "}";
            }

            writer.append(",start" + tokenType + "=" + tmp1);
            writer.append(",min" + tokenType + "=" + tmp2);
            writer.append(",max" + tokenType + "=" + tmp3);
                
            //writer.append(",t(final unit=\"custom unit\")");
            writer.append(")");
            //writer.append(" annotation(Placement(visible=true, transformation(origin={0.0,0.0}, extent={{0,0}, {0,0}}, rotation=0)))");
            writer.append(";");
            writer.println();
        }

        /**
         * Transitions.
         */
        for (IPN_Node nTransition : transitions) {

            transition = (Transition)nTransition;

            function = transition.getFunction();

            writer.append("  ");
            if (isColoredPn) {
                writer.append(identColoredTransitionContinuous);
                functionType = "maximumSpeed";
            } else {
                if (transition.getTransitionType() == Transition.Type.CONTINUOUS) {
                    writer.append(identTransitionContinuous);
                    functionType = "maximumSpeed";
                } else if (transition.getTransitionType() == Transition.Type.DISCRETE) {
                    writer.append(identTransitionDiscrete);
                    functionType = "delay";
                } else {
                    writer.append(identTransitionStochastic);
                    functionType = "h"; // TODO
                }
            }
            writer.append(" '" + transition.getId() + "'");
            writer.append("(nIn=" + transition.getArcsIn().size());
            writer.append(",nOut=" + transition.getArcsOut().size());
            writer.append("," + functionType);
            if (!function.getUnit().isEmpty()) {
                writer.append("(final unit=\"" + function.getUnit() + "\")");
            }
            writer.append("=" + function.toString());

            /**
             * Weights, incoming
             */
            tmp1 = "";
            isFirst = true;

            for (IPN_Arc arc : transition.getArcsIn()) {

                if (isFirst) {

                    isFirst = false;

                } else {

                    tmp1 += ",";

                }

                tmp3 = "";
                isInnerFirst = true;

                for (Colour color : colors) {

                    weight = arc.getWeight(color);

                    if (isInnerFirst) {

                        isInnerFirst = false;

                    } else {

                        tmp3 += ",";

                    }

                    if (isColoredPn) {

                        if (weight != null) {

                            tmp3 += weight.getValue();

                        } else {

                            tmp3 += "0";

                        }

                    } else {

                        tmp1 += weight.getValue();

                    }

                }

                if (isColoredPn) {

                    tmp1 += "{" + tmp3 + "}/*" + arc.getSource().getId() + "*/";

                }

            }

            /**
             * Weights, outgoing
             */
            tmp2 = "";
            isFirst = true;

            for (IPN_Arc arc : transition.getArcsOut()) {

                if (isFirst) {

                    isFirst = false;

                } else {

                    tmp2 += ",";

                }

                tmp3 = "";
                isInnerFirst = true;

                for (Colour color : colors) {

                    weight = arc.getWeight(color);

                    if (isInnerFirst) {

                        isInnerFirst = false;

                    } else {

                        tmp3 += ",";

                    }

                    if (isColoredPn) {

                        if (weight != null) {

                            tmp3 += weight.getValue();

                        } else {

                            tmp3 += "0";

                        }

                    } else {

                        tmp2 += weight.getValue();

                    }

                }

                if (isColoredPn) {

                    tmp2 += "{" + tmp3 + "}/*" + arc.getSource().getId() + "*/";

                }

            }

            writer.append(",arcWeightIn={" + tmp1 + "}");
            writer.append(",arcWeightOut={" + tmp2 + "}");
            writer.append(")");
            //writer.append(" annotation(Placement(visible=true, transformation(origin={0.0,0.0}, extent={{0,0}, {0,0}}, rotation=0)))");
            writer.append(";");
            writer.println();
        }

        /**
         * Connections.
         */
        writer.append("equation");
        writer.println();

        for (IPN_Arc a : arcs) {

            writer.append("  ");
            writer.append("connect(");

            arcSource = a.getSource();
            arcTarget = a.getTarget();

            index = 1;
            for (IPN_Arc arc : arcSource.getArcsOut()) {
                if (arcTarget.equals(arc.getTarget())) {
                    break;
                }
                index++;
            }
            if (arcSource instanceof Place) {
                writer.append("'" + arcSource.getId() + "'.outTransition[" + index + "],");
            } else {
                writer.append("'" + arcSource.getId() + "'.outPlaces[" + index + "],");
            }

            index = 1;
            for (IPN_Arc arc : arcTarget.getArcsIn()) {
                if (arcSource.equals(arc.getSource())) {
                    break;
                }
                index++;
            }
            if (arcTarget instanceof Place) {
                writer.append("'" + arcTarget.getId() + "'.inTransition[" + index + "]");
            } else {
                writer.append("'" + arcTarget.getId() + "'.inPlaces[" + index + "]");
            }

            writer.append(")");
            //writer.append(" annotation(Line(color={0, 0, 0}, points={{0.0,0.0}, {0.0,0.0}}))");
            writer.append(";");
            writer.println();
        }
        writer.append("  annotation(Icon(coordinateSystem(extent={{0.0,0.0},{0.0,0.0}})), Diagram(coordinateSystem(extent={{0.0,0.0},{0.0,0.0}})));");
        writer.println();

        writer.append("end '" + petriNet.getName() + "'");
        writer.append(";");
        writer.println();
        
        writer.close();
        
        return file;
    }
    
    /**
     * 
     * @param petriNet
     * @param fileMOS
     * @param fileMO
     * @param workDirectory
     * @return
     * @throws IOException 
     */
    public static File exportMOS(PetriNet petriNet, File fileMOS, File fileMO, File workDirectory) throws IOException {
        
        String filter = "variableFilter=\"";
        String tmp;
        int index;
        
        // Removes previous filter names
        for (IPN_Element ele : petriNet.getArcs()) {
            ele.setFilterNames(new ArrayList());
        }
        for (IPN_Element ele : petriNet.getPlaces()) {
            ele.setFilterNames(new ArrayList());
        }
        for (IPN_Element ele : petriNet.getTransitions()) {
            ele.setFilterNames(new ArrayList());
        }
        
        for (IPN_Node place : petriNet.getPlaces()) {
            
            tmp = "'" + place.getId() + "'.t";
            filter += tmp + "|";
            place.addFilterName(tmp);
            
            index = 1;
            for (IPN_Arc arc : place.getArcsOut()) {
                
                tmp = "'" + arc.getSource().getId() + "'.tokenFlow.outflow[" + index + "]";
                filter += tmp + "|";
                arc.addFilterName(tmp);

                tmp = "der(" + tmp + ")";
                filter += tmp + "|";
                arc.addFilterName(tmp);
                
                index++;
            }
            
            index = 1;
            for (IPN_Arc arc : place.getArcsIn()) {
                
                tmp = "'" + arc.getTarget().getId() + "'.tokenFlow.inflow[" + index + "]";
                filter += tmp + "|";
                arc.addFilterName(tmp);
                
                tmp = "der(" + tmp + ")";
                filter += tmp + "|";
                arc.addFilterName(tmp);
                
                index++;
            }
        }

        for (IPN_Node transition : petriNet.getTransitions()) {
            
            tmp = "'" + transition.getId() + "'.fire|";
            filter += tmp + "|";
            transition.addFilterName(tmp);
            
            tmp = "'" + transition.getId() + "'.actualSpeed|";
            filter += tmp + "|";
            transition.addFilterName(tmp);
        }
        
        filter = filter.substring(0 , filter.length() - 1);
        filter = filter.replace(".", "\\\\."); // might cause problems when custom names are used
        filter = filter.replace("[", "\\\\[");
        filter = filter.replace("]", "\\\\]");
        filter = filter.replace("(", "\\\\(");
        filter = filter.replace(")", "\\\\)");
        filter += "\"";
        
        FileWriter fstream = new FileWriter(fileMOS);
        BufferedWriter out = new BufferedWriter(fstream);
        
        out.write("cd(\"" + workDirectory.getPath().replace('\\' , '/') + "/\"); ");
        out.write("getErrorString();\r\n");
        out.write("loadModel(" + OM_SIM_LIB + ");");
        out.write("getErrorString();\r\n");
        out.write("loadFile(\"" + fileMO.getPath().replace('\\' , '/')+ "\"); ");
        out.write("getErrorString();\r\n");
        out.write("setCommandLineOptions(\"--preOptModules+=unitChecking\");");
        out.write("getErrorString();\r\n");
        out.write("buildModel('" + petriNet.getName() + "', " + filter + "); ");
        out.write("getErrorString();\r\n");

        out.close();
        
        return fileMOS;
    }
    
    /**
     * Creates a temporary file. 
     * File can only be read, written or executed by the creator. 
     * Automatically deletes the file on application exit.
     * @param prefix file prefix
     * @param suffix file suffix
     * @return the created file
     * @throws java.io.IOException 
     */
    public static File CreateTempFile(String prefix, String suffix) throws IOException {
        
        /*if (tempDirectoryPath == null) {
            tempDirectoryPath = Files.createTempDirectory(null);
            System.getSecurityManager().checkDelete(tempDirectoryPath.toFile().toString());
            System.out.println("Temp path: " + tempDirectoryPath);
            System.out.println("Temp path: " + tempDirectoryPath.toString());
            System.out.println("Temp path: " + tempDirectoryPath.toFile().toString());
        }*/
        
        File tempFile = null;
        InputStream input = null;
        OutputStream output = null;
        
        try {
            // Create temporary file
            //tempFile = File.createTempFile(tempDirectoryPath.getFileName() + File.separator + fileName , null);
            //System.out.println("Temp file: " + tempFile);
            tempFile = File.createTempFile(prefix + "_" , suffix);
            tempFile.setReadable(false , false);
            tempFile.setReadable(true , true);
            tempFile.setWritable(true , true);
            tempFile.setExecutable(true , true);
            tempFile.deleteOnExit();
            
            // Read resource and write to temp file
            output = new FileOutputStream(tempFile);
            
        } catch (IOException ex) {
            throw(ex);
        } finally {
            close(input);
            close(output);
        }
        return tempFile;
    }

    /**
     * Closes a given Closable.
     * @param stream
     */
    public static void close(final Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (final IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}