import * as XLSX from "xlsx/xlsx.js";
import {debug} from "./index.js";
import * as fs from "fs";

const pattern = /с ..-../;

// Needs if in one day there are 2 new schedules was released
let shedulesInDay = 0;
let today = new Date().getDate();

export async function processDocument(url) {
    debug(`Reading a document from ${url}.`);
    const buf = await ((await fetch(url)).arrayBuffer());

    // uncomment to debug
    // const buf = fs.readFileSync('7_sentyabrya.xlsx');

    const workbook = XLSX.read(buf);

    let message = "📅 Новое расписание:\n";

    shedulesInDay += 1;

    // get first sheet
    const worksheet = workbook.Sheets[workbook.SheetNames[0]];

    // get a column by groupName
    const groupColumn = getGroupColumn(worksheet);
    if (!groupColumn) {
        debug(`Can't find groupName!`);
        return;
    }

    debug(`Group column: ${groupColumn}.`);

    const date = new Date();
    const dayOfWeek = date.getDay();

    // Resets `schedulesInDay` when a new day comes
    if (today != date.getDate()) {
        shedulesInDay = 0;
        today = date.getDate();
    }

    debug(`DayOfWeek: ${dayOfWeek}`);
    debug(`SchedulesInDay: ${shedulesInDay}`);

    // Day height
    const rowStart = 7 * (dayOfWeek + shedulesInDay) - 1;
    const rowEnd = rowStart + 6;

    // Day width
    const colStart = groupColumn - 1;
    const colEnd = groupColumn + 2;

    // get day range by width and height
    const range = XLSX.utils.encode_range({s: {r: rowStart, c: colStart}, e: {r: rowEnd, c: colEnd}});
    debug(`Day range is ${range}.`);

    // get data from range
    const dayRange = XLSX.utils.sheet_to_json(worksheet, {range: range});

    for (const dayRow of dayRange) {
        if (Object.keys(dayRow).length > 1) {
            const subject = Object.values(dayRow)[1];

            if (pattern.test(Object.values(dayRow)[1])) {
                message += "Занятия начинаются " + subject;
            } else {
                message += Object.values(dayRow).join(" - ") + "\n";
            }
        }
    }

    return message;
}

function getGroupColumn(worksheet) {
    // Range of group names (ex.: КС-20-1, КС-20-3...)
    const groupsCellRange = XLSX.utils.decode_range("D5:CF5");

    for (let colNum = groupsCellRange.s.c; colNum < groupsCellRange.e.c; colNum++) {
        const cellAddress = XLSX.utils.encode_cell({r: 4, c: colNum});
        const groupCell = worksheet[cellAddress];

        // if cell equals groupName (ex.: КС-20-2)
        if (groupCell && groupCell.v === process.env.RECEIVER_NAME) {
            return colNum;
        }
    }
}
