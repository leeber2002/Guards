package com.bxbservers.Guards;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import org.kitteh.tag.TagAPI;

import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import com.matejdro.bukkit.jail.Jail;
import com.matejdro.bukkit.jail.JailAPI;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;


public class Guards extends JavaPlugin
implements Listener
{
	
	  public List<String> onDuty;
	  public static Economy econ = null;
	  public List<String> help;
	  public GuardsListener listener;
	  public FileConfiguration configFile;
	  public Logger logger;
	  public PotionEffectType potion;
	  private FileConfiguration customConfig = null;
	  private File customConfigFile = null;
	  public JailAPI jail;
	  public String prefix = ChatColor.DARK_RED + "[Guards] " + ChatColor.GOLD;
	  public WorldGuardPlugin getWorldGuard() {
		    Plugin WGplugin = getServer().getPluginManager().getPlugin("WorldGuard");
		 
		    // WorldGuard may not be loaded
		    if (WGplugin == null || !(WGplugin instanceof WorldGuardPlugin)) {
		        logger.info("WorldGuardError");
		    	return null; // Maybe you want throw an exception instead

		    }
		 
		    return (WorldGuardPlugin) WGplugin;
		}
	  
	  
	@Override
	public void onEnable() {
		
		if (!setupEconomy() ) {
            logger.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
		
		logger = getLogger();
		configFile = getConfig();
		configFile.options().copyDefaults(true);
		saveDefaultConfig();
		
		getLogger().info("Guards has been Enabled");
		listener = new GuardsListener(this);
		getServer().getPluginManager().registerEvents(listener, this);
		this.onDuty = getConfig().getStringList("onDuty");
		this.help = getConfig().getStringList("help");
        reloadCustomConfig();
        
        Plugin jailPlugin = getServer().getPluginManager().getPlugin("Jail");
        if (jailPlugin != null)
        {
            jail = ((Jail) jailPlugin).API;
        }
        else
        {
            //Code here will run if player don't have Jail installed.
            //Use that to disable features of your plugin that include Jail to prevent errors.
        }
	}
	 

	 private boolean setupEconomy() {
	        if (getServer().getPluginManager().getPlugin("Vault") == null) {
	            return false;
	        }
	        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
	        if (rsp == null) {
	            return false;
	        }
	        econ = rsp.getProvider();
	        return econ != null;
	    }


	
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
    	boolean silent = false;
    	if(cmd.getName().equalsIgnoreCase("duty")){
    		// If the player typed /basic then do the following...
    		if (!(sender instanceof Player)) {
    			sender.sendMessage("This command can Only be run by a player");
    			return false;
    		} else {   
        		if (args.length == 1) {
        			//logger.info("arguement Detected");
        			//logger.info(args[0]);
        			if (args[0].equalsIgnoreCase("silent")) {
        				if (sender.hasPermission("guards.duty.silent")){
        					silent=true;
        					//logger.info("Silently going on duty");
        				}
        			}
        		}
    			Player player = (Player) sender;
    			String name = player.getName();
    			if (player.hasPermission("guards.duty")) {
    				if (!this.onDuty.contains(name)) {
    		              setOnDuty(player, silent);
    		              this.onDuty.add(name);
    		              return true;
    		            } else if (this.onDuty.contains(name)) {
    		              setOffDuty(player, silent);
    		              this.onDuty.remove(name);
    		              return true;
    		            }
    				}
    			}
    		} 
    		else if (cmd.getName().equalsIgnoreCase("kit")){ 
    			if (!(sender instanceof Player)) {
    				sender.sendMessage("This command can Only be run by a player");
    				return false;
    			} else {
    				Player player = (Player) sender;
    				if (player.hasPermission("guards.kit")) {
    				this.giveKit(player);
    				player.sendMessage(prefix + "Your Guard Kit has been Issued. Visit the Guard room to restock");
    				return true;
    			}
    		}
    	}
    	else if (cmd.getName().equalsIgnoreCase("guard")){
    		if (!(sender instanceof Player)) {
    			sender.sendMessage("This command can Only be run by a player");
    			return false;
    		} else {
    			Player player = (Player) sender;
    			if (args.length != 1) {
    		           return false;
    		        }
    			if (player.hasPermission("guards.guard")) {
    				Player target = (Bukkit.getServer().getPlayer(args[0]));
    				if (target == null) {
    					OfflinePlayer offlineTarget = getServer().getOfflinePlayer(args[0]);
    					//getServer().dispatchCommand(getServer().getConsoleSender(), "/pex user "+offlineTarget.getName()+" add guards.duty");
    					player.chat("/pex user "+offlineTarget.getName()+" add guards.duty");
    					player.sendMessage(offlineTarget.getName()+" has been promoted");
    					return true;
    				} else {
    					PermissionUser user = PermissionsEx.getUser(target);
    					user.addPermission("guards.duty");
    					target.sendMessage(prefix + "Congratulations on promotion to guard");
    					player.sendMessage(prefix + target.getName()+" has been promoted");
    					return true;
    				}
    			}
    		}
       	}
    	return false;
    	
    }
	
	public ItemStack setColor(ItemStack item, int color) {
/*CraftItemStack craftStack = null;
net.minecraft.server.v1_4_5.ItemStack itemStack = null;
if (item instanceof CraftItemStack) {
craftStack = (CraftItemStack) item;
itemStack = craftStack.getHandle();
} else if (item instanceof ItemStack) {
craftStack = new CraftItemStack(item);
itemStack = craftStack.getHandle();
}
NBTTagCompound tag = itemStack.tag;
if (tag == null) {
tag = new NBTTagCompound();
tag.setCompound("display", new NBTTagCompound());
itemStack.tag = tag;
}
tag = itemStack.tag.getCompound("display");
tag.setInt("color", color);
itemStack.tag.setCompound("display", tag);*/
LeatherArmorMeta im = (LeatherArmorMeta) item.getItemMeta();
im.setColor(Color.fromRGB(color));
item.setItemMeta(im);
return item;
}

	@SuppressWarnings("deprecation")
	public void giveKit(Player player) {

		this.help.remove(player.getName());
		getLogger().info("Giving kit to " + player.getName());
		int slot;
		String className = "Guard";
		
		// Item Section
		for (slot = 0; slot<=35; slot++) {
			ItemStack i = new ItemStack(0);
			String getSlot = this.getConfig().getString("kits." + className + ".items." + slot);
            if (this.getConfig().contains("kits." + className + ".items." + slot) && !(this.getConfig().getString("kits." + className + ".items." + slot).equals("0")) && !(this.getConfig().getString("kits." + className + ".items." + slot).equals(""))) {
			String[] s = getSlot.split(" ");
			String[] item = s[0].split(":");

			//Sets the block/item
			i.setTypeId(Integer.parseInt(item[0]));

			//Sets the amount and durability
			if (item.length > 1) {
			i.setAmount(Integer.parseInt(item[1]));

			if (item.length > 2) {
			i.setDurability((short)Integer.parseInt(item[2]));
			}

			} else {
			i.setAmount(1); //Default amount is 1
			}

			if (this.getConfig().contains("kits." + className + ".items" + ".names." + slot) ) {
			//get item name
			String name = ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("kits." + className + ".items" + ".names." + slot));
			if (name.equalsIgnoreCase("<username>")){
				name=player.getName();
			}
			ItemMeta im = i.getItemMeta();
			if (name.equals(ChatColor.RESET + "" + ChatColor.RESET)) {
			im.setDisplayName(name + im.getDisplayName());
			} else {
			im.setDisplayName(name);
			//im.setLore(lore);
			}

			i.setItemMeta(im);

			}

			// Sets the enchantments and level
			Boolean first = true;

			if (s.length > 1) {
			for (String a : s) {
			if (!first) {
			String[] enchant = a.split(":");
			Enchantment enchantmentInt = new EnchantmentWrapper(Integer.parseInt(enchant[0]));
			int levelInt = Integer.parseInt(enchant[1]);
			i.addUnsafeEnchantment(enchantmentInt,levelInt);
			}
			first = false;
			}
			}	
			player.getInventory().setItem(slot, i);
			}
			}
		// End of Item section
		
		//Sets the armor contents
		String getHelmet = this.getConfig().getString("kits." + className + ".items" + ".helmet");
		String getChestplate = this.getConfig().getString("kits." + className + ".items" + ".chestplate");
		String getLeggings = this.getConfig().getString("kits." + className + ".items" + ".leggings");
		String getBoots = this.getConfig().getString("kits." + className + ".items" + ".boots");



		//These hold the chosen colours for dying
		int helmColor = 0;
		int chestColor = 0;
		int legColor = 0;
		int bootColor = 0;

		/**
		* Main item stacks for various armour types.
		* They will not necessarily all be used, only those
		* that the user wishes to use and has defined in the config.
		*/
		ItemStack lhelmet = new ItemStack(Material.LEATHER_HELMET);
		ItemStack lchestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
		ItemStack lleggings = new ItemStack(Material.LEATHER_LEGGINGS);
		ItemStack lboots = new ItemStack(Material.LEATHER_BOOTS);

		ItemStack ihelmet = new ItemStack(Material.IRON_HELMET, 1);
		ItemStack ichestplate = new ItemStack(Material.IRON_CHESTPLATE, 1);
		ItemStack ileggings = new ItemStack(Material.IRON_LEGGINGS, 1);
		ItemStack iboots = new ItemStack(Material.IRON_BOOTS, 1);

		ItemStack ghelmet = new ItemStack(Material.GOLD_HELMET, 1);
		ItemStack gchestplate = new ItemStack(Material.GOLD_CHESTPLATE, 1);
		ItemStack gleggings = new ItemStack(Material.GOLD_LEGGINGS, 1);
		ItemStack gboots = new ItemStack(Material.GOLD_BOOTS, 1);

		ItemStack dhelmet = new ItemStack(Material.DIAMOND_HELMET, 1);
		ItemStack dchestplate = new ItemStack(Material.DIAMOND_CHESTPLATE, 1);
		ItemStack dleggings = new ItemStack(Material.DIAMOND_LEGGINGS, 1);
		ItemStack dboots = new ItemStack(Material.DIAMOND_BOOTS, 1);

		ItemStack chelmet = new ItemStack(Material.CHAINMAIL_HELMET);
		ItemStack cchestplate = new ItemStack(Material.CHAINMAIL_CHESTPLATE);
		ItemStack cleggings = new ItemStack(Material.CHAINMAIL_LEGGINGS);
		ItemStack cboots = new ItemStack(Material.CHAINMAIL_BOOTS);

		//ItemStack for final armour items
		ItemStack finalHelmet = null;
		ItemStack finalChestplate = null;
		ItemStack finalLeggings = null;
		ItemStack finalBoots = null;

		//Dying leather armour
		if (this.getConfig().contains("kits." + className + ".items" + ".helmetColor")) {
		helmColor = Integer.parseInt(this.getConfig().getString("kits." + className + ".items.helmetColor").replace("#", ""), 16);
		lhelmet = this.setColor(lhelmet, helmColor);	
		}

		if (this.getConfig().contains("kits." + className + ".items" + ".chestplateColor")) {
		chestColor = Integer.parseInt(this.getConfig().getString("kits." + className + ".items.chestplateColor").replace("#", ""), 16);
		lchestplate = this.setColor(lchestplate, chestColor);
		}

		if (this.getConfig().contains("kits." + className + ".items" + ".leggingColor")) {
		legColor = Integer.parseInt(this.getConfig().getString("kits." + className + ".items.leggingColor").replace("#", ""), 16);
		lleggings = this.setColor(lleggings, legColor);
		}

		if (this.getConfig().contains("kits." + className + ".items" + ".bootColor")) {
		bootColor = Integer.parseInt(this.getConfig().getString("kits." + className + ".items.bootColor").replace("#", ""), 16);
		lboots= this.setColor(lboots, bootColor);
		}

		//Determine which type of armour they want to use
		if (getHelmet != null) {
		if (getHelmet.equals("leather")) {
		finalHelmet = lhelmet;
		}
		if (getHelmet.equals("iron")) {
		finalHelmet = ihelmet;
		}
		if (getHelmet.equals("gold")) {
		finalHelmet = ghelmet;
		}
		if (getHelmet.equals("diamond")) {
		finalHelmet = dhelmet;
		}
		if (getHelmet.equals("chainmail")) {
		finalHelmet = chelmet;
		}

		}

		if (getChestplate != null) {
		if (getChestplate.equals("leather")) {
		finalChestplate = lchestplate;
		}
		if (getChestplate.equals("iron")) {
		finalChestplate = ichestplate;
		}
		if (getChestplate.equals("gold")) {
		finalChestplate = gchestplate;
		}
		if (getChestplate.equals("diamond")) {
		finalChestplate = dchestplate;
		}
		if (getChestplate.equals("chainmail")) {
		finalChestplate = cchestplate;
		}

		}

		if (getLeggings != null) {
		if (getLeggings.equals("leather")) {
		finalLeggings = lleggings;
		}
		if (getLeggings.equals("iron")) {
		finalLeggings = ileggings;
		}
		if (getLeggings.equals("gold")) {
		finalLeggings = gleggings;
		}
		if (getLeggings.equals("diamond")) {
		finalLeggings = dleggings;
		}
		if (getLeggings.equals("chainmail")) {
		finalLeggings = cleggings;
		}
		}

		if (getBoots != null) {
		if (getBoots.equals("leather")) {
		finalBoots = lboots;
		}
		if (getBoots.equals("iron")) {
		finalBoots = iboots;
		}
		if (getBoots.equals("gold")) {
		finalBoots = gboots;
		}
		if (getBoots.equals("diamond")) {
		finalBoots = dboots;
		}
		if (getBoots.equals("chainmail")) {
		finalBoots = cboots;
		}

		}

		short s1 = (short) this.getConfig().getInt("kits." + className + ".items.helmetDurability", -2);
		short s2 = (short) this.getConfig().getInt("kits." + className + ".items.chestplateDurability", -2);
		short s3 = (short) this.getConfig().getInt("kits." + className + ".items.leggingsDurability", -2);
		short s4 = (short) this.getConfig().getInt("kits." + className + ".items.bootsDurability", -2);

		if (s1 == -1) s1 = finalHelmet.getType().getMaxDurability();
		if (s2 == -1) s2 = finalChestplate.getType().getMaxDurability();
		if (s3 == -1) s3 = finalLeggings.getType().getMaxDurability();
		if (s4 == -1) s4 = finalBoots.getType().getMaxDurability();

		if (this.getConfig().getString("kits." + className + ".items.helmetName") != null) {
		ItemMeta im = finalHelmet.getItemMeta();
		String name = this.getConfig().getString("kits." + className + ".items.helmetName");
		                         name = ChatColor.translateAlternateColorCodes('&', name);
		                         im.setDisplayName(name);
		finalHelmet.setItemMeta(im);
		}
		if (this.getConfig().getString("kits." + className + ".items.chestplateName") != null) {
		                         ItemMeta im = finalChestplate.getItemMeta();
		                         String name = this.getConfig().getString("kits." + className + ".items.chestplateName");
		                         name = ChatColor.translateAlternateColorCodes('&', name);
		                         im.setDisplayName(name);
		                         finalChestplate.setItemMeta(im);
		                     }
		if (this.getConfig().getString("kits." + className + ".items.leggingsName") != null) {
		                         ItemMeta im = finalLeggings.getItemMeta();
		                         String name = this.getConfig().getString("kits." + className + ".items.leggingsName");
		                         name = ChatColor.translateAlternateColorCodes('&', name);
		                         im.setDisplayName(name);
		                         finalLeggings.setItemMeta(im);
		                     }
		if (this.getConfig().getString("kits." + className + ".items.bootsName") != null) {
		                         ItemMeta im = finalBoots.getItemMeta();
		                         String name = this.getConfig().getString("kits." + className + ".items.bootsName");
		                         name = ChatColor.translateAlternateColorCodes('&', name);
		                         im.setDisplayName(name);
		                         finalBoots.setItemMeta(im);
		                     }

		                     if (s2 == -3) s2 = finalChestplate.getType().getMaxDurability();
		                     if (s3 == -3) s3 = finalLeggings.getType().getMaxDurability();
		                     if (s4 == -3) s4 = finalBoots.getType().getMaxDurability();

		if (finalHelmet != null && s1 != -2 && s1 != -3) finalHelmet.setDurability(s1); logger.info("Setting durability to " + s1);
		if (finalChestplate != null && s2 != -2 && s1 != -3) finalChestplate.setDurability(s2);
		if (finalLeggings != null && s3 != -2 && s1 != -3) finalLeggings.setDurability(s3);
		if (finalBoots != null && s4 != -2 && s1 != -3) finalBoots.setDurability(s4);




		if (this.getConfig().contains("kits." + className + ".items.helmetEnchant") && finalHelmet != null) {
		for (String a : this.getConfig().getString("kits." + className + ".items.helmetEnchant").split(" ")) {
		String[] enchant = a.split(":");
		Enchantment enchantmentInt = new EnchantmentWrapper(Integer.parseInt(enchant[0]));
		int levelInt = Integer.parseInt(enchant[1]);
		finalHelmet.addUnsafeEnchantment(enchantmentInt,levelInt);
		}
		}



		if (this.getConfig().contains("kits." + className + ".items.chestplateEnchant") && finalChestplate != null) {
		for (String a : this.getConfig().getString("kits." + className + ".items.chestplateEnchant").split(" ")) {
		String[] enchant = a.split(":");
		Enchantment enchantmentInt = new EnchantmentWrapper(Integer.parseInt(enchant[0]));
		int levelInt = Integer.parseInt(enchant[1]);
		finalChestplate.addUnsafeEnchantment(enchantmentInt,levelInt);
		}
		}

		if (this.getConfig().contains("kits." + className + ".items.leggingsEnchant") && finalLeggings != null) {
		for (String a : this.getConfig().getString("kits." + className + ".items.leggingsEnchant").split(" ")) {
		String[] enchant = a.split(":");
		Enchantment enchantmentInt = new EnchantmentWrapper(Integer.parseInt(enchant[0]));
		int levelInt = Integer.parseInt(enchant[1]);
		finalLeggings.addUnsafeEnchantment(enchantmentInt,levelInt);
		}
		}

		if (this.getConfig().contains("kits." + className + ".items.bootsEnchant") && finalBoots != null) {
		for (String a : this.getConfig().getString("kits." + className + ".items.bootsEnchant").split(" ")) {
		String[] enchant = a.split(":");
		Enchantment enchantmentInt = new EnchantmentWrapper(Integer.parseInt(enchant[0]));
		int levelInt = Integer.parseInt(enchant[1]);
		finalBoots.addUnsafeEnchantment(enchantmentInt,levelInt);
		}
		}

		if (finalHelmet != null) { player.getInventory().setHelmet(finalHelmet); }
		if (finalChestplate != null) { player.getInventory().setChestplate(finalChestplate); }
		if (finalLeggings != null) { player.getInventory().setLeggings(finalLeggings); }
		if (finalBoots != null) { player.getInventory().setBoots(finalBoots); }
		player.updateInventory();

		if (this.getConfig().contains(("kits." + className + ".commands"))) {
		List<String> commands = this.getConfig().getStringList("kits." + className + ".commands");

		for (String s : commands) {
		s = s.replace("<player>", player.getName());
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), s);
		}

		}
		//End of Armour
		
		//Start of Potion
		List<String> listPotions;
		listPotions = getConfig().getStringList("kits.Guard.potions");
		//logger.info(listPotions.get(1));
		int n = listPotions.size() -1;
		int i;
		for(i=0; i<=n ; i++) {
			String data = listPotions.get(i);
			String[] Potion = data.split(":");
			//logger.info(Potion[0]);
			//logger.info(Potion[1]);
			//logger.info("PotionEffectType."+Potion[0]);

			@SuppressWarnings("unused")
			String baseName = "null";
			
			switch (Potion[0].toLowerCase())
			{
			case "blindness":
			case "blind":
			potion = PotionEffectType.BLINDNESS;
			baseName = "blindness";
			break;
			case "nausea":
			case "confuse":
			case "confusion":
			potion = PotionEffectType.CONFUSION;
			baseName = "confusion";
			break;
			case "dmgresist":
			case "dr":
			case "damage_resistance":
			potion = PotionEffectType.DAMAGE_RESISTANCE;
			baseName = "damageresistance";
			break;
			case "haste":
			case "dig":
			case "fastdig":
			case "digspeed":
			potion = PotionEffectType.FAST_DIGGING;
			baseName = "haste";
			break;
			case "fire":
			case "fireresistance":
			case "fr":
			potion = PotionEffectType.FIRE_RESISTANCE;
			baseName = "fireresistance";
			break;
			case "harm":
			case "harming":
			case "hurt":
			potion = PotionEffectType.HARM;
			baseName = "harming";
			break;
			case "heal":
			case "healing":
			case "health":
			potion = PotionEffectType.HEAL;
			baseName = "healing";
			break;
			case "hunger":
			case "hungry":
			case "food":
			potion = PotionEffectType.HUNGER;
			baseName = "hunger";
			break;
			case "jump":
			case "highjump":
			case "jumpboost":
			potion = PotionEffectType.JUMP;
			baseName = "jumpboost";
			break;
			case "poison":
			potion = PotionEffectType.POISON;
			baseName = "poison";
			break;
			case "regen":
			case "regenration":
			potion = PotionEffectType.REGENERATION;
			baseName = "regeneration";
			break;
			case "slow":
			case "slowness":
			potion = PotionEffectType.SLOW;
			baseName = "slowness";
			break;
			case "speed":
			case "quick":
			case "swift":
			case "swiftness":
			potion = PotionEffectType.SPEED;
			baseName = "swiftness";
			break;
			case "increaseddamage":
			case "damage":
			case "strong":
			case "strength":
			potion = PotionEffectType.INCREASE_DAMAGE;
			baseName = "strength";
			break;
			case "waterbreathing":
			case "breathing":
			potion = PotionEffectType.WATER_BREATHING;
			baseName = "waterbreathing";
			break;
			case "weak":
			case "weakness":
			potion = PotionEffectType.WEAKNESS;
			baseName = "weakness";
			break;
			case "scare":
			case "freakout":
			potion = PotionEffectType.SLOW;
			baseName = "scare";
			break;
			case "flicker":
			case "dim":
			potion = PotionEffectType.BLINDNESS;
			baseName = "flicker";
			break;
			case "wither":
			case "witherboss":
			potion = PotionEffectType.WITHER;
			baseName = "wither";
			break; // TODO: 1.4
			case "invisible":
			case "invisibility":
			potion = PotionEffectType.INVISIBILITY;
			baseName = "invisibility";
			break; // TODO: 1.4
			case "nightvision":
			case "nv":
			potion = PotionEffectType.NIGHT_VISION;
			baseName = "nightvision";
			break; // TODO: 1.4
			default:
			potion = null;
			break;
			}
			
		    if (potion != null) {
		    	player.addPotionEffect(new PotionEffect(potion,Integer.MAX_VALUE,Integer.parseInt(Potion[1])));
		    }
		}
	//	String[] data = listPotions.toArray(new String[listPotions.size()]);
	}



	private void setOnDuty(Player player, Boolean silent){
		
		TagAPI.refreshPlayer(player);
		//Save Inventory Section
        getCustomConfig().set(player.getName() + ".inventory", player.getInventory().getContents());
        getCustomConfig().set(player.getName() + ".armor", player.getInventory().getArmorContents());
        //End of Section
        
        //Clear Inventory and Potion effects
		player.getInventory().clear();
		player.getInventory().setArmorContents(null);
		for (PotionEffect effect : player.getActivePotionEffects())
	        player.removePotionEffect(effect.getType());
        
        //Give Kit
		this.giveKit(player);
		player.sendMessage(prefix + "Your Guard Kit has been Issued. Visit the Guard room to restock");
        
        //Set Perm Group
        PermissionUser user = PermissionsEx.getUser(player);
        user.addGroup("Guard");
        
        //Announce
        if (silent) {
            player.sendMessage(prefix+"You Silently come on duty");
        } else {
        	player.sendMessage(prefix+"You are now on Duty");
        	for(Player all: getServer().getOnlinePlayers()) {
        		if (all != player){
        			all.sendMessage(prefix + "Guard " + player.getName() + " is now On Duty");
        		}
        	}
		}
	}
	


	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void setOffDuty(Player player, Boolean silent){
		
		TagAPI.refreshPlayer(player);
		
		//Clear Inventory and Potion Effects
		player.getInventory().clear();
		player.getInventory().setArmorContents(null);
		for (PotionEffect effect : player.getActivePotionEffects())
	        player.removePotionEffect(effect.getType());
		
		//Load Inventory Section
		Object a = getCustomConfig().get(player.getName() + ".inventory");
        Object b = getCustomConfig().get(player.getName() + ".armor");
        if(a == null || b == null){
            player.sendMessage(prefix +"No saved inventory to load");
            return;
        }
        ItemStack[] inventory = null;
        ItemStack[] armor = null;
        if (a instanceof ItemStack[]){
              inventory = (ItemStack[]) a;
        } else if (a instanceof List){
                List lista = (List) a;
                inventory = (ItemStack[]) lista.toArray(new ItemStack[0]);
        }
        if (b instanceof ItemStack[]){
                armor = (ItemStack[]) b;
          } else if (b instanceof List){
              List listb = (List) b;
              armor = (ItemStack[]) listb.toArray(new ItemStack[0]);
          }
        player.getInventory().clear();
        player.getInventory().setContents(inventory);
        player.getInventory().setArmorContents(armor);
        player.sendMessage(prefix+"Your Inventory was loaded");
        //End of Section
        
		//Set Perm Group
        PermissionUser user = PermissionsEx.getUser(player);
        user.removeGroup("Guard");
		
		//Announce
        if (silent) {
            player.sendMessage(prefix+"You Silently come off duty");
        } else {
        	player.sendMessage(prefix+"You are now off Duty");
        	for(Player all: getServer().getOnlinePlayers()) {
        		if (all != player){
        			all.sendMessage(prefix + "Guard " + player.getName() + " is now Off Duty");
        		}
        	}
		}
	}
	
    //Method from http://wiki.bukkit.org/Configuration_API_Reference
    public void reloadCustomConfig() {
        if (customConfigFile == null) {
        customConfigFile = new File(getDataFolder(), "PlayerInventory.yml");
        }
        customConfig = YamlConfiguration.loadConfiguration(customConfigFile);
 
        // Look for defaults in the jar
        InputStream defConfigStream = this.getResource("PlayerInventory.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            customConfig.setDefaults(defConfig);
        }
    }
 
    //Method from http://wiki.bukkit.org/Configuration_API_Reference
    public FileConfiguration getCustomConfig() {
        if (customConfig == null) {
            this.reloadCustomConfig();
        }
        return customConfig;
    }
 
    //Method from http://wiki.bukkit.org/Configuration_API_Reference
    public void saveCustomConfig() {
        if (customConfig == null || customConfigFile == null) {
        return;
        }
        try {
            getCustomConfig().save(customConfigFile);
        } catch (IOException ex) {
            this.getLogger().log(Level.SEVERE, "Could not save config to " + customConfigFile, ex);
        }
    }


	@Override
	public void onDisable() {
	    getConfig().set("onDuty", this.onDuty);
	    getConfig().set("help", this.help);
	    saveConfig();
		getLogger().info("Guards has been disabled");
		saveCustomConfig();
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}