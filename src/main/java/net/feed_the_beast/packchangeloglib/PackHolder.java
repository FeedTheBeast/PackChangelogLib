package net.feed_the_beast.packchangeloglib;

import addons.curse.AddOnFile;
import com.thiakil.curseapi.json.manifests.MinecraftModpackManifest;
import lombok.Data;
import addons.curse.AddOn;

import java.io.File;

@Data
public class PackHolder {
    private AddOn addon;
    private AddOnFile file;
    private File localLocation;
    private MinecraftModpackManifest minecraftModpackManifest;
}
