import puppeteer from 'puppeteer';

async function test() {
    console.log(`Testing with ${process.env.PUPPETEER_EXECUTABLE_PATH}`)
    const browser = await puppeteer.launch({
        executablePath: process.env.PUPPETEER_EXECUTABLE_PATH,
        dumpio: true,
        ignoreDefaultArgs: ['--disable-crash-reporter', '--disable-breakpad']
    });

    const context = await browser.createBrowserContext();

    await context.newPage();

    await browser.close();
}

for (let i = 0; i < 50; i++) {
    await test();
}