import puppeteer from 'puppeteer';

async function test() {
    const p = process.argv[2] ?? process.env.PUPPETEER_EXECUTABLE_PATH;
    console.log(`Testing with ${p}`)
    const browser = await puppeteer.launch({
        executablePath: p,
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