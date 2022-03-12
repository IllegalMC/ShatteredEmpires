package me.grplayer.lib.naj0jerk;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import me.grplayer.ShatteredEmpires;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class BrewingRecipe {
    private static List<BrewingRecipe> recipes = new ArrayList<BrewingRecipe>();
    private ItemStack ingridient;
    private BrewAction action;
    private boolean perfect;

    public BrewingRecipe(ItemStack ingridient, BrewAction action, boolean perfect) {
        this.ingridient = ingridient;
        this.action = action;
        this.perfect = perfect;
        recipes.add(this);
    }

    public BrewingRecipe(Material ingridient, BrewAction action)
    {
        this(new ItemStack(ingridient), action, false);
    }
    public ItemStack getIngridient() {
        return ingridient;
    }

    public BrewAction getAction() {
        return action;
    }

    public boolean isPerfect() {
        return perfect;
    }

    /**
     * Get the BrewRecipe of the given recipe, will return null if no recipe is found
     * @param inventory The inventory
     * @return The recipe
     */
    @Nullable
    public static BrewingRecipe getRecipe(BrewerInventory inventory)
    {
        boolean notAllAir = false;
        for(int i = 0 ; i < 3 && !notAllAir ; i++)
        {
            if(inventory.getItem(i) == null)
                continue;
            if(inventory.getItem(i).getType() == Material.AIR)
                continue;
            notAllAir = true;
        }
        if(!notAllAir)
            return null;
        for(BrewingRecipe recipe : recipes)
        {
            if(!recipe.isPerfect() && inventory.getIngredient().getType() == recipe.getIngridient().getType())
            {
                return recipe;
            }
            if(recipe.isPerfect() && inventory.getIngredient().isSimilar(recipe.getIngridient()))
            {
                return recipe;
            }
        }
        return null;
    }

    public void startBrewing(BrewerInventory inventory, List<HumanEntity> viewers)
    {
        new BrewClock(this, inventory, viewers);
    }

    private class BrewClock extends BukkitRunnable
    {
        private BrewerInventory inventory;
        private BrewingRecipe recipe;
        private ItemStack ingridient;
        private BrewingStand stand;
        private List<HumanEntity> viewers;
        private int time = 400; //Like I said the starting time is 400

        public BrewClock(BrewingRecipe recipe, BrewerInventory inventory, List<HumanEntity> viewers) {
            this.recipe = recipe;
            this.inventory = inventory;
            this.ingridient = inventory.getIngredient();
            this.stand = inventory.getHolder();
            this.viewers = viewers;
            System.out.println("Starting brewing: " + this.stand.getBrewingTime());
            runTaskTimer(ShatteredEmpires.getInstance(), 0L, 1L);
        }

        @Override
        public void run() {
            if(inventory.getIngredient() == null || inventory.getIngredient().getType() == Material.AIR) return;

            // Make sure there is enough fuel
            if(inventory.getFuel() == null || inventory.getFuel().getType() == Material.AIR || inventory.getFuel().getAmount() < 1) return;

            if(time == 0)
            {
                for(int i = 0; i < 3 ; i ++)
                {
                    // Check if the ingredient has enough space
                    if(inventory.getIngredient().getAmount() >= 1) {
                        if(inventory.getItem(i) == null || Objects.requireNonNull(inventory.getItem(i)).getType() == Material.AIR)
                            continue;
                        if(recipe.getAction().brew(inventory, inventory.getItem(i), ingridient)) {
                            // Remove one from the ingredient
                            inventory.getIngredient().setAmount(inventory.getIngredient().getAmount() - 1);
                        }
                    }else return;
                }
                cancel();
                return;
            }else if(ingridient.getType() == Objects.requireNonNull(inventory.getIngredient()).getType())
            {
                stand.setBrewingTime(400); //Reseting everything
                cancel();
                return;
            }
            //You should also add here a check to make sure that there are still items to brew
            time--;
            stand.setBrewingTime(time);
            viewers.forEach(humanEntity -> {
                if(humanEntity instanceof Player)
                {
                    Player player = (Player) humanEntity;
                    player.sendMessage(ChatColor.GOLD + "Brewing: " + ChatColor.GREEN + time);
                    player.updateInventory();
                }
            });
        }
    }
}