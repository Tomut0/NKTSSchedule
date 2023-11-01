import 'dotenv/config';
import * as XSSReader from './XSSReader.js';
import { debug, findSimilarity, sendMessage } from "./utils.js";
import { vkApp } from "./bootstrap.js";
import { enabled } from "./index.js";

const dateRegex = /(\d+)\s*([а-я]+)/i;
const russianMonths = [
    "января", "февраля", "марта", "апреля",
    "мая", "июня", "июля", "августа",
    "сентября", "октября", "ноября", "декабря"
];

let postId;

async function run() {
    if (!enabled) {
        return;
    }

    debug(`Running...`);
    const response = await vkApp.api.wall.get({
        owner_id: process.env.WALL_ID,
        count: 2
    });

    // if latest post was pinned, take next one
    let latestPost = response.items[0];
    if (response.items[0].is_pinned) {
        debug(`post ${latestPost.id} is pinned, picking the next one.`);
        latestPost = response.items[1];
    }

    if (latestPost.id === postId) {
        debug(`No new posts, latest: ${postId}`);
        return;
    }

    postId = latestPost.id;

    let similarity = findSimilarity(latestPost.text, "технического отделения");
    debug(`Similarity (${latestPost.text}) is ${similarity}`);
    if (similarity >= 0.55) {
        if (latestPost.attachments.length === 1 && latestPost.attachments[0].type === 'doc') {
            debug(`Post has an attachment, trying to process the document...`);

            const match = latestPost.text.match(dateRegex);
            if (!match) {
                return;
            }

            const day = parseInt(match[1]);
            const monthName = match[2].toLowerCase();
            const monthIndex = russianMonths.findIndex(month => month.toLowerCase() === monthName);
            const currentDate = new Date();

            if (isNaN(day) || monthIndex === -1) {
                return;
            }

            currentDate.setDate(day);
            currentDate.setMonth(monthIndex);

            const message = await XSSReader.processDocument(latestPost.attachments[0].doc.url, currentDate.getDay());
            if (message) {
                await sendMessage(message, latestPost);
            }
        } else {
            await sendMessage("Новое сообщение для технического отделения: ", latestPost);
        }
    }
}

export { run };