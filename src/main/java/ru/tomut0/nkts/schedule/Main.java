package ru.tomut0.nkts.schedule;

import com.sun.net.httpserver.HttpServer;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.client.actors.ServiceActor;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import org.apache.commons.codec.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tomut0.nkts.schedule.tasks.UpdateWallTask;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class Main {

    public static HttpServer getServer() {
        return server;
    }

    private static HttpServer server;

    public static ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    private static ScheduledExecutorService scheduler;
    private static GroupActor groupActor;
    private static VkHandler handler;

    public static Properties getConfiguration() {
        return configuration;
    }

    private static Properties configuration;

    public static Logger getLogger() {
        return logger;
    }

    private static Logger logger;

    public static VkApiClient getVk() {
        return vk;
    }

    private static VkApiClient vk;

    public static Pattern fromClock = Pattern.compile("с ..-..");

    public static void main(String[] args) throws IOException {
        logger = LoggerFactory.getLogger(Main.class.getSimpleName());
        logger.info("Initialization...");
        scheduler = Executors.newScheduledThreadPool(1);

        server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/", new ScheduleServer());
        server.start();

        // Initial banned word
        VkUtils.addBannedWord("ПП");

        configuration = loadConfiguration();
        TransportClient client = HttpTransportClient.getInstance();
        vk = new VkApiClient(client);

        logger.info("Retrieve configuration data...");
        int appId = Integer.parseInt(configuration.getProperty("APP_ID"));
        int wallId = Integer.parseInt(configuration.getProperty("WALL_ID"));
        int groupId = Integer.parseInt(configuration.getProperty("GROUP_ID"));
        String clientSecret = configuration.getProperty("APP_SECRET");
        String clientAccess = configuration.getProperty("APP_ACCESS");
        String groupAccess = configuration.getProperty("GROUP_ACCESS");

        configuration.stringPropertyNames().forEach(property -> logger.info(property + ": " + configuration.getProperty(property)));

        ServiceActor actor = new ServiceActor(appId, clientSecret, clientAccess);
        groupActor = new GroupActor(groupId, groupAccess);

        if (args.length > 0 && Objects.equals(args[0], "-m")) {
            VkUtils.sendMessage(Integer.parseInt(args[1]), args[2]);
        } else {
            scheduler.scheduleAtFixedRate(new UpdateWallTask(vk, actor, wallId), 0, 1, TimeUnit.MINUTES);
        }

        // Debugging
        /*URI uri = new File("E:/JetBrains/apps/IDEA-U/Projects/NKTSShedule/june.xlsx").toURI();
        WallpostFull doc = new WallpostFull();
        doc.setOwnerId(-103163350);
        doc.setPostId(5021);
        Thread thread = new Thread(new sendXSSData(uri, doc));
        thread.start();*/
    }

    private static Properties loadConfiguration() {
        Properties properties = new Properties();

        // Load from file or resource
        try (FileReader fileReader = new FileReader("config.properties", Charsets.UTF_8)) {
            properties.load(fileReader);
        } catch (IOException ex) {
            loadResourceConfiguration(properties);
        }

        return properties;
    }

    private static void loadResourceConfiguration(Properties properties) {
        try (InputStream stream = Main.class.getClassLoader().getResourceAsStream("config.example.properties");
             InputStreamReader reader = new InputStreamReader(Objects.requireNonNull(stream), StandardCharsets.UTF_8)) {
            properties.load(reader);

            try (FileWriter writer = new FileWriter("config.example.properties", StandardCharsets.UTF_8)) {
                properties.store(writer, "Configuration file");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static GroupActor getGroupActor() {
        return groupActor;
    }

    public static VkHandler getHandler() {
        return handler;
    }
}