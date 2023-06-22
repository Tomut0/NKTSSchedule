package ru.tomut0.nkts.schedule.tasks;


import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.wall.WallpostFull;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import ru.tomut0.nkts.schedule.Main;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class SendXSSDataTask implements Runnable {

    private final URI uri;
    private final WallpostFull latest;

    public SendXSSDataTask(URI uri, WallpostFull latest) {
        this.uri = uri;
        this.latest = latest;
    }

    @Override
    public void run() {
        Main.getLogger().info("Found an attachment, trying to open...");

        try (InputStream inputStream = uri.toURL().openStream(); ReadableWorkbook wb = new ReadableWorkbook(inputStream)) {
            Optional<Sheet> sheet = wb.getSheet(0);
            if (sheet.isEmpty()) {
                return;
            }

            LocalDate date = LocalDate.now();
            DayOfWeek dayOfWeek = date.getDayOfWeek();

            List<Row> rows = sheet.get().read();
            Row groupsRow = rows.get(4);

            StringBuilder message = new StringBuilder("\uD83D\uDCC5 Расписание на сегодня:\n");

            for (Cell groups : groupsRow) {
                if (!groups.getText().equals("КС 20-3")) {
                    continue;
                }

                int daySize = 7 * (dayOfWeek.getValue() - 1);

                for (int i = daySize + 1; i < daySize + 7; i++) {
                    int rowBegin = 5 + i; // `5` is padding from sheet's top

                    Row cells = rows.get(rowBegin);
                    var cellIdStart = (groups.getColumnIndex() - 1);
                    var cellIdEnd = (groups.getColumnIndex() + 3);
                    List<String> hourCells = cells.getCells(cellIdStart, cellIdEnd).stream().
                            map(Cell::getText).
                            filter(s -> !s.isBlank()).
                            map(String::trim).
                            toList();

                    if (hourCells.size() > 4 || hourCells.size() < 2) {
                        continue;
                    }

                    String subject = hourCells.get(1);

                    if (subject.isEmpty()) {
                        continue;
                    }

                    if (Main.fromClock.matcher(subject).matches()) {
                        message.append("Занятия начинаются ").append(subject).append("!\n");
                        continue;
                    }

                    String joinedHours = String.join(" - ", hourCells.stream().filter(s -> !s.isBlank()).toList());

                    message.append(joinedHours).append("\n");
                }

                Main.getLogger().info("Sending a message...");
                // Send message
                Main.getVk().messages().send(Main.getGroupActor()).
                        userId(184396723).
                        randomId(0).
                        attachment(String.format("wall%d_%d", latest.getOwnerId(), latest.getId())).
                        message(message.toString()).
                        execute();
            }
        } catch (IOException | ApiException | ClientException e) {
            Main.getLogger().error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
