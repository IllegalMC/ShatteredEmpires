package me.grplayer.lib.naj0jerk;

import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;

public interface BrewAction {
    /**
     * This method would be called when ever a brewer is done and would be called for the certin brew recipe
     * @param inventory The invetory everything is getting brewed on
     * @param item The item that is currently getting "Brewed"
     * @param ingridient The ingridient of the brewing recipe
     * @return If the item is changed or not
     */
    public boolean brew(BrewerInventory inventory, ItemStack item, ItemStack ingridient);
}