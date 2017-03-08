/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.unibi.agbi.gnius.core.simulation;

import edu.unibi.agbi.gnius.core.model.entity.simulation.Simulation;
import edu.unibi.agbi.gnius.core.service.SimulationService;
import edu.unibi.agbi.gnius.util.OS_Validator;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author PR
 */
@Component
public class OpenModelicaServer
{
    @Autowired private SimulationService simulationService;
    
    private final int SIZE_OF_INT; // size of modelica int;
    
    private boolean isTerminated = false;
    private boolean isRunning = false;
    
    private ServerSocket serverSocket;
    private DataInputStream inputStream;

    public OpenModelicaServer() {
        if (OS_Validator.isOsWindows()) {
            SIZE_OF_INT = 4;
        } else {
            SIZE_OF_INT = 8;
        }
    }
    
    /**
     * Starts the server thread. Waits for the thread to start before returning.
     * @param port
     * @return 
     */
    public Thread StartThread(int port) {
        
        final Boolean serverSync = true;
        Thread serverThread;
        
        /**
         * Start server thread.
         */
        serverThread = new Thread(() -> {
            
            Socket client;
            String[] names;
            Simulation simulation;
            
            try {
                isTerminated = false;
                isRunning = true;
                serverSocket = new java.net.ServerSocket(port);
                synchronized (serverSync) {
                    serverSync.notify();
                }
                while (true) {
                    if (serverSocket.isClosed()) {
                        break;
                    }
                    System.out.println("Waiting for client...");
                    client = serverSocket.accept();
                    System.out.println("Client connected!");
                    
                    inputStream = new DataInputStream(client.getInputStream());
                    
                    names = InitData();
                    simulation = simulationService.InitSimulation(names);
                    ReadData(simulation);
                    
                    System.out.println("Client disconnect!");
                }
            } catch (IOException ex) {
                if (isTerminated) {
                    System.out.println("Server terminated!");
                } else {
                    System.out.println("Exception while processing client request!");
                    System.out.println(ex.toString());
                }
            }
        });
        serverThread.start();
        
        /**
         * Wait for server thread before returning.
         */
        synchronized (serverSync) {
            try {
                System.out.println("Waiting for server to start...");
                serverSync.wait();
                System.out.println("Server started!");
            } catch (InterruptedException ex) {
                System.out.println("Exception while waiting for server start!");
                System.out.println(ex);
            }
        }
        
        return serverThread;
    }
    
    /**
     * Stops the server thread. Closes the server socket.
     */
    public void StopThread() {
        System.out.println("Stopping server...");
        try {
            serverSocket.close();
        } catch (IOException ex) {
            // TODO
            System.out.println(ex.toString());
        } finally {
            isRunning = false;
        }
    }
    
    private byte[] buffer;
    private ByteBuffer byteBuffer;
    
    private int vars, doubles, ints, bools, expected, length;
    private byte btmp;
    
    /**
     * Reads data from the given input stream.
     * @param inputStream
     * @throws IOException 
     */
    private String[] InitData() throws IOException {
        
        String[] names;
        
        int lengthMax = 2048;
        buffer = new byte[lengthMax];
        
        inputStream.readFully(buffer, 0, 1);
//        id = (int) buffer[0];
//        System.out.println("Server ID: " + id);
        inputStream.readFully(buffer , 0 , 4);

        byteBuffer = ByteBuffer.wrap(buffer);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        
        length = byteBuffer.getInt();
        if (lengthMax < length) {
            buffer = new byte[length];
        }

        inputStream.readFully(buffer , 0 , length); // blocks until msg received
        
        byteBuffer = ByteBuffer.wrap(buffer , 0 , 4);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        doubles = byteBuffer.getInt();
        byteBuffer = ByteBuffer.wrap(buffer , 4 , 4);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        ints = byteBuffer.getInt();

        byteBuffer = ByteBuffer.wrap(buffer , 8 , 4);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        bools = byteBuffer.getInt();

        expected = doubles * 8 + ints * SIZE_OF_INT + bools;

        byteBuffer = ByteBuffer.wrap(buffer , 12 , 4);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
//        int strings = byteBuffer.getInt();
//        System.out.println("string: " + strings);
        
        names = new String(buffer , 16 , buffer.length - 17).split("\u0000");
        vars = names.length;
        
        System.out.println(">> START: INCOMING NODE NAMES:");
        for (String n : names) {
            System.out.println(n);
        }
        System.out.println("<< END: INCOMING NODE NAMES:");
        
        return names;
    }
    
    public void ReadData(Simulation simulation) {

        Object[] data;
        String[] messages;
        int id, index;
        
        try {
            int count = 0;
            while (isRunning) {
                
//                try {
////                    Thread.sleep(20); // sleep thread to save resources
//                    Thread.sleep(1); // sleep thread to save resources
//                } catch (InterruptedException e) {
//                    System.out.println("Thread interrupted while sleeping!");
//                    System.out.println(e);
//                }

                System.out.println("#" + count++ + ": " + inputStream.available());
                inputStream.readFully(buffer , 0 , 5);
                id = (int)buffer[0];

                byteBuffer = ByteBuffer.wrap(Arrays.copyOfRange(buffer , 1 , buffer.length - 2)); // length
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                length = byteBuffer.getInt();
                
                System.out.println("#" + count++ + ": ID = " + id + " | " + length);

                switch (id) {
                    case 4:
                        if (length > 0) {
                            
                            inputStream.readFully(buffer , 0 , length);
                            
                            // reihenfolge passt zu reihenfolge der variablen?
                            data = new Object[vars];
                            index = 0;
                            for (int r = 0; r < doubles; r++) {
                                byteBuffer = ByteBuffer.wrap(buffer , r * 8 , 8);
                                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                                data[index] = byteBuffer.getDouble();
                                if (r == 0) {
                                    System.out.println("time = " + data[index]);
                                }
                                index++;
                            }
                            for (int i = 0; i < ints; i++) {
                                byteBuffer = ByteBuffer.wrap(buffer , doubles * 8 + i * SIZE_OF_INT , SIZE_OF_INT);
                                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                                data[index] = byteBuffer.getInt();
                                index++;
                            }
                            for (int b = 0; b < bools; b++) {
                                btmp = buffer[doubles * 8 + ints * SIZE_OF_INT + b];
                                data[index] = btmp;
                                index++;
                            }
                            
                            byteBuffer = ByteBuffer.wrap(buffer , expected , length - expected);
                            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

                            messages = (new String(buffer , expected , length - expected)).split("\u0000");
                            for (int i = 0; i < messages.length; i++) {
                                if (messages[i].length() > 0) {
                                    System.out.println(messages[i]);
                                }
                            }
                            
                            simulation.addResult(data);
                        }
                        break;

                    case 6:
                        isTerminated = true;
                        isRunning = false;
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("Exception while reading data!");
//            System.out.println(e);
            e.printStackTrace();
        } finally {
            StopThread();
        }
    }
    
    public boolean isRunning() {
        return isRunning;
    }
}