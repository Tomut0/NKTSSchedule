import 'dotenv/config';
import * as XLSX from "xlsx/xlsx.js";
import { debug } from './utils.js';
import * as fs from "node:fs";

const pattern = /с ..-../;

export async function processDocument(url, dayOfWeek) {
    debug(`Reading a document from ${url}.`);
    // comment to debug
    // const buf = await ((await fetch(url)).arrayBuffer());

    // uncomment to debug
    const buf = fs.readFileSync('8_sentyabrya.xlsx');

    const workbook = XLSX.read(buf);

    let message = "📅 Новое расписание:\n===================\n";

    // get first sheet
    const worksheet = workbook.Sheets[workbook.SheetNames[0]];

    // get a column by groupName
    const groupColumn = getGroupColumn(worksheet);
    if (!groupColumn) {
        debug(`Can't find groupName!`);
        return;
    }

    debug(`Group column: ${groupColumn}.`);

    // Day height
    const rowStart = 7 * dayOfWeek - 2;
    const rowEnd = rowStart + 7;

    // Day width
    const colStart = groupColumn - 1;
    const colEnd = groupColumn + 2;

    // get day range by width and height
    const range = XLSX.utils.encode_range({ s: { r: rowStart, c: colStart }, e: { r: rowEnd, c: colEnd } });
    debug(`Day range is ${range}.`);

    // get data from range
    const dayRange = XLSX.utils.sheet_to_json(worksheet, { range: range });

    for (const dayRow of dayRange) {
        let day = Object.values(dayRow).filter(value => typeof value === 'string').map(value => value.trim()).filter(value => value != "");
        let cabinet = Object.values(dayRow).filter(value => typeof value === 'number').map(value => value += " каб.");

        day = day.concat(cabinet);

        if (day.length >= 2) {
            day[0] = "⏳" + day[0];
            const subject = day[1];

            if (pattern.test(subject)) {
                message += "Занятия начинаются " + subject;
            } else {
                message += day.join("\n-- ") + "\n";
            }

            message += "\n"
        }
    }

    // a daily schedule was found, but no lessons here
    if (message === "📅 Новое расписание:\n===================\n") {
        message = null;
    }

    return message;
}

function getGroupColumn(worksheet) {
    // Range of group names (ex.: КС-20-1, КС-20-3...)
    const groupsCellRange = XLSX.utils.decode_range("D5:CF5");

    for (let colNum = groupsCellRange.s.c; colNum < groupsCellRange.e.c; colNum++) {
        const cellAddress = XLSX.utils.encode_cell({ r: 4, c: colNum });
        const groupCell = worksheet[cellAddress];

        // if cell equals groupName (ex.: КС-20-2)
        if (groupCell && groupCell.v === process.env.RECEIVER_NAME) {
            return colNum;
        }
    }
}