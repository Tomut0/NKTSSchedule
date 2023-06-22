package ru.tomut0.nkts.schedule.tasks;

import com.github.demidko.aot.WordformMeaning;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.ServiceActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.docs.Doc;
import com.vk.api.sdk.objects.wall.GetFilter;
import com.vk.api.sdk.objects.wall.WallpostAttachment;
import com.vk.api.sdk.objects.wall.WallpostFull;
import com.vk.api.sdk.objects.wall.responses.GetResponse;
import ru.tomut0.nkts.schedule.Main;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class UpdateWallTask implements Runnable {

    private final VkApiClient vk;
    private final ServiceActor actor;
    private final int wallId;

    private int postId;

    public UpdateWallTask(VkApiClient vk, ServiceActor actor, int wallId) {
        this.vk = vk;
        this.actor = actor;
        this.wallId = wallId;
    }

    private boolean lemmaEquals(WordformMeaning meaning, String word) {
        return Objects.equals(meaning.getLemma().toString(), word);
    }

    @Override
    public void run() {
        GetResponse response;

        Main.getLogger().info("Running scheduler...");

        try {
            response = vk.wall().get(actor).ownerId(wallId).count(2).filter(GetFilter.OWNER).execute();

            WallpostFull latest = response.getItems().get(0);

            // We don't need pinned posts, so, take next post
            if (latest.getIsPinned() == 1) {
                latest = response.getItems().get(1);
            }

            Main.getLogger().info("Response's latest id: " + latest.getId());

            // Do not process anything if latest post was already sent.
            if (latest.getId() == postId) {
                Main.getLogger().info("postID is the same, cancelling: " + postId);
                return;
            }

            postId = latest.getId();
            String text = latest.getText();
            String[] words = text.split("[ \n]");

            boolean hasTechnical = false, hasDepartment = false;

            for (String word : words) {
                // Speedup a search process
                if (!word.toLowerCase().startsWith("техн") && !word.toLowerCase().startsWith("отдел")) {
                    continue;
                }

                List<WordformMeaning> meanings = WordformMeaning.lookupForMeanings(word);

                for (WordformMeaning meaning : meanings) {
                    hasTechnical = hasTechnical || lemmaEquals(meaning, "технический");
                    hasDepartment = hasDepartment || lemmaEquals(meaning, "отделение");
                }
            }

            if (hasTechnical && hasDepartment) {
                List<WallpostAttachment> attachments = latest.getAttachments();

                for (WallpostAttachment attachment : attachments) {
                    Doc doc = attachment.getDoc();
                    URI uri = doc.getUrl();

                    Thread excelThread = new Thread(new SendXSSDataTask(uri, latest));
                    excelThread.start();
                }
            } else {
                Main.getLogger().info("Doesn't found keywords: " + Arrays.toString(words));
            }
        } catch (ApiException | ClientException e) {
            Main.getLogger().error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private File saveToFile(InputStream stream, Doc doc) throws IOException {
        String format = String.format("E:/JetBrains/apps/IDEA-U/Projects/NKTSShedule/%s", doc.getTitle());
        Path target = Path.of(format);
        Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
        return target.toFile();
    }

}
