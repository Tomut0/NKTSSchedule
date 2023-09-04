import {VK} from "vk-io";
import {HearManager} from '@vk-io/hear';
import 'dotenv/config';
import * as XSSReader from "./XSSReader.js";
import * as fs from "fs";

const vkApp = new VK({
    token: process.env.APP_ACCESS,
});

const vkGroup = new VK({
    token: process.env.GROUP_ACCESS,
});


const hearManager = new HearManager();

vkGroup.updates.on('message_new', hearManager.middleware);

hearManager.hear("!scheduleoff", async (context) => {
    if (context.senderId === parseInt(process.env.OWNER_ID)) {
        enabled = false;
        debug("Disabled!");
        await context.send("✅ Schedule turned off.");
    }
});

const messagePattern = /^!message (\d+) (.+)$/;
hearManager.hear(messagePattern, async (context) => {
    if (context.senderId === parseInt(process.env.OWNER_ID)) {
        debug(`Sending a message to ${process.env.RECEIVER_ID}.`);
        const match = context.text.match(messagePattern);
        if (match) {
            await vkGroup.api.messages.send({
                peer_id: match[1],
                message: match[2],
                random_id: Math.floor(Math.random() * 100000),
            });

            debug(`Message was successfully sent!`);
        }
    }
});


// 1 min on update
const updateTime = 1000 * 60;
setInterval(run, updateTime);

let postId;
let enabled = true;

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
    if (similarity > 0.55) {
        if (latestPost.attachments.length === 1 && latestPost.attachments[0].type === 'doc') {
            debug(`Post has an attachment, trying to process the document...`);
            const message = await XSSReader.processDocument(latestPost.attachments[0].doc.url);
            if (message) {
                await sendMessage(message, latestPost);
            }
        } else {
            await sendMessage(latestPost.text, latestPost);
        }
    }
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

async function sendMessage(text, latestPost) {
    debug(`Sending a message to ${process.env.RECEIVER_ID}.`);
    await vkGroup.api.messages.send({
        peer_id: process.env.OWNER_ID,
        message: text,
        random_id: Math.floor(Math.random() * 100000),
        attachment: `wall${latestPost.owner_id}_${latestPost.id}`
    });
    debug(`Message was successfully sent!`);
}

export function debug(message) {
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

run().catch(console.error)
vkGroup.updates.start().catch(console.error);