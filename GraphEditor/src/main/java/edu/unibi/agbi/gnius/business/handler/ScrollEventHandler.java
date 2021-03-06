/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.unibi.agbi.gnius.business.handler;

import edu.unibi.agbi.gravisfx.presentation.GraphPane;
import javafx.scene.input.ScrollEvent;
import org.springframework.stereotype.Component;

/**
 *
 * @author PR
 */
@Component
public class ScrollEventHandler
{
    private final double scaleBase = 1.0;
    private final double scaleFactor = 1.1;
    private int scalePower = 0;
    
    private final double SCALE_MAX = 10.d;
    private final double SCALE_MIN = .01d;
    
    public void registerTo(GraphPane graphPane) {
        
        graphPane.setOnScroll(( ScrollEvent event ) -> {
            
            double scale_t1, scale_t0;
            
            scale_t0 = scaleBase * Math.pow(scaleFactor , scalePower);
            
            if (event.getDeltaY() > 0) {
                scalePower++;
            } else {
                scalePower--;
            }
            
            scale_t1 = scaleBase * Math.pow(scaleFactor , scalePower);
            
            graphPane.getTopLayer().getScale().setX(scale_t1);
            graphPane.getTopLayer().getScale().setY(scale_t1);
            
            /**
             * Following is used to make sure focus is kept on the mouse pointer location.
             * TODO apply max / min zoom.
             */
            double startX, startY, endX, endY;
            double translateX, translateY;
            
            startX = event.getX() - graphPane.getTopLayer().translateXProperty().get();
            startY = event.getY() - graphPane.getTopLayer().translateYProperty().get();

            endX = startX * scale_t1 / scale_t0;
            endY = startY * scale_t1 / scale_t0;

            translateX = startX - endX;
            translateY = startY - endY;
                
            graphPane.getTopLayer().setTranslateX(graphPane.getTopLayer().translateXProperty().get() + translateX);
            graphPane.getTopLayer().setTranslateY(graphPane.getTopLayer().translateYProperty().get() + translateY);
        });
    }
}
