import './bootstrap.js';
import './commands.js';
import { CronJob } from 'cron';
import { run } from './findPostTask.js';

export let enabled = true;

const job = CronJob.from({
    cronTime: '0 */1 * * * *',
    onTick: run,
    start: true,
    timeZone: 'Europe/Moscow'
});

export function setEnabled(enabled) {
    this.enabled = enabled;
}