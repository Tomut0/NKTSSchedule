package ru.tomut0.nkts.schedule;

import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.queries.messages.MessagesSendQuery;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class VkUtils {

    public static void addBannedWord(String word) {
        blackList.add(word);
    }

    public static List<String> getBlackList() {
        return blackList;
    }

    private static final List<String> blackList = new ArrayList<>();

    public static MessagesSendQuery formSendMessage(int receiverId, @NotNull String message) {
        return Main.getVk().messages().send(Main.getGroupActor()).
                peerId(receiverId).
                randomId((int) Math.floor(Math.random() * 10000000)).
                message(message);
    }

    public static void sendMessage(int receiverId, @NotNull String message) {
        try {
            formSendMessage(receiverId, message).execute();
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendMessage(int receiverId, @NotNull String attachment, @NotNull String message) {
        try {
            formSendMessage(receiverId, message).attachment(attachment).execute();
        } catch (ApiException | ClientException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendToReceiver(@NotNull String attachment, @NotNull String message) {
        int receiverId = Integer.parseInt(Main.getConfiguration().getProperty("RECEIVER_ID"));
        sendMessage(receiverId, attachment, message);
    }
}
