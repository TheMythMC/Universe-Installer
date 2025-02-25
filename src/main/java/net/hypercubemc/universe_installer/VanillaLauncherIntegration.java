package net.hypercubemc.universe_installer;

import net.fabricmc.installer.util.Reference;
import net.fabricmc.installer.util.Utils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class VanillaLauncherIntegration {
    public static void installToLauncher(Path vanillaGameDir, Path instanceDir, String profileName, String gameVersion, String loaderName, String loaderVersion, Icon icon) throws IOException {
        String versionId = String.format("%s-%s-%s", loaderName, loaderVersion, gameVersion);

        installVersion(vanillaGameDir, gameVersion, loaderName, loaderVersion);
        installProfile(vanillaGameDir, instanceDir, profileName, versionId, icon);
    }

    public static void installVersion(Path mcDir, String gameVersion, String loaderName, String loaderVersion) throws IOException {
        System.out.println("Installing " + gameVersion + " with fabric " + loaderVersion);
        String versionId = String.format("%s-%s-%s", loaderName, loaderVersion, gameVersion);
        Path versionsDir = mcDir.resolve("versions");
        Path profileDir = versionsDir.resolve(versionId);
        Path profileJson = profileDir.resolve(versionId + ".json");
        if (!Files.exists(profileDir)) {
            Files.createDirectories(profileDir);
        }

        Path dummyJar = profileDir.resolve(versionId + ".jar");
        Files.deleteIfExists(dummyJar);
        Files.createFile(dummyJar);
        URL profileUrl = new URL(Reference.getMetaServerEndpoint(String.format("v2/versions/loader/%s/%s/profile/json", gameVersion, loaderVersion)));
        Utils.downloadFile(profileUrl, profileJson);
    }

    private static void installProfile(Path mcDir, Path instanceDir, String profileName, String versionId, Icon icon) throws IOException {
        Path launcherProfiles = mcDir.resolve("launcher_profiles.json");
        if (!Files.exists(launcherProfiles)) {
            System.out.println("Could not find launcher_profiles");
            return;
        }

        System.out.println("Creating profile");

        JSONObject jsonObject = new JSONObject(Utils.readString(launcherProfiles));

        JSONObject profiles = jsonObject.getJSONObject("profiles");

        String foundProfileName = profileName;

        for (Iterator<String> it = profiles.keys(); it.hasNext();) {
            String key = it.next();

            JSONObject foundProfile = profiles.getJSONObject(key);
            if (foundProfile.has("lastVersionId") && foundProfile.getString("lastVersionId").equals(versionId) && foundProfile.has("gameDir") && foundProfile.getString("gameDir").equals(instanceDir.toString())) {
                foundProfileName = key;
            }
        }

        // If the profile already exists, use it instead of making a new one so that user's settings are kept (e.g icon)
        JSONObject profile = profiles.has(foundProfileName) ? profiles.getJSONObject(foundProfileName) : createProfile(profileName, instanceDir, versionId, icon);
        profile.put("name", profileName);
        profile.put("lastUsed", Utils.ISO_8601.format(new Date())); // Update timestamp to bring to top of profile list
        profile.put("lastVersionId", versionId);

        profiles.put(foundProfileName, profile);
        jsonObject.put("profiles", profiles);

        Utils.writeToFile(launcherProfiles, jsonObject.toString());
    }

    private static JSONObject createProfile(String name, Path instanceDir, String versionId, Icon icon) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("type", "custom");
        jsonObject.put("created", Utils.ISO_8601.format(new Date()));
        jsonObject.put("gameDir", instanceDir.toString());
        jsonObject.put("lastUsed", Utils.ISO_8601.format(new Date()));
        jsonObject.put("lastVersionId", versionId);
        jsonObject.put("icon", getProfileIcon(icon));
        return jsonObject;
    }

    private static String getProfileIcon(Icon icon) {
        if (icon == Icon.FABRIC) {
            return Utils.getProfileIcon();
        }

        try {
            InputStream is = Utils.class.getClassLoader().getResourceAsStream("universe_profile_icon.png");

            String var4;
            try {
                byte[] ret = new byte[4096];
                int offset = 0;

                int len;
                while((len = is.read(ret, offset, ret.length - offset)) != -1) {
                    offset += len;
                    if (offset == ret.length) {
                        ret = Arrays.copyOf(ret, ret.length * 2);
                    }
                }

                var4 = "data:image/png;base64," + Base64.getEncoder().encodeToString(Arrays.copyOf(ret, offset));
            } catch (Throwable var6) {
                if (is != null) {
                    try {
                        is.close();
                    } catch (Throwable var5) {
                        var6.addSuppressed(var5);
                    }
                }

                throw var6;
            }

            if (is != null) {
                is.close();
            }

            return var4;
        } catch (IOException var7) {
            var7.printStackTrace();
            return "TNT";
        }
    }

    public enum Icon {
        UNIVERSE,
        FABRIC
    }
}
