import 'dotenv/config';
import { HearManager } from '@vk-io/hear';
import { vkGroup } from './bootstrap.js';
import { debug } from './utils.js';
import { setEnabled } from './index.js';

const hearManager = new HearManager();
const messagePattern = /^!message (\d+) (.+)$/;

vkGroup.updates.on('message_new', hearManager.middleware);

hearManager.hear("!scheduleoff", async (context) => {
    if (context.senderId === parseInt(process.env.OWNER_ID)) {
        setEnabled(false);
        debug("Disabled!");
        await context.send("✅ Schedule has been turned off.");
    }
});

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

vkGroup.updates.start().catch(console.error);