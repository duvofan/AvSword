package org.duvo.avsword;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;

public class UpdateChecker {

    private final AvSword plugin;
    private final String projectSlug;

    public UpdateChecker(AvSword plugin, String projectSlug) {
        this.plugin = plugin;
        this.projectSlug = projectSlug;
    }

    public void getVersion(final Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {

                URL url = new URL("https://api.modrinth.com/v2/project/" + this.projectSlug + "/version");



                HttpURLConnection connection = (HttpURLConnection) url.openConnection();


                connection.setRequestMethod("GET");


                connection.setRequestProperty("User-Agent", "Duvo/AvSword/" + plugin.getDescription().getVersion());

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStreamReader reader = new InputStreamReader(connection.getInputStream());

                    JsonElement element = new JsonParser().parse(reader);

                    if (element.isJsonArray()) {
                        JsonArray versions = element.getAsJsonArray();
                        if (versions.size() > 0) {

                            String latestVersion = versions.get(0).getAsJsonObject().get("version_number").getAsString();
                            consumer.accept(latestVersion);
                        }
                    }
                    reader.close();
                } else {
                    plugin.getLogger().warning("Modrinth API'ye bağlanırken hata oluştu. Yanıt kodu: " + connection.getResponseCode());
                }
            } catch (Exception exception) {
                plugin.getLogger().warning("Modrinth güncelleme kontrolü yapılamadı: " + exception.getMessage());
            }
        });
    }
}