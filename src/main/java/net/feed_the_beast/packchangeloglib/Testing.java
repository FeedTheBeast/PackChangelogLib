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
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Testing {
    private static int pyramid_project_id = 290013;
    private static int pyramid_201 = 2542772;
    private static int pyramid_210 = 2544116;
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

    public static PackHolder getVersion (AddOnService svc, int pack, int version) throws IOException {
        AddOn a = svc.getAddOn(pyramid_project_id);
        AddOnFile af = svc.getAddOnFile(pack, version);
        if (a != null && af != null) {
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
            return ph;
        }
        return null;
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

    public static FileData getFilesInPackManifest (AddOnService svc, PackHolder pack) throws RemoteException {
        List<AddOnFile> out = Lists.newArrayList();
        List<ManifestResource> packmods = pack.getMinecraftModpackManifest().files;
        List<Integer> mods = Lists.newArrayList();
        List<AddOnFileKey> files = Lists.newArrayList();
        System.out.println("pack " + pack.getFile().getFileName());
        for (ManifestResource mf : packmods) {
            files.add(mf.toAddOnFileKey());
            mods.add(mf.projectID);
        }
        AddOnFileKey[] afk = packmods.stream().map(mf -> mf.toAddOnFileKey()).toArray(AddOnFileKey[]::new);
        List<AddOn> packaddons = svc.v2GetAddOns(mods.stream().mapToInt(i -> i).toArray());
        for (AddOn a : packaddons) {
            System.out.println(a.getName() + " " + a.getId());
        }
        Int2ObjectMap<List<AddOnFile>> packfiles = svc.getAddOnFiles(afk);
        for (Map.Entry<Integer, List<AddOnFile>> f : packfiles.entrySet()) {
            for (AddOnFile af : f.getValue()) {
                out.add(af);
                System.out.println(af.getFileNameOnDisk() + " " + af.getId() + " " + af.getFileDate().getTime().toString());
            }
        }
        FileData fd = new FileData();
        fd.setFiles(out);
        fd.setMappings(afk);
        return fd;
    }

    public static void main (String args[]) {
        try {
            FileUtils.deleteDirectory(new File("tmp"));
            packs = Lists.newArrayList();
            CurseToken token = getToken();
            AddOnService svc = AddOnService.initialise(token);
            packs.add(getVersion(svc, pyramid_project_id, pyramid_201));
            packs.add(getVersion(svc, pyramid_project_id, pyramid_210));
            //packs.addAll(getLatestVersions(svc, pyramid_project_id));
            FileData files0 = getFilesInPackManifest(svc, packs.get(0));
            Collections.sort(files0.getFiles(), (AddOnFile a1, AddOnFile a2) -> a1.getFileName().compareTo(a2.getFileName()));

            System.out.println("\n\n\n\n\n\n\n");
            FileData files1 = getFilesInPackManifest(svc, packs.get(1));
            Collections.sort(files1.getFiles(), (AddOnFile a1, AddOnFile a2) -> a1.getFileName().compareTo(a2.getFileName()));
            Javers javers = JaversBuilder.javers().build();
            System.out.println("\n\n\n\n\n\n\n");
            for (AddOnFile fl : files0.getFiles()) {
                Optional<AddOnFile> other = files1.getFiles().stream().parallel().filter(f -> f.getId() == fl.getId()).findFirst();
                if (other.isPresent()) {
                    Diff diff = javers.compare(fl, other.get());
                    if (diff.getChanges().size() > 0) {
                        System.out.println(diff.prettyPrint());
                    }
                } else {
                    int id = -1;
                    int otherflid = -1;
                    for (AddOnFileKey afk : files0.getMappings()) {
                        if (afk.getFileID() == fl.getId()) {
                            id = afk.getAddOnID();
                        }
                    }
                    for (AddOnFileKey afk : files1.getMappings()) {
                        if (id == afk.getAddOnID()) {
                            otherflid = afk.getFileID();
                        }
                    }
                    if (otherflid > 0) {
                        final int otid = otherflid;
                        Optional<AddOnFile> otm = files1.getFiles().stream().parallel().filter(f -> f.getId() == otid).findFirst();
                        if (otm.isPresent()) {
                            System.out.println(fl.getFileName() + " has been updated to " + otm.get().getFileName());
                            //Diff diff = javers.compare(fl, otm.get());
                            //if (diff.getChanges().size() > 0) {
                            //    System.out.println(diff.prettyPrint());
                            //}
                        }
                    } else {
                        System.out.println(fl.getFileName() + " " + fl.getId() + " has been removed");
                    }
                }
            }
            for (AddOnFile fl : files1.getFiles()) {
                Optional<AddOnFile> other = files0.getFiles().stream().parallel().filter(f -> f.getId() == fl.getId()).findFirst();
                if (!other.isPresent()) {
                    {
                        int id = -1;
                        int otherflid = -1;
                        for (AddOnFileKey afk : files1.getMappings()) {
                            if (afk.getFileID() == fl.getId()) {
                                id = afk.getAddOnID();
                            }
                        }
                        for (AddOnFileKey afk : files0.getMappings()) {
                            if (id == afk.getAddOnID()) {
                                otherflid = afk.getFileID();
                            }
                        }
                        if (otherflid < 0) {
                            System.out.println(fl.getFileName() + " " + fl.getId() + " has been added");
                        }
                    }
                }
            }

            //Diff diff = javers.compareCollections(files0.getFiles(), files1.getFiles(), AddOnFile.class);
            System.out.println("\n\n\n\n\n\n\n");
            System.out.println(files0.getFiles().size() + " " + files1.getFiles().size());
            // System.out.println(diff);

        } catch (CurseAuthException | IOException e) {
            e.printStackTrace();
        }
    }
}