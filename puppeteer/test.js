const puppeteer = require('puppeteer');
const assert = require('assert');
const path = require('path');

describe('Puppeteer Repro', function () {
  let browser;
  let page;

  before(async function () {
    browser = await puppeteer.launch({ headless: false, slowMo: 200 });
    page = await browser.newPage();
  });

  after(async function () {
    if (browser) {
      await browser.close();
    }
  });

  it('ISSUE REPRODUCTION: Select element interaction with keys', async function () {
    const fileUrl = 'file://' + path.resolve(__dirname, 'repro.html');
    await page.goto(fileUrl);

    const select = await page.$('#select_field');
    await select.click();
    
    // Press ArrowDown twice and then Enter
    await page.keyboard.press('ArrowDown');
    await page.keyboard.press('ArrowDown');
    await page.keyboard.press('Enter');

    // assert value is 3 (Option 3)
    const value = await page.evaluate(el => el.value, select);
    assert.strictEqual(value, '3');
  });
});
