/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package example.es;

import com.simsilica.es.EntityComponent;

/**
 *
 * @author Asser
 */
public class Damage implements EntityComponent {

    private int damage;

    public Damage(int damage) {
        this.damage = damage;
    }

    public int getDamage() {
        return damage;
    }
}
