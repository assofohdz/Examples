/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package example.es;

import com.simsilica.es.EntityComponent;

/**
 *
 * @author ss
 */
public class Range implements EntityComponent{
    
    private double range;
    
    public Range(){
    }
    
    public Range(double range){
        this.range = range;
    }
    
    public double getRange(){
        return this.range;
    }
    
}
