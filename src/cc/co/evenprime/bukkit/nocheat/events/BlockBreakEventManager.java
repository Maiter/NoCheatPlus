package cc.co.evenprime.bukkit.nocheat.events;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.plugin.PluginManager;

import cc.co.evenprime.bukkit.nocheat.NoCheat;
import cc.co.evenprime.bukkit.nocheat.checks.blockbreak.BlockBreakCheck;
import cc.co.evenprime.bukkit.nocheat.config.Permissions;
import cc.co.evenprime.bukkit.nocheat.config.cache.ConfigurationCache;
import cc.co.evenprime.bukkit.nocheat.data.BlockBreakData;
import cc.co.evenprime.bukkit.nocheat.debug.Performance;
import cc.co.evenprime.bukkit.nocheat.debug.PerformanceManager.Type;

/**
 * Central location to listen to player-interact events and dispatch them to
 * relevant checks
 * 
 * @author Evenprime
 * 
 */
public class BlockBreakEventManager extends BlockListener implements EventManager {

    private final BlockBreakCheck blockBreakCheck;
    private final NoCheat         plugin;
    private final Performance     blockBreakPerformance;
    private final Performance     blockDamagePerformance;


    public BlockBreakEventManager(NoCheat plugin) {

        this.plugin = plugin;
        this.blockBreakCheck = new BlockBreakCheck(plugin);
        this.blockBreakPerformance = plugin.getPerformanceManager().get(Type.BLOCKBREAK);
        this.blockDamagePerformance = plugin.getPerformanceManager().get(Type.BLOCKDAMAGE);

        PluginManager pm = Bukkit.getServer().getPluginManager();

        pm.registerEvent(Event.Type.BLOCK_BREAK, this, Priority.Lowest, plugin);
        pm.registerEvent(Event.Type.BLOCK_DAMAGE, this, Priority.Monitor, plugin);
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {

        if(event.isCancelled()) {
            return;
        }

        // Performance counter setup
        long nanoTimeStart = 0;
        final boolean performanceCheck = blockBreakPerformance.isEnabled();

        if(performanceCheck)
            nanoTimeStart = System.nanoTime();

        final Player player = event.getPlayer();
        final ConfigurationCache cc = plugin.getConfigurationManager().getConfigurationCacheForWorld(player.getWorld().getName());

        // Find out if checks need to be done for that player
        if(cc.blockbreak.check && !player.hasPermission(Permissions.BLOCKBREAK)) {

            boolean cancel = false;

            // Get the player-specific stored data that applies here
            final BlockBreakData data = plugin.getDataManager().getData(player).blockbreak;

            cancel = blockBreakCheck.check(player, event.getBlock(), data, cc);

            if(cancel) {
                event.setCancelled(true);
            }
        }

        // store performance time
        if(performanceCheck)
            blockBreakPerformance.addTime(System.nanoTime() - nanoTimeStart);
    }

    @Override
    public void onBlockDamage(BlockDamageEvent event) {

        // Only interested in insta-break events
        if(!event.isCancelled() && !event.getInstaBreak()) {
            return;
        }
        
        // Performance counter setup
        long nanoTimeStart = 0;
        final boolean performanceCheck = blockDamagePerformance.isEnabled();
        
        if(performanceCheck)
            nanoTimeStart = System.nanoTime();

        final Player player = event.getPlayer();
        // Get the player-specific stored data that applies here
        final BlockBreakData data = plugin.getDataManager().getData(player).blockbreak;

        // Remember this location. We ignore block breaks in the block-break
        // direction check that are insta-breaks
        data.instaBrokeBlockLocation = event.getBlock().getLocation();
        
        // store performance time
        if(performanceCheck)
            blockDamagePerformance.addTime(System.nanoTime() - nanoTimeStart);
    }

    public List<String> getActiveChecks(ConfigurationCache cc) {
        LinkedList<String> s = new LinkedList<String>();

        if(cc.blockbreak.check && cc.blockbreak.directionCheck)
            s.add("blockbreak.direction");
        if(cc.blockbreak.check && cc.blockbreak.reachCheck)
            s.add("blockbreak.reach");

        return s;
    }

    public List<String> getInactiveChecks(ConfigurationCache cc) {
        LinkedList<String> s = new LinkedList<String>();

        if(!(cc.blockbreak.check && cc.blockbreak.directionCheck))
            s.add("blockbreak.direction");
        if(!(cc.blockbreak.check && cc.blockbreak.reachCheck))
            s.add("blockbreak.reach");

        return s;
    }
}