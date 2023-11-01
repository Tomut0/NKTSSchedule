import 'dotenv/config';
import * as fs from "fs";
import { vkGroup } from "./bootstrap.js";

function debug(message) {
    const currentDate = new Date();
    const day = currentDate.getDate().toString().padStart(2, '0');
    const month = (currentDate.getMonth() + 1).toString().padStart(2, '0'); // Months are zero-indexed, so we add 1.
    const year = currentDate.getFullYear();
    const hours = currentDate.getHours().toString().padStart(2, '0');
    const minutes = currentDate.getMinutes().toString().padStart(2, '0');
    const seconds = currentDate.getSeconds().toString().padStart(2, '0');

    const formattedDate = `[${day}-${month}-${year} ${hours}:${minutes}:${seconds}]`;
    const debugMessage = `${formattedDate} ${message}`;

    fs.appendFile('debug.log', debugMessage + '\n', (err) => {
        if (err) {
            console.error('Error writing to the file:', err);
        }
    });

    console.debug(debugMessage);
}

async function sendMessage(text, latestPost) {
    debug(`Sending a message to ${process.env.RECEIVER_ID}.`);
    await vkGroup.api.messages.send({
        peer_id: process.env.RECEIVER_ID,
        message: text,
        random_id: Math.floor(Math.random() * 100000),
        attachment: `wall${latestPost.owner_id}_${latestPost.id}`
    });
    debug(`Message was successfully sent!`);
}

function findSimilarity(first, second) {
    // Convert the words to sets of characters
    const set1 = new Set(first);
    const set2 = new Set(second);

    // Calculate the intersection and union of the sets
    const intersection = new Set([...set1].filter(char => set2.has(char)));
    const union = new Set([...set1, ...set2]);

    // Calculate the Jaccard similarity coefficient
    return intersection.size / union.size;
}

export {debug, sendMessage, findSimilarity};