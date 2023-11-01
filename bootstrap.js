import 'dotenv/config';
import { VK } from "vk-io";

const vkApp = new VK({
    token: process.env.APP_ACCESS,
});

const vkGroup = new VK({
    token: process.env.GROUP_ACCESS,
});

export { vkApp, vkGroup };