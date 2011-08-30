package com.daemitus.betterfurnaces;

import com.daemitus.lockette.Lockette;
import com.griefcraft.lwc.LWCPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerListener extends org.bukkit.event.player.PlayerListener {

    private final BetterFurnaces plugin;
    private final String tag = ChatColor.RED + "[" + ChatColor.GOLD + "BF" + ChatColor.RED + "] " + ChatColor.WHITE;
    private final String byteMismatch = tag + "You cant mix different types of %s";
    private final String reagentAdd = tag + "You add %d %s to be smelted";
    private final String reagentAddFull = tag + "You cannot add anymore %s to be smelted";
    private final String reagentOccupied = tag + "You cannot add %d %s, it already has %d %s";
    private final String reagentOutputWarning = tag + "WARNING: Furnace cannot smelt with %d %s still in it";
    private final String fuelAdd = tag + "You add %d %s as fuel";
    private final String fuelAddFull = tag + "You cannot add anymore %s as fuel";
    private final String fuelOccupied = tag + "You cannot add %d %s, it already has %d %s";
    private final String outputDrop = tag + "You strike the furnace causing %d %s to fall";
    private final String outputFail = tag + "You strike the furnace but nothing happens";
    private final Lockette lockette;
    private final LWCPlugin lwc;
    private final boolean locketteEnabled;
    private final boolean lwcEnabled;

    public PlayerListener(final BetterFurnaces plugin) {
        this.plugin = plugin;
        lockette = (Lockette) plugin.getServer().getPluginManager().getPlugin("Lockette");
        locketteEnabled = lockette == null ? false : true;

        lwc = (LWCPlugin) plugin.getServer().getPluginManager().getPlugin("LWC");
        lwcEnabled = lwc == null ? false : true;
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.isCancelled())
            return;
        if (!event.getAction().equals(Action.LEFT_CLICK_BLOCK))
            return;
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
        Furnace furnace = (Furnace) block.getState();
        ItemStack reagent = furnace.getInventory().getItem(0);
        ItemStack fuel = furnace.getInventory().getItem(1);
        ItemStack output = furnace.getInventory().getItem(2);


        if (isSmeltable(held.getType())) {
            //smeltable item held
            if (reagent.getType().equals(Material.AIR)) {
                //no reagent
                player.sendMessage(String.format(reagentAdd, held.getAmount(), held.getType().toString()));
                furnace.getInventory().setItem(0, new ItemStack(held.getType(), held.getAmount()));
                player.setItemInHand(null);
            } else if (reagent.getType().equals(held.getType())) {
                //reagent heldItem Material match
                if (reagent.getData() != null && held.getData() != null && reagent.getData().getData() != held.getData().getData()) {
                    //reagent heldItem byte mismatch
                    player.sendMessage(String.format(byteMismatch, held.getType()));
                } else {
                    //add reagent
                    int max = reagent.getType().getMaxStackSize() - reagent.getAmount();
                    if (max == 0) {
                        player.sendMessage(String.format(reagentAddFull, held.getType().toString()));
                    } else if (held.getAmount() > max) {
                        player.sendMessage(String.format(reagentAdd, max, held.getType().toString()));
                        held.setAmount(held.getAmount() - max);
                        reagent.setAmount(reagent.getAmount() + max);
                    } else {
                        player.sendMessage(String.format(reagentAdd, held.getAmount(), held.getType()));
                        reagent.setAmount(reagent.getAmount() + held.getAmount());
                        player.setItemInHand(null);
                    }
                    if (!output.getType().equals(Material.AIR) && !output.getType().equals(getFurnaceOutput(reagent.getType()))) {
                        player.sendMessage(String.format(reagentOutputWarning, output.getAmount(), output.getType().toString()));
                    }
                }
            } else {
                //reagent heldItem mismatch
                player.sendMessage(String.format(reagentOccupied, held.getAmount(), held.getType().toString(), reagent.getAmount(), reagent.getType().toString()));
            }
        } else if (isFuel(held.getType())) {
            //fuel item held
            if (fuel.getType().equals(Material.AIR)) {
                //no fuel
                player.sendMessage(String.format(fuelAdd, held.getAmount(), held.getType().toString()));
                furnace.getInventory().setItem(1, new ItemStack(held.getType(), held.getAmount()));
                player.setItemInHand(null);
            } else if (fuel.getType().equals(held.getType())) {
                //fuel heldItem Material match
                if (fuel.getData() != null && held.getData() != null && fuel.getData().getData() != held.getData().getData()) {
                    //fuel heldItem byte mismatch
                    player.sendMessage(String.format(byteMismatch, held.getType()));
                } else {
                    //add fuel
                    int max = fuel.getType().getMaxStackSize() - fuel.getAmount();
                    if (max == 0) {
                        player.sendMessage(String.format(fuelAddFull, held.getType().toString()));
                    } else if (held.getAmount() > max) {
                        player.sendMessage(String.format(fuelAdd, max, held.getType().toString()));
                        held.setAmount(held.getAmount() - max);
                        fuel.setAmount(fuel.getAmount() + max);
                    } else {
                        player.sendMessage(String.format(fuelAdd, held.getAmount(), held.getType()));
                        fuel.setAmount(fuel.getAmount() + held.getAmount());
                        player.setItemInHand(null);
                    }
                }
            } else {
                //reagent heldItem mismatch
                player.sendMessage(String.format(fuelOccupied, held.getAmount(), held.getType().toString(), fuel.getAmount(), fuel.getType().toString()));
            }
        } else {
            if (output.getAmount() > 0) {
                player.sendMessage(String.format(outputDrop, output.getAmount(), output.getType().toString()));
                player.getWorld().dropItemNaturally(block.getLocation(), output);
                furnace.getInventory().remove(output);
            } else {
                player.sendMessage(String.format(outputFail));
            }
        }
        furnace.update();
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
                return true;
            case GOLD_ORE:
                return true;
            case SAND:
                return true;
            case COBBLESTONE:
                return true;
            case PORK:
                return true;
            case RAW_FISH:
                return true;
            case CLAY_BALL:
                return true;
            case CACTUS:
                return true;
            case LOG:
                return true;
            default:
                return false;
        }
    }

    private boolean isFuel(Material m) {
        switch (m) {
            case COAL:
                return true;
            case WOOD:
                return true;
            case SAPLING:
                return true;
            case STICK:
                return true;
            case FENCE:
                return true;
            case WOOD_STAIRS:
                return true;
            case TRAP_DOOR:
                return true;
            case LOG:
                return true;
            case WORKBENCH:
                return true;
            case BOOKSHELF:
                return true;
            case CHEST:
                return true;
            case JUKEBOX:
                return true;
            case NOTE_BLOCK:
                return true;
            case LOCKED_CHEST:
                return true;
            case LAVA_BUCKET:
                return true;
            default:
                return false;
        }
    }

    private Material getFurnaceOutput(Material m) {
        switch (m) {
            case IRON_ORE:
                return Material.IRON_INGOT;
            case GOLD_ORE:
                return Material.GOLD_INGOT;
            case SAND:
                return Material.GLASS;
            case COBBLESTONE:
                return Material.STONE;
            case PORK:
                return Material.GRILLED_PORK;
            case RAW_FISH:
                return Material.COOKED_FISH;
            case CLAY_BALL:
                return Material.CLAY_BRICK;
            case CACTUS:
                return Material.INK_SACK;
            case LOG:
                return Material.COAL;
            default:
                return Material.AIR;
        }
    }
}
