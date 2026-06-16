package io.papermc.testplugin;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.testplugin.trollyloki.ShapeCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class TestPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);

        // io.papermc.testplugin.brigtests.Registration.registerViaOnEnable(this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(ShapeCommand.createCommand(this).build());
        });
    }
}
