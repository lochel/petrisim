/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.unibi.agbi.gnius.core.service;

import edu.unibi.agbi.gnius.core.model.dao.DataDao;
import edu.unibi.agbi.gnius.core.model.dao.SimulationDao;
import edu.unibi.agbi.gnius.core.model.entity.simulation.Simulation;
import edu.unibi.agbi.gnius.core.service.exception.SimulationServiceException;
import edu.unibi.agbi.gnius.core.simulation.OpenModelicaServer;
import edu.unibi.agbi.gnius.util.OS_Validator;
import edu.unibi.agbi.petrinet.util.OpenModelicaExport;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import static java.lang.Thread.sleep;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author PR
 */
@Service
public class SimulationService
{
    @Autowired private OpenModelicaServer server;
    
    private final SimulationDao simulationDao;
    private final DataDao dataDao;
    
    private final int serverPort = 11111;
    private Thread serverThread;
    
    private String simIntegrator;
    private double simStopTime;
    private int simIntervals;
    
    private final String envOpenModelicaHome = "OPENMODELICAHOME";
    private File localStorageDirectory = null;
    
    @Autowired 
    public SimulationService(SimulationDao simulationDao, DataDao dataDao) {
        this.simulationDao = simulationDao;
        this.dataDao = dataDao;
    }
    
    public Simulation InitSimulation(String[] variables) {
        
        Simulation simulation = new Simulation(variables);
        simulationDao.addSimulation(simulation);
        
        return simulation;
    }
    
    public void setSimulationIntegrator(String integrator) {
        simIntegrator = integrator;
    }
    
    public void setSimulationIntervals(int intervals) {
        simIntervals = intervals;
    }
    
    public void setSimulationStopTime(double stopTime) {
        simStopTime = stopTime;
    }
    
    public void simulate() throws SimulationServiceException {
        
        final Process buildProcess, simulationProcess;
        final BufferedReader simulationOutput;
        ProcessBuilder pb;
        
        File dirStorage, dirWork , fileMo, fileMos;
        String pathCompiler, pathSimulation, nameSimulation;
        
        dirStorage = getStorageDirectory();
        if (dirStorage == null) {
            throw new SimulationServiceException("Application's storage directory not accessible!");
        }
        
        dirWork = getWorkingDirectory();
        if (dirWork == null) {
            throw new SimulationServiceException("Application's working directory not accessible!");
        }
        
        pathCompiler = getCompilerPath();
        if (pathCompiler == null) {
            throw new SimulationServiceException("OpenModelica compiler not found!");
        }
        
        if (server.isRunning()) {
            throw new SimulationServiceException("Server is still running!");
        }

        /**
         * Exporting data for OpenModelica.
         */
        nameSimulation = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME).replace(":" , "").substring(0, 6) + "-" + dataDao.getName();
        try {
            System.out.println("Exporting data...");
            fileMo = new File(dirStorage + File.separator + nameSimulation + ".mo");
            fileMos = new File(dirStorage + File.separator + nameSimulation + ".mos");
            OpenModelicaExport.exportMO(dataDao , fileMo);
            OpenModelicaExport.exportMOS(dataDao , fileMos , fileMo , dirWork);
        } catch (IOException ex) {
            throw new SimulationServiceException("Data export for OpenModelica failed! (" + ex + ")");
        }

        /**
         * Building simulation.
         */
        pb = new ProcessBuilder(pathCompiler , fileMos.getPath());
        pb.directory(dirWork);
        try {
            System.out.println("Building simulation...");
            buildProcess = pb.start();
        } catch (IOException ex) {
            throw new SimulationServiceException("Exception starting compilation process! (" + ex + ")");
        }

        /**
         * Terminates process after 20 minutes.
         */
        Thread compilerWatcherThread = new Thread()
        {
            @Override
            public void run() {
                long totalTime = 1200000;
                try {
                    for (long t = 0; t < totalTime; t += 1000) {
                        if (buildProcess.isAlive()) {
                            sleep(1000);
                        }
                    }
                    buildProcess.destroy();
                    // stopped = true;
                } catch (Exception e) {
                }
            }
        };
        compilerWatcherThread.start();
        
        /**
         * Waits for build process and interrupt waiting thread.
         */
        try {
            buildProcess.waitFor();
            compilerWatcherThread.interrupt();
        } catch (InterruptedException ex) {
            throw new SimulationServiceException("Exception while waiting for compiler thread! (" + ex + ")");
        }

        /**
         * Reading build process output.
         */
        InputStream os = buildProcess.getInputStream();
        try {
            byte[] bytes = new byte[os.available()];
            os.read(bytes);
            String buildOutput = new String(bytes);
            
            System.out.println(buildOutput);

//            // Evaluating for warnings
//            if (buildOutput.contains("Warning: ")) {
//                String[] split = buildOutput.split("Warning: ");
//                System.out.println("Compilation output contains warnings!");
//                for (int i = 1; i < split.length; i++) {
//                    System.out.println(i + ": " + split[i]);
//                }
//            }
            
            pathSimulation = parseSimulationExecutablePath(buildOutput);
            if (pathSimulation == null) {
                throw new SimulationServiceException("Simulation could not be built!");
            }
        } catch (IOException ex) {
            throw new SimulationServiceException("Exception reading process output! (" + ex + ")");
        }
        
        /**
         * Start server.
         */
        serverThread = server.StartThread(serverPort);
        
        
        /**
         * Ausführen der Simulation.
         */
        boolean noEmmit = true;

        String override = "-override=outputFormat=ia,stopTime=" + simStopTime + ",stepSize=" + simStopTime / simIntervals + ",tolerance=0.0001";
        
//                    if (flags.isParameterChanged()) {
//
//                        Iterator<Parameter> it = pw.getChangedParameters().keySet().iterator();
//                        GraphElementAbstract gea;
//                        Parameter param;
//
//                        while (it.hasNext()) {
//                            param = it.next();
//                            gea = pw.getChangedParameters().get(param);
//                            BiologicalNodeAbstract bna;
//                            if (gea instanceof BiologicalNodeAbstract) {
//                                bna = (BiologicalNodeAbstract)gea;
//                                override += ",'_" + bna.getName() + "_" + param.getName() + "'=" + param.getValue();
//                            }
//                        }
//                    }

//                    if (flags.isInitialValueChanged()) {
//                        Iterator<Place> it = pw.getChangedInitialValues().keySet().iterator();
//                        Place p;
//                        Double d;
//                        while (it.hasNext()) {
//                            p = it.next();
//                            d = pw.getChangedInitialValues().get(p);
//                            override += ",'" + p.getName() + "'.startMarks=" + d;
//                        }
//                    }
//                    if (flags.isBoundariesChanged()) {
//                        Iterator<Place> it = pw.getChangedBoundaries().keySet().iterator();
//                        Place p;
//                        Boundary b;
//                        while (it.hasNext()) {
//                            p = it.next();
//                            b = pw.getChangedBoundaries().get(p);
//                            if (b.isLowerBoundarySet()) {
//                                override += ",'" + p.getName() + "'.minMarks=" + b.getLowerBoundary();
//                            }
//                            if (b.isUpperBoundarySet()) {
//                                override += ",'" + p.getName() + "'.maxMarks=" + b.getUpperBoundary();
//                            }
//                        }
//                    }
            
        pb = new ProcessBuilder();
//        noEmmit = false;
        if (noEmmit) {
            pb.command(pathSimulation , "-s=" + simIntegrator , override , "-port=11111" , "-noEventEmit" , "-lv=LOG_STATS");
        } else {
            pb.command(pathSimulation , override , "-port=11111" );
        }
        pb.directory(dirWork);
        pb.redirectOutput();

        Map<String , String> env = pb.environment();
        
        pathCompiler = pathCompiler.substring(0 , pathCompiler.lastIndexOf(File.separator));
        env.put("PATH" , env.get("PATH") + ";" + pathCompiler);
        
        pathCompiler = pathCompiler.substring(0 , pathCompiler.lastIndexOf(File.separator));
        env.put("PATH" , env.get("PATH") + ";" + pathCompiler);

        System.out.println("Path: " + pb.environment().get("PATH"));
        System.out.println("Override: " + override);

        try {
            System.out.println("Starting simulation...");
            simulationProcess = pb.start();
            simulationOutput = new BufferedReader(new InputStreamReader(simulationProcess.getInputStream()));
        } catch (IOException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
            throw new SimulationServiceException("Exception starting simulation! (" + ex + ")");
        }
        
        /**
         * Simulation Output. (?)
         */
//        try {
        Thread t3 = new Thread()
        {
            public void run() {
                // System.out.println("running");
                String line;
                if (simulationOutput != null) {
                    while (server.isRunning()) {
                        try {
                            line = simulationOutput.readLine();
                            if (line != null && line.length() > 0) {
                                System.out.println(line);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        System.out.println("Server stopped");
                        while ((line = simulationOutput.readLine()) != null && line.length() > 0) {
                            System.out.println(line);
                        }
                        simulationOutput.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // w.updatePCPView();
            }
        };
        t3.start();
        
        /**
         * Zeichnen der Graphen.
         */
//                Thread t2 = new Thread()
//                {
//                    public void run() {
//                        pw.getGraph().getVisualizationViewer().requestFocus();
//                        w.redrawGraphs();
//                        // CHRIS sometimes nullpointer
//                        List<Double> v = pw.getPetriNet().getSimResController().get().getTime().getAll();
//                        // System.out.println("running");
//                        while (s.isRunning()) {
//                            w.redrawGraphs();
//
//                            // double time =
//                            if (v.size() > 0) {
//                                menue.setTime((v.get(v.size() - 1)).toString());
//                            }
//                            try {
//                                sleep(100);
//                            } catch (InterruptedException e) {
//                                // TODO Auto-generated catch block
//                                e.printStackTrace();
//                            }
//                        }
//                        menue.stopped();
//                        System.out.println("end of simulation");
//                        w.updatePCPView();
//                        w.redrawGraphs();
//
//                        w.repaint();
//                        if (v.size() > 0) {
//                            menue.setTime((v.get(v.size() - 1)).toString());
//                        }
//                        // w.updatePCPView();
//                    }
//                };
    }
    
    /**
     * Gets the directory for storing files related to the simulation.
     * @return 
     */
    private File getStorageDirectory() {
        
        File dir;
        
        dir = new File(System.getProperty("java.io.tmpdir"));
        if (!dir.exists() || !dir.isDirectory()) {
            return null;
        }
        
        dir = new File(dir + File.separator + "GraVisFX");
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdir();
        }
        
        dir = new File(dir + File.separator + "data");
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdir();
        }
        
        dir = new File(dir + File.separator + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdir();
        }
        
        return dir;
    }
    
    /**
     * Gets the working directory. Used for storing compiled sources.
     * @return 
     */
    private File getWorkingDirectory() {
        
        if (localStorageDirectory == null) {

            File dir;

            dir = new File(System.getProperty("java.io.tmpdir"));
            if (!dir.exists() || !dir.isDirectory()) {
                return null;
            }

            dir = new File(dir + File.separator + "GraVisFX");
            if (!dir.exists() || !dir.isDirectory()) {
                dir.mkdir();
            }

            dir = new File(dir + File.separator + "data");
            if (!dir.exists() || !dir.isDirectory()) {
                dir.mkdir();
            }
            
            localStorageDirectory = dir;
        }
        
        return localStorageDirectory;
    }
    
    /**
     * Gets the path of the OMC compiler.
     * @return 
     */
    private String getCompilerPath() {
        
        String pathOpenModelica, pathCompiler;
        File dirOpenModelica;

        pathOpenModelica = System.getenv(envOpenModelicaHome);

        if (pathOpenModelica != null) {

            dirOpenModelica = new File(pathOpenModelica);
            if (dirOpenModelica.exists() && dirOpenModelica.isDirectory()) {

                pathCompiler = pathOpenModelica + "bin" + File.separator + "omc";
                if (OS_Validator.isOsWindows()) {
                    pathCompiler = pathCompiler + ".exe";
                } else if (OS_Validator.isOsUnix()) {
                    // TODO : OS is Linux
                } else if (OS_Validator.isOsMac()) {
                    // TODO : OS is Mac
                } else {
                    // TODO : OS not supported
                }
                return pathCompiler;

            } else {
                // ENV is present, but directory does not exist: check if om is installed and ENV variable is set properly 
            }
        } else {
            // ENV is not present: check if om is installed and ENV variable is set properly
        }
        return null;
    }
    
    /**
     * Parses name of the simulation executable. Parses the String generated
     * by the compiler to output the name of the executable simulation file.
     * @param output
     * @return 
     */
    private String parseSimulationExecutablePath(String output) {
        
        output = output.substring(output.lastIndexOf("{"));
        output = parseSubstring(output, "\"", "\"");
        
        if (output == null) {
            return null;
        }
        
        if (OS_Validator.isOsWindows()) {
            output += ".exe";
        }
        
        return output;
    }
        
    /**
     * Parses a substring. Parses a subject String from a given starting String 
     * to a given ending String. Returns NULL if the String cannot be parsed.
     * @param subject
     * @param start
     * @param end
     * @return 
     */
    private static String parseSubstring(String subject, String start, String end){
        
        if (subject == null)
            return null;
        
        int s = subject.indexOf(start) + start.length();
        int e = subject.indexOf(end, s);
        
        if (s >= e)
            return null;
        
        return subject.substring(s, e);
    }
}