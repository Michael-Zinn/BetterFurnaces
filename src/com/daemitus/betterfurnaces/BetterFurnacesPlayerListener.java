package com.daemitus.betterfurnaces;

import com.daemitus.lockette.Lockette;
import com.griefcraft.lwc.LWCPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class BetterFurnacesPlayerListener extends org.bukkit.event.player.PlayerListener {

 //   private final BetterFurnaces plugin;
    private final String tag = ChatColor.RED + "[" + ChatColor.GOLD + "BF" + ChatColor.RED + "] " + ChatColor.WHITE;
 /*   private final String byteMismatch = tag + "You cant mix different types of %s";
    private final String reagentAdd = tag + "You add %d %s to be smelted";
    private final String reagentAddFull = tag + "You cannot add anymore %s to be smelted";
    private final String reagentOccupied = tag + "You cannot add %d %s, it already has %d %s";
    private final String reagentOutputWarning = tag + "WARNING: Furnace cannot smelt with %d %s still in it";
    private final String fuelAdd = tag + "You add %d %s as fuel";
    private final String fuelAddFull = tag + "You cannot add anymore %s as fuel";
    private final String fuelOccupied = tag + "You cannot add %d %s, it already has %d %s";
    private final String outputDrop = tag + "You strike the furnace causing %d %s to fall";
    private final String outputFail = tag + "You strike the furnace but nothing happens";*/
	
    private final Lockette lockette;
    private final LWCPlugin lwc;
    private final boolean locketteEnabled;
    private final boolean lwcEnabled;
    
    private final int REAGENT = 0;
    private final int FUEL = 1;
    private final int OUTPUT = 2;

    public BetterFurnacesPlayerListener(final BetterFurnaces plugin) {
        //this.plugin = plugin;
        lockette = (Lockette) plugin.getServer().getPluginManager().getPlugin("Lockette");
        locketteEnabled = lockette == null ? false : true;

        lwc = (LWCPlugin) plugin.getServer().getPluginManager().getPlugin("LWC");
        lwcEnabled = lwc == null ? false : true;
    }

    /**
     * Left click: shake output out of the furnace
     * Right click: Put hand item into furnace if possible. If not possible, open furnace menu instead.
     */
    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {

    	// ???
    	if (event.isCancelled())
            return;
        
    	//if (!event.getAction().equals(Action.LEFT_CLICK_BLOCK))
        //    return;

        
        Block block = event.getClickedBlock();
        if (!block.getType().equals(Material.FURNACE)
            && !block.getType().equals(Material.BURNING_FURNACE))
            return;

        Player player = event.getPlayer();

        if (!checkPermission(player, block)) {
            player.sendMessage(tag + "Permission denied for this furnace");
            return;
        }

        ItemStack held = player.getItemInHand();
        Furnace furnace = (Furnace)block.getState();
        //ItemStack reagent = furnace.getInventory().getItem(REAGENT);
        //ItemStack fuel = furnace.getInventory().getItem(FUEL);
        //ItemStack output = furnace.getInventory().getItem(OUTPUT);

        // on left click: Punch output out of the furnace (and don't do anything else)
    	if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
		    if (get(furnace,OUTPUT).getAmount() > 0) {
                //player.sendMessage(String.format(outputDrop, output.getAmount(), output.getType().toString()));
                //player.getWorld().dropItemNaturally(block.getLocation(), output);
		    	eject(furnace,OUTPUT,event.getBlockFace());
                
            } //else {
              //  player.sendMessage(String.format(outputFail)); // I hate talking furnaces!
            //}
        
    	} else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
    		
	        if (isSmeltable(held.getType())) {
	        	// if furnace is occupied by something else: Eject it.
	        	if(!same(get(furnace,REAGENT), held))
	        		eject(furnace, REAGENT,event.getBlockFace());
        		
	            //smeltable item held
	            if (get(furnace,REAGENT).getType().equals(Material.AIR)) {
	                //no reagent
	                //player.sendMessage(String.format(reagentAdd, held.getAmount(), held.getType().toString())); // Talking furnaces are of the devil!
	                furnace.getInventory().setItem(0, held);//new ItemStack(held.getType(), held.getAmount()));
	                player.setItemInHand(null);
	            } else if (get(furnace,REAGENT).getType().equals(held.getType())) {
	                //reagent heldItem Material match
	                if (get(furnace,REAGENT).getData() != null && held.getData() != null && get(furnace,REAGENT).getData().getData() != held.getData().getData()) {
	                    //reagent heldItem byte mismatch
	                    //player.sendMessage(String.format(byteMismatch, held.getType())); // Talking furnaces will take over the world!
	                } else {
	                    //add reagent
	                    int max = get(furnace,REAGENT).getType().getMaxStackSize() - get(furnace,REAGENT).getAmount();
	                    if (max == 0) {
	                        //player.sendMessage(String.format(reagentAddFull, held.getType().toString())); // Talk to players, not furnaces
	                    } else if (held.getAmount() > max) {
	                        //player.sendMessage(String.format(reagentAdd, max, held.getType().toString()));
	                        held.setAmount(held.getAmount() - max);
	                        get(furnace,REAGENT).setAmount(get(furnace,REAGENT).getAmount() + max);
	                    } else {
	                        //player.sendMessage(String.format(reagentAdd, held.getAmount(), held.getType()));
	                    	get(furnace,REAGENT).setAmount(get(furnace,REAGENT).getAmount() + held.getAmount());
	                        player.setItemInHand(null);
	                    }
	                    
	                    // Warn about furnace being clogged up
	                    //if (!output.getType().equals(Material.AIR) && !output.getType().equals(getFurnaceOutput(reagent.getType()))) {
	                    //    player.sendMessage(String.format(reagentOutputWarning, output.getAmount(), output.getType().toString()));
	                    //}
	                }
	            }
	            
	            // now, is the output clogging up the furnace? If so, eject item
        		if(get(furnace,OUTPUT).getType() != Material.AIR)
        			// FIXME Doesn't check data. Doesn't matter in current minecraft, but might matter in the future
        			if(get(furnace,OUTPUT).getType() != getSmeltTo(get(furnace,REAGENT).getType())) { 
        				eject(furnace,OUTPUT,event.getBlockFace());
        			}
        				
	            
	            event.setCancelled(true); // don't open menu if you put something in
	            
	        } else if (isFuel(held.getType())) {
	        	// if furnace is occupied by something else: Eject it.
	        	
	        	
	        	if(!same(get(furnace,FUEL), held))
	        		eject(furnace,FUEL,event.getBlockFace());
	        	
	        	
	            //fuel item held
	            if (get(furnace,FUEL).getType().equals(Material.AIR)) {
	                //no fuel
	                //player.sendMessage(String.format(fuelAdd, held.getAmount(), held.getType().toString()));
	                furnace.getInventory().setItem(1, held);
	                player.setItemInHand(null);
	            } else if (get(furnace,FUEL).getType().equals(held.getType())) {
	                //fuel heldItem Material match
	                if (get(furnace,FUEL).getData() != null && held.getData() != null && get(furnace,FUEL).getData().getData() != held.getData().getData()) {
	                    //fuel heldItem byte mismatch
	                    //player.sendMessage(String.format(byteMismatch, held.getType()));
	                } else {
	                    //add fuel
	                    int availableFuelSpace = get(furnace,FUEL).getType().getMaxStackSize() - get(furnace,FUEL).getAmount();
	                    if (availableFuelSpace == 0) {
	                        //player.sendMessage(String.format(fuelAddFull, held.getType().toString()));
	                    } else if (held.getAmount() > availableFuelSpace) {
	                        //player.sendMessage(String.format(fuelAdd, max, held.getType().toString()));
	                        held.setAmount(held.getAmount() - availableFuelSpace);
	                        get(furnace,FUEL).setAmount(get(furnace,FUEL).getAmount() + availableFuelSpace);
	                    } else {
	                        //player.sendMessage(String.format(fuelAdd, held.getAmount(), held.getType()));
	                    	get(furnace,FUEL).setAmount(get(furnace,FUEL).getAmount() + held.getAmount());
	                        player.setItemInHand(null);
	                    }
	                }
	            } 
	            
	            event.setCancelled(true); // don't open menu if you put something in
	        }
    	}
        furnace.update();
    }

    
	/**
	 * gives the "face" of a block that can "look" into different directions
	 * @param directionalBlock
	 * @return piston top, furnace face, pumpkin...
	 */
	BlockFace getBlockFace(Block directionalBlock) {
		final BlockFace[] FACE = { BlockFace.DOWN, BlockFace.UP, BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH};
		return FACE[directionalBlock.getData() & 7];
	}
    
    /**
     * Ejects the content of the slot out the front of the furnace.
     * If the front of the furnace is blocked, ejects it out where
     * the furnace was clicked. This is helpful for furnaces that
     * are burried in the ground or integrated into the ceiling.
     * 
     * Does nothing if the slot is empty.
     * 
     * @param slot
     * @param clickedFace
     */
    private void eject(Furnace furnace, int slot, BlockFace clickedFace) {
    	
    	ItemStack content = furnace.getInventory().getItem(slot);
    	if(content.getType() == Material.AIR) return;
    	
    	//Block furnaceBlock = furnace.getBlock();
    	//Block furnaceFront = furnaceBlock.getRelative(getBlockFace(furnaceBlock));
    	
    	furnace.getWorld().dropItemNaturally(furnace.getBlock().getRelative(clickedFace).getLocation(), content);
        furnace.getInventory().clear(slot);
        //furnace.update();
    }
    
    private boolean checkPermission(Player player, Block block) {
        boolean perm = true;
        if (locketteEnabled) {
            return Lockette.isAuthorized(player, block);
        }
        if (lwcEnabled) {
            perm = perm && lwc.getLWC().canAccessProtection(player, block);
        }
        return perm;
    }

    private boolean isSmeltable(Material m) {
        switch (m) {
            case IRON_ORE:
            case GOLD_ORE:
            case SAND:
            case COBBLESTONE:
            case PORK:
            case RAW_FISH:
            case CLAY_BALL:
            case CACTUS:
            case LOG:
                return true;
            
            default: return false;
        }
    }

    private boolean isFuel(Material m) {
        switch (m) {
            case COAL:
            case WOOD:
            case SAPLING:
            case STICK:
            case FENCE:
            case WOOD_STAIRS:
            case TRAP_DOOR:
            case LOG:
            case WORKBENCH:
            case BOOKSHELF:
            case CHEST:
            case JUKEBOX:
            case NOTE_BLOCK:
            case LOCKED_CHEST:
            case LAVA_BUCKET:
            	return true;

            default: return false;
        }
    }

    private Material getSmeltTo(Material m) {
        switch (m) {
            case IRON_ORE:    return Material.IRON_INGOT;
            case GOLD_ORE:    return Material.GOLD_INGOT;
            case SAND:        return Material.GLASS;
            case COBBLESTONE: return Material.STONE;
            case PORK:        return Material.GRILLED_PORK;
            case RAW_FISH:    return Material.COOKED_FISH;
            case CLAY_BALL:   return Material.CLAY_BRICK;
            case CACTUS:      return Material.INK_SACK;
            case LOG:         return Material.COAL;
            
            default: return Material.AIR;
        }
    }
    
    private ItemStack get(Furnace furnace, int slot) {
    	return furnace.getInventory().getItem(slot);
    }
    
    private boolean same(ItemStack a, ItemStack b) {
    	if(a.getType() != b.getType())
    		return false;
    	else if(a.getData() != null) {
    		if(a.getData().getData() != b.getData().getData())
    			return false;
    		else return true;
    	} else return true;
    }
}
