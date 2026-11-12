package org.trivait.placechunk.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "placechunk")
public class PlaceChunkConfig implements ConfigData {
    public boolean modEnabled = true;
    public boolean onBreak = false;
    public boolean onPlace = true;
}
