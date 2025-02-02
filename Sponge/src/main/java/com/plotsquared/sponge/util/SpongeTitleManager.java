package com.plotsquared.sponge.util;

import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.util.AbstractTitle;
import com.plotsquared.sponge.object.SpongePlayer;
import org.spongepowered.api.text.title.Title;

public class SpongeTitleManager extends AbstractTitle {

    @Override
    public void sendTitle(PlotPlayer player, String head, String sub, int in, int delay, int out) {
        Title title = Title.builder().title(SpongeUtil.getText(head)).subtitle(SpongeUtil.getText(sub)).fadeIn(in * 20).stay(delay * 20).fadeOut(out * 20).build();
        ((SpongePlayer) player).player.get().sendTitle(title);
    }
}
