package ru.tomut0.nkts.schedule;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.vk.api.sdk.objects.callback.messages.CallbackMessage;

import java.io.*;
import java.util.Objects;

public class ScheduleServer implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        byte[] response = "ok".getBytes();
        exchange.sendResponseHeaders(200, response.length);

        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(response);
        outputStream.close();


        String requestBody = convertStreamToString(exchange.getRequestBody());
        CallbackMessage callbackMessage = Main.getVk().getGson().fromJson(requestBody, CallbackMessage.class);

        JsonObject message = callbackMessage.getObject().get("message").getAsJsonObject();
        JsonElement text = message.get("text");
        JsonElement peerId = message.get("peer_id");

        if (text.isJsonNull() || peerId.isJsonNull()) {
            return;
        }

        int peer = peerId.getAsInt();
        String msg = text.getAsString();

        // Commands should work only on my id
        if (!Objects.equals(peerId.getAsString(), Main.getConfiguration().getProperty("OWNER_ID"))) {
            return;
        }

        if (msg.contains("scheduleoff")) {
            Main.getScheduler().shutdownNow();
            VkUtils.sendMessage(peer, "✅ Schedule turned off.");
            Main.getServer().stop(5);
        }

        if (msg.contains("blacklistadd")) {
            String replace = msg.replace("blacklistadd ", "");
            VkUtils.sendMessage(peer, "✅ Blacklist word has been added.");
            VkUtils.getBlackList().add(replace);
        }
    }


    private static String convertStreamToString(InputStream inputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }

        return stringBuilder.toString();
    }
}
