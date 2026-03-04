#!/bin/bash

npm i puppeteer@latest
export PUPPETEER_EXECUTABLE_PATH=$(npx puppeteer browsers install chrome@146.0.7680.31 --format "{{path}}")
node test.mjs