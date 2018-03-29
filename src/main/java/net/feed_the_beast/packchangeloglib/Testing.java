package net.feed_the_beast.packchangeloglib;

import addons.curse.AddOn;
import addons.curse.AddOnFile;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.thiakil.curseapi.json.manifests.ManifestResource;
import com.thiakil.curseapi.json.manifests.MinecraftModpackManifest;
import com.thiakil.curseapi.login.CurseAuth;
import com.thiakil.curseapi.login.CurseAuthException;
import com.thiakil.curseapi.login.CurseToken;
import com.thiakil.curseapi.login.OauthPopup;
import com.thiakil.curseapi.soap.AddOnService;
import com.thiakil.twitch.TwitchOAuth;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.datacontract.schemas._2004._07.curse_addonservice_requests.AddOnFileKey;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Testing {
    private static int pyramid_project_id = 290013;
    private static Gson GSON = new Gson();
    private static List<PackHolder> packs;

    private static void unzipFile (File zip, String outputDir) throws IOException {
        java.util.zip.ZipFile zipFile = new ZipFile(zip);
        try {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File entryDestination = new File(outputDir, entry.getName());
                if (entry.isDirectory()) {
                    entryDestination.mkdirs();
                } else {
                    entryDestination.getParentFile().mkdirs();
                    InputStream in = zipFile.getInputStream(entry);
                    OutputStream out = new FileOutputStream(entryDestination);
                    IOUtils.copy(in, out);
                    IOUtils.closeQuietly(in);
                    out.close();
                }
            }
        } finally {
            zipFile.close();
        }
    }

    public static String getManifest (String location) throws IOException {
        return FileUtils.readFileToString(new File(location), StandardCharsets.UTF_8);
    }

    public static List<PackHolder> getLatestVersions (AddOnService svc, int pack) throws IOException {
        List<PackHolder> out = Lists.newArrayList();
        AddOn a = svc.getAddOn(pyramid_project_id);
        if (a != null) {
            for (AddOnFile af : a.getLatestFiles()) {
                File fl = new File("tmp/" + af.getFileNameOnDisk());
                FileUtils.copyURLToFile(new URL(af.getDownloadURL()), fl);
                unzipFile(fl, "tmp/" + af.getFileNameOnDisk().replace(".zip", ""));
                System.out.println(af.getFileName() + " " + af.getFileNameOnDisk() + " " + af.getDownloadURL());
                MinecraftModpackManifest mf = GSON.fromJson(getManifest("tmp/" + af.getFileNameOnDisk().replace(".zip", "") + "/manifest.json"), MinecraftModpackManifest.class);
                PackHolder ph = new PackHolder();
                ph.setAddon(a);
                ph.setFile(af);
                ph.setLocalLocation(new File("tmp/" + af.getFileNameOnDisk().replace(".zip", "")));
                ph.setMinecraftModpackManifest(mf);
                out.add(ph);
            }

        }
        return out;
    }

    public static CurseToken getToken () throws CurseAuthException, IOException {
        if (new File("cursetoken.json").exists()) {
            return GSON.fromJson(FileUtils.readFileToString(new File("cursetoken.json"), StandardCharsets.UTF_8), CurseToken.class);
        }
        String oauthurl = TwitchOAuth.getAuthURL();
        String oauthcode = OauthPopup.getCode(oauthurl);
        if (oauthcode == null || oauthcode.isEmpty()) {//backup method, javafx doesn't play nice w/ the captcha's
            System.out.println("paste the code here from: " + oauthurl);
            final Scanner in = new Scanner(System.in, "UTF-8");
            oauthcode = in.nextLine();
            System.out.println();

        }
        CurseToken token = CurseAuth.getTokenFromTwitchOauth(oauthcode);
        String tokenstr = GSON.toJson(token);
        FileUtils.writeStringToFile(new File("cursetoken.json"), tokenstr, StandardCharsets.UTF_8);
        return token;
    }

    public static void main (String args[]) {
        try {
            FileUtils.deleteDirectory(new File("tmp"));
            packs = Lists.newArrayList();
            CurseToken token = getToken();
            AddOnService svc = AddOnService.initialise(token);
            packs.addAll(getLatestVersions(svc, pyramid_project_id));
            System.out.println("pack 0 " + packs.get(0).getFile().getFileName());
            List<ManifestResource> pack0mods = packs.get(0).getMinecraftModpackManifest().files;
            List<Integer> mods0 = Lists.newArrayList();
            List<AddOnFileKey> files0 = Lists.newArrayList();
            List<Integer> mods1 = Lists.newArrayList();
            List<AddOnFileKey> files1 = Lists.newArrayList();

            for (ManifestResource mf : pack0mods) {
                files0.add(mf.toAddOnFileKey());
                mods0.add(mf.projectID);
            }
            List<AddOn> pack0addons = svc.v2GetAddOns(mods0.stream().mapToInt(i -> i).toArray());
            for (AddOn a : pack0addons) {
                System.out.println(a.getName() + " " + a.getId());
            }
            Int2ObjectMap<List<AddOnFile>> pack0files = svc.getAddOnFiles((AddOnFileKey [])files0.stream().toArray());
            for (AddOnFile f: pack0files.defaultReturnValue()) {
                System.out.println(f.getFileNameOnDisk() + " " + f.getFileDate());
            }
            System.out.println("pack 1 " + packs.get(1).getFile().getFileName());
            List<ManifestResource> pack1mods = packs.get(1).getMinecraftModpackManifest().files;
            for (ManifestResource mf : pack1mods) {
                files1.add(mf.toAddOnFileKey());
                mods1.add(mf.projectID);
            }
            List<AddOn> pack1addons = svc.v2GetAddOns(mods1.stream().mapToInt(i -> i).toArray());
            for (AddOn a : pack1addons) {
                System.out.println(a.getName() + " " + a.getId());
            }
            Int2ObjectMap<List<AddOnFile>> pack1files = svc.getAddOnFiles((AddOnFileKey [])files1.stream().toArray());
            for (AddOnFile f: pack1files.defaultReturnValue()) {
                System.out.println(f.getFileNameOnDisk() + " " + f.getFileDate());
            }

        } catch (CurseAuthException | IOException e) {
            e.printStackTrace();
        }
    }
}