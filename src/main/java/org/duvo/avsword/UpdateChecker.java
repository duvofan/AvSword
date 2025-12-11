package org.duvo.avsword;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
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
                URL url = new URI("https://api.modrinth.com/v2/project/" + this.projectSlug + "/version").toURL();

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                connection.setRequestProperty("User-Agent", "Duvo/AvSword/" + plugin.getDescription().getVersion());

                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                        JsonElement element = new JsonParser().parse(reader);

                        if (element != null && element.isJsonArray()) {
                            JsonArray versions = element.getAsJsonArray();
                            if (versions.size() > 0) {
                                String latestVersion = versions.get(0).getAsJsonObject().get("version_number").getAsString();
                                consumer.accept(latestVersion);
                            }
                        }
                    }
                } else if (responseCode == 404) {
                    plugin.getLogger().warning("Modrinth projesi bulunamadı (404). Proje ID'si yanlış olabilir: " + this.projectSlug);
                } else {
                    plugin.getLogger().warning("Güncelleme kontrolü başarısız. API kodu: " + responseCode);
                }

            } catch (Exception exception) {
                plugin.getLogger().warning("Güncelleme kontrolü yapılamadı: " + exception.getMessage());
            }
        });
    }
}