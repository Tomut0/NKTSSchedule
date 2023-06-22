package ru.tomut0.nkts.schedule;

import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.client.actors.ServiceActor;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tomut0.nkts.schedule.tasks.UpdateWallTask;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;

public class Main {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static GroupActor groupActor;

    public static Logger getLogger() {
        return logger;
    }

    private static Logger logger;

    public static VkApiClient getVk() {
        return vk;
    }

    private static VkApiClient vk;

    public static Pattern fromClock = Pattern.compile("с ..-..");

    public static void main(String[] args) {
        logger = LoggerFactory.getLogger(Main.class.getSimpleName());
        logger.info("Initialization...");

        Properties configuration = loadConfiguration();
        TransportClient client = HttpTransportClient.getInstance();
        vk = new VkApiClient(client);

        logger.info("Retrieve configuration data...");
        int appId = Integer.parseInt(configuration.getProperty("APP_ID"));
        int wallId = Integer.parseInt(configuration.getProperty("WALL_ID"));
        int groupId = Integer.parseInt(configuration.getProperty("GROUP_ID"));
        String clientSecret = configuration.getProperty("CLIENT_SECRET");
        String clientAccess = configuration.getProperty("CLIENT_ACCESS");
        String groupAccess = configuration.getProperty("GROUP_ACCESS");

        ServiceActor actor = new ServiceActor(appId, clientSecret, clientAccess);
        groupActor = new GroupActor(groupId, groupAccess);

        // Debugging
        /*URI uri = new File("E:/JetBrains/apps/IDEA-U/Projects/NKTSShedule/june.xlsx").toURI();
        WallpostFull doc = new WallpostFull();
        doc.setOwnerId(-103163350);
        doc.setPostId(5021);
        Thread thread = new Thread(new sendXSSData(uri, doc));
        thread.start();*/

        scheduler.scheduleAtFixedRate(new UpdateWallTask(vk, actor, wallId), 0, 1, TimeUnit.MINUTES);
    }

    private static Properties loadConfiguration() {
        Properties properties = new Properties();


        try (InputStream stream = Main.class.getClassLoader().getResourceAsStream("config.properties")) {
            properties.load(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return properties;
    }

    public static GroupActor getGroupActor() {
        return groupActor;
    }
}